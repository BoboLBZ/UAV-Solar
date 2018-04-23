package com.hitices.autopatrol.entity.missions;

import com.amap.api.maps2d.model.LatLng;
import com.hitices.autopatrol.algorithm.Point;
import com.hitices.autopatrol.algorithm.SlopePathPlanningAlgorithm;

import java.util.ArrayList;
import java.util.List;

import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;

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
        this.cameraAngel = 90;
        this.headingMode = WaypointMissionHeadingMode.USING_INITIAL_DIRECTION;
        //slope land
        this.speed = 6;
        this.distanceToPanel = 10;
        this.width = 15;
        this.overlapRate = 20;
        this.altitude = 2;
    }

    @Override
    public void generateExecutablePoints(LatLng formerPoint) {
        executePoints = new ArrayList<>();
        LatLng a = new LatLng(baselineA.coordinate.getLatitude(), baselineA.coordinate.getLongitude());
        LatLng b = new LatLng(baselineB.coordinate.getLatitude(), baselineB.coordinate.getLongitude());
        SlopePathPlanningAlgorithm algorithm = new SlopePathPlanningAlgorithm(vertexs, width, a, b);
        List<Point> points = algorithm.generateWaypoints(baselineA.altitude, baselineB.altitude);

        //convert to waypoint
        int size = points.size();
        startPoint = new LatLng(points.get(0).getX(), points.get(0).getY());
        endPoint = new LatLng(points.get(size - 1).getX(), points.get(size - 1).getY());
        safeAltitude = (float) points.get(size - 1).getArCos() + altitude + distanceToPanel;
        //以安全高度进入斜面区域，并调整云台角度
        Waypoint w = new Waypoint(startPoint.latitude, startPoint.longitude, safeAltitude);
        w.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH, -cameraAngel));
        executePoints.add(w);
        for (int i = 0; i < points.size(); i++) {
            Point point = points.get(i);
            double alt = point.getArCos() + altitude + distanceToPanel;
            Waypoint waypoint = new Waypoint(point.getX(), point.getY(), (float) alt);
            waypoint.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 0));
            executePoints.add(waypoint);
        }

    }
    public void addVertex(LatLng latLng) {
        //添加顶点
        vertexs.add(latLng);
    }

    public void setAltitude(float altitude) {
        this.altitude = altitude;
    }

    public float getAltitude() {
        return altitude;
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
}
