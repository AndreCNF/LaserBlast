package com.andrecnf.android.laserblast;

import java.util.Date;

/**
 * Created by AndreCNF on 02/04/2018.
 */

public abstract class ThreeDCharact {
    protected double x;
    protected double y;
    protected double z;
    protected Date timestamp;

    ThreeDCharact(){
        x = 0;
        y = 0;
        z = 0;
        timestamp = null;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
