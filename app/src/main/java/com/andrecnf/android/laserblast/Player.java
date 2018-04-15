package com.andrecnf.android.laserblast;

import java.util.Date;

/**
 * Created by Bruno Alves on 28/03/2018.
 */

public class Player {

    /*Fields*/
    private int id, score;
    private String name;
    private String password;
    //String email;
    private boolean isDead;
    private ThreeDCharact coordinates;
    private ThreeDCharact orientation;
    private Date timestamp;

    /* Constructors */
    public Player(int id_, String name_, String password_){
        id = id_;
        name = name_;
        password = password_;
        isDead = false;
        score = 0;
        coordinates = new ThreeDCharact();
        // coordinates.setTimestamp(timestamp_);
        orientation = new ThreeDCharact();
    }

    public Player(int id_, String name_){
        this(id_, name_, "Bla");
    }

    public Player(){
        this(0, "Bla", "Bla");
    }

    public int getId (){
        return this.id;
    }

    public String getName(){
        return this.name;
    }

    protected String getPassword(){
        return this.password;
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
}