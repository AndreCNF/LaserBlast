package com.andrecnf.android.laserblast;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_GPS = 42; // Constant to ask for location permission
    private TextView CorText;
    private TextView OriText;
    private Button shootBtn;
    private Location mCurrentLocation;
    private float [] mCurrentOrientation;
    private BroadcastReceiver broadcastReceiverGPS;
    private BroadcastReceiver broadcastReceiverSensor;
    private Context context;
    private LocationManager locationManager;

    @Override
    protected void onResume() {
        super.onResume();

        // Register GPS broadcast receiver and define behaviour when receiving data
        if(broadcastReceiverGPS == null){
            broadcastReceiverGPS = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    mCurrentLocation = (Location) intent.getExtras().get("location");
                    CorText.setText("Latitude: " + mCurrentLocation.getLatitude() +
                                    "\nLongitude: " + mCurrentLocation.getLongitude() +
                                    "\nAltitude: " + mCurrentLocation.getAltitude());
//                    CorText.setText("Latitude: " + mCurrentLocation.getLatitude() +
//                            "\nLongitude: " + mCurrentLocation.getLongitude());
                }
            };
        }
        registerReceiver(broadcastReceiverGPS, new IntentFilter("location_update"));

        // Register sensor broadcast receiver and define behaviour when receiving data
        if(broadcastReceiverSensor == null){
            broadcastReceiverSensor = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    mCurrentOrientation = (float []) intent.getExtras().get("orientation");
                    OriText.setText("Azimuth: " + mCurrentOrientation[0] +
                                    "\nPitch: " + mCurrentOrientation[1] +
                                    "\nRoll: " + mCurrentOrientation[2]);
                }
            };
        }
        registerReceiver(broadcastReceiverSensor, new IntentFilter("sensors_update"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(broadcastReceiverGPS != null){
            unregisterReceiver(broadcastReceiverGPS);
        }

        if(broadcastReceiverSensor != null){
            unregisterReceiver(broadcastReceiverSensor);
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CorText = findViewById(R.id.CorText);
        OriText = findViewById(R.id.OriText);
        context = this;

        // Check if location permission has already been granted
        if (!runtime_permissions()){
            // Start GPS location service
            Intent i = new Intent(getApplicationContext(),GPS_Service.class);
            startService(i);
        }

        // Start sensor data service
        Intent i2 = new Intent(getApplicationContext(),Sensor_Service.class);
        startService(i2);

        // Get first coordinates based on the last known location
        locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        mCurrentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        CorText.setText("Latitude: " + mCurrentLocation.getLatitude() +
                        "\nLongitude: " + mCurrentLocation.getLongitude() +
                        "\nAltitude: " + mCurrentLocation.getAltitude());

        shootBtn = findViewById(R.id.shootBtn);
        shootBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                Toast.makeText(MainActivity.this, "Fire in the hole!", Toast.LENGTH_SHORT).show();
            }
        });

        // Write a message to the database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("message");

        myRef.setValue("Hello, World!");
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
                      Intent i = new Intent(getApplicationContext(),GPS_Service.class);
                      startService(i);

                  } else {
                      // permission denied, boo!
                      Toast.makeText(this, "Why don't you trust us!? :(", Toast.LENGTH_SHORT).show();
                      runtime_permissions();
                  }
                  return;
              }

              // other 'case' lines to check for other
              // permissions this app might request.
          }
      }
}