package com.hitices.autopatrol.missions;

/**
 * Created by Rhys on 2018/1/28.
 * email: bozliu@outlook.com
 */

public abstract class BaseMission {
    public  String missionName;
    public MissionType missionType;
    public boolean FLAG_ISSAVED;
    public BaseMission(String name,MissionType type){
        this.missionName=name;
        this.missionType=type;
    }

    public BaseMission() {
    }
    public abstract boolean saveMission();
}
