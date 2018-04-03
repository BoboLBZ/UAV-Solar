package com.hitices.autopatrol.entity.dataSupport;

import com.hitices.autopatrol.entity.dataSupport.PatrolMission;

import org.litepal.annotation.Column;
import org.litepal.crud.DataSupport;

import java.util.List;

/**
 * Created by dusz7 on 20180403.
 */

public class PowerStation extends DataSupport {

    @Column(unique = true, defaultValue = "unknown")
    private int id;

    // 电站名称
    private String name;
    // 电站位置：待实现
    private String location;
    // 封面图片：待实现
    private String thumbUrl;

    // 外建：在本电站对应的任务
    private List<PatrolMission> missions;

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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getThumbUrl() {
        return thumbUrl;
    }

    public void setThumbUrl(String thumbUrl) {
        this.thumbUrl = thumbUrl;
    }

    public List<PatrolMission> getMissions() {
        return missions;
    }

    public void setMissions(List<PatrolMission> missions) {
        this.missions = missions;
    }
}
