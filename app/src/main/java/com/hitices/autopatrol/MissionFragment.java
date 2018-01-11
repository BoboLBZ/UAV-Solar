package com.hitices.autopatrol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdate;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.sdkmanager.DJISDKManager;

public class MissionFragment extends Fragment
        implements View.OnClickListener, AMap.OnMapClickListener {

    private OnFragmentInteractionListener mListener;
    private static final String MISSION_STATE_SAVE_IS_HIDDEN="MISSION_STATE_SAVE_IS_HIDDEN";

    FloatingActionsMenu menu;
    FloatingActionButton btn_waypoint,btn_polygon,btn_import,btn_adjust;
    private AMap aMap;
    private MapView mapView;
    private Spinner spinner;
    //mission
    private boolean creatable;
    private float altitude=50.0f;//默认相对起点高度50米
    private float speed = 10.0f;
    private List<Waypoint> waypointList = new ArrayList<>();
    private final Map<Integer,Marker> mMarkers = new ConcurrentHashMap<>();
    public static WaypointMission.Builder waypoinyMissionBuilder;
    private WaypointMissionOperator instance;
    private WaypointMissionFinishedAction missionFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
    private WaypointMissionHeadingMode missionHeadingMode = WaypointMissionHeadingMode.AUTO; //航向


    public MissionFragment() {
        // Required empty public constructor
    }
    public static MissionFragment newInstance() {
        MissionFragment fragment = new MissionFragment();
        return fragment;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(savedInstanceState != null){
            boolean isSupportHidden=savedInstanceState.getBoolean(MISSION_STATE_SAVE_IS_HIDDEN);
            FragmentTransaction fragmentTransaction=getFragmentManager().beginTransaction();
            if(isSupportHidden){
                fragmentTransaction.hide(this);
            }else {
                fragmentTransaction.show(this);
            }
            fragmentTransaction.commit();
        }
    }
    @Override
    public void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        outState.putBoolean(MISSION_STATE_SAVE_IS_HIDDEN,isHidden());
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view=  inflater.inflate(R.layout.fragment_mission, container, false);
        mapView=view.findViewById(R.id.map);
        initMapView();
        mapView.onCreate(savedInstanceState);
        creatable=false;
        spinner=view.findViewById(R.id.missionSelected_mission);
        refreshSpinner();
        menu=view.findViewById(R.id.mission_menu);
        btn_adjust=view.findViewById(R.id.adjust_mission);
        btn_polygon=view.findViewById(R.id.add_polygon_mission);
        btn_waypoint=view.findViewById(R.id.add_waypoint_mission);
        btn_import=view.findViewById(R.id.import_mission);
        btn_adjust.setOnClickListener(this);
        btn_polygon.setOnClickListener(this);
        btn_waypoint.setOnClickListener(this);
        btn_import.setOnClickListener(this);
        return view;
    }
    protected void refreshSpinner(){
        ArrayAdapter<String> arrayAdapter=new ArrayAdapter<>(getContext(),android.R.layout.simple_spinner_item,AutoPatrolApplication.getMissionList());
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(arrayAdapter);
        spinner.setOnItemSelectedListener(onItemSelectedListener);
    }
    @Override
    public void onClick(View view){
        switch (view.getId()){
            case R.id.add_polygon_mission:
                break;
            case R.id.add_waypoint_mission:
                break;
            case R.id.import_mission:
                break;
            case R.id.adjust_mission:
                break;
            default:
                break;
        }
    }
    @Override
    public void onAttach(Context context) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(AutoPatrolApplication.FLAG_CONNECTION_CHANGE);
        context.registerReceiver(mReceiver, filter);
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }
    @Override
    public void onDetach() {
        getContext().unregisterReceiver(mReceiver);
        super.onDetach();
        mListener = null;
    }
    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
           // refreshSDKRelativeUI();
        }
    };
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
    private void initMapView(){
        if(aMap == null){
            aMap=mapView.getMap();
            aMap.setOnMapClickListener(this);
        }
        LatLng harbin = new LatLng(45.75,126.63);
        aMap.addMarker(new MarkerOptions().position(harbin).title("marker"));
        aMap.moveCamera(CameraUpdateFactory.newLatLng(harbin));
        cameraUpdate(harbin);
    }
    @Override
    public void onMapClick(LatLng latLng){
        markWaypoint(latLng);
        setWaypointList(latLng);
    }
    private void markWaypoint(LatLng latLng){
        //amap about
        if(creatable){
            MarkerOptions markerOptions =new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
            aMap.addMarker(markerOptions);
            Marker marker =aMap.addMarker(markerOptions);
            mMarkers.put(mMarkers.size(),marker);
         }
    }
    private void setWaypointList(LatLng latLng){
        //dji mission about
        if(creatable){
            Waypoint waypoint=new Waypoint(latLng.latitude,latLng.longitude,altitude);
            waypointList.add(waypoint);
        }
    }
    private void cameraUpdate(LatLng pos){
//        LatLng pos =new LatLng(droneLocationLat,droneLocationLng);
        float zoomLevel = (float)18.0;
        CameraUpdate cameraupdate =CameraUpdateFactory.newLatLngZoom(pos,zoomLevel);
        aMap.moveCamera(cameraupdate);
    }
    private void showSettingDialog(){
        LinearLayout wayPointSettings = (LinearLayout)getLayoutInflater().inflate(R.layout.dialog_waypointsetting,null);
        final TextView tv_wayPointAltitude= wayPointSettings.findViewById(R.id.altitude);
        final TextView missionTitle=wayPointSettings.findViewById(R.id.name);
        RadioGroup rg_speed = wayPointSettings.findViewById(R.id.speed);
        RadioGroup rg_actionAfterFinished =wayPointSettings.findViewById(R.id.actionAfterFinished);
        RadioGroup rg_heading =wayPointSettings.findViewById(R.id.heading);

        rg_speed.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if(i == R.id.lowSpeed){
                    speed = 3.0f;
                }else if(i == R.id.MidSpeed){
                    speed = 5.0f;
                }else if (i == R.id.HighSpeed){
                    speed = 10.0f;
                }
            }
        });
        rg_actionAfterFinished.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if(i == R.id.finishNone){
                    missionFinishedAction=WaypointMissionFinishedAction.NO_ACTION;
                }else if(i == R.id.finishGoHome){
                    missionFinishedAction=WaypointMissionFinishedAction.GO_HOME;
                }else if (i == R.id.finishAutoLanding){
                    missionFinishedAction=WaypointMissionFinishedAction.AUTO_LAND;
                }else if (i == R.id.finishToFirst){
                    missionFinishedAction=WaypointMissionFinishedAction.GO_FIRST_WAYPOINT;
                }
            }
        });
        rg_heading.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if(i == R.id.headingNext){
                    missionHeadingMode=WaypointMissionHeadingMode.AUTO;
                }else if(i == R.id.headingInitDirec){
                    missionHeadingMode=WaypointMissionHeadingMode.USING_INITIAL_DIRECTION;
                }else if (i == R.id.headingRC){
                    missionHeadingMode=WaypointMissionHeadingMode.CONTROL_BY_REMOTE_CONTROLLER;
                }else if (i == R.id.headingWP){
                    missionHeadingMode=WaypointMissionHeadingMode.USING_WAYPOINT_HEADING;
                }
            }
        });
        new AlertDialog.Builder(getContext()).setTitle("")
                .setView(wayPointSettings)
                .setPositiveButton("finish", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String altitudeStr = tv_wayPointAltitude.getText().toString();
                        altitude = Integer.parseInt(nulltoIntgerDefault(altitudeStr));
                        //rem
                        configWaypointMission();
                        saveMission(missionTitle.getText()+".xml");
                    }
                })
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                })
                .create().show();
    }
    private void configWaypointMission(){
        if(waypoinyMissionBuilder == null){
            waypoinyMissionBuilder =new WaypointMission.Builder().finishedAction(missionFinishedAction)
                    .headingMode(missionHeadingMode)
                    .autoFlightSpeed(speed)
                    .maxFlightSpeed(speed)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);
        }else {
            waypoinyMissionBuilder.finishedAction(missionFinishedAction)
                    .headingMode(missionHeadingMode)
                    .autoFlightSpeed(speed)
                    .maxFlightSpeed(speed)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);
        }
        if(waypoinyMissionBuilder.getWaypointList().size() > 0){
            for(int i=0;i<waypoinyMissionBuilder.getWaypointList().size();i++){
                waypoinyMissionBuilder.getWaypointList().get(i).altitude=altitude;
            }
            setResultToToast("set waypoint altitude success");
        }
//        DJIError djiError = getWaypointMissionOperator().loadMission(waypoinyMissionBuilder.build());
//        if(djiError == null){
//            setResultToToast("load waypoint succeed");
//        }else {
//            setResultToToast("load waypoint failed");
//        }
    }
    public WaypointMissionOperator getWaypointMissionOperator(){
        if(instance == null){
            instance = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
        }
        return instance;
    }
    private void setResultToToast(final String msg){
                Toast.makeText(getActivity(),msg,Toast.LENGTH_LONG).show();
    }
    String nulltoIntgerDefault(String str){
        if (!isInteger(str)){
            str="0";
        }
        return str;
    }
    boolean isInteger(String str){
        try{
            str = str.replace(" ","");
            Integer.parseInt(str);
        }catch (Exception e){
            return false;
        }
        return true;
    }
    private boolean saveMission(String name){
        File dir=new File(AutoPatrolApplication.missionDir);
        if(!dir.exists()){
            if(!dir.mkdirs()){
                setResultToToast("can't create dir");
            }
        }
        File newMission=new File(AutoPatrolApplication.missionDir+"/"+name);
        try {
            newMission.createNewFile();
        }catch (IOException e){
            e.printStackTrace();
            return false;
        }
        refreshSpinner();
        Intent intent=new Intent("MISSION_ITEMS_CHANGE");
        getActivity().sendBroadcast(intent);
        return true;
    }
    AdapterView.OnItemSelectedListener onItemSelectedListener =new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    };
}
