package tallerii.udrive;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;

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

        Log.e("MY CURRENT LOCATION", myLocation);

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

    public double getLatitud(){
        return latitud;
    }

    public double getLongitud(){
        return longitud;
    }

    public Location getLocation(){
        return location;
    }
}
