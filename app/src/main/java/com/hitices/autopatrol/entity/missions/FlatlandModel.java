package com.hitices.autopatrol.entity.missions;


import com.amap.api.maps2d.model.LatLng;
import com.hitices.autopatrol.algorithm.FullCoveragePathPlanningAlgorithm;

import java.util.ArrayList;
import java.util.List;

import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;


/**
 * Created by Rhys on 2018/1/11.
 * email: bozliu@outlook.com
 * 针对地形为平面的情况
 */

public class FlatlandModel extends BaseModel {
    private List<LatLng> vertexs = new ArrayList<>();  //polygon 定点集合
    private float altitude;   //飞行高度，高度为面相对于飞行器起点的高度
    private float speed;    //飞行速度
    private float width;   //扫描宽度
    private int OverlapRate; //重叠率

    public FlatlandModel(String name) {
        //base
        missionName = name;
        modelType = ModelType.Flatland;
        cameraAngel = 0;
        headingMode = WaypointMissionHeadingMode.AUTO;
        //flatland
        altitude = 20.0f;
        speed = 9f;
        OverlapRate = 20;
        width = 15;
    }


    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float value) {
        this.speed = value;
    }

    public float getAltitude() {
        return altitude;
    }

    public void setAltitude(float value) {
        this.altitude = value;
    }

    public int getOverlapRate() {
        return OverlapRate;
    }

    public void setOverlapRate(int value) {
        this.OverlapRate = value;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float value) {
        this.width = value;
    }

    public List<LatLng> getVertexs() {
        return vertexs;
    }

    public void setVertexs(List<LatLng> vs) {
        //修改Vertexs，
        vertexs.clear();
        vertexs = vs;
    }

    @Override
    public String getMissionName() {
        return missionName;
    }

    @Override
    public void setMissionName(String missionName) {
        this.missionName = missionName;
    }

    @Override
    public ModelType getModelType() {
        return modelType;
    }

    @Override
    public void setModelType(ModelType modelType) {
        this.modelType = modelType;
    }

    @Override
    public WaypointMissionHeadingMode getHeadingMode() {
        return headingMode;
    }

    @Override
    public void setHeadingMode(WaypointMissionHeadingMode headingMode) {
        this.headingMode = headingMode;
    }

    @Override
    public float getSafeAltitude() {
        return safeAltitude;
    }

    @Override
    public void setSafeAltitude(float safeAltitude) {
        this.safeAltitude = safeAltitude;
    }

    @Override
    public int getCameraAngel() {
        return cameraAngel;
    }

    @Override
    public void setCameraAngel(int cameraAngel) {
        this.cameraAngel = cameraAngel;
    }

    @Override
    public LatLng getEndPoint() {
        return endPoint;
    }

    @Override
    public void setEndPoint(LatLng endPoint) {
        this.endPoint = endPoint;
    }

    @Override
    public LatLng getStartPoint() {
        return startPoint;
    }

    @Override
    public void setStartPoint(LatLng startPoint) {
        this.startPoint = startPoint;
    }

    @Override
    public List<Waypoint> getExecutePoints() {
        return executePoints;
    }

    public void addVertex(LatLng latLng) {
        //添加顶点
        vertexs.add(latLng);
    }

    @Override
    public void generateExecutablePoints(LatLng formerPoint) {
        executePoints = new ArrayList<>();
        //顶点，宽度，速度，航点时间，起点
        FullCoveragePathPlanningAlgorithm algorithm =
                new FullCoveragePathPlanningAlgorithm(vertexs, width, speed, 0, formerPoint);
        List<LatLng> points = algorithm.getShotWaypoints(); //add waypoint in main line to take photo
        //need to set special waypoint action
        startPoint = points.get(0);
        endPoint = points.get(points.size() - 1);
        safeAltitude = altitude;
        for (int i = 0; i < points.size(); i++) {
            Waypoint waypoint = new Waypoint(points.get(i).latitude, points.get(i).longitude, altitude);
            //设置航点动作
            waypoint.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 0));
            executePoints.add(waypoint);
        }
        Waypoint waypoint = executePoints.get(0);
        WaypointAction action = new WaypointAction(WaypointActionType.GIMBAL_PITCH, -cameraAngel);
        waypoint.waypointActions.add(0, action);
    }
}
