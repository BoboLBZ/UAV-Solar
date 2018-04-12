package com.hitices.autopatrol.entity.dataSupport;

import org.litepal.annotation.Column;
import org.litepal.crud.DataSupport;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by Rhys on 2018/3/26.
 * email: bozliu@outlook.com
 */

public class FlightRecord extends DataSupport implements Serializable {
    @Column(unique = true, defaultValue = "unknown")
    private int id;

    // 本次执行时间
    private Date startTime;
    private Date endTime;
    // 包含图集
    private boolean hasVisible;
    private boolean hasInfrared;
    // 是否下载
    private boolean isDownload;

    // 外建：所执行的任务
    private PatrolMission mission;

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
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

    public PatrolMission getMission() {
        return mission;
    }

    public void setMission(PatrolMission mission) {
        this.mission = mission;
    }

    public PatrolMission getExecuteMission() {
        return DataSupport.find(FlightRecord.class, id, true).getMission();
    }
}
