package com.hitices.autopatrol.entity.missions;

import com.amap.api.maps2d.model.LatLng;

import dji.common.mission.waypoint.WaypointMissionHeadingMode;

/**
 * Created by Rhys on 2018/1/28.
 * email: bozliu@outlook.com
 * BaseModel为基类，定义公用参数和接口
 */

public abstract class BaseModel {
    protected String missionName; //任务名称
    protected ModelType modelType; //任务类型
    protected WaypointMissionHeadingMode headingMode;
    protected int cameraAngel;
    protected LatLng startPoint, lastPoint;//
    protected float safeAltitude;//安全高度，使用该安全高度进入任务

    public BaseModel(String name, ModelType type) {
        //初始化，需指明 名称和类型
        this.missionName = name;
        this.modelType = type;
    }

    public BaseModel() {
    }

    public String getMissionName() {
        return missionName;
    }

    public void setMissionName(String missionName) {
        this.missionName = missionName;
    }

    public ModelType getModelType() {
        return modelType;
    }

    public void setModelType(ModelType modelType) {
        this.modelType = modelType;
    }

    public WaypointMissionHeadingMode getHeadingMode() {
        return headingMode;
    }

    public void setHeadingMode(WaypointMissionHeadingMode headingMode) {
        this.headingMode = headingMode;
    }

    public float getSafeAltitude() {
        return safeAltitude;
    }

    public void setSafeAltitude(float safeAltitude) {
        this.safeAltitude = safeAltitude;
    }

    public int getCameraAngel() {
        return cameraAngel;
    }

    public void setCameraAngel(int cameraAngel) {
        this.cameraAngel = cameraAngel;
    }

    public LatLng getLastPoint() {
        return lastPoint;
    }

    public void setLastPoint(LatLng lastPoint) {
        this.lastPoint = lastPoint;
    }

    public LatLng getStartPoint() {
        return startPoint;
    }

    public void setStartPoint(LatLng startPoint) {
        this.startPoint = startPoint;
    }

}
