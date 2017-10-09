package com.apps.gerdjes.locationprovider;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.internal.StreetViewLifecycleDelegate;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;

import java.io.IOException;
import java.util.List;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener {

    private static final int PERMISSION_ACCESS_COARSE_LOCATION = 77;
    TextView tvCaptureGeolocation;
    private GoogleApiClient mGoogleApiClient;
    ImageView ivRadar;
    private SupportMapFragment mapFragment;
    private double longitude = 49.894634;
    private double latitude = -98.22876;

    ////////////////////////////////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ivRadar = (ImageView) findViewById(R.id.ivRadar);
        tvCaptureGeolocation = (TextView) findViewById(R.id.tvCaptureGeolocation);



        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();

    }
    ////////////////////////////////////////////////////////////////////////////////

    public void btnCaptureLocationOnClick(View view) {
        if (checkPermission()) {
        } else {
            requestPermission();
        }
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0.0f, locationListner);

    }

    ////////////////////////////////////////////////////////////
    // check users permission
    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);

        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    //////////////////////////////////////////////////////////
    //request permission
    private void requestPermission() {
        if (
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            Toast.makeText(this, "GPS permission needed in order to capture your location", Toast.LENGTH_LONG).show();

        } else {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_ACCESS_COARSE_LOCATION);

        }
    }


    ///////////////////////////////////////////////////////////////
    //handle callback when permission is granted
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        switch (requestCode) {
            case PERMISSION_ACCESS_COARSE_LOCATION:

                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Snackbar.make(tvCaptureGeolocation, "Permission Granted, now you can access location data.", Snackbar.LENGTH_LONG).show();
                } else {
                    Snackbar.make(tvCaptureGeolocation, "Permission Denied, you cannot access location data.", Snackbar.LENGTH_LONG).show();
                }
                break;
        }
    }

//////////////////////////////////////////////////////////////////////


    LocationListener locationListner = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            String msg = "Location captured successfully: logitude: %s, latitude:%s";
            longitude = location.getLongitude();
            latitude = location.getLatitude();
            msg = String.format(msg, longitude, latitude);

            mapFragment.getMapAsync(MainActivity.this);

            try {
                msg += "\nStreet: " + getAddressFromLocation(latitude, longitude);
            } catch (IOException e) {
                e.printStackTrace();
            }
            tvCaptureGeolocation.setText(msg);
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        public void onProviderEnabled(String provider) {
            Toast.makeText(MainActivity.this, " Provider Enabled", Toast.LENGTH_SHORT).show();
        }

        public void onProviderDisabled(String provider) {
            Toast.makeText(MainActivity.this, "Provider Disabled", Toast.LENGTH_SHORT).show();
        }
    };
///////////////////////////////////////////////////////////////////////////////

    private String getAddressFromLocation(double latitude, double longitude) throws IOException {
        Geocoder geocoder = new Geocoder(this);
        List<Address> matches = geocoder.getFromLocation(latitude, longitude, 1);
        if (matches.size() < 1) return "";
        return (matches.isEmpty() ? null : matches.get(0)).getAddressLine(0).toString();

    }


    ///////////////////////////////////////////////////////////////////////////
    @Override
    public void onMapReady(GoogleMap googleMap) {

        CameraUpdate center = CameraUpdateFactory.newLatLng(new LatLng(longitude, latitude));

        CameraUpdate zoom = CameraUpdateFactory.zoomTo(10);
        googleMap.moveCamera(center);
        googleMap.animateCamera(zoom);
        getNearByRestaurant(longitude, latitude, googleMap);
    }

    private void getNearByRestaurant(double lat, double longu, GoogleMap googleMap) {
        googleMap.clear();
        String url = getURL(lat, longu);
        Object[] DataTransfer = new Object[2];
        DataTransfer[0]=googleMap;
        DataTransfer[1] = url;
        NearByPlacesProvider nearByPlacesProvider = new NearByPlacesProvider();
        nearByPlacesProvider.execute(DataTransfer);
        Toast.makeText(this, "Nearby Restaurants", Toast.LENGTH_LONG).show();
    }

    private String getURL(double latitude, double longitude) {
        StringBuilder googlePlacesUrl = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        googlePlacesUrl.append("location=" + latitude + "," + longitude);
        googlePlacesUrl.append("&radius=" + 15000);
        googlePlacesUrl.append("&type=restaurant");
        googlePlacesUrl.append("&sensor=true");
        googlePlacesUrl.append("&key=" + "AIzaSyDlrUzPGOA4wV8h44hAYdof0babLO1VDqM");
        Log.d("URL", googlePlacesUrl.toString());
        return (googlePlacesUrl.toString());
    }
/////////////////////////////////////////////////////////////////////

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, "Unable to find nearby restaurants, check your internet connection", Toast.LENGTH_LONG).show();

    }
}



