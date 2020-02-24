package eu.wallhack.domkirkear;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.ar.core.Anchor;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.Material;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import eu.wallhack.domkirkear.common.LocationUtils;
import eu.wallhack.domkirkear.common.PermissionHelper;
import eu.wallhack.domkirkear.common.imageTracking;
import eu.wallhack.domkirkear.listeners.LocationListener;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    // Debug
    private int newAndys = 0;

    Vector3 gpsCoordsOfQrCode = new Vector3(58.937956f, 60f, 5.694259f);
    Vector3 gpsCoordsOfTest = new Vector3(58.937943f, 60f,5.693165f);
    Vector3 offset = null;
    Vector3 testPos = new Vector3();

    private boolean andyCreated = false;

    private ModelRenderable andyRenderable;
    private ArFragment arFragment;

    private TextView textView;

    // The system Location Manager
    private LocationManager locationManager;
    private LocationListener gpsListener;
    private Session session;
    private Anchor test;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        textView = findViewById(R.id.topView);

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

        ModelRenderable.builder()
                .setSource(this, R.raw.andy)
                .build()
                .thenAccept(renderable -> andyRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            Log.e(TAG, "Unable to load Renderable.", throwable);
                            return null;
                        });

        arFragment.setOnTapArPlaneListener(
                (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
                    if (andyRenderable == null) {
                        return;
                    }

                    // Create the Anchor.
                    Anchor anchor = hitResult.createAnchor();
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    anchorNode.setParent(arFragment.getArSceneView().getScene());


                    renderObject(arFragment, anchor, R.raw.andy);
                    // Create the transformable andy and add it to the anchor.
//                    TransformableNode andy = new TransformableNode(arFragment.getTransformationSystem());
//                    andy.setParent(anchorNode);
//                    andy.setRenderable(andyRenderable);
//                    andy.select();
                });
    }


    @SuppressLint("MissingPermission")
    @Override
    protected void onResume() {
        super.onResume();

        // Check location permission
        while (!PermissionHelper.getGPSPermission(this)) {
            PermissionHelper.askGPSPermission(this);
        }

        // Set up GPS
        // TODO Does a new LocationListener need to be created on every onResume, or could it be moved to onCreate?
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        gpsListener = new LocationListener();

        // Request location updates from GPS
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, gpsListener);
        // Request location updates from network provider
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, gpsListener);

        // TODO Now that we're using Sceneform, can tracking images be set up through Sceneform now?
        // Configure ARCore session to track images
        Config config = session.getConfig();
        AugmentedImageDatabase imageDatabase = imageTracking.createImageDatabase(getApplicationContext(), session);
        if (imageDatabase != null) {
            config.setAugmentedImageDatabase(imageDatabase);
        }
        session.configure(config);

        arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdateFrame);
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

    private void onUpdateFrame(FrameTime frameTime){

        Frame frame = arFragment.getArSceneView().getArFrame();

        Collection<AugmentedImage> augmentedImages = frame.getUpdatedTrackables(AugmentedImage.class);

        for (AugmentedImage augmentedImage : augmentedImages){
            String debugText = String.format("Our pos %s" +
                            "New Andys: %d\n" +
                            "Tracking image name: %s\n" +
                            "Tracking state: %s\n" +
                            "CenterPose: %s\n" +
                            "No of nodes: %d\n" +
                            "Second andy pose: %s",
                    arFragment.getArSceneView().getScene().getCamera().getLocalPosition(),
                    newAndys,
                    augmentedImage.getName(),
                    augmentedImage.getTrackingState(),
                    augmentedImage.getCenterPose(),
                    arFragment.getArSceneView().getScene().getChildren().size(),
                    ((test != null) ?test.getPose() : ""));
            textView.setText(debugText);
            if (augmentedImage.getTrackingState() == TrackingState.TRACKING){

                if (augmentedImage.getName().contains("qrCode")){
                    // here we got that image has been detected
                    // we will render our 3D asset in center of detected image
                    if (!andyCreated) {
                        newAndys++;

                        Anchor qrAnchor = augmentedImage.createAnchor(augmentedImage.getCenterPose());

                        renderObject(arFragment,
                                qrAnchor,
                                R.raw.andy);
                        offset = arFragment.getArSceneView().getScene().getChildren().get(0).getLocalPosition();

                        testPos = LocationUtils.createLocalLocation(gpsCoordsOfTest, gpsCoordsOfQrCode, offset);
                        test = arFragment.getArSceneView().getSession().createAnchor(Pose.makeTranslation(testPos.x, testPos.y, testPos.z));
                        renderObject(arFragment, test, R.raw.andy);
                        andyCreated = true;
                    }
                }
            }
        }

    }

    private void renderObject(ArFragment fragment, Anchor anchor, int model){
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

    private void addNodeToScene(ArFragment fragment, Anchor anchor, ModelRenderable renderable){
        AnchorNode anchorNode = new AnchorNode(anchor);
        TransformableNode node = new TransformableNode(fragment.getTransformationSystem());
        node.setRenderable(renderable);
        addHighlightToNode(node);
        node.getScaleController().setMaxScale(15f);
        node.setLocalScale(new Vector3(5f, 15f, 5f));
        node.setParent(anchorNode);
        fragment.getArSceneView().getScene().addChild(anchorNode);
        node.select();
    }

    private void addHighlightToNode(Node node) {
        CompletableFuture<Material> materialCompletableFuture =
                MaterialFactory.makeOpaqueWithColor(this, new Color(255, 0, 0));

        materialCompletableFuture.thenAccept(material -> {
            Renderable r2 = node.getRenderable().makeCopy();
            r2.setMaterial(material);
            node.setRenderable(r2);
        });
    }

}