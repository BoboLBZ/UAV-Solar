package com.hitices.autopatrol.entity.missions;


import com.amap.api.maps2d.model.LatLng;
import com.hitices.autopatrol.algorithm.AntColonyAlgorithm;
import com.hitices.autopatrol.helper.MissionConstraintHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;

/**
 * Created by Rhys on 2018/1/11.
 * email: bozliu@outlook.com
 * 航点任务类
 */

public class MultiPointsModel extends BaseModel {
    private final Map<LatLng, Waypoint> waypoints = new ConcurrentHashMap<>(); //map，坐标与航点的对应，方便查找
    private List<Waypoint> waypointList = new ArrayList();  // 航点集合
    private float altitude; //通用高度
    private float speed; //飞行速度

    public MultiPointsModel(String mName) {
        //base
        missionName = mName;
        modelType = ModelType.MultiPoints;
        headingAngle = 0; //默认正北方向
        cameraAngle = 90;
        //multi point
        altitude = 15f;
        speed = 9f;
    }

    public float getAltitude() {
        return altitude;
    }

    public void setAltitude(float altitude) {
        this.altitude = altitude;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public List<Waypoint> getWaypointList() {
        return waypointList;
    }

    public void setWaypointList(List<Waypoint> waypointList) {
        this.waypointList = waypointList;
    }

    public Waypoint getWaypoint(LatLng latLng) {
        return waypointList.get(findWaypoint(latLng));
    }

    public Map<LatLng, Waypoint> getWaypoints() {
        return waypoints;
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

    public int findWaypoint(LatLng latLng) {
        //返回航点索引
        Waypoint waypoint = waypoints.get(latLng);
        return waypointList.indexOf(waypoint);
    }

    public void addPointToList(LatLng latLng) {
        //添加航点，使用系统默认参数
        Waypoint waypoint = new Waypoint(latLng.latitude, latLng.longitude, altitude);
        List<WaypointAction> currentGeneralActions = MissionConstraintHelper.getGeneralWaypointActions();
        for (int i = 0; i < currentGeneralActions.size(); i++) {
            waypoint.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, this.headingAngle));
            waypoint.addAction(currentGeneralActions.get(i));
        }
        waypointList.add(waypoint);
        waypoints.put(latLng, waypoint);
    }

    public void updatePoint(LatLng latLng, float altitude) {
        int i = findWaypoint(latLng);
        Waypoint w = waypointList.get(i);
        w.altitude = altitude;
    }

    public void addWaypointToList(Waypoint waypoint) {
        this.waypointList.add(waypoint);
    }

    public void removeWaypoint(LatLng latLng) {
        //删除航点
        int i = findWaypoint(latLng);
        waypointList.remove(i);
        waypoints.remove(latLng);
    }

    public void changeActionOfAllPoints(int angle) {
        this.headingAngle = angle;
        if (!waypointList.isEmpty()) {
            for (int i = 0; i < waypointList.size(); i++) {
                Waypoint waypoint = waypointList.get(i);
                waypoint.removeAllAction();
                waypoint.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, this.headingAngle));
                waypoint.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 0));
            }
        }

    }
    @Override
    public void generateExecutablePoints(LatLng formerPoint) {
        AntColonyAlgorithm antColonyAlgorithm = new AntColonyAlgorithm(waypointList, formerPoint);
        List<Integer> sortedIndex = antColonyAlgorithm.getSortedWaypoints();
        executePoints = new ArrayList<>();
        //built new waypoint list,include convert
        for (int i = 0; i < sortedIndex.size(); i++) {
            executePoints.add(waypointList.get(sortedIndex.get(i)));
        }
        safeAltitude = findMaxAltitude() + 1;
        int index = executePoints.size();
        Waypoint w = executePoints.get(index - 1);
        endPoint = new LatLng(w.coordinate.getLatitude(), w.coordinate.getLongitude());
        w = executePoints.get(0);
        startPoint = new LatLng(w.coordinate.getLatitude(), w.coordinate.getLongitude());
        //以安全高度进入子任务区域
        Waypoint waypoint = new Waypoint(startPoint.latitude, startPoint.longitude, safeAltitude);
        WaypointAction action = new WaypointAction(WaypointActionType.GIMBAL_PITCH, -cameraAngle);
        waypoint.addAction(action);
        executePoints.add(0, waypoint);
        //以安全高度离开子任务区域
        executePoints.add(new Waypoint(endPoint.latitude, endPoint.longitude, safeAltitude));
    }

    private float findMaxAltitude() {
        float tempMax = altitude;
        for (int i = 0; i < waypointList.size(); i++) {
            if (waypointList.get(i).altitude > tempMax) {
                tempMax = waypointList.get(i).altitude;
            }
        }
        return tempMax;
    }
}
