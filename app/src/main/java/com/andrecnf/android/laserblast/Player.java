package com.andrecnf.android.laserblast;

import java.util.Date;

/**
 * Created by Bruno Alves on 28/03/2018.
 */

public class Player {

    /*Fields*/
    private int score;
    private String name, id;
    private boolean isDead;
    private boolean isLoggedIn;
    private ThreeDCharact coordinates;
    private ThreeDCharact orientation;

    /* Constructors */
    public Player(String id_, String name_){
        id = id_;
        name = name_;
        isDead = false;
        score = 0;
        coordinates = new ThreeDCharact();
        // coordinates.setTimestamp(timestamp_);
        orientation = new ThreeDCharact();
        isLoggedIn = true;
    }

    public Player(){
        this("-1", "DummyPlayer");
    }

    public String getName(){
        return this.name;
    }

    public int getScore(){
        return this.score;
    }

    public boolean isDead(){
        return this.isDead;
    }

    public void setDead(boolean dead){
        isDead = dead;
    }

    public ThreeDCharact getCoordinates() {
        return this.coordinates;
    }

    public ThreeDCharact getOrientation() {
        return this.orientation;
    }

    public void setCoordinates(ThreeDCharact coordinate) {
        this.coordinates = coordinate;
    }

    public void setOrientation(ThreeDCharact orientation) {
        this.orientation = orientation;
    }

    public boolean getIsLoggedIn(){
        return this.isLoggedIn;
    }

    public String getId() {
        return id;
    }
}