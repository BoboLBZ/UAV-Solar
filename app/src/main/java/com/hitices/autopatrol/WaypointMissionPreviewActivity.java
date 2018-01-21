package com.hitices.autopatrol;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.maps2d.model.LatLng;
import com.hitices.autopatrol.missions.WaypointsMission;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import dji.common.flightcontroller.FlightControllerState;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;

public class WaypointMissionPreviewActivity extends AppCompatActivity{
     private WaypointsMission waypointsMission;
     private BaseProduct baseProduct;
     private FlightController mFlightController;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview_waypoint);

        Intent intent=getIntent();
        String path=AutoPatrolApplication.missionDir+"/"+intent.getStringExtra("missionName")+".xml";
        waypointsMission=readMission(path);
        baseProduct=AutoPatrolApplication.getProductInstance();
        initUI();
    }
    private void initUI(){
        final TextView name,aircraft,camera,time,startpoint;
        name=findViewById(R.id.preview_current_mission_name);
        aircraft=findViewById(R.id.preview_aircraft_name);
        camera=findViewById(R.id.preview_camera);
        time=findViewById(R.id.preview_time);
        startpoint=findViewById(R.id.preview_start_point);
        final TextView tv_speed=findViewById(R.id.preview_seekBar_text);
        final SeekBar sb_speed=findViewById(R.id.preview_speed);
        final  RadioGroup rg_actionAfterFinished =findViewById(R.id.preview_actionAfterFinished);
        final RadioGroup rg_heading =findViewById(R.id.preview_heading);
        final GridView gv_missions=findViewById(R.id.preview_gv_action);

        if(waypointsMission != null){
            name.setText("任务名称:"+waypointsMission.missionName);
            time.setText("预计执行时间");
            //convert between float and int
            //rate=waypointsMission.speed/15
            sb_speed.setProgress((int)(waypointsMission.speed/15+0.5));
            rg_actionAfterFinished.check(getFinishCheckedId(waypointsMission.finishedAction));
            rg_heading.check(getHeadingCheckedId(waypointsMission.headingMode));
            gv_missions.setAdapter(new WPGridviewAdapter(this));
            gv_missions.setOnItemClickListener(onItemClickListener);
        }
        else {
            name.setText("XXXXXXXXXXXXXXXXXXXXXXXXXXX");
        }
        if (null != baseProduct && baseProduct.isConnected()) {
            if (null != baseProduct.getModel()) {
                aircraft.setText("飞行器型号:" + baseProduct.getModel().getDisplayName());
                if(null != baseProduct.getCamera()) {
                    camera.setText("相机型号:" + baseProduct.getCamera().getDisplayName());
                }
                else {
                    camera.setText("相机型号：xxx");
                }
                mFlightController=  ((Aircraft)baseProduct).getFlightController();
                if (mFlightController != null) {
                    mFlightController.setStateCallback(
                            new FlightControllerState.Callback() {
                                @Override
                                public void onUpdate(FlightControllerState
                                                             djiFlightControllerCurrentState) {
                                    double Lat = djiFlightControllerCurrentState.getAircraftLocation().getLatitude();
                                    double Lng = djiFlightControllerCurrentState.getAircraftLocation().getLongitude();
                                    startpoint.setText(" ["+ String.valueOf(Lat)+","+ String.valueOf(Lng)+"] ");
                                }
                            });
                }else
                    startpoint.setText("none");
            } else {
                aircraft.setText("飞行器型号:xxx");
                camera.setText("相机型号：xxx");
                startpoint.setText("none");
            }
        }else {
            aircraft.setText("飞行器型号:xxx");
            camera.setText("相机型号：xxx");
            startpoint.setText("none");
        }
    }
    private class WPGridviewAdapter extends BaseAdapter {
        private LayoutInflater inflater;
        WPGridviewAdapter(Context context){
            inflater=LayoutInflater.from(context);
        }
        @Override
        public int getCount() {
            return waypointsMission.waypointList.size();
        }
        @Override
        public Object getItem(int position) {
            return waypointsMission.waypointList.get(position);
        }
        @Override
        public long getItemId(int position) {
            return position;
        }
        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView=inflater.inflate(R.layout.w_p_gv_item, null);
                holder.id = convertView.findViewById(R.id.preview_gvitem_id);
                holder.lat = convertView.findViewById(R.id.preview_gvitem_lat);
                holder.lng = convertView.findViewById(R.id.preview_gvitem_lng);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.id.setText(position);
            holder.lat.setText(String.valueOf(waypointsMission.waypointList.get(position).coordinate.getLatitude()));
            holder.lng.setText(String.valueOf(waypointsMission.waypointList.get(position).coordinate.getLongitude()));
            return convertView;
        }
        class ViewHolder{
            TextView id,lat,lng;
        }

    }
    AdapterView.OnItemClickListener onItemClickListener=new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
             setResultToToast("click "+String.valueOf(i));
        }
    };
    private WaypointsMission readMission(String path){
        try {
            //need test,
            File file = new File(path);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
//            //root
//            doc.getDocumentElement().normalize();
//            System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
            //name
            NodeList nodes=doc.getElementsByTagName("missionName");
            if(nodes.item(0) == null){
                return null;
            }
            WaypointsMission newMission=new WaypointsMission(nodes.item(0).getTextContent());

            nodes=doc.getElementsByTagName("speed");
            if(nodes.item(0) != null){
                newMission.speed=Float.parseFloat(nodes.item(0).getTextContent());
            }
            //finishedAction
            nodes=doc.getElementsByTagName("finishedAction");
            if(nodes.item(0) != null){
                newMission.finishedAction=getFinishedAction(nodes.item(0).getTextContent());
            }
            //headingMode
            nodes=doc.getElementsByTagName("headingMode");
            if(nodes.item(0) != null){
                newMission.headingMode=getHeadingMode(nodes.item(0).getTextContent());
            }
            //Waypoints
            nodes=doc.getElementsByTagName("Waypoints");
            Node node=nodes.item(0);
            //single waypoint
            NodeList nWaypointList = ((Element)node).getElementsByTagName("waypoint");
            for (int temp = 0; temp < nWaypointList.getLength(); temp++) {
                Node nNode = nWaypointList.item(temp);
                System.out.println("\nCurrent Element :" + nNode.getNodeName());
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    LatLng ll=new LatLng(
                            Double.parseDouble(eElement.getElementsByTagName("latitude").item(0).getTextContent()),
                            Double.parseDouble(eElement.getElementsByTagName("longitude").item(0).getTextContent()));
                    Waypoint w=new Waypoint(
                            ll.latitude,ll.longitude,
                            Float.parseFloat(eElement.getElementsByTagName("altitude").item(0).getTextContent()));
                    NodeList eActions=eElement.getElementsByTagName("actions");
                    for(int j=0;j<eActions.getLength();j++){
                        Node n=eActions.item(j);
                        if(n.getNodeType() == Node.ELEMENT_NODE) {
                            Element e=(Element)n;
                            NodeList t=e.getChildNodes();
                            for(int k=0;k<t.getLength();k++){
                                WaypointActionType type=getAction(t.item(k).getNodeName());
                                w.addAction(new WaypointAction(type,k));
                            }
                        }
                    }
                    newMission.waypointList.add(w);
                    newMission.waypoints.put(ll,w);
                    System.out.println("\nlat and lng" + String.valueOf(ll.latitude)+" : "+String.valueOf(ll.longitude));
                }
            }
            newMission.FLAG_ISSAVED=true;
            return newMission;
        }catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    private WaypointMissionFinishedAction getFinishedAction(String s){
        if(s.equals(WaypointMissionFinishedAction.AUTO_LAND.toString())){
            return WaypointMissionFinishedAction.AUTO_LAND;
        }else if(s.equals(WaypointMissionFinishedAction.GO_FIRST_WAYPOINT.toString())) {
            return WaypointMissionFinishedAction.GO_FIRST_WAYPOINT;
        }else if(s.equals(WaypointMissionFinishedAction.GO_HOME.toString())) {
            return WaypointMissionFinishedAction.GO_HOME;
        }else  {
            return WaypointMissionFinishedAction.NO_ACTION;
        }
    }
    private WaypointMissionHeadingMode getHeadingMode(String s){
        if(s.equals(WaypointMissionHeadingMode.AUTO.toString())){
            return WaypointMissionHeadingMode.AUTO;
        }else if(s.equals(WaypointMissionHeadingMode.USING_INITIAL_DIRECTION.toString())) {
            return WaypointMissionHeadingMode.USING_INITIAL_DIRECTION;
        }else if(s.equals(WaypointMissionHeadingMode.CONTROL_BY_REMOTE_CONTROLLER.toString())) {
            return WaypointMissionHeadingMode.CONTROL_BY_REMOTE_CONTROLLER;
        }else  {
            return WaypointMissionHeadingMode.USING_WAYPOINT_HEADING;
        }
    }
    private WaypointActionType getAction(String s){
        if( WaypointActionType.STAY.toString().equals(s))
            return WaypointActionType.STAY;
        else if( WaypointActionType.CAMERA_ZOOM.toString().equals(s))
            return WaypointActionType.CAMERA_ZOOM;
        else if( WaypointActionType.CAMERA_FOCUS.toString().equals(s))
            return WaypointActionType.CAMERA_FOCUS;
        else if( WaypointActionType.GIMBAL_PITCH.toString().equals(s))
            return WaypointActionType.GIMBAL_PITCH;
        else if( WaypointActionType.START_RECORD.toString().equals(s))
            return WaypointActionType.START_RECORD;
        else if( WaypointActionType.STOP_RECORD.toString().equals(s))
            return WaypointActionType.STOP_RECORD;
        else if( WaypointActionType.ROTATE_AIRCRAFT.toString().equals(s))
            return WaypointActionType.ROTATE_AIRCRAFT;
        else if( WaypointActionType.START_TAKE_PHOTO.toString().equals(s))
            return WaypointActionType.START_TAKE_PHOTO;
        else return null;
    }
    private int getFinishCheckedId(WaypointMissionFinishedAction action){
        if(action == WaypointMissionFinishedAction.NO_ACTION){
            return R.id.preview_finishNone;
        }else if(action == WaypointMissionFinishedAction.GO_HOME){
            return R.id.preview_finishGoHome;
        }else if (action == WaypointMissionFinishedAction.AUTO_LAND){
            return R.id.preview_finishAutoLanding;
        }else if (action == WaypointMissionFinishedAction.GO_FIRST_WAYPOINT){
            return R.id.preview_finishToFirst;
        }else
            return R.id.preview_finishNone;
    }
    private int getHeadingCheckedId(WaypointMissionHeadingMode action){
        if(action == WaypointMissionHeadingMode.CONTROL_BY_REMOTE_CONTROLLER){
            return R.id.preview_headingRC;
        }else if (action == WaypointMissionHeadingMode.USING_INITIAL_DIRECTION){
            return R.id.preview_headingInitDirec;
        }else if (action == WaypointMissionHeadingMode.USING_WAYPOINT_HEADING){
            return R.id.preview_headingWP;
        }else
            return R.id.preview_headingNext;
    }
    private void setResultToToast(final String msg){
        Toast.makeText(this,msg,Toast.LENGTH_LONG).show();
    }
}
