package com.hitices.autopatrol.entity;

import org.litepal.annotation.Column;
import org.litepal.crud.DataSupport;

import java.util.Date;

/**
 * Created by Rhys on 2018/3/26.
 * email: bozliu@outlook.com
 */

public class FlightRecord extends DataSupport {
    @Column(unique = true, defaultValue = "unknown")
    private int id;
    private String name;
    private String location;
    private Date startTime;
    private Date endTime;
//    private boolean isDistributed;
    private boolean hasVisible;
    private boolean hasInfrared;
    private boolean isDownload;

    public String getName() {
        return name;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStartTime(Date time) {
        this.startTime = time;
    }

    public void setEndTime(Date time) {
        this.endTime = time;
    }

    public void setId(int i) {
        this.id = i;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

//    public boolean isDistributed() {
//        return isDistributed;
//    }

//    public void setDistributed(boolean distributed) {
//        isDistributed = distributed;
//    }

    public boolean isHasVisible() {
        return hasVisible;
    }

    public void setHasVisible(boolean hasVisible) {
        this.hasVisible = hasVisible;
    }

    public boolean isHasInfrared() {
        return hasInfrared;
    }

    public void setHasInfrared(boolean hasInfrared) {
        this.hasInfrared = hasInfrared;
    }

    public int getId() {
        return id;
    }

    public boolean isDownload() {
        return isDownload;
    }

    public void setDownload(boolean download) {
        isDownload = download;
    }
}
