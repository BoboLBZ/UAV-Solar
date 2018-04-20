package com.hitices.autopatrol.helper;

import android.os.Environment;

import java.util.ArrayList;
import java.util.List;

import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;

/**
 * Created by Rhys on 2018/4/4.
 * email: bozliu@outlook.com
 * save some constraint on the uav,such as speed,altitude
 */
public class MissionConstraintHelper {
    public static final String MISSION_DIR = Environment.getExternalStorageDirectory().getPath() + "/AutoPatrol/Missions";  //默认任务保存位置
    public static final String PHOTO_DIR = Environment.getExternalStorageDirectory().getPath() + "/AutoPatrol/RawData";  //默认照片保存位置
    public static final String MISSION_PHOTO_DIR = Environment.getExternalStorageDirectory().getPath() + "/AutoPatrol/MissionPhoto";  //任务采集照片保存位置
    private static float maxSpeed = 10f;
    private static float maxAltitude = 500f;
    private static int maxFlatlandOverRate = 50;
    private static int maxFlatlandWidth = 30;
    private static int MaxShotDistance = 30;
    private static int MinShotDistance = 5;
    private static int defaultReturnHomeAltitude = 30;
    private static int maxPitch = 90;

    public static List<WaypointAction> getGeneralWaypointActions() {
        List<WaypointAction> actions = new ArrayList<>();
        actions.add(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 0));
        return actions;
    }

    public static float getMaxAltitude() {
        return maxAltitude;
    }

    public static float getMaxSpeed() {
        return maxSpeed;
    }

    public static int getMaxFlatlandOverRate() {
        return maxFlatlandOverRate;
    }

    public static int getMaxFlatlandWidth() {
        return maxFlatlandWidth;
    }

    public static int getMaxPitch() {
        return maxPitch;
    }

    public static int getMaxShotDistance() {
        return MaxShotDistance;
    }

    public static int getMinShotDistance() {
        return MinShotDistance;
    }

    public static int getDefaultReturnHomeAltitude() {
        return defaultReturnHomeAltitude;
    }
}
