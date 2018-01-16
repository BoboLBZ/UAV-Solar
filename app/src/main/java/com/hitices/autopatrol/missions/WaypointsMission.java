package com.hitices.autopatrol.missions;


import android.util.Log;
import android.widget.Toast;

import com.amap.api.maps2d.model.LatLng;
import com.hitices.autopatrol.AutoPatrolApplication;

import java.io.File;
import java.io.IOException;
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
    private final Map<Waypoint,WaypointActions> mWaypointActions=new ConcurrentHashMap<>();
    private final Map<LatLng,Waypoint> waypoints=new ConcurrentHashMap<>();
    public WaypointMission.Builder builder;
    public WaypointsMission(String mName){
        missionName=mName;
        date=new Date();
        altitude=50.0f;
        speed=10.0f;
        finishedAction=WaypointMissionFinishedAction.GO_HOME;
        headingMode=WaypointMissionHeadingMode.USING_WAYPOINT_HEADING;
        builder=new WaypointMission.Builder();
    }
    public boolean saveMisson(){
        Log.e("rhys","in save class");
        File dir=new File(AutoPatrolApplication.missionDir);
        if(!dir.exists()){
            if(!dir.mkdirs()){
                Log.e("rhys","dirs failed");
                return false;
            }
        }
        File newMission=new File(AutoPatrolApplication.missionDir+"/"+missionName+".xml");
        try {
            Log.e("rhys",newMission.getName().toString());
            newMission.createNewFile();
        }catch (IOException e){
            e.printStackTrace();
            return false;
        }
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
    public int findWaypoint(LatLng latLng){
        Waypoint waypoint=waypoints.get(latLng);
        return waypointList.indexOf(waypoint);
    }
    public void addWaypointList(LatLng latLng){
        Waypoint waypoint=new Waypoint(latLng.latitude,latLng.longitude,altitude);
        waypointList.add(waypoint);
        waypoints.put(latLng,waypoint);
    }
    public void removeWaypoint(LatLng latLng){
        int i=findWaypoint(latLng);
        Waypoint waypoint=waypointList.get(i);
        waypointList.remove(i);
        waypoints.remove(latLng);
        mWaypointActions.remove(waypoint);
    }
    public void initAllWaypoint(){

    }
    public void singleWaypointsetting(){

    }
    private class WaypointActions{
        public List<WaypointAction> waypointActions=new ArrayList<>();
        WaypointActions(List<WaypointAction> waypointActions){
            this.waypointActions=waypointActions;
        }
    }
}
