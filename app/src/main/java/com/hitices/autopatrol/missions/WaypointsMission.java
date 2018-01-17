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
import dji.common.mission.waypoint.WaypointActionType;
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
    private final Map<LatLng,Waypoint> waypoints=new ConcurrentHashMap<>();
    private final List<WaypointAction> defaultActions=new ArrayList<>();
    public WaypointMission.Builder builder;
    public WaypointsMission(String mName){
        missionName=mName;
        date=new Date();
        altitude=50.0f;
        speed=10.0f;
        finishedAction=WaypointMissionFinishedAction.GO_HOME;
        headingMode=WaypointMissionHeadingMode.USING_WAYPOINT_HEADING;
        //init actions
        defaultActions.add(new WaypointAction(WaypointActionType.START_TAKE_PHOTO,0));
        defaultActions.add(new WaypointAction(WaypointActionType.START_RECORD,1));
        defaultActions.add(new WaypointAction(WaypointActionType.CAMERA_FOCUS,2));
        defaultActions.add(new WaypointAction(WaypointActionType.CAMERA_ZOOM,3));
        defaultActions.add(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT,4));
        defaultActions.add(new WaypointAction(WaypointActionType.GIMBAL_PITCH,5));
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
    public WaypointsMission loadMission(String path){
        return new WaypointsMission(path);
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
    public int findWaypoint(LatLng latLng){
        Waypoint waypoint=waypoints.get(latLng);
        return waypointList.indexOf(waypoint);
    }
    public Waypoint getWaypoint(LatLng latLng){
        return waypointList.get(findWaypoint(latLng));
    }
    public void addWaypointList(LatLng latLng){
        Waypoint waypoint=new Waypoint(latLng.latitude,latLng.longitude,altitude);
        for(int i=0;i<defaultActions.size();i++)
            waypoint.addAction(defaultActions.get(i));
        waypointList.add(waypoint);
        waypoints.put(latLng,waypoint);
    }
    public void removeWaypoint(LatLng latLng){
        int i=findWaypoint(latLng);
        waypointList.remove(i);
        waypoints.remove(latLng);
    }
    public void initAllWaypoint(){

    }
    public void singleWaypointsetting(){

    }

}
