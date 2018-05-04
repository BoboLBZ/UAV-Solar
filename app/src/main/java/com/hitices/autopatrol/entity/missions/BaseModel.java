package com.hitices.autopatrol.entity.missions;

import com.amap.api.maps2d.model.LatLng;

import java.util.List;

import dji.common.mission.waypoint.Waypoint;

/**
 * Created by Rhys on 2018/1/28.
 * email: bozliu@outlook.com
 * BaseModel为基类，定义公用参数和接口
 */

public abstract class BaseModel {
    protected String missionName; //任务名称
    protected ModelType modelType; //任务类型
    protected int headingAngle;
    protected int cameraAngle;
    protected LatLng startPoint, endPoint;//use to connect models,does not need to write to file
    protected float safeAltitude;//安全高度，使用该安全高度进入任务

    protected List<Waypoint> executePoints;

    public BaseModel(String name, ModelType type) {
        //初始化，需指明 名称和类型
        this.missionName = name;
        this.modelType = type;
    }

    public BaseModel() {
    }

    public abstract   String getMissionName();

    public abstract void setMissionName(String missionName);

    public abstract ModelType getModelType();

    public abstract void setModelType(ModelType modelType);

    public abstract int getHeadingAngle();

    public abstract void setHeadingAngle(int headingAngle);

    public abstract float getSafeAltitude();

    public abstract void setSafeAltitude(float safeAltitude);

    public abstract int getCameraAngle();

    public abstract void setCameraAngle(int cameraAngle);

    public abstract LatLng getEndPoint();

    public abstract void setEndPoint(LatLng endPoint);

    public abstract LatLng getStartPoint();

    public abstract void setStartPoint(LatLng startPoint);

    public abstract List<Waypoint> getExecutePoints();

    public abstract void generateExecutablePoints(LatLng formerPoint);
}
