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
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;

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
    private ArFragment arFragment;

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

                            } catch (InterruptedException | ExecutionException e) {
                                e.printStackTrace();
                            }
                            return null;
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

        if (locationScene == null) {
            locationScene = new LocationScene(this, arFragment.getArSceneView());
            locationScene.mLocationMarkers.add(
                    new LocationMarker(
                            5.694220,
                            58.937933,
                            getAndy()));
            Toast.makeText(this, "Location Marker created", Toast.LENGTH_SHORT).show();
        }

        String debugText = String.format("Our pos %s\n" +
                        "No of nodes: %d\n",
                arFragment.getArSceneView().getScene().getCamera().getLocalPosition(),
                arFragment.getArSceneView().getScene().getChildren().size());
        textView.setText(debugText);

        if (locationScene != null) {
            locationScene.processFrame(frame);

        }
    }
}