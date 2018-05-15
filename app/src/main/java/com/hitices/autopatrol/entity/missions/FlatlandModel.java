package com.hitices.autopatrol.entity.missions;


import com.amap.api.maps2d.model.LatLng;
import com.hitices.autopatrol.algorithm.FullCoveragePathPlanningAlgorithm;
import com.hitices.autopatrol.helper.MissionConstraintHelper;

import java.util.ArrayList;
import java.util.List;

import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;


/**
 * Created by Rhys on 2018/1/11.
 * email: bozliu@outlook.com
 * 针对地形为平面的情况
 */

public class FlatlandModel extends BaseModel {
    private List<LatLng> vertexs = new ArrayList<>();  //polygon 定点集合
    private float altitude;   //飞行高度，高度为面相对于飞行器起点的高度
    private float distanceToPanel;
    private float speed;    //飞行速度
    private float width;   //扫描宽度
    private int overlapRate; //重叠率

    public FlatlandModel(String name) {
        //base
        missionName = name;
        modelType = ModelType.Flatland;
        cameraAngle = MissionConstraintHelper.getDefaultCameraAngle();
        headingAngle = MissionConstraintHelper.getDefaultHeading();
        //flatland
        altitude = 20.0f;
        distanceToPanel=MissionConstraintHelper.getDefaultDistanceToPanel();
        speed = MissionConstraintHelper.getDefaultSpeed();
        overlapRate = MissionConstraintHelper.getDefaultOverlapRate();
        width = MissionConstraintHelper.getDefaultWidth();
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
        return overlapRate;
    }

    public void setOverlapRate(int value) {
        this.overlapRate = value;
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

    public float getDistanceToPanel() {
        return distanceToPanel;
    }

    public void setDistanceToPanel(float distanceToPanel) {
        this.distanceToPanel = distanceToPanel;
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
    public int getHeadingAngle() {
        return headingAngle;
    }

    @Override
    public void setHeadingAngle(int headingAngle) {
        this.headingAngle = headingAngle;
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
    public int getCameraAngle() {
        return cameraAngle;
    }

    @Override
    public void setCameraAngle(int cameraAngle) {
        this.cameraAngle = cameraAngle;
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

    public boolean adjustVertes(int index, LatLng latLng) {
        if (index >= 0 && index < vertexs.size()) {
            vertexs.remove(index);
            vertexs.add(index, latLng);
            return true;
        } else {
            return false;
        }
    }
    @Override
    public void generateExecutablePoints(LatLng formerPoint) {
        executePoints = new ArrayList<>();
        //顶点，宽度，速度，航点时间，起点
        float actualWidth = width * (1 - this.overlapRate / 100f);
        FullCoveragePathPlanningAlgorithm algorithm =
                new FullCoveragePathPlanningAlgorithm(vertexs, actualWidth, speed, 0, formerPoint);
        //use my point
        List<LatLng> points = algorithm.getShotWaypoints(); //add waypoint in main line to take photo
        //need to set special waypoint action
        //use distance interval
//        List<LatLng> points = algorithm.getPlanningWaypoints();
//        startPoint = points.get(0);
        endPoint = points.get(points.size() - 1);
        safeAltitude = altitude+distanceToPanel;
        for (int i = 0; i < points.size(); i++) {
            Waypoint waypoint = new Waypoint(points.get(i).latitude, points.get(i).longitude, altitude+distanceToPanel);
            //设置航点动作
            //waypoint.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, this.headingAngle));
            waypoint.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 0));
            executePoints.add(waypoint);
        }
        Waypoint waypoint = executePoints.get(0);
        WaypointAction action = new WaypointAction(WaypointActionType.GIMBAL_PITCH, -cameraAngle);
        waypoint.waypointActions.add(0, action);
        waypoint.waypointActions.add(0,new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, this.headingAngle));
    }

    public List<Waypoint> getAdjustPoints() {
        List<Waypoint> points = new ArrayList<>();
        for (int i = 0; i < vertexs.size(); i++) {
            Waypoint waypoint = new Waypoint(vertexs.get(i).latitude, vertexs.get(i).longitude, altitude + distanceToPanel);
//            waypoint.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, this.headingAngle));
//            waypoint.addAction(new WaypointAction(WaypointActionType.))
            points.add(waypoint);
        }
        Waypoint waypoint = points.get(0);
        waypoint.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, this.headingAngle));
        waypoint.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH, -this.cameraAngle));
        return points;
    }
}
