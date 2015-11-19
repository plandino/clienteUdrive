package tallerii.udrive;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;

/**
 * Ubica al telefono, devuelve la longitud y latitud del telefono.
 */
public class MyLocationListener implements LocationListener {

    private double latitud;
    private double longitud;
    Location location;

    @Override
    public void onLocationChanged(Location location) {

        this.location = location;

        latitud  = location.getLatitude();
        longitud = location.getLongitude();

        String myLocation = "Latitude = " + location.getLatitude() + " Longitude = " + location.getLongitude();

        Log.d("LOCATION_LISTENER", myLocation);

    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    /**
     * Devuelve la latitud del telefono.
     * @return la latitud del telefono.
     */
    public double getLatitud(){
        Log.d("LOCATION_LISTENER", "Latitud \"" + latitud + "\".");
        return latitud;
    }

    /**
     * Devuelve la longitud del telefono.
     * @return la longitud del telefono.
     */
    public double getLongitud(){
        Log.d("LOCATION_LISTENER", "Longitud \"" + latitud + "\".");
        return longitud;
    }
}
