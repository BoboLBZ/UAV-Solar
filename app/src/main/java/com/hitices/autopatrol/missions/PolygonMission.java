package com.hitices.autopatrol.missions;


import android.util.Log;

import com.amap.api.maps2d.model.LatLng;
import com.hitices.autopatrol.AutoPatrolApplication;
import com.hitices.autopatrol.algorithm.FullCoveragePathPlanningAlgorithm;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.midware.data.model.P3.DataFlycUploadWayPointMissionMsg;


/**
 * Created by Rhys on 2018/1/11.
 * email: bozliu@outlook.com
 * 区域任务类
 *
 */

public class PolygonMission extends BaseMission {
    private List<LatLng> vertexs=new ArrayList<>();  //polygon 定点集合
    private PolygonScenario scenario;  //任务场景
    private float altitude,speed;   //飞行高度和速度
    private int horizontalOverlapRate,verticalOverlapRate; //重叠率，horizontal 指主航线上的照片重叠率，vertical指主航线间的重叠率
    private  static WaypointMission.Builder builder;
    public List<Waypoint> waypointList;
    public PolygonMission(String name){
        //使用默认参数，后续根据类型做不同初始化
        missionName =name;
        missionType=MissionType.PolygonMission;
        scenario=PolygonScenario.TYPEA;
        altitude= 30.0f;
        speed=10f;
        horizontalOverlapRate=70;
        verticalOverlapRate=70;
        FLAG_ISSAVED=false;
    }
    @Override
    public boolean saveMission(){
        //保存任务到xml文件
        File dir=new File(AutoPatrolApplication.missionDir);
        if(!dir.exists()){
            if(!dir.mkdirs()){
                Log.e("rhys","dirs failed");
                FLAG_ISSAVED=false;
                return false;
            }
        }
        try{
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            // root elements 表示任务类型，读取时判断
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("PolygonMission");
            doc.appendChild(rootElement);
            //general parameter
            Element eName=doc.createElement("missionName");
            eName.appendChild(doc.createTextNode(missionName));
            rootElement.appendChild(eName);

            Element eSpeed=doc.createElement("speed");
            eSpeed.appendChild(doc.createTextNode(String.valueOf(speed)));
            rootElement.appendChild(eSpeed);

            Element eAltitude=doc.createElement("altitude");
            eAltitude.appendChild(doc.createTextNode(String.valueOf(altitude)));
            rootElement.appendChild(eAltitude);

            Element eScenario=doc.createElement("scenario");
            eScenario.appendChild(doc.createTextNode(scenario.name()));
            rootElement.appendChild(eScenario);

            Element eHorRate=doc.createElement("horizontalOverlapRate");
            eHorRate.appendChild(doc.createTextNode(String.valueOf(horizontalOverlapRate)));
            rootElement.appendChild(eHorRate);

            Element eVerRate=doc.createElement("verticalOverlapRate");
            eVerRate.appendChild(doc.createTextNode(String.valueOf(verticalOverlapRate)));
            rootElement.appendChild(eVerRate);

            // vertex elements
            Element eVertexs = doc.createElement("Vertexs");
            eVertexs.setAttribute("nums",String.valueOf(vertexs.size()));
            rootElement.appendChild(eVertexs);
            for(int i=0;i<vertexs.size();i++){
                LatLng latLng=vertexs.get(i);
                Element eVertex = doc.createElement("vertex");
                eVertex.setAttribute("id",String.valueOf(i));
                //lat & lng
                Element lat=doc.createElement("latitude");
                lat.appendChild(doc.createTextNode(String.valueOf(latLng.latitude)));
                eVertex.appendChild(lat);
                Element lng=doc.createElement("longitude");
                lng.appendChild(doc.createTextNode(String.valueOf(latLng.longitude)));
                eVertex.appendChild(lng);

                eVertexs.appendChild(eVertex);
            }
            // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(AutoPatrolApplication.missionDir+"/"+missionName+".xml"));
            StreamResult result1 = new StreamResult(System.out); //for test
            transformer.transform(source, result);
            transformer.transform(source, result1); //for test
            FLAG_ISSAVED=true;
        }catch (ParserConfigurationException pce) {
            pce.printStackTrace();
            FLAG_ISSAVED=false;
            return false;
        } catch (TransformerException tfe) {
            tfe.printStackTrace();
            FLAG_ISSAVED=false;
            return false;
        }
        return true;
    }
    public PolygonScenario getScenario(){
        return scenario;
    }
    public void setScenario(PolygonScenario scenario){
        this.scenario=scenario;
    }
    public float getSpeed(){
        return speed;
    }
    public void setSpeed(float value){
        this.speed=value;
    }
    public float getAltitude(){
        return altitude;
    }
    public void setAltitude(float value){
        this.altitude=value;
    }
    public int getHorizontalOverlapRate(){
        return horizontalOverlapRate;
    }
    public void setHorizontalOverlapRate(int value){
        this.horizontalOverlapRate=value;
    }
    public int getVerticalOverlapRate(){
        return verticalOverlapRate;
    }
    public void setVerticalOverlapRate(int value){
        this.verticalOverlapRate=value;
    }
    public List<LatLng> getVertexs(){
        return vertexs;
    }
    public void addVertex(LatLng latLng){
        //添加顶点
        vertexs.add(latLng);
    }
    public void setVertexs(List<LatLng> vs){
        //修改Vertexs，
        vertexs.clear();
        vertexs=vs;
    }
    public WaypointMission.Builder getMissionBuilder(LatLng startPoint){
        //任务执行前调用
        waypointList=generateWaypoints(startPoint);
        if(builder == null) {
            builder=new WaypointMission.Builder();
        }
        if(builder != null) {
            builder.waypointList(waypointList);
            builder.waypointCount(waypointList.size());
            builder.autoFlightSpeed(speed);
            builder.maxFlightSpeed(speed);
            builder.finishedAction(WaypointMissionFinishedAction.AUTO_LAND);
            builder.headingMode(WaypointMissionHeadingMode.USING_WAYPOINT_HEADING);
            builder.flightPathMode(WaypointMissionFlightPathMode.NORMAL);
            return builder;
        }
        else return null;
    }
    private List<Waypoint> generateWaypoints(LatLng startPoint){
        List<Waypoint> waypointList=new ArrayList<>();
        //顶点，宽度，速度，航点时间，起点
        FullCoveragePathPlanningAlgorithm algorithm=
                new FullCoveragePathPlanningAlgorithm(vertexs,20,speed,0,startPoint);
        List<LatLng> points=algorithm.getPlanningWaypoints();
        //need to set special waypoint action
        for(int i=0;i<points.size();i++){
            Waypoint waypoint=new Waypoint(points.get(i).latitude,points.get(i).longitude,altitude);
            //设置航点动作
            waypoint.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO,0));
            waypointList.add(waypoint);
        }
        //add droneStartPoint as land point
        waypointList.add(new Waypoint(startPoint.latitude,startPoint.longitude,altitude));
        return waypointList;
    }
}
