package com.hitices.autopatrol.missions;


import android.util.Log;

import com.amap.api.maps2d.model.LatLng;
import com.hitices.autopatrol.AutoPatrolApplication;

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


/**
 * Created by Rhys on 2018/1/11.
 */

public class PolygonMission extends BaseMission {
    private List<LatLng> vertexs=new ArrayList<>();
    private PolygonScenario scenario;
    private float altitude,speed;
    private int horizontalOverlapRate,verticalOverlapRate;
    public PolygonMission(String name){
        missionName =name;
        missionType=MissionType.PolygonMission;
        scenario=PolygonScenario.TYPEA;
        altitude= 40.0f;
        speed=10f;
        horizontalOverlapRate=70;
        verticalOverlapRate=70;
        FLAG_ISSAVED=false;
    }
    @Override
    public boolean saveMission(){
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
            // root elements
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

            // waypoint elements
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
            StreamResult result1 = new StreamResult(System.out);
            transformer.transform(source, result);
            transformer.transform(source, result1);
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
        vertexs.add(latLng);
    }
    public void setVertexs(List<LatLng> vs){
        vertexs.clear();
        vertexs=vs;
    }
}
