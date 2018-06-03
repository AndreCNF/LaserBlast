package com.andrecnf.android.laserblast;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.content.Intent;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
    private Button btnSignUp;
    private Button btnSignIn;
    private Button btnForgotPwd;
    private View userInput;
    private List<Player> list_players = new ArrayList<>();
    private int flag_DupName = 0;
    final FirebaseDatabase database = FirebaseDatabase.getInstance();
    final DatabaseReference mPlayersReference = database.getReference("players");
    static FirebaseAuth mAuth;
    private Boolean isSignUp = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        final EditText name = (EditText) findViewById(R.id.TFusername);
        final EditText mEmailField = (EditText) findViewById(R.id.TFemail);
        final EditText mPasswordField = (EditText) findViewById(R.id.TFpasswordTxt);

        button_play = (Button) findViewById(R.id.button_play);
        btnSignIn = (Button) findViewById(R.id.btnSignIn);
        btnSignUp = (Button) findViewById(R.id.btnSignUp);
        btnForgotPwd = (Button) findViewById(R.id.btnForgotPwd);
        userInput = findViewById(R.id.userInputBlock);

        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Disable the sign in and sign up buttons and show the log in UI on the screen
                btnSignUp.setEnabled(false);
                btnSignIn.setEnabled(false);
                btnSignUp.setVisibility(View.INVISIBLE);
                btnSignIn.setVisibility(View.INVISIBLE);
                userInput.setVisibility(View.VISIBLE);
                button_play.setVisibility(View.VISIBLE);
                btnForgotPwd.setVisibility(View.VISIBLE);
                button_play.setEnabled(true);
                btnForgotPwd.setEnabled(true);

                isSignUp = false;
            }
        });

        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Disable the sign in and sign up buttons and show the sign up UI on the screen
                btnSignUp.setEnabled(false);
                btnSignIn.setEnabled(false);
                btnSignUp.setVisibility(View.INVISIBLE);
                btnSignIn.setVisibility(View.INVISIBLE);
                userInput.setVisibility(View.VISIBLE);
                button_play.setVisibility(View.VISIBLE);
                button_play.setEnabled(true);

                isSignUp = true;
            }
        });

        btnForgotPwd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email_str = mEmailField.getText().toString().trim();

                // Send password reset e-mail
                if(!email_str.isEmpty()) {
                    mAuth.sendPasswordResetEmail(email_str)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Log.d(TAG, "Password reset e-mail sent to " + email_str);
                                        Toast.makeText(LoginActivity.this, "Sent password reset e-mail", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }
            }
        });

        button_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Validate the inserted e-mail and password before continuing
                if(validateForm(mEmailField, mPasswordField)){
                    final String name_str = name.getText().toString().trim();
                    final String email_str = mEmailField.getText().toString().trim();
                    final String password_str = mPasswordField.getText().toString().trim();

                    if(isSignUp) {
                        // Creating new account (sign up)

                        mAuth.createUserWithEmailAndPassword(email_str, password_str)
                                .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                                    @Override
                                    public void onComplete(@NonNull Task<AuthResult> task) {
                                        if (task.isSuccessful()) {
                                            // Sign in success, update UI with the signed-in user's information
                                            Log.d(TAG, "createUserWithEmail:success");
                                            Log.d(TAG, "Username: " + name_str + "; e-mail: "
                                                    + email_str + "; password: " + password_str);

                                            final FirebaseUser user = mAuth.getCurrentUser();

                                            // Get the user's unique ID from Firebase
                                            final String UID = user.getUid();

                                            createPlayer(UID, name_str, database);

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

                                                    Log.d(TAG, "Username: " + name_str + "; e-mail: "
                                                            + email_str + "; password: " + password_str);

                                                    // if the username has already been chosen ask for another username
                                                    for(int i = 0; i < list_players.size(); i++){
                                                        Log.d(TAG, "onDataChange: Current player: " + list_players.get(i).getName());
                                                        if(name_str.equals(list_players.get(i).getName()) && !UID.equals(list_players.get(i).getId())){
                                                            Toast.makeText(LoginActivity.this, "Sorry, that username is already taken. Please choose another one", Toast.LENGTH_SHORT).show();
                                                            flag_DupName = 1;

                                                            // Delete the created player if a duplicate name was found
                                                            database.getReference("players/" + UID).setValue(null);
                                                        }
                                                    }

                                                    if(flag_DupName == 0) {
                                                        // Send verification e-mail
                                                        user.sendEmailVerification()
                                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task<Void> task) {
                                                                        if (task.isSuccessful()) {
                                                                            Log.d(TAG, "Verification e-mail sent to " + email_str);
                                                                        }
                                                                    }
                                                                });

                                                        openMainActivity(UID);
                                                    }

                                                    else{
                                                        Log.d(TAG, "onClick: Duplicate name found, try again");
                                                        flag_DupName = 0;
                                                        user.delete();
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(DatabaseError databaseError) {

                                                }
                                            });

                                        } else {
                                            // If sign in fails, display a message to the user.
                                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                                            Toast.makeText(LoginActivity.this, "Sign up authentication failed.",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    }

                    else {
                        // Sign in existing account

                        Log.d(TAG, "Username: " + name_str + "; e-mail: "
                                + email_str + "; password: " + password_str);

                        mAuth.signInWithEmailAndPassword(email_str, password_str)
                                .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                                    @Override
                                    public void onComplete(@NonNull Task<AuthResult> task) {
                                        if (task.isSuccessful()) {
                                            // Sign in success, update UI with the signed-in user's information
                                            Log.d(TAG, "signInWithEmail:success");
                                            FirebaseUser user = mAuth.getCurrentUser();

                                            // Get the user's unique ID from Firebase
                                            final String UID = user.getUid();

                                            DatabaseReference firstAddBoolean = database.getReference("FirstAdd");
                                            firstAddBoolean.setValue(true);
                                            database.getReference("players/" + UID + "/isLoggedIn").setValue(true);
                                            firstAddBoolean.setValue(false);
                                            openMainActivity(UID);

                                        } else {
                                            // If sign in fails, display a message to the user.
                                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                                            Toast.makeText(LoginActivity.this, "Sign in authentication failed.",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    }
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        // Check if user is signed in (non-null)
        FirebaseUser currentUser = mAuth.getCurrentUser();
    }

    public void openMainActivity(String id) {
        Log.d(TAG, "openMainActivity: Opening Main Activity...");
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    // Create and upload a player to the Firebase Realtime Database
    private void createPlayer(String id, String name, FirebaseDatabase database) {
        Player p1 = new Player(id, name);
        DatabaseReference firstAddBoolean = database.getReference("FirstAdd");
        firstAddBoolean.setValue(true);
        DatabaseReference myRef = database.getReference("players/" + id);
        myRef.setValue(p1);
        firstAddBoolean.setValue(false);
    }

    // Validate e-mail and password format
    private boolean validateForm(EditText mEmailField, EditText mPasswordField) {
        boolean valid = true;

        String email = mEmailField.getText().toString();
        if (TextUtils.isEmpty(email)) {
            mEmailField.setError("Required.");
            valid = false;
        } else {
            mEmailField.setError(null);
        }

        String password = mPasswordField.getText().toString();
        if (TextUtils.isEmpty(password)) {
            mPasswordField.setError("Required.");
            valid = false;
        } else {
            mPasswordField.setError(null);
        }

        return valid;
    }
}