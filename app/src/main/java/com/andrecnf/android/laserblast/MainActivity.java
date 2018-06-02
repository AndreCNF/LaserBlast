package com.andrecnf.android.laserblast;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Camera;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Debug;
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

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_GPS = 42; // Constant to ask for location permission
    private static final int REQUEST_CAMERA = 13; // Constant to ask for camera permission
    private TextView CorText;
    private TextView OriText;
    private TextView DebugText;
    private TextView ScoreText;
    private Button shootBtn;
    private Location mCurrentLocation;
    private float [] mCurrentOrientation;
    private Location tmpmCurrentLocation;
    private float [] tmpmCurrentOrientation;
    private BroadcastReceiver broadcastReceiverGPS;
    private BroadcastReceiver broadcastReceiverSensor;
    private Context context;
    private LocationManager locationManager;
    private RecyclerView recyclerView;
    private List<Player> list_players = new ArrayList<>();
    private ArrayList<String> mShotPlayers = new ArrayList<>();
    private final FirebaseDatabase database = FirebaseDatabase.getInstance();
    private final DatabaseReference mPlayersReference = database.getReference("players");
    private DatabaseReference mCurPlayerRef;
    private Query scoreQuery;
    private String name;
    private int id;
    private ThreeDCharact mCurrentCoordinates3D;
    private ThreeDCharact mCurrentOrientation3D;
    private boolean isDead = false;
    private Iterable<DataSnapshot> topScoredPlayers;
    public static int topScore = 0;
    private int CurScore = 0;

    // Maximum score that, when reached, ends the game (gameover)
    public static int maxScore = 20;

    // Respawn time in seconds
    public static int respawn_tm = 10;

    // Pop up window time in seconds
    public static int pop_tm = 1;

    // Flags
    private static int sensor_upd_flag = 0;

    // Listeners
    ValueEventListener deathListener;
    ValueEventListener shootListener;
    ValueEventListener scoreListener;
    ValueEventListener curScoreListener;

    @Override
    protected void onResume() {
        super.onResume();

        // Register GPS broadcast receiver and define behaviour when receiving data
        if(broadcastReceiverGPS == null){
            broadcastReceiverGPS = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    try{
                        mCurrentLocation = (Location) intent.getExtras().get("location");
                        CorText.setText("Latitude: " + mCurrentLocation.getLatitude() +
                                "\nLongitude: " + mCurrentLocation.getLongitude() +
                                "\nAltitude: " + mCurrentLocation.getAltitude());

                        mCurrentCoordinates3D.setX(mCurrentLocation.getLatitude());
                        mCurrentCoordinates3D.setY(mCurrentLocation.getLongitude());
                        mCurrentCoordinates3D.setZ(mCurrentLocation.getAltitude());
                        mCurrentOrientation3D.setX(mCurrentOrientation[0]);
                        mCurrentOrientation3D.setY(mCurrentOrientation[1]);
                        mCurrentOrientation3D.setZ(mCurrentOrientation[2]);
                        database.getReference("players/" + name + id + "/coordinates").setValue(mCurrentCoordinates3D);
                        database.getReference("players/" + name + id + "/orientation").setValue(mCurrentOrientation3D);
                    } finally{

                    }
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

                    if(sensor_upd_flag >= 50) {
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

        DatabaseReference mLoggedInRef = database.getReference("players/" + name + id + "/isLoggedIn");

        // Sign out the current player
        LoginActivity.mAuth.signOut();
        mLoggedInRef.setValue(false);
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CorText = findViewById(R.id.CorText);
        OriText = findViewById(R.id.OriText);
        DebugText = findViewById(R.id.Debug);
        ScoreText = findViewById(R.id.ScoreText);
        context = this;

        // Check if location permission has already been granted
        if (!runtime_permissions()){
            // Start GPS location service
            Intent i = new Intent(getApplicationContext(),GPS_Service.class);
            startService(i);
        }

        // Get the current player's name and ID from the LoginActivity
        name = getIntent().getStringExtra("Username");
        id = getIntent().getIntExtra("ID", -1);
        mCurPlayerRef = database.getReference("players/" + name + id + "/dead");

        final DatabaseReference playerScore = database.getReference("players/" + name + id + "/score");
        Log.d(TAG, "onCreate: playerScore = " + playerScore);

        playerScore.addListenerForSingleValueEvent(curScoreListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    CurScore = Integer.valueOf(dataSnapshot.getValue().toString());
                    Log.d(TAG, "onDataChange: CurScore = " + CurScore);
                    ScoreText.setText(String.valueOf(CurScore));
                }
                catch(NullPointerException e){
                    ScoreText.setText("Problems getting the score");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                ScoreText.setText("Cancelled");
            }
        });

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

        shootBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button

                // Save shooting player's location and orientation at time of shooting
                tmpmCurrentLocation = mCurrentLocation;
                tmpmCurrentOrientation = mCurrentOrientation;

                // Retrieve all the current player status one single time
                mPlayersReference.addListenerForSingleValueEvent(shootListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Log.d(TAG, "onDataChange: Retrieving players' info...");
                        if(list_players.size() > 0)
                            list_players.clear();
                        for(DataSnapshot playerSnapshot:dataSnapshot.getChildren()){
                            Player player = playerSnapshot.getValue(Player.class);
                            list_players.add(player);
                            Log.d(TAG, "onDataChange: Adding player " + player.getName() + " to the list...");
                        }

                        mShotPlayers.clear();
                        mShotPlayers.add("Shot players:");

                        Log.d(TAG, "onClick: list_players before dead removal: " + list_players);
                        for(int i = 0; i < list_players.size(); i++){
                            Log.d(TAG, "onClick: Seeing if player " + list_players.get(i).getName() + " is dead...");
                            if(list_players.get(i).isDead()){
                                Log.d(TAG, "onClick: Removing dead player " + list_players.get(i).getName());
                                list_players.remove(i);

                                // As player i was removed, next player has index i again
                                i--;
                            }
                        }

                        for(int i = 0; i < list_players.size(); i++){
                            // Skip current player
                            if(list_players.get(i).getName().equals(name) && list_players.get(i).getId() == id){
                                continue;
                            }

                            // Skip players that are already dead
                            if(list_players.get(i).isDead()){
                                continue;
                            }

                            // Skip players that are signed out
                            if(list_players.get(i).getIsLoggedIn()){
                                continue;
                            }

                            Log.d(TAG, "onClick: Seeing if player " + list_players.get(i).getName() + " is shot...");
                            if(isShot(tmpmCurrentOrientation[0],
                                      tmpmCurrentLocation.getLatitude(),
                                      tmpmCurrentLocation.getLongitude(),
                                      list_players.get(i).getCoordinates().x,
                                      list_players.get(i).getCoordinates().y)){
                                Log.d(TAG, "onDataChange: Player " + list_players.get(i).getName() + " was shot.");

                                // Kill player
                                DatabaseReference shotPlayerDead = database.getReference(
                                        "players/" + list_players.get(i).getName()
                                                      + list_players.get(i).getId() + "/dead");
                                shotPlayerDead.setValue(true);

                                playerScore.addListenerForSingleValueEvent(curScoreListener = new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        try {
                                            CurScore = Integer.valueOf(dataSnapshot.getValue().toString());
                                            Log.d(TAG, "onDataChange: CurScore = " + CurScore);
                                            CurScore += 1;
                                            Log.d(TAG, "onDataChange: New CurScore = " + CurScore);
                                            ScoreText.setText(String.valueOf(CurScore));
                                            playerScore.setValue(CurScore);
                                        }
                                        catch(NullPointerException e){
                                            ScoreText.setText("Problems getting the score");
                                        }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                        ScoreText.setText("Cancelled");
                                    }
                                });

                                // Add to the shot players names list
                                mShotPlayers.add(list_players.get(i).getName());
                            }
                        }

                        Log.d(TAG, "onClick: mShotPlayers = " + mShotPlayers);

                        // Print pop-up message if some player was shot.
                        // At least 1 player is shot if the mShotPlayers text arraylist has more
                        // than one lines of text (the first line is always "Shot players:").
                        if(mShotPlayers.size() > 1) {
                            // Change message background color to green
                            recyclerView.setBackgroundColor(Color.parseColor("#507CFC00"));

                            updateRecyclerView();

                            // Start timer of 1 second
                            new CountDownTimer(pop_tm * 1000, 1000) {

                                public void onTick(long millisUntilFinished) {

                                }

                                // End of timer
                                public void onFinish() {
                                    // Clean screen
                                    mShotPlayers.clear();
                                    recyclerView.setVisibility(View.INVISIBLE);
                                }
                            }.start();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
        });

        // Check if player was killed by someone
        mCurPlayerRef.addValueEventListener(deathListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                isDead = dataSnapshot.getValue(Boolean.class);
                Log.d(TAG, "onDataChange: Current dead status is " + isDead);

                if(isDead){
                    mShotPlayers.clear();
                    mShotPlayers.add("You're dead :(");
                    mShotPlayers.add("");

                    // Deactivate shooting button
                    shootBtn.setEnabled(false);

                    // Change message background color to red
                    recyclerView.setBackgroundColor(Color.parseColor("#50b71503"));

                    // Start timer of 10 seconds
                    new CountDownTimer(respawn_tm * 1000, 1000) {

                        public void onTick(long millisUntilFinished) {
                            int secondsLeft = (int) Math.ceil(millisUntilFinished / 1000);

                            // Put respawn time on the screen
                            mShotPlayers.set(1, String.valueOf(secondsLeft));
                            updateRecyclerView();
                            Log.d(TAG, "onTick: Respawn time = " + millisUntilFinished + " s");
                        }

                        // End of timer
                        public void onFinish() {
                            // Clean screen
                            mShotPlayers.clear();
                            recyclerView.setVisibility(View.INVISIBLE);

                            // Reactivate shooting button
                            shootBtn.setEnabled(true);

                            // Send info to database that the player is not dead anymore
                            isDead = false;
                            mCurPlayerRef.setValue(isDead);

                            Log.d(TAG, "onFinish: Player has respawned.");
                        }
                    }.start();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        // Get top scored player
        scoreQuery = mPlayersReference.orderByChild("score").limitToLast(1);

        // Check for current top score
        scoreQuery.addValueEventListener(scoreListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                topScoredPlayers = dataSnapshot.getChildren();

                for(DataSnapshot playerSnapshot: dataSnapshot.getChildren()) {
                    topScore = playerSnapshot.child("score").getValue(Integer.class);
                    Log.d(TAG, "onDataChange: Current top score: " + topScore);

                    Player player = playerSnapshot.getValue(Player.class);
                    Log.d(TAG, "onDataChange: Top scored player: " + player.getName());

                    // Check for gameover (score reached the maximum value)
                    if(topScore >= maxScore){
                        Toast.makeText(context, "GAMEOVER: " +
                                       playerSnapshot.child("name").getValue(String.class) +
                                       " won the game", Toast.LENGTH_LONG).show();

                        detachListeners();
                        openGameOverActivity();
                    }

                    // Only the top score matters
                    break;
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "onCancelled", databaseError.toException());
            }
        });
    }

    private boolean runtime_permissions() {
        if(Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){

            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_GPS);
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_GPS);

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

            // TODO Camera permissions
            case REQUEST_CAMERA: {
//                private Camera mCamera;
//                private CameraPreview mPreview;
//
//                // Create an instance of Camera
//                mCamera = getCameraInstance();
//
//                // Create our Preview view and set it as the content of our activity.
//                mPreview = new CameraPreview(this, mCamera);
//                FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
//                preview.addView(mPreview);
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
        recyclerView.setVisibility(View.VISIBLE);
    }

//    // Complex version to calculate distance
//    private int calculateDistance(double lat1, double lon1, double lat2, double lon2) {
//        double R = 6371; // km
//        double dLat = (lat2 - lat1).toRad();
//        double dLon = (lon2 - lon1).toRad();
//        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
//                   Math.cos(lat1.toRad()) * Math.cos(lat2.toRad()) *
//                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
//        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
//        double d = R * c;
//        return d;
//    }

    // Flat earth approximation to calculate the distance between two points.
    // 1 is the current player, 2 is the other player.
    // Reference: http://www.movable-type.co.uk/scripts/latlong.html
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Earth's radius in meters
        final double R = 6371000;

        // Latitudes in radian
        double lat1r = lat1 * Math.PI / 180;
        double lat2r = lat2 * Math.PI / 180;

        // Longitude in radian
        double lon1r = lon1 * Math.PI / 180;
        double lon2r = lon2 * Math.PI / 180;

        // Difference in latitude, in radians
        double dLat = lat2r - lat1r;

        // Difference in longitude, in radians
        double dLon = lon2r - lon1r;

        double x = dLon * Math.cos((lat1r + lat2r)/2);
        double y = dLat;
        double d = Math.sqrt(x*x + y*y) * R;
        return d;
    }

    // Calculate initial bearing (1 is the current player, 2 is the other player)
    // Reference: http://www.movable-type.co.uk/scripts/latlong.html
    private double calculateBearing(double lat1, double lon1, double lat2, double lon2) {
        // Latitudes in radian
        double lat1r = lat1 * Math.PI / 180;
        double lat2r = lat2 * Math.PI / 180;

        // Longitude in radian
        double lon1r = lon1 * Math.PI / 180;
        double lon2r = lon2 * Math.PI / 180;

        // Difference in longitude, in radians
        double dLon = lon2r - lon1r;

        double y = Math.sin(dLon) * Math.cos(lat2r);
        double x = Math.cos(lat1r)*Math.sin(lat2r) - Math.sin(lat1r)*Math.cos(lat2r)*Math.cos(dLon);
        // double brng = Math.atan2(y, x) * 180 / Math.PI;
        double brng = Math.atan2(y, x);
        return brng;
    }

    // See if a given player is shot by the current player, knowing the coordinates of both players.
    // 1 is the current player, 2 is the other player
    private boolean isShot(double azimuth, double lat1, double lon1, double lat2, double lon2) {
        // Distance between the two players
        double dist = calculateDistance(lat1, lon1, lat2, lon2);

        // Correct azimuth for horizontal handling of the smartphone
        double correctedAzimuth = azimuth + (Math.PI/2);

        if(correctedAzimuth > Math.PI){
            correctedAzimuth = correctedAzimuth - 2 * Math.PI;
        }

        // If the distance between the players is bigger than 30 meters, the other player isn't shot
        if(dist > 30){
            Log.d(TAG, "isShot: Player too far away");
            return false;
        }

        // Rounded coordinates to allow a wider shooting area, compensating GPS and compass
        // precision errors
//        double lat1rnd = roundFourDecimals(lat1);
//        double lat2rnd = roundFourDecimals(lat2);
//        double lon1rnd = roundFourDecimals(lon1);
//        double lon2rnd = roundFourDecimals(lon2);

//        double brng = calculateBearing(lat1rnd, lon1rnd, lat2rnd, lon2rnd);
        double brng = calculateBearing(lat1, lon1, lat2, lon2);

        // The other player is shot only if it's in the direction of the shooting player's orientation
        // See if the shooting player's orientation is similar to the bearing calculated.
        if(areAnglesSimilar(correctedAzimuth, brng)){
            return true;
        }
        else{
            return false;
        }
    }

    // Round a double number to 4 decimal digits
    double roundFourDecimals(double d)
    {
        DecimalFormat twoDForm = new DecimalFormat("##.####");
        return Double.valueOf(twoDForm.format(d));
    }

    private boolean areAnglesSimilar(double azimuth, double brng){
        // Angle error threshold
        double ang_thr = 25 * Math.PI / 180;

        // Adjust azimuth
        double adj_azimuth = -azimuth;

        // Same signal, no discontinuities in the angles
        if(Math.signum(adj_azimuth) == Math.signum(brng)){
            DebugText.setText("Azimuth: " + adj_azimuth * 180 / Math.PI + "; Bearing: " + brng * 180 / Math.PI +
                    "\n Difference: " + Math.abs(adj_azimuth - brng) * 180 / Math.PI);

            if(Math.abs(adj_azimuth - brng) < ang_thr){
                return true;
            }
        }
    // Different signal, see if the angles are near PI, where a discontinuity resides
        else if(Math.signum(adj_azimuth) > 0){
            if(Math.abs(adj_azimuth - Math.PI) < Math.abs(adj_azimuth) ||
                    Math.abs(brng - (-Math.PI)) < Math.abs(brng)){

                DebugText.setText("Azimuth: " + adj_azimuth * 180 / Math.PI + "; Bearing: " + brng * 180 / Math.PI +
                        "\n Difference: " + Math.abs(adj_azimuth - (2 * Math.PI + brng)) * 180 / Math.PI);

                // Force both angles to have the same sign by inverting the sign of brng
                if(Math.abs(adj_azimuth - (2 * Math.PI + brng)) < ang_thr){
                    return true;
                }
            }
            // Different signal but both are close to 0 rads, where there isn't a discontinuity
            else{
                DebugText.setText("Azimuth: " + adj_azimuth * 180 / Math.PI + "; Bearing: " + brng * 180 / Math.PI +
                        "\n Difference: " + Math.abs(adj_azimuth - brng) * 180 / Math.PI);

                if(Math.abs(adj_azimuth - brng) < ang_thr){
                    return true;
                }
            }
        }
        else if(Math.signum(adj_azimuth) < 0){
            if(Math.abs(adj_azimuth - (-Math.PI)) < Math.abs(adj_azimuth) ||
                    Math.abs(brng - Math.PI) < Math.abs(brng)){

                DebugText.setText("Azimuth: " + adj_azimuth * 180 / Math.PI + "; Bearing: " + brng * 180 / Math.PI +
                        "\n Difference: " + Math.abs((2 * Math.PI + adj_azimuth) - brng) * 180 / Math.PI);

                // Force both angles to have the same sign by inverting the sign of adj_azimuth
                if(Math.abs((2 * Math.PI + adj_azimuth) - brng) < ang_thr){
                    return true;
                }
            }
            // Different signal but both are close to 0 rads, where there isn't a discontinuity
            else{
                DebugText.setText("Azimuth: " + adj_azimuth * 180 / Math.PI + "; Bearing: " + brng * 180 / Math.PI +
                        "\n Difference: " + Math.abs(adj_azimuth - brng) * 180 / Math.PI);

                if(Math.abs(adj_azimuth - brng) < ang_thr){
                    return true;
                }
            }
        }

        // No angle compatibility found, then the player isn't in shooting range
        return false;
    }

    public void openGameOverActivity() {
        Log.d(TAG, "openLoginActivity: Opening Login Activity...");
        Intent intent = new Intent(this, GameOverActivity.class);
        intent.putExtra("Username", name);
        intent.putExtra("ID", id);
        startActivity(intent);
    }

    private void detachListeners() {
        if(shootListener != null) {
            mPlayersReference.removeEventListener(shootListener);
        }

        if(deathListener != null) {
            mCurPlayerRef.removeEventListener(deathListener);
        }

        if(scoreListener != null) {
            scoreQuery.removeEventListener(scoreListener);
        }

//        if(curScoreListener != null) {
//            playerScore.removeEventListener(curScoreListener);
//        }
    }
}