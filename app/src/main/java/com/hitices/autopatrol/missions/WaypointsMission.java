package com.hitices.autopatrol.missions;


import android.util.Log;

import com.amap.api.maps2d.model.LatLng;
import com.hitices.autopatrol.AutoPatrolApplication;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

/**
 * Created by Rhys on 2018/1/11.
 * email: bozliu@outlook.com
 * 航点任务类
 */

public class WaypointsMission extends BaseMission {
    private static WaypointMission.Builder builder; //任务builder，DJI SDK提供
    public final Map<LatLng, Waypoint> waypoints = new ConcurrentHashMap<>(); //map，坐标与航点的对应，方便查找
    //public String missionName;
    public Date date; //创建时间，还没启用
    //mission
    public List<Waypoint> waypointList = new ArrayList();  // 航点集合
    public float altitude; //通用高度
    public float speed; //飞行速度
    public WaypointMissionFinishedAction finishedAction; //任务结束动作
    public WaypointMissionHeadingMode headingMode; //飞行器朝向
    public List<WaypointAction> currentGeneralActions = new ArrayList<>(); //航点通用动作

    public WaypointsMission(String mName) {
        missionName = mName;
        missionType = MissionType.WaypointMission;
        FLAG_ISSAVED = false;
        date = new Date();
        altitude = 5f;
        speed = 3f;
        finishedAction = WaypointMissionFinishedAction.AUTO_LAND;
        headingMode = WaypointMissionHeadingMode.USING_WAYPOINT_HEADING;
        //init actions
        currentGeneralActions.add(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 0));
        builder = new WaypointMission.Builder();
    }

    @Override
    public boolean saveMission() {
//        Log.e("rhys","in save class");
        File dir = new File(AutoPatrolApplication.MISSION_DIR);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Log.e("rhys", "dirs failed");
                FLAG_ISSAVED = false;
                return false;
            }
        }
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            // root elements
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("WaypointsMission");
            doc.appendChild(rootElement);
            //general parameter
            Element eName = doc.createElement("missionName");
            eName.appendChild(doc.createTextNode(missionName));
            rootElement.appendChild(eName);

            Element eSpeed = doc.createElement("speed");
            eSpeed.appendChild(doc.createTextNode(String.valueOf(speed)));
            rootElement.appendChild(eSpeed);

            Element eAlt = doc.createElement("altitude");
            eAlt.appendChild(doc.createTextNode(String.valueOf(altitude)));
            rootElement.appendChild(eAlt);

            Element eFinishedAction = doc.createElement("finishedAction");
            eFinishedAction.appendChild(doc.createTextNode(finishedAction.name()));
            rootElement.appendChild(eFinishedAction);

            Element eHeadingMode = doc.createElement("headingMode");
            eHeadingMode.appendChild(doc.createTextNode(headingMode.name()));
            rootElement.appendChild(eHeadingMode);
            // waypoint elements
            Element waypoints = doc.createElement("Waypoints");
            waypoints.setAttribute("nums", String.valueOf(waypointList.size()));
            rootElement.appendChild(waypoints);
            for (int i = 0; i < waypointList.size(); i++) {
                Waypoint w = waypointList.get(i);
                Element eWaypoint = doc.createElement("waypoint");
                eWaypoint.setAttribute("id", String.valueOf(i));
                //lat & lng & altitude
                Element lat = doc.createElement("latitude");
                lat.appendChild(doc.createTextNode(String.valueOf(w.coordinate.getLatitude())));
                eWaypoint.appendChild(lat);
                Element lng = doc.createElement("longitude");
                lng.appendChild(doc.createTextNode(String.valueOf(w.coordinate.getLongitude())));
                eWaypoint.appendChild(lng);
                Element alt = doc.createElement("altitude");
                alt.appendChild(doc.createTextNode(String.valueOf(w.altitude)));
                eWaypoint.appendChild(alt);
                //waypoint actions
                Element eActions = doc.createElement("actions");
                eActions.setAttribute("nums", String.valueOf(w.waypointActions.size()));
                for (int j = 0; j < w.waypointActions.size(); j++) {
                    Element a = doc.createElement(w.waypointActions.get(j).actionType.toString());
                    //a.appendChild(doc.createTextNode(w.waypointActions.get(j).actionType.toString()));
                    eActions.appendChild(a);
                }
                eWaypoint.appendChild(eActions);
                waypoints.appendChild(eWaypoint);
            }
            // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(AutoPatrolApplication.MISSION_DIR + "/" + missionName + ".xml"));
            StreamResult result1 = new StreamResult(System.out);
            transformer.transform(source, result);
            transformer.transform(source, result1);
            FLAG_ISSAVED = true;
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
            FLAG_ISSAVED = false;
            return false;
        } catch (TransformerException tfe) {
            tfe.printStackTrace();
            FLAG_ISSAVED = false;
            return false;
        }
        return true;
    }

    public WaypointMission.Builder getMissionBuilder() {
        //任务执行前调用
        if (builder == null) {
            builder = new WaypointMission.Builder();
        }
        if (builder != null) {
            builder.waypointList(waypointList);
            builder.waypointCount(waypointList.size());
            builder.autoFlightSpeed(speed);
            builder.maxFlightSpeed(speed);
            builder.finishedAction(finishedAction);
            builder.headingMode(headingMode);
            builder.flightPathMode(WaypointMissionFlightPathMode.NORMAL);
            return builder;
        } else return null;
    }

    public int findWaypoint(LatLng latLng) {
        //返回航点索引
        Waypoint waypoint = waypoints.get(latLng);
        return waypointList.indexOf(waypoint);
    }

    public Waypoint getWaypoint(LatLng latLng) {
        return waypointList.get(findWaypoint(latLng));
    }

    public void addWaypointList(LatLng latLng) {
        //添加航点，使用系统默认参数
        Waypoint waypoint = new Waypoint(latLng.latitude, latLng.longitude, altitude);
        for (int i = 0; i < currentGeneralActions.size(); i++)
            waypoint.addAction(currentGeneralActions.get(i));
        waypointList.add(waypoint);
        waypoints.put(latLng, waypoint);
    }

    public void removeWaypoint(LatLng latLng) {
        //删除航点
        int i = findWaypoint(latLng);
        waypointList.remove(i);
        waypoints.remove(latLng);
    }

    public void genernalWaypointSetting(float alt, List<WaypointActionType> selectedType) {
        //通用航点设置，主要修改高度和航点动作
        //altitude,waypoint actions
        currentGeneralActions.clear();
        for (int i = 0; i < selectedType.size(); i++) {
            currentGeneralActions.add(new WaypointAction(selectedType.get(i), i));
        }
        for (int i = 0; i < waypointList.size(); i++) {
            waypointList.get(i).altitude = alt;
            waypointList.get(i).removeAllAction();
            for (int j = 0; j < selectedType.size(); j++) {
                waypointList.get(i).addAction(new WaypointAction(selectedType.get(j), j));
            }
        }
        this.altitude = alt;
    }

    public void singleWaypointsetting(LatLng latLng, float alt, List<WaypointAction> aActions) {
        //对单个航点设置
        int index = findWaypoint(latLng);
        if (index >= 0) {
            waypointList.get(index).altitude = alt;
            waypointList.get(index).removeAllAction();
            for (int j = 0; j < aActions.size(); j++) {
                waypointList.get(index).addAction(aActions.get(j));
            }
        }
    }

}
