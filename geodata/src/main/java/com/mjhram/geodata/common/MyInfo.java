package com.mjhram.geodata.common;

import android.location.Location;

/**
 * Created by mohammad.haider on 10/3/2015.
 */
public class MyInfo {
    public Location loc;
    public int avState;
    public boolean updateStateOnly;

    public MyInfo(Location curLoc) {
        loc = curLoc;
        avState = Session.availabilityState;
    }

    public void update() {
        avState = Session.availabilityState;
    }
}
