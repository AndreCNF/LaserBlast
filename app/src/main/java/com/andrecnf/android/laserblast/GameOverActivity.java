package com.andrecnf.android.laserblast;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;

public class GameOverActivity extends AppCompatActivity {
    private static final String TAG = "GameOverActivity";
    private Button play_again;
    private ArrayList<String> mTopPlayers = new ArrayList<String>();
    private final FirebaseDatabase database = FirebaseDatabase.getInstance();
    private final DatabaseReference mPlayersReference = database.getReference("players");
    private RecyclerView recyclerView;
    private String name;
    private int id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_over);

        // Order players by their score
        Query scoreQuery = mPlayersReference.orderByChild("score");

        // Get players sorted in ascending order, according to their score
        scoreQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot playerSnapshot: dataSnapshot.getChildren()) {
                    Player player = playerSnapshot.getValue(Player.class);
                    mTopPlayers.add(player.getName() + " " + player.getScore());

                    Log.d(TAG, "onCreate: Building list before reverse: " + mTopPlayers);

                    // Reset each player's score to 0
                    database.getReference("players/" + player.getId() + "/score").setValue(0);
                }

                Log.d(TAG, "onCreate: List before reverse: " + mTopPlayers);
                // Get players sorted in descending order, according to their score
                Collections.reverse(mTopPlayers);
                Log.d(TAG, "onCreate: List after reverse: " + mTopPlayers);

                // Display top score list
                initRecyclerView();
                DatabaseReference mLoggedInRef = database.getReference("players/" + id + "/isLoggedIn");

                // Sign out the current player
                LoginActivity.mAuth.signOut();
                mLoggedInRef.setValue(false);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "onCancelled", databaseError.toException());
            }
        });

        play_again = (Button) findViewById(R.id.start_again);
        play_again.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openLoginActivity();
            }
        });
    }

    public void openLoginActivity() {
        Log.d(TAG, "openLoginActivity: Opening Login Activity...");
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    private void initRecyclerView(){
        Log.d(TAG, "initRecyclerView: init recyclerview.");
        recyclerView = findViewById(R.id.recycler_view);
        RecyclerViewAdapter adapter = new RecyclerViewAdapter(this, mTopPlayers, R.layout.layout_listitem_white);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Get the current player's name and ID from the MainActivity
        name = getIntent().getStringExtra("Username");
        id = getIntent().getIntExtra("ID", -1);

        DatabaseReference mLoggedInRef = database.getReference("players/" + name + id + "/isLoggedIn");

        // Sign out the current player
        LoginActivity.mAuth.signOut();
        mLoggedInRef.setValue(false);
    }

}
