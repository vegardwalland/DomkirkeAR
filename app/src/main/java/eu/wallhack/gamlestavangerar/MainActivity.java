package eu.wallhack.gamlestavangerar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.FootprintSelectionVisualizer;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.ar.sceneform.ux.TransformationSystem;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import eu.wallhack.gamlestavangerar.common.PermissionHelper;
import eu.wallhack.gamlestavangerar.items.Item;
import eu.wallhack.gamlestavangerar.items.ItemFetcher;
import eu.wallhack.gamlestavangerar.listeners.LocationListener;
import uk.co.appoly.arcorelocation.LocationMarker;
import uk.co.appoly.arcorelocation.LocationScene;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String BASE_URL_ITEM_FETCHER = BuildConfig.BARE_URL_ITEM_FETCHER;

    // The renderable used for the nodes in AR
    private ViewRenderable nodeLayoutRenderable;

    // The layout elements
    private ImageButton privacyInfoBtn;
    private ImageButton closeOverlayBtn;
    private ConstraintLayout outerConstraintLayout;
    private TextView titleText;
    private TextView contentText;
    private ImageView contentImage;

    // Alert dialog to show when permissions has been denied
    private AlertDialog noPermissionAlert;

    // The system Location Manager
    private LocationManager locationManager;
    private LocationListener gpsListener;

    // ARCore and ARCoreLocation
    private LocationScene locationScene;
    private ArSceneView arSceneView;
    private Session session;

    // Set to true ensures requestInstall() triggers installation of Google Play Services for AR if necessary.
    private boolean mUserRequestedInstall = true;

    // Inside how many meters around the user the nodes should be rendered. Set to -1 to set to max
    private int ONLY_RENDER_NODES_WITHIN = -1;

    // How many meters the user can move before a forced node re-rendering happens. Set to -1 to disable.
    private int FORCE_UPDATE_NODES_AFTER_METERS = 20;
    private double previousLongitude;
    private double previousLatitude;
    // Have location nodes been created?
    private boolean locationNodesCreated = false;

    private Collection<Item> locations;

    // Debug text variables
    Location location;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get the different View elements and assign them to global variables
        assignViews();

        // Set outerConstraintLayout to visible to show usage information about the app
        outerConstraintLayout.setVisibility(View.VISIBLE);
        contentText.setMovementMethod(LinkMovementMethod.getInstance());

        // Set up GPS
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        gpsListener = new LocationListener();

        // Configure button to close popup window and make popup window close when pressed outside of it.
        setupInformationOverlayFunctionality();

        // Make a alert dialog to display if the user has denied permissions before
        configurePermissionAlert();

        // Build a renderable from a 2D View.
        CompletableFuture<ViewRenderable> nodeLayout =
                ViewRenderable.builder()
                        .setView(this, R.layout.node_layout)
                        .build();

        CompletableFuture.allOf(nodeLayout)
                .handle(
                        (notUsed, throwable) ->
                        {
                            if (throwable != null) {
                                Log.e(TAG, "Unable to load Renderable.", throwable);
                                return null;
                            }

                            try {
                                nodeLayoutRenderable = nodeLayout.get();
                                View renderableView = nodeLayoutRenderable.getView();
                                ImageView nodeImage = renderableView.findViewById(R.id.nodeImageView);
                                nodeImage.setImageResource(R.mipmap.launcher_icon);

                            } catch (InterruptedException | ExecutionException e) {
                                e.printStackTrace();
                            }
                            return null;
                        });
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onResume() {
        super.onResume();

        // Set previous longitude and latitude to 0 to force a node refresh
        previousLongitude = 0;
        previousLatitude = 0;

        // Check camera & location permission
        PermissionHelper.askCameraPermission(this, noPermissionAlert);
        PermissionHelper.askGPSPermission(this, noPermissionAlert);

        // Setup everything if permissions are granted
        if (PermissionHelper.getCameraPermission(this) && PermissionHelper.getGPSPermission(this)) {

            // Make sure Google Play Services for AR is installed and up to date.
            try {
                if (arSceneView == null || arSceneView.getSession() == null) {
                    switch (ArCoreApk.getInstance().requestInstall(this, mUserRequestedInstall)) {
                        case INSTALLED:
                            // Success, continue onResume()
                            break;
                        case INSTALL_REQUESTED:
                            // Ensures next invocation of requestInstall() will either return
                            // INSTALLED or throw an exception.
                            mUserRequestedInstall = false;
                            return;
                    }
                }
            } catch (UnavailableUserDeclinedInstallationException | UnavailableDeviceNotCompatibleException e) {
                // Display an appropriate message to the user and return gracefully.
                Toast.makeText(this, "Installation or update of Google Play Services for AR could not be completed. " +
                        "Make sure you are using a device that supports ARCore.", Toast.LENGTH_LONG)
                        .show();
                return;
            }

            // TODO Move this to onCreate and check for internet access
            RequestQueue queue = Volley.newRequestQueue(this);
            ItemFetcher itemFetcher = new ItemFetcher(BASE_URL_ITEM_FETCHER, queue);
            itemFetcher.getItems().thenAccept(items -> locations = items);

            if (arSceneView == null) {
                setupArSceneView();
            }

            if (arSceneView != null) {
                try {
                    arSceneView.resume();
                } catch (CameraNotAvailableException e) {
                    // TODO show message that camera not available
                    e.printStackTrace();
                    return;
                }
            }

            if (locationScene != null) {
                locationScene.resume();
            }

            if (arSceneView.getSession() == null) {
                createSession();
            }

            // Request location updates from GPS
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, gpsListener);
            // Request location updates from network provider
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, gpsListener);

            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
        // If the app has no permission to access GPS or camera, none of the processes needed can be initialised.
        // The app will then just show an AlertDialog which informs the user that these permissions are needed
    }

    @Override
    public void onPause() {
        super.onPause();
        if (locationScene != null) {
            locationScene.pause();
        }
        if (arSceneView != null) {
            arSceneView.pause();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        noPermissionAlert.dismiss();
        if (arSceneView != null) {
            arSceneView.destroy();
        }
    }

    private void setupArSceneView() {
        arSceneView = findViewById(R.id.ar_scene_view);
        createSession();
        setupAutoFocus();

        arSceneView.
                getScene().
                addOnUpdateListener(frameTime -> {
                    if (locationScene == null) {
                        // If our locationScene object hasn't been setup yet, this is a good time to do it
                        // We know that here, the AR components have been initiated.
                        locationScene = new LocationScene(this, arSceneView);
                    }

                    Frame frame = arSceneView.getArFrame();
                    if (frame == null) {
                        return;
                    }

                    if (frame.getCamera().getTrackingState() != TrackingState.TRACKING) {
                        return;
                    }

                    // Check if a forced node update should occur
                    // TODO this could might be done with height instead of location
                    if (location != null && location.hasAccuracy()) {
                        if (checkMovement(FORCE_UPDATE_NODES_AFTER_METERS)) {
                            locationScene.clearMarkers();
                            locationNodesCreated = false;
                        }
                    }

                    // Create the nodes that are to be rendered in the app if the renderable has been created
                    // and if they have not been created before
                    if (nodeLayoutRenderable != null && !locationNodesCreated) {
                        createLocationNodes(locations);
                    }

                    // ArCore Location creates a new node each time it updates the location of its locationMarkers
                    // By checking if the children of the sceneview scene is bigger than the amount of locationMarkers plus camera and sun node
                    // We can delete the surplus unused nodes
                    if (arSceneView.getScene().getChildren().size() != locationScene.mLocationMarkers.size() + 2) {
                        deleteSurplusNodes();
                    }

                    if (locationScene != null) {
                        locationScene.processFrame(frame);
                    }
                });
    }

    private void deleteSurplusNodes() {
        for (int i = arSceneView.getScene().getChildren().size() - locationScene.mLocationMarkers.size() - 1; i > 1; i--) {
            Node node = arSceneView.getScene().getChildren().get(i);
            arSceneView.getScene().removeChild(node);
            // TODO Can check if it has anchor instead of amount of nodes. Worse runtime
        }
    }

    private boolean checkMovement(double meters) {
        if (meters == -1) return false;
        meters = meters * 0.000001; // Turn meters into meters in latitude or longitude.
        if (hasDeviceMoved(meters, location.getLatitude(), previousLatitude)
                || hasDeviceMoved(meters, location.getLongitude(), previousLongitude)) {
            previousLongitude = location.getLongitude();
            previousLatitude = location.getLatitude();
            return true;
        }
        return false;
    }

    // Return true if the device has moved more than the supplied meters in latitude or longitude
    private boolean hasDeviceMoved(double meters, double currentPos, double previousPos) {
        if (currentPos > (previousPos + meters) || currentPos < (previousPos - meters)) {
            return true;
        }
        return false;
    }

    private void createSession() {
        try {
            session = new Session(this);
            arSceneView.setupSession(session);
        } catch (UnavailableArcoreNotInstalledException | UnavailableDeviceNotCompatibleException | UnavailableSdkTooOldException | UnavailableApkTooOldException e) {
            e.printStackTrace();
            finish();
        }
    }

    private void setupAutoFocus() {
        Config config = new Config(session);

        // Check if the configuration is set to fixed
        if (config.getFocusMode() == Config.FocusMode.FIXED) {
            config.setFocusMode(Config.FocusMode.AUTO);
        }

        // Sceneform requires that the ARCore session is configured to the UpdateMode LATEST_CAMERA_IMAGE.
        config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);

        session.configure(config);
    }

    private void configurePermissionAlert() {
        noPermissionAlert = PermissionHelper.setupNoPermissionAlert(this);

        noPermissionAlert.setButton(DialogInterface.BUTTON_NEUTRAL, "Change Permissions", (dialog, which) -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        });
        noPermissionAlert.setButton(DialogInterface.BUTTON_NEGATIVE, "Exit", (dialog, which) -> {
            dialog.cancel();
            finish();
        });
    }

    // TODO Fix overlapping node raytracing hitting both nodes
    private void setupInformationOverlayFunctionality() {

        privacyInfoBtn.setOnClickListener(v -> {
            outerConstraintLayout.setVisibility(View.VISIBLE);
            contentText.setText(R.string.privacy_information);
            contentText.setMovementMethod(LinkMovementMethod.getInstance());
        });

        // Configure button to close popup window
        closeOverlayBtn.setOnClickListener(v -> outerConstraintLayout.setVisibility(View.GONE));

        // Make popup window close when pressed outside of it.
        outerConstraintLayout.setOnClickListener(v -> outerConstraintLayout.setVisibility(View.GONE));
    }

    private Node getLocationNode(Renderable renderable) {
        TransformationSystem transformationSystem = new TransformationSystem(getResources().getDisplayMetrics(), new FootprintSelectionVisualizer());
        TransformableNode base = new TransformableNode(transformationSystem);
        base.getScaleController().setMaxScale(30.0f);
        base.getScaleController().setMinScale(03.0f);
        base.setRenderable(renderable);
        return base;
    }

    private void createLocationNodes(Collection<Item> locations) {
        // TODO Possibly unnecessary null check
        if (locations == null) return;

        for (Item location : locations) {
            Node locationNode = getLocationNode(nodeLayoutRenderable);
            locationNode.setOnTapListener((v, event) -> {
                if (outerConstraintLayout.getVisibility() == View.GONE) {
                    outerConstraintLayout.setVisibility(View.VISIBLE);
                }
                titleText.setText(location.getName());
                contentText.setText(location.getDescription());
                contentText.setMovementMethod(null);
            });

            LocationMarker marker = new LocationMarker(
                    location.getLongitude(), location.getLatitude(),
                    locationNode);
            if (ONLY_RENDER_NODES_WITHIN != -1) {
                marker.setOnlyRenderWhenWithin(ONLY_RENDER_NODES_WITHIN);
            }
            marker.setScalingMode(LocationMarker.ScalingMode.GRADUAL_TO_MAX_RENDER_DISTANCE);

            // Adding a location marker of a 2D model
            locationScene.shouldOffsetOverlapping();
            locationScene.mLocationMarkers.add(marker);
        }
        locationNodesCreated = true;
    }

    private void assignViews() {
        privacyInfoBtn = findViewById(R.id.privacyInfoBtn);
        closeOverlayBtn = findViewById(R.id.closeOverlayBtn);
        outerConstraintLayout = findViewById(R.id.outerConstraintLayout);
        titleText = findViewById(R.id.titleTextView);
        contentText = findViewById(R.id.contentTextView);
        contentImage = findViewById(R.id.contentImageView);
    }
}
