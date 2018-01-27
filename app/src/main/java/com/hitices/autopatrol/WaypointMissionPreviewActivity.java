package com.hitices.autopatrol;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.AMapOptions;
import com.amap.api.maps2d.CameraUpdate;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.UiSettings;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;
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

import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

public class WaypointMissionPreviewActivity extends AppCompatActivity {
     private WaypointsMission waypointsMission;
     public static WaypointMission.Builder builder;
     private FlightController mFlightController;
     private Waypoint currentWaypoint;
     private WaypointMissionOperator insatnce;
     private MapView mapView;
     private AMap aMap;
     private Marker droneMarker;
     private LatLng droneLocation;

     private  Button button;
     private Button retest;
     private Button start;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_execute);
        setContentView(R.layout.layout_test);
        Intent intent=getIntent();
        String path=AutoPatrolApplication.missionDir+"/"+intent.getStringExtra("missionName")+".xml";
        waypointsMission=readMission(path);

        initUI();
        initMapView(savedInstanceState);
        addListener();
        showMissionChangeDialog();
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
//        if(getRequestedOrientation()== ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
//            WaypointMissionPreviewActivity.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        removeListener();
    }
    @Override
    protected void onResume(){
        super.onResume();
        initFlightController();
    }
    private void initUI(){
       button=findViewById(R.id.execute_button);
        retest=findViewById(R.id.execute_retest);
        start=findViewById(R.id.execute_start);
       button.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               uploadMission();
           }
       });
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startMission();
            }
        });
        retest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopMission();
            }
        });
    }
    private void initMapView(Bundle savedInstanceState){
        mapView=findViewById(R.id.execute_mapview);
        mapView.onCreate(savedInstanceState);
        if(aMap == null){
            aMap=mapView.getMap();
        }
        UiSettings settings=aMap.getUiSettings();
        settings.setZoomControlsEnabled(false);
        //markWaypoint();
    }
    private void initFlightController() {
        BaseProduct product = AutoPatrolApplication.getProductInstance();
        if (product != null && product.isConnected()) {
            if (product instanceof Aircraft) {
                mFlightController = ((Aircraft) product).getFlightController();
            }
        }
        if (mFlightController != null) {

            mFlightController.setStateCallback(
                    new FlightControllerState.Callback() {
                        @Override
                        public void onUpdate(FlightControllerState
                                                     djiFlightControllerCurrentState) {
                            droneLocation=new LatLng(djiFlightControllerCurrentState.getAircraftLocation().getLatitude(),
                                    djiFlightControllerCurrentState.getAircraftLocation().getLongitude());
                            updateDroneLocation(droneLocation);
                        }
                    });
            setResultToToast("in flight control");
        }

    }
    private void markWaypoint(){
        for(int i=0;i<waypointsMission.waypointList.size();i++){
            MarkerOptions markerOptions =new MarkerOptions();
            LatLng ll=new LatLng(waypointsMission.waypointList.get(i).coordinate.getLatitude(),
                    waypointsMission.waypointList.get(i).coordinate.getLongitude());
            markerOptions.position(ll);
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
            aMap.addMarker(markerOptions);
            aMap.moveCamera(CameraUpdateFactory.newLatLng(ll));
            cameraUpdate(ll);
            aMap.addMarker(markerOptions);
        }
    }
    private void updateDroneLocation(LatLng latLng){
            final MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(),
                R.drawable.ic_location_marker)));
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (droneMarker != null) {
                        droneMarker.remove();
                    }
                        droneMarker = aMap.addMarker(markerOptions);
                }
            });

    }
    private void cameraUpdate(LatLng pos){
//        LatLng pos =new LatLng(droneLocationLat,droneLocationLng);
        float zoomLevel = (float)16.0;
        CameraUpdate cameraupdate =CameraUpdateFactory.newLatLngZoom(pos,zoomLevel);
        aMap.moveCamera(cameraupdate);
    }

    private WaypointMissionOperator getWaypointMissionOperator() {
        if (insatnce == null) {
            insatnce = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
        }
        if(insatnce == null)
        {
            setResultToToast("waypointMissionOperator is null");
        }
        return insatnce;
    }
    //Add Listener for WaypointMissionOperator
    private void addListener() {
        if (getWaypointMissionOperator() != null) {
            getWaypointMissionOperator().addListener(eventNotificationListener);
            setResultToToast("add listener");
        }
    }
    private void removeListener() {

        if (getWaypointMissionOperator() != null) {
            getWaypointMissionOperator().removeListener(eventNotificationListener);
            setResultToToast("remove listener");
        }
    }
    private WaypointMissionOperatorListener eventNotificationListener = new WaypointMissionOperatorListener() {
        @Override
        public void onDownloadUpdate(WaypointMissionDownloadEvent downloadEvent) {
        }
        @Override
        public void onUploadUpdate(WaypointMissionUploadEvent uploadEvent) {
        }
        @Override
        public void onExecutionUpdate(WaypointMissionExecutionEvent executionEvent) {
        }
        @Override
        public void onExecutionStart() {
        }
        @Override
        public void onExecutionFinish(@Nullable final DJIError error) {
            setResultToToast("Execution finished: " + (error == null ? "Success!" : error.getDescription()));
        }
    };
    private void showMissionChangeDialog(){
        LinearLayout wpPreview = (LinearLayout)getLayoutInflater().inflate(R.layout.activity_preview_waypoint,null);
        //ui
        final TextView name,aircraft,camera,time,startpoint,seekBar_text;
        //final TextView tv_speed=findViewById(R.id.preview_seekBar_text);
        SeekBar sb_speed;
        RadioGroup rg_actionAfterFinished;
        RadioGroup rg_heading;
        GridView gv_missions;

        name=wpPreview.findViewById(R.id.preview_current_mission_name);
        aircraft=wpPreview.findViewById(R.id.preview_aircraft_name);
        camera=wpPreview.findViewById(R.id.preview_camera);
        time=wpPreview.findViewById(R.id.preview_time);
        startpoint=wpPreview.findViewById(R.id.preview_start_point);
        seekBar_text=wpPreview.findViewById(R.id.preview_seekBar_text);
        //final TextView tv_speed=findViewById(R.id.preview_seekBar_text);
        sb_speed=wpPreview.findViewById(R.id.preview_speed);
        rg_actionAfterFinished =wpPreview.findViewById(R.id.preview_actionAfterFinished);
        rg_heading =wpPreview.findViewById(R.id.preview_heading);
        gv_missions=wpPreview.findViewById(R.id.preview_gv_action);
        if(waypointsMission != null){
            sb_speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    float tSpeed=(((float) i)/100)*15;
                    seekBar_text.setText(String.valueOf((int)(tSpeed+0.5))+"m/s");
                    waypointsMission.speed=tSpeed;
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
            rg_actionAfterFinished.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup radioGroup, int i) {
                    if(i == R.id.finishNone){
                        waypointsMission.finishedAction=WaypointMissionFinishedAction.NO_ACTION;
                    }else if(i == R.id.finishGoHome){
                        waypointsMission.finishedAction=WaypointMissionFinishedAction.GO_HOME;
                    }else if (i == R.id.finishAutoLanding){
                        waypointsMission.finishedAction=WaypointMissionFinishedAction.AUTO_LAND;
                    }else if (i == R.id.finishToFirst){
                        waypointsMission.finishedAction=WaypointMissionFinishedAction.GO_FIRST_WAYPOINT;
                    }
                }
            });
            rg_heading.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup radioGroup, int i) {
                    if(i == R.id.headingNext){
                        waypointsMission.headingMode=WaypointMissionHeadingMode.AUTO;
                    }else if(i == R.id.headingInitDirec){
                        waypointsMission.headingMode=WaypointMissionHeadingMode.USING_INITIAL_DIRECTION;
                    }else if (i == R.id.headingRC){
                        waypointsMission.headingMode=WaypointMissionHeadingMode.CONTROL_BY_REMOTE_CONTROLLER;
                    }else if (i == R.id.headingWP){
                        waypointsMission.headingMode=WaypointMissionHeadingMode.USING_WAYPOINT_HEADING;
                    }
                }
            });
            gv_missions.setAdapter(new WPGridviewAdapter(this));
            gv_missions.setOnItemClickListener(onItemClickListener);
            //update text
            name.setText("任务名称:"+waypointsMission.missionName);
            time.setText("预计执行时间:"+String.valueOf(waypointsMission.speed));
            //convert between float and int
            //rate=waypointsMission.speed/15
            sb_speed.setProgress((int)(waypointsMission.speed/15*100+0.5));
            seekBar_text.setText(String.valueOf((int)(waypointsMission.speed+0.5))+"m/s");

            rg_actionAfterFinished.check(getFinishCheckedId(waypointsMission.finishedAction));
            rg_heading.check(getHeadingCheckedId(waypointsMission.headingMode));
        }
        else {
            name.setText("XXXXXXXXXXXXXXXXXXXXXXXXXXX");
        }
        Aircraft myAircraft=AutoPatrolApplication.getAircraftInstance();
        Camera myCamera=AutoPatrolApplication.getCameraInstance();
        if(myAircraft != null){
            aircraft.setText("飞行器型号:" + myAircraft.getModel().getDisplayName());
        }else {
            aircraft.setText("飞行器型号:xxx");
        }
        if(myCamera != null){
            camera.setText("相机型号:" + myCamera.getDisplayName());
        }else {
            camera.setText("相机型号：xxx");
        }
        if(droneLocation != null) {
            startpoint.setText(" [" + String.valueOf(droneLocation.latitude) + "," + String.valueOf(droneLocation.longitude) + "] ");
        }else {
            startpoint.setText("null");
        }
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setTitle("")
                .setView(wpPreview)
                .setPositiveButton("upload", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
        final AlertDialog dialog=builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                        missionProcessing();
                    }
                });
        Window window=dialog.getWindow();
        //window.getDecorView().setPadding(0,0,0,0);
        WindowManager.LayoutParams params=window.getAttributes();
        params.width=WindowManager.LayoutParams.MATCH_PARENT;
        params.height=WindowManager.LayoutParams.MATCH_PARENT;
        window.setAttributes(params);
    }
    private void missionProcessing(){
        markWaypoint();
        loadMission();
//        uploadMission();
//        startMission();
//        new Handler().postDelayed(new Runnable(){
//            public void run() {
//                loadMission();
//            }
//        }, 10000);
//        new Handler().postDelayed(new Runnable(){
//            public void run() {
//                uploadMission();
//            }
//        }, 3000);
//        new Handler().postDelayed(new Runnable(){
//            public void run() {
//                startMission();
//            }
//        }, 3000);
    }
    private void loadMission(){
            setResultToToast("on load");
            builder = waypointsMission.getMissionBuilder();
            if(builder != null) {
                DJIError error = getWaypointMissionOperator().loadMission(builder.build());
                if (error == null) {
                    setResultToToast("load success");
                } else {
                    setResultToToast("load mission failed:" + error.getDescription());
                }
            } else {
                setResultToToast("builder is null");
            }

    }
    private void uploadMission(){
        setResultToToast("on upload");
        getWaypointMissionOperator().uploadMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error2) {
                if (error2 == null) {
                    setResultToToast("Mission upload successfully!");
                } else {
                    setResultToToast("Mission upload failed, error: " + error2.getDescription() + " retrying...");
                    getWaypointMissionOperator().retryUploadMission(null);
                }
            }
        });
    }
    private void startMission(){
        setResultToToast("on start");
        getWaypointMissionOperator().startMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error3) {
                setResultToToast("Mission Start: " + (error3 == null ? "Successfully" : error3.getDescription()));
            }
        });
    }
    private void stopMission(){
        setResultToToast("on stop");
        getWaypointMissionOperator().startMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error3) {
                setResultToToast("Mission Stop: " + (error3 == null ? "Successfully" : error3.getDescription()));
            }
        });
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
            holder.id.setText(String.valueOf(position));
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
             showWaypointDetail(i);
        }
    };
    private void showWaypointDetail(int i){
        //preview_wp_setting
        LinearLayout waypointDetail = (LinearLayout)getLayoutInflater().inflate(R.layout.waypoint_preview_waypoint_detail,null);
        GridView detail=waypointDetail.findViewById(R.id.preview_wp_setting);
        final EditText altitude=waypointDetail.findViewById(R.id.preview_wp_altitude);
        currentWaypoint=waypointsMission.waypointList.get(i);
        final WPWaypointGridviewAdapter adapter=new WPWaypointGridviewAdapter(currentWaypoint.waypointActions,this);
        detail.setAdapter(adapter);
        altitude.setInputType(InputType.TYPE_CLASS_NUMBER);
        altitude.setText(String.valueOf(currentWaypoint.altitude));
        new AlertDialog.Builder(this).setTitle("")
                .setView(waypointDetail)
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        currentWaypoint.altitude=Float.valueOf(altitude.getText().toString());
                        currentWaypoint.removeAllAction();
                        for(int j=0;j<adapter.getSelectedAction().size();j++){
                            currentWaypoint.addAction(new WaypointAction(adapter.getSelectedAction().get(j),j));
                        }
                    }
                })
                .create().show();
    }
    class WPWaypointGridviewAdapter extends BaseAdapter {
        private List<WaypointAction> actions;
        private List<WaypointActionType> allAction;
        private List<WaypointActionType> selectedAction;
        private LayoutInflater inflater = null;
        WPWaypointGridviewAdapter(List<WaypointAction> list, Context context) {
            this.actions = list;
            inflater = LayoutInflater.from(context);
            initData();
        }
        @Override
        public int getCount() {
            return allAction.size();
        }
        @Override
        public Object getItem(int position) {
            return allAction.get(position);
        }
        @Override
        public long getItemId(int position) {
            return position;
        }
        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder holder ;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = inflater.inflate(R.layout.gridview_action, null);
                holder.name = convertView.findViewById(R.id.item_name);
                holder.cb = convertView.findViewById(R.id.item_cb);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.name.setText(getActionChinese(allAction.get(position)));
            holder.cb.setId(position);
            holder.cb.setChecked(isSelected(allAction.get(position)));
            holder.cb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    CheckBox cb=(CheckBox)view;
                    int position=cb.getId();
                    boolean b=cb.isChecked();
                    if(b){
                        if(!selectedAction.contains(allAction.get(position)))
                            selectedAction.add(allAction.get(position));
                    }else {
                        selectedAction.remove(allAction.get(position));
                    }
                }
            });
            return convertView;
        }
        class ViewHolder{
            CheckBox cb;
            TextView name;
        }
        private boolean isSelected(WaypointActionType wat) {
            for(int i=0;i<actions.size();i++){
                if(actions.get(i).actionType.equals(wat))
                    return true;
            }
            return false;
        }
        public List<WaypointActionType> getSelectedAction(){
            return selectedAction;
        }
        private void initData(){
            allAction=new ArrayList<>();
            allAction.add(WaypointActionType.CAMERA_FOCUS);
            allAction.add(WaypointActionType.CAMERA_ZOOM);
            allAction.add(WaypointActionType.GIMBAL_PITCH);
            allAction.add(WaypointActionType.START_RECORD);
            allAction.add(WaypointActionType.STOP_RECORD);
            allAction.add(WaypointActionType.START_TAKE_PHOTO);
            allAction.add(WaypointActionType.STAY);
            allAction.add(WaypointActionType.ROTATE_AIRCRAFT);
            selectedAction=new ArrayList<>();
            for(int i=0;i<actions.size();i++){
                selectedAction.add(actions.get(i).actionType);
            }
        }
    }
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
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
            }
        });
    }
    private String getActionChinese(WaypointActionType action){
        String chinese="";
        switch (action){
            case STAY:
                chinese="停下";
                break;
            case CAMERA_ZOOM:
                chinese="相机变焦";
                break;
            case CAMERA_FOCUS:
                chinese="相机焦点";
                break;
            case GIMBAL_PITCH:
                chinese="云台调整";
                break;
            case START_RECORD:
                chinese="开始录像";
                break;
            case STOP_RECORD:
                chinese="停止录像";
                break;
            case ROTATE_AIRCRAFT:
                chinese="旋转";
                break;
            case START_TAKE_PHOTO:
                chinese="拍照";
                break;
            default:
                break;
        }
        return chinese;
    }
}
