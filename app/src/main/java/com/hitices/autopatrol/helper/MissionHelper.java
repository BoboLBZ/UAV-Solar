package com.hitices.autopatrol.helper;

import android.support.v4.math.MathUtils;
import android.util.Log;
import android.widget.Switch;

import com.hitices.autopatrol.AutoPatrolApplication;
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
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import dji.common.mission.waypoint.WaypointMission;

/**
 * Created by Rhys on 2018/4/4.
 * email: bozliu@outlook.com
 */
public class MissionHelper {
    private final String TAG = MissionHelper.class.getName();
    private String filePath;
    private PatrolMission patrolMission;
    private List<BaseModel> modelList;
    private boolean flag = false;

    public MissionHelper(String path, PatrolMission patrolMission) {
        this.filePath = path;
        this.patrolMission = patrolMission;
        init();
    }

    public static WaypointMission.Builder getBuilder() {
        return new WaypointMission.Builder();
    }

    public static boolean saveMissionToFile(PatrolMission patrolMission, List<BaseModel> modelList) {
        try {
            File file = new File(patrolMission.getFilePath());
            if (file == null) {
                return false;
            }
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            //root,patrolmission
            Element root = doc.createElement("PatrolMission");
            doc.appendChild(root);
            Element name = doc.createElement("missionName");
            name.setNodeValue(patrolMission.getName());
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
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(file);
            transformer.transform(source, result);
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
            return false;
        } catch (TransformerException tfe) {
            tfe.printStackTrace();
            return false;
        }
        return true;
    }

    public static List<PatrolMission> readMissionsFromDataBase() {
        return DataSupport.findAll(PatrolMission.class);
    }

    public static boolean saveMissionToDatabase(PatrolMission patrolMission) {
        patrolMission.save();
        return true;
    }

    private static void createElementOfMultiPoint(Document doc, Element root, MultiPointsModel multiPointsModel) {
        Element localRoot = doc.createElement("ChildMission");
        localRoot.setAttribute("type", "MultiPoints");
        Element name = doc.createElement("modelName");
        name.setNodeValue(multiPointsModel.getMissionName());
        localRoot.appendChild(name);

        root.appendChild(localRoot);
    }

    private static void createElementOfFlatland(Document doc, Element root, FlatlandModel flatlandModel) {
        Element localRoot = doc.createElement("ChildMission");
        localRoot.setAttribute("type", "Flatland");
        root.appendChild(localRoot);

        Element name = doc.createElement("modelName");
        name.setNodeValue(flatlandModel.getMissionName());
        localRoot.appendChild(name);
    }

    private static void createElementOfSlope(Document doc, Element root, SlopeModel slopeModel) {
        Element localRoot = doc.createElement("ChildMission");
        localRoot.setAttribute("type", "Slope");
        root.appendChild(localRoot);
        Element name = doc.createElement("modelName");
        name.setNodeValue(slopeModel.getMissionName());
        localRoot.appendChild(name);
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

//            patrolMission = new PatrolMission();
//            NodeList nodeList = doc.getElementsByTagName("missionName");
//            if (nodeList.item(0) != null) {
//                patrolMission.setName(nodeList.item(0).getTextContent());
//            }
            NodeList nodeList = doc.getElementsByTagName("ChildMission");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node item = nodeList.item(i);
                String type = item.getAttributes().getNamedItem("type").toString();
                if (type.equals("MultiPoints")) {
                    readMultiPointsModel(item);
                } else if (type.equals("Flatland")) {
                    readFlatlandModel(item);
                } else if (type.equals("Slope")) {
                    readSlopeModel(item);
                }
            }
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
            modelList.add(multiPointsModel);
        }
    }

    private void readFlatlandModel(Node item) {
        if (item.getNodeType() == Node.ELEMENT_NODE) {
            Element element = (Element) item;
            String name = element.getElementsByTagName("modelName").item(0).getTextContent();
            FlatlandModel model = new FlatlandModel(name);
            modelList.add(model);
        }
    }

    private void readSlopeModel(Node item) {
        if (item.getNodeType() == Node.ELEMENT_NODE) {
            Element element = (Element) item;
            String name = element.getElementsByTagName("modelName").item(0).getTextContent();
            SlopeModel model = new SlopeModel();
            modelList.add(model);
        }
    }
}
