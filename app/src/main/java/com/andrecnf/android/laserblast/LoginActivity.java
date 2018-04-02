package com.andrecnf.android.laserblast;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.content.Intent;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private Button button_play;
    private List<Player> list_players = new ArrayList<>();
    private int flag_DupName = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference mPlayersReference = database.getReference("players");
        Random rand = new Random();
        final int id = rand.nextInt(100) + 1;

        button_play = (Button) findViewById(R.id.button_play);
        button_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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

                        //if (v.getId() == R.id.button_play)
                        EditText name = (EditText) findViewById(R.id.TFusername);
                        String str = name.getText().toString();

                        // if the username has alredy been chosen ask for another username
                        for(int i = 0; i < list_players.size(); i++){
                            Log.d(TAG, "onDataChange: Current player: " + list_players.get(i).getName());
                            if(str.equals(list_players.get(i).getName())){
                                Toast.makeText(LoginActivity.this, "Sorry, that username is already taken. Please choose another one", Toast.LENGTH_SHORT).show();
                                flag_DupName = 1;
                            }
                        }

                        if(flag_DupName == 0){
                            Log.d(TAG, "onClick: No duplicate names found");
                            createPlayer(id, str, database);
                            openMainActivity(str, id);
                        }
                        else{
                            Log.d(TAG, "onClick: Duplicate name found, try again");
                            flag_DupName = 0;
                        }

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
        });
    }

    public void openMainActivity(String str, int id) {
        Log.d(TAG, "openMainActivity: Opening Main Activity...");
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("Username", str);
        intent.putExtra("ID", id);
        startActivity(intent);
    }

    // Create and upload a player to the Firebase Realtime Database
    private void createPlayer(int id, String name, FirebaseDatabase database) {
        Player p1 = new Player(id, name);
        DatabaseReference myRef = database.getReference("players/" + name + id);
        myRef.setValue(p1);
    }
}