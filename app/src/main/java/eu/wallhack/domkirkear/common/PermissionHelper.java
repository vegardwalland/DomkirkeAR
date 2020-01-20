package eu.wallhack.domkirkear.common;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionHelper {

    // Ties the permission request to a corresponding permission request callback.
    // Value is arbitrary.
    public static final int CAMERA_REQUEST_CODE = 9001;

    public static void askCameraPermission(Activity activity) {
        if (!getCameraPermission(activity)) {
            if (hasPermissionBeenDeniedBefore(activity)) {
                showCameraPermissionExplanation();
            }
            askForCameraPermission(activity);
        }
    }

    public static boolean getCameraPermission(Activity activity) {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private static boolean hasPermissionBeenDeniedBefore(Activity activity) {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA);
    }

    private static void askForCameraPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
    }

    private static void showCameraPermissionExplanation() {
        // TODO Show message
        // TODO Implement localization
    }

}
