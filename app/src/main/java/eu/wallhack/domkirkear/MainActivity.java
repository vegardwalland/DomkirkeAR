package eu.wallhack.domkirkear;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.Material;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import eu.wallhack.domkirkear.common.PermissionHelper;
import eu.wallhack.domkirkear.common.imageTracking;
import eu.wallhack.domkirkear.listeners.LocationListener;
import uk.co.appoly.arcorelocation.LocationMarker;
import uk.co.appoly.arcorelocation.LocationScene;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private ModelRenderable andyRenderable;

    // The layout elements
    private TextView textView;
    private ImageButton closeOverlayBtn;
    private ConstraintLayout outerConstraintLayout;
    private TextView titleText;
    private TextView contentText;
    private ImageView contentImage;

    private AlertDialog noPermissionAlert;

    // The system Location Manager
    private LocationManager locationManager;
    private LocationListener gpsListener;
    private LocationScene locationScene;
    private ArSceneView arSceneView;
    private Session session;


    //Debug text variables
    Boolean createStartMarker = true;
    Location location;
    private boolean firstAndyCreated = false;
    private Random random;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get the different View elements
        textView = findViewById(R.id.topView);
        closeOverlayBtn = findViewById(R.id.closeOverlayBtn);
        outerConstraintLayout = findViewById(R.id.outerConstraintLayout);
        titleText = findViewById(R.id.titleTextView);
        contentText = findViewById(R.id.contentTextView);
        contentImage = findViewById(R.id.contentImageView);


        random = new Random();

        // Set up GPS
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        gpsListener = new LocationListener();

        //Configure button
        closeOverlayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                outerConstraintLayout.setVisibility(arSceneView.GONE);
            }
        });

        //Make popup window close when pressed outside of it.
        outerConstraintLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                outerConstraintLayout.setVisibility(arSceneView.GONE);
            }
        });

        // Make a alert dialog to display if the user has denied permissions before
        configurePermissionAlert();




        CompletableFuture<ModelRenderable> andy = ModelRenderable.builder()
                .setSource(this, R.raw.andy)
                .build();


        CompletableFuture.allOf(andy)
                .handle(
                        (notUsed, throwable) ->
                        {
                            if (throwable != null) {
                                Log.e(TAG, "Unable to load Renderable.", throwable);
                                return null;
                            }

                            try {
                                andyRenderable = andy.get();

                                // Make andyRenderable red
                                CompletableFuture<Material> redAndyMaterial =
                                        MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.RED));

                                redAndyMaterial.thenAccept(material -> {
                                    andyRenderable.setMaterial(material);
                                });

                            } catch (InterruptedException | ExecutionException e) {
                                e.printStackTrace();
                            }
                            return null;
                        });


    }

    private Node getAndy() {
        return getAndy(andyRenderable);
    }

    private Node getAndy(Renderable renderable) {
        Node base = new Node();
        base.setRenderable(renderable);
        Context c = this;
        base.setOnTapListener((v, event) -> {
            int colorValue = random.nextInt(0xFFFFFF) + 0xFF000000;
            Color color = new Color(colorValue);
            MaterialFactory.makeOpaqueWithColor(this, color).thenAccept(m -> v.getNode().getRenderable().setMaterial(m));
            Toast.makeText(
                    c, "Andy touched. New color is " + Integer.toHexString(colorValue), Toast.LENGTH_LONG)
                    .show();
            if(outerConstraintLayout.getVisibility()==arSceneView.GONE) {
                outerConstraintLayout.setVisibility(arSceneView.VISIBLE);
            }

        });
        return base;
    }


    @SuppressLint("MissingPermission")
    @Override
    protected void onResume() {
        super.onResume();


        // Check camera & location permission
        PermissionHelper.askCameraPermission(this, noPermissionAlert);
        PermissionHelper.askGPSPermission(this, noPermissionAlert);

        if(PermissionHelper.getCameraPermission(this) && PermissionHelper.getGPSPermission(this)) {

            if(arSceneView == null) {
                setupArSceneView();
            }

            if (locationScene != null) {
                locationScene.resume();
            }

            if(arSceneView != null) {
                try {
                    arSceneView.resume();
                } catch (CameraNotAvailableException e) {
                    // TODO show message that camera not available
                    e.printStackTrace();
                    return;
                }
            }

            if (arSceneView.getSession() == null) {
                createSession();
            }

            // Request location updates from GPS
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, gpsListener);
            // Request location updates from network provider
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, gpsListener);

            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            // TODO Now that we're using Sceneform, can tracking images be set up through Sceneform now?
            // Configure ARCore session to track images
            Config config = arSceneView.getSession().getConfig();
            AugmentedImageDatabase imageDatabase = imageTracking.createImageDatabase(getApplicationContext(), session);
            if (imageDatabase != null) {
                config.setAugmentedImageDatabase(imageDatabase);
            }
            arSceneView.getSession().configure(config);

            arSceneView.getScene().addOnUpdateListener(this::onUpdateFrame);
        }
        // If the app has no permission to access GPS or camera, none of the processes needed can be initialised.
        // The app will then just show an AlertDialog which informs the user that these permissions are needed
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

                    if (locationScene != null) {
                        locationScene.processFrame(frame);
                    }

                    if (andyRenderable != null && !firstAndyCreated) {
                        Node firstAndy = getAndy(andyRenderable.makeCopy());
                        MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.BLUE)).thenAccept(firstAndy.getRenderable()::setMaterial);
                        // Adding a simple location marker of a 3D model
                        locationScene.mLocationMarkers.add(
                                new LocationMarker(
                                        5.691703, 58.938292,
                                        firstAndy));
                        firstAndyCreated = true;
                    }
                });
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

        noPermissionAlert.setButton(DialogInterface.BUTTON_NEUTRAL, "Endre tillatelser", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        });
        noPermissionAlert.setButton(DialogInterface.BUTTON_NEGATIVE, "Avslutt", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                finish();
            }
        });
    }


    @SuppressLint("MissingPermission")
    private void onUpdateFrame(FrameTime frameTime) {
        Frame frame = arSceneView.getArFrame();

        double latitude = 0;
        double longitude = 0;
        int noOfMarkers = 0;
        double distanceInAR = 0;
        Vector3 anchorNodePosition = Vector3.zero();
        Vector3 markerNodePosition = Vector3.zero();
        for (LocationMarker marker : locationScene.mLocationMarkers) {
            noOfMarkers = locationScene.mLocationMarkers.size();
            latitude = marker.latitude;
            longitude = marker.longitude;
            if (marker.anchorNode != null) {
                marker.anchorNode.setLocalPosition(Vector3.zero());
                anchorNodePosition = marker.anchorNode.getLocalPosition();
                distanceInAR = marker.anchorNode.getDistanceInAR();
            }
            if (location != null && location.hasAccuracy() && createStartMarker) {
                locationScene.mLocationMarkers.add(
                        new LocationMarker(
                                location.getLongitude(), location.getLatitude(),
                                getAndy()));
                createStartMarker = false;
            }
            if (marker.node.isActive()) {
                marker.node.setLocalPosition(Vector3.zero());
                markerNodePosition = marker.node.getLocalPosition();
            }
        }

        String debugText = String.format("Our pos %s\n" +
                        "No of nodes: %d\n" +
                        "Marker latitude: %f\n" +
                        "Marker longitude: %f\n" +
                        "Number of markers: %d\n" +
                        "Distance in AR: %f\n" +
                        "Anchor position: %s\n" +
                        "Node position: %s\n",
                arSceneView.getScene().getCamera().getLocalPosition(),
                arSceneView.getScene().getChildren().size(),
                latitude,
                longitude,
                noOfMarkers,
                distanceInAR,
                anchorNodePosition,
                markerNodePosition);
        textView.setText(debugText);

        if (locationScene != null) {
            locationScene.processFrame(frame);
        }
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

}
