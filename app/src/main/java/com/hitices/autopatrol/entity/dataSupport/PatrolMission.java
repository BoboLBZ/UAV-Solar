package com.hitices.autopatrol.entity.dataSupport;

import org.litepal.annotation.Column;
import org.litepal.crud.DataSupport;

import java.util.List;

/**
 * Created by dusz7 on 20180403.
 */

public class PatrolMission extends DataSupport {

    @Column(unique = true, defaultValue = "unknown")
    private int id;

    // 任务名称
    private String name;
    // 其他属性：

    // 外键：巡检任务所在电站
    private PowerStation powerStation;

    // 外建：该任务对应的执行记录
    private List<FlightRecord> records;

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
    }

    public PowerStation getPowerStation() {
        return powerStation;
    }

    public void setPowerStation(PowerStation powerStation) {
        this.powerStation = powerStation;
    }

    public List<FlightRecord> getRecords() {
        return records;
    }

    public void setRecords(List<FlightRecord> records) {
        this.records = records;
    }
}
