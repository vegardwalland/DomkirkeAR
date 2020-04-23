package eu.wallhack.domkirkear;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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
    private TextView textView;

    // The system Location Manager
    private LocationManager locationManager;
    private LocationListener gpsListener;
    private LocationScene locationScene;
    private ArSceneView arSceneView;
    private Session session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.topView);

        // Make sure we have a ArSceneView
        while (arFragment.getArSceneView() == null) ;

        // Set up GPS
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        gpsListener = new LocationListener();

        arSceneView = findViewById(R.id.ar_scene_view);
        createSession();

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
            arSceneView.resume();
        } catch (CameraNotAvailableException ex) {
            // TODO show message that camera not available
            finish();
            return;
        }

        if (arSceneView.getSession() == null) {
            createSession();
        }

        // Check location permission
        while (!PermissionHelper.getGPSPermission(this)) {
            PermissionHelper.askGPSPermission(this);
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

    private void onUpdateFrame(FrameTime frameTime) {
        Frame frame = arFragment.getArSceneView().getArFrame();

        Collection<AugmentedImage> augmentedImages = frame.getUpdatedTrackables(AugmentedImage.class);

        for (AugmentedImage augmentedImage : augmentedImages) {
            if (augmentedImage.getTrackingState() == TrackingState.TRACKING) {

                if (augmentedImage.getName().contains("qrCode")) {
                    // here we got that image has been detected
                    // we will render our 3D asset in center of detected image
                    renderObject(arFragment,
                            augmentedImage.createAnchor(augmentedImage.getCenterPose()),
                            R.raw.andy);
                }
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

    private void renderObject(ArFragment fragment, Anchor anchor, int model) {
        ModelRenderable.builder()
                .setSource(this, model)
                .build()
                .thenAccept(renderable -> addNodeToScene(fragment, anchor, renderable))
                .exceptionally((throwable -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(throwable.getMessage())
                            .setTitle("Error!");
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    return null;
                }));

    }

    private void addNodeToScene(ArFragment fragment, Anchor anchor, ModelRenderable renderable) {
        AnchorNode anchorNode = new AnchorNode(anchor);
        TransformableNode node = new TransformableNode(fragment.getTransformationSystem());
        node.setRenderable(renderable);
        node.setParent(anchorNode);
        fragment.getArSceneView().getScene().addChild(anchorNode);
        node.select();
    }

}
