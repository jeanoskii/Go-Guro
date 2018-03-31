package com.upou.jeano.goguro.HistoryRecyclerView;

/**
 * Created by Jeano on 16/03/2018.
 */

public class HistoryObject {
    private String rideId;
    private String time;
    private String name;

    public HistoryObject(String rideId, String time, String name) {
        this.rideId = rideId;
        this.time = time;
        this.name = name;
    }

    public String getRideId() {
        return rideId;
    }

    public void setRideId(String rideId) {
        this.rideId = rideId;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
