package com.hitices.autopatrol.helper;

import org.litepal.annotation.Column;
import org.litepal.crud.DataSupport;

import java.util.Date;

/**
 * Created by Rhys on 2018/3/26.
 * email: bozliu@outlook.com
 */

public class FlightRecords extends DataSupport {
    @Column(unique = true, defaultValue = "unknown")
    private int id;
    private String name;
    private Date startTime;
    private Date endTime;

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
}
