package com.hitices.autopatrol.missions;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;

/**
 * Created by Rhys on 2018/1/11.
 */

public class WaypointsMission extends Object {
    public String missionName;
    public Date date;
    //mission
    public List<Waypoint> waypointList=new ArrayList();
    public float altitude;
    public float speed;
    public WaypointMissionFinishedAction finishedAction;
    public WaypointMissionHeadingMode headingMode;
    public final Map<Waypoint,WaypointActions> mWaypointActions=new ConcurrentHashMap<>();
    public WaypointMission.Builder builder;
    public WaypointsMission(String name){
        missionName=name;
        date=new Date();
        altitude=50.0f;
        speed=10.0f;
        finishedAction=WaypointMissionFinishedAction.GO_HOME;
        headingMode=WaypointMissionHeadingMode.USING_WAYPOINT_HEADING;
        builder=new WaypointMission.Builder();
    }
    public boolean saveMisson(){
        return true;
    }
    public WaypointMission.Builder getMissionBuilder(){
        if(builder == null) {
            builder=new WaypointMission.Builder();
        }
        builder.waypointList(waypointList);
        builder.waypointCount(waypointList.size());
        builder.autoFlightSpeed(speed);
        builder.maxFlightSpeed(speed);
        builder.finishedAction(finishedAction);
        builder.headingMode(headingMode);
        builder.flightPathMode(WaypointMissionFlightPathMode.NORMAL);
        return builder;
    }
    public void addWaypointActions(Waypoint waypoint,List<WaypointAction> waypointActions){
        mWaypointActions.put(waypoint,new WaypointActions(waypointActions));
    }
    private class WaypointActions{
        public List<WaypointAction> waypointActions=new ArrayList<>();
        WaypointActions(List<WaypointAction> waypointActions){
            this.waypointActions=waypointActions;
        }
    }
}
