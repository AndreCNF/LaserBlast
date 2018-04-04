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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_GPS = 42; // Constant to ask for location permission
    private static int sensor_upd_flag = 0;
    private TextView CorText;
    private TextView OriText;
    private Button shootBtn;
    private Location mCurrentLocation;
    private float [] mCurrentOrientation;
    private BroadcastReceiver broadcastReceiverGPS;
    private BroadcastReceiver broadcastReceiverSensor;
    private Context context;
    private LocationManager locationManager;
    private RecyclerView recyclerView;
    private List<Player> list_players = new ArrayList<>();
    private ArrayList<String> mShotPlayers = new ArrayList<>();
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    final DatabaseReference mPlayersReference = database.getReference("players");
    private String name;
    private int id;
    private ThreeDCharact mCurrentCoordinates3D;
    private ThreeDCharact mCurrentOrientation3D;


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

                    mCurrentCoordinates3D.setX(mCurrentLocation.getLatitude());
                    mCurrentCoordinates3D.setY(mCurrentLocation.getLongitude());
                    mCurrentCoordinates3D.setZ(mCurrentLocation.getAltitude());
                    mCurrentOrientation3D.setX(mCurrentOrientation[1]);
                    mCurrentOrientation3D.setY(mCurrentOrientation[2]);
                    mCurrentOrientation3D.setZ(mCurrentOrientation[3]);
                    database.getReference("players/" + name + id + "/coordinates").setValue(mCurrentCoordinates3D);
                    database.getReference("players/" + name + id + "/orientation").setValue(mCurrentOrientation3D);
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

                    if(sensor_upd_flag >= 5000) {
                        OriText.setText("Azimuth: " + mCurrentOrientation[0] +
                                "\nPitch: " + mCurrentOrientation[1] +
                                "\nRoll: " + mCurrentOrientation[2]);
                        sensor_upd_flag = 0;
                    }

                    sensor_upd_flag += 1;
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

        name = getIntent().getStringExtra("Username");
        id = getIntent().getIntExtra("ID", -1);

        mCurrentCoordinates3D = new ThreeDCharact();
        mCurrentOrientation3D = new ThreeDCharact();

        // Start sensor data service
        Intent i2 = new Intent(getApplicationContext(),Sensor_Service.class);
        startService(i2);

        // Get first coordinates based on the last known location
        try{
            locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
            mCurrentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            CorText.setText("Latitude: " + mCurrentLocation.getLatitude() +
                    "\nLongitude: " + mCurrentLocation.getLongitude() +
                    "\nAltitude: " + mCurrentLocation.getAltitude());
        } finally {

        }

        initRecyclerView();

        shootBtn = findViewById(R.id.shootBtn);
//        shootBtn.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                // Code here executes on main thread after user presses button
//                Toast.makeText(MainActivity.this, "Fire in the hole!", Toast.LENGTH_SHORT).show();
//            }
//        });

        shootBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button

                // Retrieve all the current player status one single time
                mPlayersReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Log.d(TAG, "onDataChange: Retrieving players' info...");
                        if(list_players.size() > 0)
                            list_players.clear();
                        for(DataSnapshot postSnapshot:dataSnapshot.getChildren()){
                            Player player = postSnapshot.getValue(Player.class);
                            list_players.add(player);
                            Log.d(TAG, "onDataChange: Adding player " + player.getName() + " to the list...");
                        }

                        mShotPlayers.clear();
                        mShotPlayers.add("Shot players:");

                        Log.d(TAG, "onClick: list_players before low score removal: " + list_players);
                        for(int i = 0; i < list_players.size(); i++){
                            Log.d(TAG, "onClick: Seeing player " + list_players.get(i).getName() + "'s score...");
                            if(list_players.get(i).getScore() < 5){
                                Log.d(TAG, "onClick: Removing low scored player " + list_players.get(i).getName());
                                list_players.remove(i);

                                // As player i was removed, next player has index i again
                                i--;
                            }
                        }

                        // Put players with the desired score on the list to print on the screen
                        for(int i = 0; i < list_players.size(); i++){
                            mShotPlayers.add(list_players.get(i).getName());
                        }

                        Log.d(TAG, "onClick: mShotPlayers = " + mShotPlayers);
                        updateRecyclerView();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
        });

//        createPlayer(123, 6, "Gelson", database);
//        createPlayer(314, 21, "Dost", database);
//        createPlayer(165, 2, "Patricio", database);
//        createPlayer(794, 0, "Wendel", database);
//        createPlayer(275, 8, "Montero", database);
    }

    // Create and upload a player to the Firebase Realtime Database
    private void createPlayer(int id, String name, FirebaseDatabase database) {
        Player p1 = new Player(id, name);
        DatabaseReference myRef = database.getReference("players/" + name + id);
        myRef.setValue(p1);
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

    private void initRecyclerView(){
        Log.d(TAG, "initRecyclerView: init recyclerview.");
        recyclerView = findViewById(R.id.recycler_view);
        RecyclerViewAdapter adapter = new RecyclerViewAdapter(this, mShotPlayers);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void updateRecyclerView(){
        Log.d(TAG, "updateRecyclerView: updating recyclerview.");
        RecyclerViewAdapter adapter = new RecyclerViewAdapter(this, mShotPlayers);
        recyclerView.setAdapter(adapter);
    }

//    private int calculateDistance(lat1, lon1, lat2, lon2) {
//        var R = 6371; // km
//        var dLat = (lat2 - lat1).toRad();
//        var dLon = (lon2 - lon1).toRad();
//        var a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
//                Math.cos(lat1.toRad()) * Math.cos(lat2.toRad()) *
//                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
//        var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
//        var d = R * c;
//        return d;
//    }
}