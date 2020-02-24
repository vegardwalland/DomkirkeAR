package eu.wallhack.domkirkear;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.wikitude.architect.ArchitectView;
import com.wikitude.common.CallStatus;
import com.wikitude.common.devicesupport.Feature;

import java.util.EnumSet;

import eu.wallhack.domkirkear.common.PermissionHelper;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private TextView debugText;
    private CallStatus geoStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        debugText = findViewById(R.id.topView);
        String test = "Hello, World\n" + TAG;
        debugText.setText(test);

        EnumSet<Feature> neededFeatures = EnumSet.of(Feature.GEO);
        geoStatus = ArchitectView.isDeviceSupporting(this, neededFeatures);

        acquirePermissions();
    }

    private void acquirePermissions() {
        if (!PermissionHelper.getCameraPermission(this)) {
            PermissionHelper.askCameraPermission(this);
        }
        if (!PermissionHelper.getLocationPermission(this)) {
            PermissionHelper.askLocationPermission(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PermissionHelper.CAMERA_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Granted
                } else {
                    // Not granted
                }
            case PermissionHelper.LOCATION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Granted
                } else {
                    // Not granted
                }
        }
    }
}
