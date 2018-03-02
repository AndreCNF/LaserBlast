package com.andrecnf.android.laserblast;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

public class MainActivity extends AppCompatActivity {
    private FusedLocationProviderClient mFusedLocationClient; // Location object that gives the GPS data
    private static final int REQUEST_GPS = 42; // Constante to ask for location permission
    private boolean mRequestingLocationUpdates = false; // Flag to handle location updates permission
    private TextView TestText;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private Location mCurrentLocation;
    private BroadcastReceiver broadcastReceiver;
    private float[] currentCoordinates;
    private Context context;

    @Override
    protected void onResume() {
        super.onResume();
        if(broadcastReceiver == null){
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
//                    mCurrentLocation = (Location) intent.getExtras().get("location");
//                    currentCoordinates = intent.getExtras().getFloatArray("coordinates");
//                    TestText.setText("Latitude: " + currentCoordinates[0] + "\nLongitude: " +currentCoordinates[1]);
//                    Toast.makeText(context, "It's working! :)", Toast.LENGTH_LONG).show();
//                    TestText.setText("Latitude: " + mCurrentLocation.getLatitude() + "\nLongitude: " + mCurrentLocation.getLongitude());
//                    TestText.setText("\n" + intent.getExtras().get("coordinates"));
                    TestText.append("\n" +intent.getExtras().get("coordinates"));
                }
            };
        }
        registerReceiver(broadcastReceiver,new IntentFilter("location_update"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(broadcastReceiver != null){
            unregisterReceiver(broadcastReceiver);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TestText = (TextView) findViewById(R.id.TestText);
        context = this;

        // Check if location permission has already been granted
        if (!runtime_permissions()){
            // Start GPS location service
            Intent i =new Intent(getApplicationContext(),GPS_Service.class);
            startService(i);
        }

//        LocationRequest mLocationRequest = createLocationRequest();
//        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
//                .addLocationRequest(mLocationRequest);
//
//        SettingsClient client = LocationServices.getSettingsClient(this);
//
//        mLocationCallback = new LocationCallback() {
//            @Override
//            public void onLocationResult(LocationResult locationResult) {
//                for (Location location : locationResult.getLocations()) {
//                    // Update UI with location data
//                    TestText.setText("Latitude: " + location.getLatitude() + "\nLongitude: " + location.getLongitude()
//                            + "\nTime: " + location.getTime());
//                }
//            };
//        };
//
//        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
//
//        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
//            @Override
//            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
//                // All location settings are satisfied. The client can initialize
//                // location requests here.
//                mRequestingLocationUpdates = true;
//            }
//        });
//
//        task.addOnFailureListener(this, new OnFailureListener() {
//            @Override
//            public void onFailure(@NonNull Exception e) {
//                if (e instanceof ResolvableApiException) {
//                    // Location settings are not satisfied, but this can be fixed
//                    // by showing the user a dialog.
//                    try {
//                        // Show the dialog by calling startResolutionForResult(),
//                        // and check the result in onActivityResult().
//                        ResolvableApiException resolvable = (ResolvableApiException) e;
//                        resolvable.startResolutionForResult(MainActivity.this,
//                                REQUEST_GPS);
//                    } catch (IntentSender.SendIntentException sendEx) {
//                        // Ignore the error.
//                    }
//                }
//            }
//        });
    }

    private boolean runtime_permissions() {
        if(Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){

            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_GPS);

            return true;
        }

        return false;
    }

      // Handle the user's answers to permission requests
      @Override
      public void onRequestPermissionsResult(int requestCode,
                                             String permissions[], int[] grantResults) {
          switch (requestCode) {
              case REQUEST_GPS: {
                  // If request is cancelled, the result arrays are empty.
                  if (grantResults.length > 0
                          && grantResults[0] == PackageManager.PERMISSION_GRANTED
                          && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                      // Start GPS location service
                      Intent i =new Intent(getApplicationContext(),GPS_Service.class);
                      startService(i);

                      // permission was granted, yay!
//                      mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
//                      mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
//                          @Override
//                          public void onSuccess(Location location) {
//                              // Got last known location. In some rare situations this can be null.
//                              if (location != null) {
//                                  // Logic to handle location object
//                                  TestText.setText("Latitude: " + location.getLatitude() + "\nLongitude: " + location.getLongitude());
//                                  mCurrentLocation = location;
//                              }
//                          }
//                      });

                  } else {

                      // permission denied, boo! Disable the
                      // functionality that depends on this permission.
                      Toast.makeText(this, "Why don't you trust us!? :(", Toast.LENGTH_SHORT).show();
                      runtime_permissions();
                  }
                  return;
              }

              // other 'case' lines to check for other
              // permissions this app might request.
          }
      }

    // Set location request parameters
//    protected LocationRequest createLocationRequest() {
//        LocationRequest mLocationRequest = new LocationRequest();
//        mLocationRequest.setInterval(1000);
//        mLocationRequest.setFastestInterval(200);
//        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//        return mLocationRequest;
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        if (mRequestingLocationUpdates) {
//            startLocationUpdates();
//        }
//    }
//
//    private void startLocationUpdates() {
//        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
//                mLocationCallback,
//                null /* Looper */);
//    }
//
//    @Override
//    protected void onPause() {
//        super.onPause();
//        stopLocationUpdates();
//    }
//
//    private void stopLocationUpdates() {
//        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
//    }
}