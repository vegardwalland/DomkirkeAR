package eu.wallhack.domkirkear;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.ar.core.Anchor;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.SceneView;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.Material;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import eu.wallhack.domkirkear.common.PermissionHelper;
import eu.wallhack.domkirkear.common.imageTracking;
import eu.wallhack.domkirkear.listeners.LocationListener;
import uk.co.appoly.arcorelocation.LocationMarker;
import uk.co.appoly.arcorelocation.LocationScene;
import uk.co.appoly.arcorelocation.rendering.LocationNode;
import uk.co.appoly.arcorelocation.rendering.LocationNodeRender;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private ModelRenderable andyRenderable;
    private ArFragment arFragment;
    private Session session;
    private TextView textView;

    // The system Location Manager
    private LocationManager locationManager;
    private LocationListener gpsListener;
    private LocationScene locationScene;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        textView = findViewById(R.id.topView);

        // Set up GPS
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        gpsListener = new LocationListener();

        // Make sure we have a ArSceneView
        while (arFragment.getArSceneView() == null);

        // Explicitly create session
        try {
            arFragment.getArSceneView().setupSession(new Session(MainActivity.this));
        } catch (UnavailableArcoreNotInstalledException e) {
            e.printStackTrace();
        } catch (UnavailableApkTooOldException e) {
            e.printStackTrace();
        } catch (UnavailableSdkTooOldException e) {
            e.printStackTrace();
        } catch (UnavailableDeviceNotCompatibleException e) {
            e.printStackTrace();
        }

        session = arFragment.getArSceneView().getSession();

        setupAutoFocus();


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

                                //Make andyRenderable red
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

        arFragment.
                getArSceneView().
                getScene().
                addOnUpdateListener(frameTime -> {
                    if (locationScene == null) {
                        // If our locationScene object hasn't been setup yet, this is a good time to do it
                        // We know that here, the AR components have been initiated.
                        locationScene = new LocationScene(this, arFragment.getArSceneView());


                        // Adding a simple location marker of a 3D model
                        locationScene.mLocationMarkers.add(
                                new LocationMarker(
                                        5.691703, 58.938292,
                                        getAndy()));
                    }

                        Frame frame = arFragment.getArSceneView().getArFrame();
                        if (frame == null) {
                            return;
                        }

                        if (frame.getCamera().getTrackingState() != TrackingState.TRACKING) {
                            return;
                        }

                        if (locationScene != null) {
                            locationScene.processFrame(frame);
                        }
                });
    }

    private Node getAndy() {
        Node base = new Node();
        base.setRenderable(andyRenderable);
        Context c = this;
        base.setOnTapListener((v, event) -> {
            Toast.makeText(
                    c, "Andy touched.", Toast.LENGTH_LONG)
                    .show();
        });
        return base;
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onResume() {
        super.onResume();

        if (locationScene != null) {
            locationScene.resume();
        }

        try {
            arFragment.getArSceneView().resume();
        } catch (CameraNotAvailableException ex) {
            // TODO show message that camera not available
            finish();
            return;
        }

        if(arFragment.getArSceneView().getSession() == null) {

        }

        // Check location permission
        while (!PermissionHelper.getGPSPermission(this)) {
            PermissionHelper.askGPSPermission(this);
        }

        // Request location updates from GPS
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, gpsListener);
        // Request location updates from network provider
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, gpsListener);

        // TODO Now that we're using Sceneform, can tracking images be set up through Sceneform now?
        // Configure ARCore session to track images
        Config config = arFragment.getArSceneView().getSession().getConfig();
        AugmentedImageDatabase imageDatabase = imageTracking.createImageDatabase(getApplicationContext(), arFragment.getArSceneView().getSession());
        if (imageDatabase != null) {
            config.setAugmentedImageDatabase(imageDatabase);
        }
        arFragment.getArSceneView().getSession().configure(config);

        arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdateFrame);
    }

    private void setupAutoFocus() {
        Config config = new Config(arFragment.getArSceneView().getSession());

        // Check if the configuration is set to fixed
        if (config.getFocusMode() == Config.FocusMode.FIXED) {
            config.setFocusMode(Config.FocusMode.AUTO);
        }

        // Sceneform requires that the ARCore session is configured to the UpdateMode LATEST_CAMERA_IMAGE.
        config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);

        arFragment.getArSceneView().getSession().configure(config);
    }

    @SuppressLint("MissingPermission")
    private void onUpdateFrame(FrameTime frameTime) {


        Frame frame = arFragment.getArSceneView().getArFrame();

        double latitude = 0;
        double longitude = 0;
        int noOfMarkers = 0;
        double distanceInAR = 0;
        Vector3 anchorNodePosition = Vector3.zero();
        Vector3 markerNodePosition = Vector3.zero();
        for(LocationMarker marker: locationScene.mLocationMarkers){
            noOfMarkers = locationScene.mLocationMarkers.size();
            latitude = marker.latitude;
            longitude = marker.longitude;
            if(marker.anchorNode != null) {
                marker.anchorNode.setLocalPosition(Vector3.zero());
                anchorNodePosition = marker.anchorNode.getWorldPosition();
                distanceInAR = marker.anchorNode.getDistanceInAR();
            }
            if(marker.node.isActive()){
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
                    arFragment.getArSceneView().getScene().getCamera().getLocalPosition(),
                    arFragment.getArSceneView().getScene().getChildren().size(),
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

        arFragment.getArSceneView().pause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        arFragment.getArSceneView().destroy();
    }

}