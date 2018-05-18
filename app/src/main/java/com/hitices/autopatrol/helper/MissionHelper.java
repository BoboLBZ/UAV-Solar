package com.hitices.autopatrol.helper;

import android.util.Log;

import com.amap.api.maps2d.model.LatLng;
import com.hitices.autopatrol.entity.dataSupport.PatrolMission;
import com.hitices.autopatrol.entity.missions.BaseModel;
import com.hitices.autopatrol.entity.missions.FlatlandModel;
import com.hitices.autopatrol.entity.missions.MultiPointsModel;
import com.hitices.autopatrol.entity.missions.SlopeModel;

import org.litepal.crud.DataSupport;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;

/**
 * Created by Rhys on 2018/4/4.
 * email: bozliu@outlook.com
 */
public class MissionHelper {
    private final String TAG = MissionHelper.class.getName();
    private String filePath;
    private PatrolMission patrolMission;
    private List<BaseModel> modelList;

    public MissionHelper(String path, PatrolMission patrolMission) {
        this.filePath = path;
        this.patrolMission = patrolMission;
        this.modelList = new ArrayList<>();
        init();
    }

    public static WaypointMission.Builder getBuilder() {
        return new WaypointMission.Builder();
    }

    public static boolean saveMissionToFile(PatrolMission patrolMission, List<BaseModel> modelList) {
        try {
            File file = new File(MissionConstraintHelper.MISSION_DIR);
            if (!file.exists()) {
                if (!file.mkdirs()) {
                    return false;
                }
            }
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            //root,patrol mission
            Element root = doc.createElement("PatrolMission");
            doc.appendChild(root);
            Element name = doc.createElement("missionName");
            name.appendChild(doc.createTextNode(patrolMission.getName()));
//            root.setAttribute("missionName",patrolMission.getName());
            root.appendChild(name);
            //child mission
            for (int i = 0; i < modelList.size(); i++) {
                switch (modelList.get(i).getModelType()) {
                    case MultiPoints:
                        createElementOfMultiPoint(doc, root, (MultiPointsModel) modelList.get(i));
                        break;
                    case Flatland:
                        createElementOfFlatland(doc, root, (FlatlandModel) modelList.get(i));
                        break;
                    case Slope:
                        createElementOfSlope(doc, root, (SlopeModel) modelList.get(i));
                        break;
                }
            }
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            DOMSource source = new DOMSource(doc);
            System.out.println("path:" + patrolMission.getFilePath());
            File file1 = new File(patrolMission.getFilePath());
            StreamResult result = new StreamResult(file1);
            transformer.transform(source, result);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static List<PatrolMission> readMissionsFromDataBase() {
        return DataSupport.findAll(PatrolMission.class);
    }

    public static boolean saveMissionLisToDatabase(PatrolMission patrolMission) {
        patrolMission.save();
        return true;
    }

    public static boolean deleteMission(PatrolMission mission, String path) {
        //需要确认删除成功
        File f = new File(path);
        if (f.exists()) {
            f.delete();
            //return true;
        }
        DataSupport.delete(PatrolMission.class, mission.getId());
        return true;
        //return false;
    }
    private static void createElementOfMultiPoint(Document doc, Element root, MultiPointsModel multiPointsModel) {
        Element localRoot = doc.createElement("ChildMission");
        localRoot.setAttribute("type", "MultiPoints");
        //name
        Element name = doc.createElement("modelName");
        name.appendChild(doc.createTextNode(multiPointsModel.getMissionName()));
        localRoot.appendChild(name);
        //camera angle
        Element cameraAngle = doc.createElement("cameraAngle");
        cameraAngle.appendChild(doc.createTextNode(String.valueOf(multiPointsModel.getCameraAngle())));
        localRoot.appendChild(cameraAngle);
        //headingAngle
        Element headingMode = doc.createElement("headingAngle");
        headingMode.appendChild(doc.createTextNode(String.valueOf(multiPointsModel.getHeadingAngle())));
        localRoot.appendChild(headingMode);
        //speed
        Element eSpeed = doc.createElement("speed");
        eSpeed.appendChild(doc.createTextNode(String.valueOf(multiPointsModel.getSpeed())));
        localRoot.appendChild(eSpeed);
        //speed
        Element eAlt = doc.createElement("altitude");
        eAlt.appendChild(doc.createTextNode(String.valueOf(multiPointsModel.getAltitude())));
        localRoot.appendChild(eAlt);
        // waypoint elements
        Element waypoints = doc.createElement("Waypoints");
        List<Waypoint> waypointList = multiPointsModel.getWaypointList();
        waypoints.setAttribute("nums", String.valueOf(waypointList.size()));
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
                eActions.appendChild(a);
            }
            eWaypoint.appendChild(eActions);
            waypoints.appendChild(eWaypoint);
        }
        localRoot.appendChild(waypoints);

        root.appendChild(localRoot);
    }

    private static void createElementOfFlatland(Document doc, Element root, FlatlandModel flatlandModel) {
        Element localRoot = doc.createElement("ChildMission");
        localRoot.setAttribute("type", "Flatland");
        root.appendChild(localRoot);

        Element name = doc.createElement("modelName");
        name.appendChild(doc.createTextNode(flatlandModel.getMissionName()));
        localRoot.appendChild(name);

        Element eAngle = doc.createElement("cameraAngle");
        eAngle.appendChild(doc.createTextNode(String.valueOf(flatlandModel.getCameraAngle())));
        localRoot.appendChild(eAngle);

        Element headingMode = doc.createElement("headingAngle");
        headingMode.appendChild(doc.createTextNode(String.valueOf(flatlandModel.getHeadingAngle())));
        localRoot.appendChild(headingMode);

        Element eSpeed = doc.createElement("speed");
        eSpeed.appendChild(doc.createTextNode(String.valueOf(flatlandModel.getSpeed())));
        localRoot.appendChild(eSpeed);

        Element eAltitude = doc.createElement("altitude");
        eAltitude.appendChild(doc.createTextNode(String.valueOf(flatlandModel.getAltitude())));
        localRoot.appendChild(eAltitude);

        Element eDis = doc.createElement("distanceToPanel");
        eDis.appendChild(doc.createTextNode(String.valueOf(flatlandModel.getDistanceToPanel())));
        localRoot.appendChild(eDis);

        Element eHorRate = doc.createElement("OverlapRate");
        eHorRate.appendChild(doc.createTextNode(String.valueOf(flatlandModel.getOverlapRate())));
        localRoot.appendChild(eHorRate);

        Element eVerRate = doc.createElement("width");
        eVerRate.appendChild(doc.createTextNode(String.valueOf(flatlandModel.getWidth())));
        localRoot.appendChild(eVerRate);

        // vertex elements
        Element eVertexs = doc.createElement("Vertexs");
        List<LatLng> vertexs = flatlandModel.getVertexs();
        eVertexs.setAttribute("nums", String.valueOf(vertexs.size()));
        localRoot.appendChild(eVertexs);
        for (int i = 0; i < vertexs.size(); i++) {
            LatLng latLng = vertexs.get(i);
            Element eVertex = doc.createElement("vertex");
            eVertex.setAttribute("id", String.valueOf(i));
            //lat & lng
            Element lat = doc.createElement("latitude");
            lat.appendChild(doc.createTextNode(String.valueOf(latLng.latitude)));
            eVertex.appendChild(lat);
            Element lng = doc.createElement("longitude");
            lng.appendChild(doc.createTextNode(String.valueOf(latLng.longitude)));
            eVertex.appendChild(lng);

            eVertexs.appendChild(eVertex);
        }
    }

    private static void createElementOfSlope(Document doc, Element root, SlopeModel slopeModel) {
        Element localRoot = doc.createElement("ChildMission");
        localRoot.setAttribute("type", "Slope");
        root.appendChild(localRoot);
        Element name = doc.createElement("modelName");
        name.appendChild(doc.createTextNode(slopeModel.getMissionName()));
        localRoot.appendChild(name);

        Element eAngle = doc.createElement("cameraAngle");
        eAngle.appendChild(doc.createTextNode(String.valueOf(slopeModel.getCameraAngle())));
        localRoot.appendChild(eAngle);

        Element headingMode = doc.createElement("headingAngle");
        headingMode.appendChild(doc.createTextNode(String.valueOf(slopeModel.getHeadingAngle())));
        localRoot.appendChild(headingMode);

        Element eSpeed = doc.createElement("speed");
        eSpeed.appendChild(doc.createTextNode(String.valueOf(slopeModel.getSpeed())));
        localRoot.appendChild(eSpeed);

        Element eAltitude = doc.createElement("altitude");
        eAltitude.appendChild(doc.createTextNode(String.valueOf(slopeModel.getAltitude())));
        localRoot.appendChild(eAltitude);

        Element eHorRate = doc.createElement("OverlapRate");
        eHorRate.appendChild(doc.createTextNode(String.valueOf(slopeModel.getOverlapRate())));
        localRoot.appendChild(eHorRate);

        Element eWidth = doc.createElement("width");
        eWidth.appendChild(doc.createTextNode(String.valueOf(slopeModel.getWidth())));
        localRoot.appendChild(eWidth);

        Element eDistance = doc.createElement("distanceToPanel");
        eDistance.appendChild(doc.createTextNode(String.valueOf(slopeModel.getDistanceToPanel())));
        localRoot.appendChild(eDistance);
        //base line
        if (slopeModel.getBaselineA() != null && slopeModel.getBaselineB() != null) {
            Element baseline = doc.createElement("baseline");

            Element linepointA = doc.createElement("linePoint");
            linepointA.setAttribute("name", "A");

            Element a_lat = doc.createElement("latitude");
            a_lat.appendChild(doc.createTextNode(String.valueOf(slopeModel.getBaselineA().coordinate.getLatitude())));
            linepointA.appendChild(a_lat);

            Element a_lng = doc.createElement("longitude");
            a_lng.appendChild(doc.createTextNode(String.valueOf(slopeModel.getBaselineA().coordinate.getLongitude())));
            linepointA.appendChild(a_lng);

            Element a_alt = doc.createElement("altitude");
            a_alt.appendChild(doc.createTextNode(String.valueOf(slopeModel.getBaselineA().altitude)));
            linepointA.appendChild(a_alt);

            baseline.appendChild(linepointA);

            Element linepointB = doc.createElement("linePoint");
            linepointB.setAttribute("name", "B");

            Element b_lat = doc.createElement("latitude");
            b_lat.appendChild(doc.createTextNode(String.valueOf(slopeModel.getBaselineB().coordinate.getLatitude())));
            linepointB.appendChild(b_lat);

            Element b_lng = doc.createElement("longitude");
            b_lng.appendChild(doc.createTextNode(String.valueOf(slopeModel.getBaselineB().coordinate.getLongitude())));
            linepointB.appendChild(b_lng);

            Element b_alt = doc.createElement("altitude");
            b_alt.appendChild(doc.createTextNode(String.valueOf(slopeModel.getBaselineB().altitude)));
            linepointB.appendChild(b_alt);

            baseline.appendChild(linepointB);

            localRoot.appendChild(baseline);
        }

        // vertex elements
        Element eVertexs = doc.createElement("Vertexs");
        List<LatLng> vertexs = slopeModel.getVertexs();
        eVertexs.setAttribute("nums", String.valueOf(vertexs.size()));
        localRoot.appendChild(eVertexs);
        for (int i = 0; i < vertexs.size(); i++) {
            LatLng latLng = vertexs.get(i);
            Element eVertex = doc.createElement("vertex");
            eVertex.setAttribute("id", String.valueOf(i));
            //lat & lng
            Element lat = doc.createElement("latitude");
            lat.appendChild(doc.createTextNode(String.valueOf(latLng.latitude)));
            eVertex.appendChild(lat);
            Element lng = doc.createElement("longitude");
            lng.appendChild(doc.createTextNode(String.valueOf(latLng.longitude)));
            eVertex.appendChild(lng);

            eVertexs.appendChild(eVertex);
        }
    }

    private boolean init() {
        try {
            File file = new File(filePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            doc.getDocumentElement().normalize();
            String mission = doc.getDocumentElement().getNodeName();
            if (!mission.equals("PatrolMission")) {
                Log.e(TAG, "mission does not equals require type:" + mission);
                return false;
            }

            NodeList nodeList = doc.getElementsByTagName("missionName");
            if (nodeList.item(0) != null) {
                patrolMission.setName(nodeList.item(0).getTextContent());
            }
            nodeList = doc.getElementsByTagName("ChildMission");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node item = nodeList.item(i);
                String type = item.getAttributes().getNamedItem("type").getNodeValue();
                if (type.equals("MultiPoints")) {
                    readMultiPointsModel(item);
                } else if (type.equals("Flatland")) {
                    readFlatlandModel(item);
                } else if (type.equals("Slope")) {
                    readSlopeModel(item);
                }
            }
            patrolMission.setChildNums(modelList.size());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public PatrolMission getPatrolMission() {
        return patrolMission;
    }

    public List<BaseModel> getModelList() {
        return modelList;
    }

    private void readMultiPointsModel(Node item) {
        if (item.getNodeType() == Node.ELEMENT_NODE) {
            Element element = (Element) item;
            String name = element.getElementsByTagName("modelName").item(0).getTextContent();
            MultiPointsModel multiPointsModel = new MultiPointsModel(name);
            //cameraAngle
            NodeList nodes = element.getElementsByTagName("cameraAngle");
            if (nodes.item(0) != null) {
                multiPointsModel.setCameraAngle(Integer.parseInt(nodes.item(0).getTextContent()));
            }
            //headingAngle
            nodes = element.getElementsByTagName("headingAngle");
            if (nodes.item(0) != null) {
                multiPointsModel.setHeadingAngle(Integer.parseInt(nodes.item(0).getTextContent()));
            }
            nodes = element.getElementsByTagName("speed");
            if (nodes.item(0) != null) {
                multiPointsModel.setSpeed(Float.parseFloat(nodes.item(0).getTextContent()));
            }
            //general altitude
            nodes = element.getElementsByTagName("altitude");
            if (nodes.item(0) != null) {
                multiPointsModel.setAltitude(Float.parseFloat(nodes.item(0).getTextContent()));
            }

            //Waypoints
            nodes = element.getElementsByTagName("Waypoints");
            Node node = nodes.item(0);
            //single waypoint
            NodeList nWaypointList = ((Element) node).getElementsByTagName("waypoint");
            for (int temp = 0; temp < nWaypointList.getLength(); temp++) {
                Node nNode = nWaypointList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    LatLng ll = new LatLng(
                            Double.parseDouble(eElement.getElementsByTagName("latitude").item(0).getTextContent()),
                            Double.parseDouble(eElement.getElementsByTagName("longitude").item(0).getTextContent()));
                    Waypoint w = new Waypoint(
                            ll.latitude, ll.longitude,
                            Float.parseFloat(eElement.getElementsByTagName("altitude").item(0).getTextContent()));
                    NodeList eActions = eElement.getElementsByTagName("actions");
                    for (int j = 0; j < eActions.getLength(); j++) {
                        Node n = eActions.item(j);
                        if (n.getNodeType() == Node.ELEMENT_NODE) {
                            Element e = (Element) n;
                            NodeList t = e.getChildNodes();
                            for (int k = 0; k < t.getLength(); k++) {
                                WaypointActionType type = getAction(t.item(k).getNodeName());
                                switch (type) {
                                    case ROTATE_AIRCRAFT:
                                        w.addAction(new WaypointAction(type, multiPointsModel.getHeadingAngle()));
                                        break;
                                    default:
                                        w.addAction(new WaypointAction(type, 0));
                                        break;
                                }
                            }
                        }
                    }
                    multiPointsModel.addWaypointToList(w);
                    multiPointsModel.getWaypoints().put(ll, w);
                }
            }

            modelList.add(multiPointsModel);
        }
    }

    private void readFlatlandModel(Node item) {
        if (item.getNodeType() == Node.ELEMENT_NODE) {
            Element element = (Element) item;
            String name = element.getElementsByTagName("modelName").item(0).getTextContent();
            FlatlandModel model = new FlatlandModel(name);

            NodeList nodes = element.getElementsByTagName("cameraAngle");
            if (nodes.item(0) != null) {
                model.setCameraAngle(Integer.parseInt(nodes.item(0).getTextContent()));
            }
            //headingAngle
            nodes = element.getElementsByTagName("headingAngle");
            if (nodes.item(0) != null) {
                model.setHeadingAngle(Integer.parseInt(nodes.item(0).getTextContent()));
            }
            nodes = element.getElementsByTagName("speed");
            if (nodes.item(0) != null) {
                model.setSpeed(Float.parseFloat(nodes.item(0).getTextContent()));
            }
            nodes = element.getElementsByTagName("altitude");
            if (nodes.item(0) != null) {
                model.setAltitude(Float.parseFloat(nodes.item(0).getTextContent()));
            }
            //distanceToPanel
            nodes = element.getElementsByTagName("distanceToPanel");
            if (nodes.item(0) != null) {
                model.setDistanceToPanel(Float.parseFloat(nodes.item(0).getTextContent()));
            }
            nodes = element.getElementsByTagName("OverlapRate");
            if (nodes.item(0) != null) {
                model.setOverlapRate(Integer.parseInt(nodes.item(0).getTextContent()));
            }
            nodes = element.getElementsByTagName("width");
            if (nodes.item(0) != null) {
                model.setWidth(Float.parseFloat(nodes.item(0).getTextContent()));
            }
            nodes = element.getElementsByTagName("Vertexs");
            Node node = nodes.item(0);
            NodeList nVertexList = ((Element) node).getElementsByTagName("vertex");
            for (int temp = 0; temp < nVertexList.getLength(); temp++) {
                Node nNode = nVertexList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    LatLng ll = new LatLng(
                            Double.parseDouble(eElement.getElementsByTagName("latitude").item(0).getTextContent()),
                            Double.parseDouble(eElement.getElementsByTagName("longitude").item(0).getTextContent()));
                    model.addVertex(ll);
                }
            }
            modelList.add(model);
        }
    }

    private void readSlopeModel(Node item) {
        if (item.getNodeType() == Node.ELEMENT_NODE) {
            Element element = (Element) item;
            String name = element.getElementsByTagName("modelName").item(0).getTextContent();
            SlopeModel model = new SlopeModel(name);

            NodeList nodes = element.getElementsByTagName("cameraAngle");
            if (nodes.item(0) != null) {
                model.setCameraAngle(Integer.parseInt(nodes.item(0).getTextContent()));
            }
            //headingAngle
            nodes = element.getElementsByTagName("headingAngle");
            if (nodes.item(0) != null) {
                model.setHeadingAngle(Integer.parseInt(nodes.item(0).getTextContent()));
            }
            nodes = element.getElementsByTagName("speed");
            if (nodes.item(0) != null) {
                model.setSpeed(Float.parseFloat(nodes.item(0).getTextContent()));
            }
            nodes = element.getElementsByTagName("altitude");
            if (nodes.item(0) != null) {
                model.setAltitude(Float.parseFloat(nodes.item(0).getTextContent()));
            }
            nodes = element.getElementsByTagName("OverlapRate");
            if (nodes.item(0) != null) {
                model.setOverlapRate(Integer.parseInt(nodes.item(0).getTextContent()));
            }
            nodes = element.getElementsByTagName("width");
            if (nodes.item(0) != null) {
                model.setWidth(Float.parseFloat(nodes.item(0).getTextContent()));
            }
            nodes = element.getElementsByTagName("distanceToPanel");
            if (nodes.item(0) != null) {
                model.setDistanceToPanel(Float.parseFloat(nodes.item(0).getTextContent()));
            }
            nodes = element.getElementsByTagName("baseline");
            if (nodes.item(0) != null) {
                NodeList list = ((Element) nodes.item(0)).getElementsByTagName("linePoint");
                for (int i = 0; i < list.getLength(); i++) {
                    Node nNode = list.item(i);
                    String type = nNode.getAttributes().getNamedItem("name").getNodeValue();
                    Element temp = (Element) nNode;
                    Waypoint ll = new Waypoint(
                            Double.parseDouble(temp.getElementsByTagName("latitude").item(0).getTextContent()),
                            Double.parseDouble(temp.getElementsByTagName("longitude").item(0).getTextContent()),
                            Float.parseFloat(temp.getElementsByTagName("altitude").item(0).getTextContent()));
                    if (type.equals("A")) {
                        model.setBaselineA(ll);
                    } else if (type.equals("B")) {
                        model.setBaselineB(ll);
                    }

                }
            }
            nodes = element.getElementsByTagName("Vertexs");
            Node node = nodes.item(0);
            NodeList nVertexList = ((Element) node).getElementsByTagName("vertex");
            for (int temp = 0; temp < nVertexList.getLength(); temp++) {
                Node nNode = nVertexList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    LatLng ll = new LatLng(
                            Double.parseDouble(eElement.getElementsByTagName("latitude").item(0).getTextContent()),
                            Double.parseDouble(eElement.getElementsByTagName("longitude").item(0).getTextContent()));
                    model.addVertex(ll);
                }
            }

            modelList.add(model);
        }
    }

    private WaypointMissionHeadingMode getHeadingMode(String s) {
        if (s.equals(WaypointMissionHeadingMode.AUTO.toString())) {
            return WaypointMissionHeadingMode.AUTO;
        } else if (s.equals(WaypointMissionHeadingMode.USING_INITIAL_DIRECTION.toString())) {
            return WaypointMissionHeadingMode.USING_INITIAL_DIRECTION;
        } else if (s.equals(WaypointMissionHeadingMode.CONTROL_BY_REMOTE_CONTROLLER.toString())) {
            return WaypointMissionHeadingMode.CONTROL_BY_REMOTE_CONTROLLER;
        } else {
            return WaypointMissionHeadingMode.USING_WAYPOINT_HEADING;
        }
    }

    private WaypointActionType getAction(String s) {
        if (WaypointActionType.STAY.toString().equals(s))
            return WaypointActionType.STAY;
        else if (WaypointActionType.CAMERA_ZOOM.toString().equals(s))
            return WaypointActionType.CAMERA_ZOOM;
        else if (WaypointActionType.CAMERA_FOCUS.toString().equals(s))
            return WaypointActionType.CAMERA_FOCUS;
        else if (WaypointActionType.GIMBAL_PITCH.toString().equals(s))
            return WaypointActionType.GIMBAL_PITCH;
        else if (WaypointActionType.START_RECORD.toString().equals(s))
            return WaypointActionType.START_RECORD;
        else if (WaypointActionType.STOP_RECORD.toString().equals(s))
            return WaypointActionType.STOP_RECORD;
        else if (WaypointActionType.ROTATE_AIRCRAFT.toString().equals(s))
            return WaypointActionType.ROTATE_AIRCRAFT;
        else if (WaypointActionType.START_TAKE_PHOTO.toString().equals(s))
            return WaypointActionType.START_TAKE_PHOTO;
        else return null;
    }
}
