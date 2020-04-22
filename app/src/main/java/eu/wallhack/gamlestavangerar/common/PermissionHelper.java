package eu.wallhack.gamlestavangerar.common;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionHelper {

    // Ties the permission request to a corresponding permission request callback.
    // Value is arbitrary.
    public static final int CAMERA_REQUEST_CODE = 9001;
    public static final int GPS_REQUEST_CODE = 9002;



    public static void askCameraPermission(Activity activity, AlertDialog alertDialog) {
        if (!getCameraPermission(activity)) {
            if (hasCameraPermissionBeenDeniedBefore(activity)) {
                showPermissionDeniedBeforeExplanation(alertDialog);
            } else askForCameraPermission(activity);
        }
    }

    public static boolean getCameraPermission(Activity activity) {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private static boolean hasCameraPermissionBeenDeniedBefore(Activity activity) {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA);
    }

    private static void askForCameraPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
    }

    private static void showPermissionDeniedBeforeExplanation(AlertDialog alertDialog) {
        alertDialog.show();
    }

    // Permissions for GPS
    public static void askGPSPermission(Activity activity, AlertDialog alertDialog) {
        if (!getGPSPermission(activity)) {
            if (hasGPSPermissionBeenDeniedBefore(activity)) {
                showPermissionDeniedBeforeExplanation(alertDialog);
            } else askForGPSPermission(activity);
        }
    }

    public static boolean getGPSPermission(Activity activity) {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private static boolean hasGPSPermissionBeenDeniedBefore(Activity activity) {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private static void askForGPSPermission(Activity activity){
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, GPS_REQUEST_CODE);
    }

    public static AlertDialog setupNoPermissionAlert(Context context) {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
        builder1.setMessage("Appen trenger tilgang til kamera og GPS for å fungere");

       return builder1.create();
    }



}
