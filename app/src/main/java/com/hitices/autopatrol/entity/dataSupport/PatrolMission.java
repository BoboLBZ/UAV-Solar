package com.hitices.autopatrol.entity.dataSupport;

import com.hitices.autopatrol.helper.MissionConstraintHelper;

import org.litepal.annotation.Column;
import org.litepal.crud.DataSupport;

import java.util.Date;
import java.util.List;

/**
 * Created by dusz7 on 20180403.
 */

public class PatrolMission extends DataSupport {

    @Column(unique = true, defaultValue = "unknown")
    private int id;

    // 任务名称
    @Column(defaultValue = "unknown")
    private String name;
    // 其他属性：
    private String filePath;
    @Column(defaultValue = "0")
    private int childNums;
    private Date lastModifiedTime;

    // 外键：巡检任务所在电站
    private PowerStation powerStation;

    // 外建：该任务对应的执行记录
//    private List<FlightRecord> records;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        setFilePath(MissionConstraintHelper.MISSION_DIR + "/" + name + ".xml");
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Date getLastModifiedTime() {
        return lastModifiedTime;
    }

    public void setLastModifiedTime(Date lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }

    public PowerStation getPowerStation() {
        return powerStation;
    }

    public void setPowerStation(PowerStation powerStation) {
        this.powerStation = powerStation;
    }

//    public List<FlightRecord> getRecords() {
//        return records;
//    }
//
//    public void setRecords(List<FlightRecord> records) {
//        this.records = records;
//    }

    public int getChildNums() {
        return childNums;
    }

    public void setChildNums(int nums) {
        this.childNums = nums;
    }
}
