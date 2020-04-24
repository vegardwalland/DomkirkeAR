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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

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
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.FootprintSelectionVisualizer;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.ar.sceneform.ux.TransformationSystem;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import eu.wallhack.gamlestavangerar.common.PermissionHelper;
import eu.wallhack.gamlestavangerar.common.RealWorldLocation;
import eu.wallhack.gamlestavangerar.listeners.LocationListener;
import uk.co.appoly.arcorelocation.LocationMarker;
import uk.co.appoly.arcorelocation.LocationScene;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    // The renderable used for the nodes in AR
    private ViewRenderable nodeLayoutRenderable;

    // The layout elements
    private TextView textView;
    private ImageButton closeOverlayBtn;
    private ConstraintLayout outerConstraintLayout;
    private TextView titleText;
    private TextView contentText;
    private ImageView contentImage;

    private Button nullProcessCountButton;
    private Button miscButton;

    // Alert dialog to show when permissions has been denied
    private AlertDialog noPermissionAlert;

    // The system Location Manager
    private LocationManager locationManager;
    private LocationListener gpsListener;

    // ARCore and ARCoreLocation
    private LocationScene locationScene;
    private ArSceneView arSceneView;
    private Session session;

    // Inside how many meters around the user the nodes should be rendered. Set to -1 to set to max
    private int ONLY_RENDER_NODES_WITHIN = -1;

    // How many meters the user can move before a forced node rerendering happens. Set to -1 to disable.
    private int FORCE_UPDATE_NODES_AFTER_METERS = 20;
    private double previousLongitude;
    private double previousLatitude;
    // Are location nodes to be created
    private boolean locationNodesCreated = false;

    private ArrayList<RealWorldLocation> realWorldLocationArray = new ArrayList<>();

    //Debug text variables
    Location location;
    private int timesProcessed = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get the different View elements and assign them to global variables
        assignViews();

        textView.setVisibility(View.GONE);

        // TODO REMOVE TESTING BUTTONS
        nullProcessCountButton = findViewById(R.id.nullProcessCount);
        nullProcessCountButton.setVisibility(View.GONE);

        nullProcessCountButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                timesProcessed = 0;
            }
        });

        miscButton = findViewById(R.id.miscButton);
        miscButton.setVisibility(View.GONE);
        miscButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {

                locationScene.clearMarkers();
                locationNodesCreated = false;

                }
        });

        // Set up GPS
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        gpsListener = new LocationListener();

        //Configure button to close popup window and make popup window close when pressed outside of it.
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

        RealWorldLocation locPoint1 = new RealWorldLocation("Eskilds hus", "Huset",58.946198, 5.699256);
        RealWorldLocation locPoint2 = new RealWorldLocation("Sattelitten barnehage", "Description", 58.946662, 5.700832);
        RealWorldLocation locPoint3 = new RealWorldLocation("Mobiltårnet", "Description",58.954914, 5.699814);
        RealWorldLocation locPoint4 = new RealWorldLocation("Plutoveien 1", "Description",58.946775, 5.704626);
        /*
        RealWorldLocation locPoint5 = new RealWorldLocation("Solfagerstien 24", "Description",58.946411, 5.699221);
        RealWorldLocation locPoint6 = new RealWorldLocation("Solfagerstien 20", "Description",58.946407, 5.698969);
        RealWorldLocation locPoint7 = new RealWorldLocation("Solfagerstien 16", "Description",58.946396, 5.698695);
        RealWorldLocation locPoint8 = new RealWorldLocation("Solfagerstien 12", "Description",58.946377, 5.698466);
        RealWorldLocation locPoint9 = new RealWorldLocation("Solfagerstien 17", "Description",58.946203, 5.698883);
        RealWorldLocation locPoint10 = new RealWorldLocation("Solfagerstien 13", "Description",58.946181, 5.698612);
        RealWorldLocation locPoint11= new RealWorldLocation("Solfagerstien 9", "Description",58.946173, 5.698329);
        RealWorldLocation locPoint12 = new RealWorldLocation("Jupiterveien 10", "Description",58.946670, 5.704123);
        RealWorldLocation locPoint13 = new RealWorldLocation("Jupiterveien 12", "Description",58.946563, 5.703635);
        RealWorldLocation locPoint14 = new RealWorldLocation("Jupiterveien 14", "Description",58.946385, 5.703243);
        RealWorldLocation locPoint15= new RealWorldLocation("Plutoveien 50", "Description",58.946317, 5.702717);
*/

        realWorldLocationArray.add(locPoint1);
        realWorldLocationArray.add(locPoint2);
        realWorldLocationArray.add(locPoint3);
        realWorldLocationArray.add(locPoint4);
        /*
        realWorldLocationArray.add(locPoint5);
        realWorldLocationArray.add(locPoint6);
        realWorldLocationArray.add(locPoint7);
        realWorldLocationArray.add(locPoint8);
        realWorldLocationArray.add(locPoint9);
        realWorldLocationArray.add(locPoint10);
        realWorldLocationArray.add(locPoint11);
        realWorldLocationArray.add(locPoint12);
        realWorldLocationArray.add(locPoint13);
        realWorldLocationArray.add(locPoint14);
        realWorldLocationArray.add(locPoint15);

         */

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

        //Setup everything if permissions are granted
        if(PermissionHelper.getCameraPermission(this) && PermissionHelper.getGPSPermission(this)) {

            if(arSceneView == null) {
                setupArSceneView();
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

            arSceneView.getScene().addOnUpdateListener(this::onUpdateFrame);
        }
        // If the app has no permission to access GPS or camera, none of the processes needed can be initialised.
        // The app will then just show an AlertDialog which informs the user that these permissions are needed
    }



    @SuppressLint("MissingPermission")
    private void onUpdateFrame(FrameTime frameTime) {

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

            if (marker.node.isActive()) {
                marker.node.setLocalPosition(Vector3.zero());
                markerNodePosition = marker.node.getLocalPosition();
            }
        }

        @SuppressLint("DefaultLocale") String debugText = String.format("Our pos %s\n" +
                        "No of nodes: %d\n" +
                        "Marker latitude: %f\n" +
                        "Marker longitude: %f\n" +
                        "Number of markers: %d\n" +
                        "Distance in AR: %f\n" +
                        "Anchor position: %s\n" +
                        "Node position: %s\n" +
                        "timesProcessed: %d\n",
                arSceneView.getScene().getCamera().getLocalPosition(),
                arSceneView.getScene().getChildren().size(),
                latitude,
                longitude,
                noOfMarkers,
                distanceInAR,
                anchorNodePosition,
                markerNodePosition,
                timesProcessed);
        textView.setText(debugText);

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
                    if (location != null && location.hasAccuracy()) {
                        if(checkMovement(FORCE_UPDATE_NODES_AFTER_METERS)) {
                            locationScene.clearMarkers();
                            locationNodesCreated = false;
                        }
                    }

                    // Create the nodes that are to be rendered in the app if the renderable has been created
                    // and if they have not been created before
                    if (nodeLayoutRenderable != null && !locationNodesCreated) {
                        createLocationNodes(realWorldLocationArray);
                    }


                    if (locationScene != null) {
                        locationScene.processFrame(frame);

                        // ArCore Location creates a new node each time it updates the location of its locationMarkers
                        // By checking if the children of the sceneview scene is bigger than the amount of locationMarkers plus camera and sun node
                        // We can delete the surplus unused nodes
                        if (arSceneView.getScene().getChildren().size() != locationScene.mLocationMarkers.size()+2) {
                            deleteSurplusNodes();
                        }
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
        if(meters == -1) return false;
        meters = meters * 0.000001; // Turn meters into meters in latitude or longitude.
        if(getMovement(meters, location.getLatitude(), previousLatitude)
                || getMovement(meters, location.getLongitude(), previousLongitude)) {
            previousLongitude = location.getLongitude();
            previousLatitude = location.getLatitude();
            return true;
        }
        return false;
    }

        //Return true if the device has moved more than the supplied meters in latitude or longitude
    private boolean getMovement(double meters, double currentPos, double previousPos) {
        if(currentPos > (previousPos + meters) || currentPos < (previousPos - meters)) {
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

        noPermissionAlert.setButton(DialogInterface.BUTTON_NEUTRAL, "Change Permissions", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        });
        noPermissionAlert.setButton(DialogInterface.BUTTON_NEGATIVE, "Exit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                finish();
            }
        });
    }

    private void setupInformationOverlayFunctionality() {
        //Configure button to close popup window
        closeOverlayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                outerConstraintLayout.setVisibility(View.GONE);
            }
        });

        //Make popup window close when pressed outside of it.
        outerConstraintLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                outerConstraintLayout.setVisibility(View.GONE);
            }
        });
    }

    private Node getLocationNode(Renderable renderable) {
        TransformationSystem transformationSystem=new TransformationSystem(getResources().getDisplayMetrics(),new FootprintSelectionVisualizer());
        TransformableNode base = new TransformableNode(transformationSystem);
        base.getScaleController().setMaxScale(40.99f);
        base.getScaleController().setMinScale(08.98f);
        base.setRenderable(renderable);
        return base;
    }

    private void createLocationNodes(ArrayList<RealWorldLocation> locations) {
            for(RealWorldLocation location: locations) {
                Node locationNode = getLocationNode(nodeLayoutRenderable);
                locationNode.setOnTapListener((v, event) -> {
                    if(outerConstraintLayout.getVisibility()== View.GONE) {
                        outerConstraintLayout.setVisibility(View.VISIBLE);
                    }
                    titleText.setText(location.getName());
                    contentText.setText(location.getDescription());
                });

                LocationMarker marker = new LocationMarker(
                        location.getLon(), location.getLat(),
                        locationNode);
                if(ONLY_RENDER_NODES_WITHIN != -1) {
                    marker.setOnlyRenderWhenWithin(ONLY_RENDER_NODES_WITHIN);
                }
                marker.setScalingMode(LocationMarker.ScalingMode.GRADUAL_TO_MAX_RENDER_DISTANCE);

                // Adding a location marker of a 2D model
                locationScene.mLocationMarkers.add(marker);
            }
            locationNodesCreated = true;
    }

    private void assignViews() {
        textView = findViewById(R.id.topView);
        closeOverlayBtn = findViewById(R.id.closeOverlayBtn);
        outerConstraintLayout = findViewById(R.id.outerConstraintLayout);
        titleText = findViewById(R.id.titleTextView);
        contentText = findViewById(R.id.contentTextView);
        contentImage = findViewById(R.id.contentImageView);
    }
}
