package eu.wallhack.domkirkear.listeners;

import android.location.Location;
import android.os.Bundle;

import eu.wallhack.domkirkear.MainActivity;

public class LocationListener implements android.location.LocationListener {

    @Override
    public void onLocationChanged(Location location) {
        MainActivity.writeGPSCoordinatesToView(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
