package com.hitices.autopatrol.entity.missions;

import com.amap.api.maps2d.model.LatLng;
import com.hitices.autopatrol.algorithm.Point;
import com.hitices.autopatrol.algorithm.SlopePathPlanningAlgorithm;
import com.hitices.autopatrol.helper.MissionConstraintHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;

/**
 * Created by Rhys on 2018/4/4.
 * email: bozliu@outlook.com
 * 针对地形为斜面的情况
 */
public class SlopeModel extends BaseModel {
    private List<LatLng> vertexs = new ArrayList<>();
    private float speed;
    private float distanceToPanel;//这个高度是拍照点到光伏板的距离
    private float width;
    private int overlapRate;
    private float altitude; //斜面最低点到起飞点的垂直高度
    private Waypoint baselineA, baselineB;   //标识斜面走向,A低点，B高点
    public SlopeModel(String name) {
        //base
        this.missionName = name;
        this.modelType = ModelType.Slope;
        this.cameraAngle = MissionConstraintHelper.getDefaultCameraAngle();
        this.headingAngle = MissionConstraintHelper.getDefaultHeading();
        //slope land
        this.speed = MissionConstraintHelper.getDefaultSpeed();
        this.distanceToPanel = MissionConstraintHelper.getDefaultDistanceToPanel();
        this.width = MissionConstraintHelper.getDefaultWidth();
        this.overlapRate = MissionConstraintHelper.getDefaultOverlapRate();
        this.altitude = 2;
    }

    @Override
    public void generateExecutablePoints(LatLng formerPoint) {
        executePoints = new ArrayList<>();
        float actualWidth = width * (1 - this.overlapRate / 100f);
        LatLng a = new LatLng(baselineA.coordinate.getLatitude(), baselineA.coordinate.getLongitude());
        LatLng b = new LatLng(baselineB.coordinate.getLatitude(), baselineB.coordinate.getLongitude());
        SlopePathPlanningAlgorithm algorithm = new SlopePathPlanningAlgorithm(vertexs, actualWidth, a, b);
        List<Point> points = algorithm.generateWaypoints(baselineA.altitude, baselineB.altitude);

        //convert to waypoint
        int size = points.size();
        startPoint = new LatLng(points.get(0).getX(), points.get(0).getY());
        endPoint = new LatLng(points.get(size - 1).getX(), points.get(size - 1).getY());
        safeAltitude = (float) points.get(size - 1).getArCos() + altitude + distanceToPanel;
        //以安全高度进入斜面区域，并调整云台角度
        Waypoint w = new Waypoint(startPoint.latitude, startPoint.longitude, safeAltitude);
        w.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH, -cameraAngle));
        w.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, this.headingAngle));
        executePoints.add(w);
        for (int i = 0; i < points.size(); i++) {
            Point point = points.get(i);
            double alt = point.getArCos() + altitude + distanceToPanel;
            Waypoint waypoint = new Waypoint(point.getX(), point.getY(), (float) alt);
            //waypoint.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, this.headingAngle));
            waypoint.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 0));
            executePoints.add(waypoint);
        }
//        Waypoint w=executePoints.get(0);

    }
    public int addVertex(LatLng latLng) {
        //添加顶点
        vertexs.add(latLng);
        return vertexs.size() - 1;
    }
    public void setVertex(int index, LatLng latLng) {
        vertexs.set(index, latLng);
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

    public Waypoint getBaselineA() {
        return baselineA;
    }

    public void setBaselineA(Waypoint baselineA) {
        this.baselineA = baselineA;
    }

    public Waypoint getBaselineB() {
        return baselineB;
    }

    public void setBaselineB(Waypoint baselineB) {
        this.baselineB = baselineB;
    }

    public float getDistanceToPanel() {
        return distanceToPanel;
    }

    public void setDistanceToPanel(float distanceToPanel) {
        this.distanceToPanel = distanceToPanel;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public int getOverlapRate() {
        return overlapRate;
    }

    public void setOverlapRate(int overlapRate) {
        this.overlapRate = overlapRate;
    }

    public List<LatLng> getVertexs() {
        return vertexs;
    }

    public void setVertexs(List<LatLng> vertexs) {
        this.vertexs = vertexs;
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

    public List<Waypoint> getAdjustPoints() {
        List<Waypoint> points = new ArrayList<>();
        float altitude = baselineB.altitude + distanceToPanel;
        points.add(new Waypoint(baselineA.coordinate.getLatitude(), baselineA.coordinate.getLongitude(), altitude));
        points.add(new Waypoint(baselineB.coordinate.getLatitude(), baselineB.coordinate.getLongitude(), altitude));
        for (int i = 0; i < vertexs.size(); i++) {
            points.add(new Waypoint(vertexs.get(i).latitude, vertexs.get(i).longitude, altitude));
        }
        return points;
    }

    public boolean updateDroneLocation(int index, LatLng latLng) {
        //index:0:low;1,high;
        if (index < 0 || index >= vertexs.size() + 2) {
            return false;
        } else {
            switch (index) {
                case 0:
                    float altA = baselineA.altitude;
                    baselineA = new Waypoint(latLng.latitude, latLng.longitude, altA);
                    break;
                case 1:
                    float altB = baselineB.altitude;
                    baselineB = new Waypoint(latLng.latitude, latLng.longitude, altB);
                    break;
                default:
                    vertexs.add(latLng);
                    Collections.swap(vertexs, index - 2, vertexs.size() - 1);
                    vertexs.remove(vertexs.size() - 1);
                    break;
            }
            return true;
        }
    }

    public boolean updateAltitude(int index, float alt) {
        //index:0:low;1,high;
        if (index < 0 || index >= vertexs.size() + 2) {
            return false;
        } else {
            switch (index) {
                case 0:
                    baselineA.altitude = alt;
                    break;
                case 1:
                    baselineB.altitude = alt;
                    break;
                default:
                    this.altitude = alt;
                    break;
            }
            return true;
        }
    }
}
