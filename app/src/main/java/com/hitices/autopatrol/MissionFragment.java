package com.hitices.autopatrol;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
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
import com.hitices.autopatrol.missions.WaypointsMission;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.sdkmanager.DJISDKManager;

public class MissionFragment extends Fragment
        implements View.OnClickListener,
             AMap.OnMapClickListener{

    private OnFragmentInteractionListener mListener;
    private static final String MISSION_STATE_SAVE_IS_HIDDEN="MISSION_STATE_SAVE_IS_HIDDEN";

    FloatingActionsMenu menu;
    FloatingActionButton btn_waypoint,btn_polygon,btn_import,btn_adjust;
    ImageButton btn_setting;
    private AMap aMap;
    private MapView mapView;
    private Spinner spinner;
    private ArrayAdapter<String> arrayAdapter;
    //mission
    private WaypointsMission currentMission;
    private boolean creatable;
    private final Map<Integer,Marker> mMarkers = new ConcurrentHashMap<>();
    private WaypointMissionOperator instance;
    //location
    private AMapLocationClient mlocationClient;

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
    public void onResume(){
        super.onResume();
        mapView.onResume();
    }
    @Override
    public void onPause(){
        super.onPause();
        mapView.onPause();
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
        mapView.onDestroy();
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
        initSpinner();
        menu=view.findViewById(R.id.mission_menu);
        btn_adjust=view.findViewById(R.id.adjust_mission);
        btn_polygon=view.findViewById(R.id.add_polygon_mission);
        btn_waypoint=view.findViewById(R.id.add_waypoint_mission);
        btn_import=view.findViewById(R.id.import_mission);
        btn_setting=view.findViewById(R.id.mission_setting);
        btn_adjust.setOnClickListener(this);
        btn_polygon.setOnClickListener(this);
        btn_waypoint.setOnClickListener(this);
        btn_import.setOnClickListener(this);
        btn_setting.setOnClickListener(this);
        return view;
    }
    protected void initSpinner(){
        arrayAdapter=new ArrayAdapter<>(getContext(),android.R.layout.simple_spinner_item,AutoPatrolApplication.getMissionList());
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(arrayAdapter);
        spinner.setOnItemSelectedListener(onItemSelectedListener);
    }
    @Override
    public void onClick(View view){
        menu.collapse();
        switch (view.getId()){
            case R.id.add_polygon_mission:
                break;
            case R.id.add_waypoint_mission:
                addWaypointMission();
                break;
            case R.id.import_mission:
                break;
            case R.id.adjust_mission:
                break;
            case R.id.mission_setting:
                if (creatable)
                   showSettingDialog();
            default:
                break;
        }
    }
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
    private void initMapView(){
        if(aMap == null){
            aMap=mapView.getMap();
            aMap.setOnMapClickListener(this);
            aMap.setOnMarkerClickListener(markerClickListener);
            aMap.setOnMarkerDragListener(markerDragListener);
        }
        //location
        AMapLocationClientOption mLocationOption  = new AMapLocationClientOption();
        mlocationClient = new AMapLocationClient(getContext());
        mlocationClient.setLocationListener(aMapLocationListener);
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        mLocationOption.setInterval(2000);
        mlocationClient.setLocationOption(mLocationOption);
        mlocationClient.startLocation();
        //info
        aMap.setOnInfoWindowClickListener(onInfoWindowClickListener);
        myInfoWindowAdapter myInfoWindowAdapter=new myInfoWindowAdapter();
        aMap.setInfoWindowAdapter(myInfoWindowAdapter);
    }
    @Override
    public void onMapClick(LatLng latLng){
        markWaypoint(latLng);
        setWaypointList(latLng);
    }
    AMap.OnMarkerClickListener markerClickListener = new AMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {
            //showSingleWaypointSetting();
            marker.showInfoWindow();
            return true;
        }
    };
    AMapLocationListener aMapLocationListener=new AMapLocationListener() {
        @Override
        public void onLocationChanged(AMapLocation amapLocation) {
            if (amapLocation != null) {
                if (amapLocation.getErrorCode() == 0) {
                    LatLng harbin = new LatLng(amapLocation.getLatitude(),amapLocation.getLongitude());
                    MarkerOptions markerOptions=  new MarkerOptions();
                    markerOptions.position(harbin);
                    markerOptions.title("marker");
                    markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(),R.drawable.ic_location_marker)));
                    aMap.addMarker(markerOptions);
                    aMap.moveCamera(CameraUpdateFactory.newLatLng(harbin));
                    cameraUpdate(harbin);
                } else {
                    //显示错误信息ErrCode是错误码，errInfo是错误信息，详见错误码表。
                    Log.e("AmapError","location Error, ErrCode:"
                            + amapLocation.getErrorCode() + ", errInfo:"
                            + amapLocation.getErrorInfo());
                }
            }
        }
    };
    AMap.OnMarkerDragListener markerDragListener = new AMap.OnMarkerDragListener() {
        LatLng tempLatlng;
        @Override
        public void onMarkerDragStart(Marker arg0) {
            tempLatlng=arg0.getPosition();
        }
        @Override
        public void onMarkerDragEnd(Marker arg0) {
            currentMission.waypointList.remove(new Waypoint(tempLatlng.latitude,tempLatlng.longitude,currentMission.altitude));
            currentMission.addWaypointList(arg0.getPosition());
        }
        @Override
        public void onMarkerDrag(Marker arg0) {
        }
    };
    AMap.OnInfoWindowClickListener onInfoWindowClickListener=new AMap.OnInfoWindowClickListener() {
        @Override
        public void onInfoWindowClick(Marker marker) {

        }
    };
    private void markWaypoint(LatLng latLng){
        //amap about
        if(creatable){
            MarkerOptions markerOptions =new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
            markerOptions.draggable(true);
            markerOptions.title("ttt");
            aMap.addMarker(markerOptions);
            Marker marker =aMap.addMarker(markerOptions);
//            marker.showInfoWindow();
            mMarkers.put(mMarkers.size(),marker);
         }else {
            setResultToToast("can't add waypoint");
        }
    }
    private void setWaypointList(LatLng latLng){
        //dji mission about
        //直接加到class里
        if(creatable && currentMission != null){
            currentMission.addWaypointList(latLng);
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
        final TextView seekBar_speed=wayPointSettings.findViewById(R.id.seekBar_text);
        final TextView mName=wayPointSettings.findViewById(R.id.setting_mName);
        SeekBar sb_speed=wayPointSettings.findViewById(R.id.speed);
        RadioGroup rg_actionAfterFinished =wayPointSettings.findViewById(R.id.actionAfterFinished);
        RadioGroup rg_heading =wayPointSettings.findViewById(R.id.heading);
        //init
        tv_wayPointAltitude.setText(String.valueOf(50));
        mName.setText(arrayAdapter.getItem(0).toString());
        sb_speed.setMax(15);
        sb_speed.setProgress(10);
        seekBar_speed.setText(String.valueOf(10)+" m/s");
//        RadioButton rb_finishedAction =wayPointSettings.findViewById(R.id.finishGoHome);
//        RadioButton rb_healing=wayPointSettings.findViewById(R.id.headingWP);
//        rb_finishedAction.setSelected(true);
//        rb_healing.setSelected(true);
        sb_speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                seekBar_speed.setText(String.valueOf(i)+" m/s");
                currentMission.speed=i;
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        rg_actionAfterFinished.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if(i == R.id.finishNone){
                    currentMission.finishedAction=WaypointMissionFinishedAction.NO_ACTION;
                }else if(i == R.id.finishGoHome){
                    currentMission.finishedAction=WaypointMissionFinishedAction.GO_HOME;
                }else if (i == R.id.finishAutoLanding){
                    currentMission.finishedAction=WaypointMissionFinishedAction.AUTO_LAND;
                }else if (i == R.id.finishToFirst){
                    currentMission.finishedAction=WaypointMissionFinishedAction.GO_FIRST_WAYPOINT;
                }
            }
        });
        rg_heading.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if(i == R.id.headingNext){
                    currentMission.headingMode=WaypointMissionHeadingMode.AUTO;
                }else if(i == R.id.headingInitDirec){
                    currentMission.headingMode=WaypointMissionHeadingMode.USING_INITIAL_DIRECTION;
                }else if (i == R.id.headingRC){
                    currentMission.headingMode=WaypointMissionHeadingMode.CONTROL_BY_REMOTE_CONTROLLER;
                }else if (i == R.id.headingWP){
                    currentMission.headingMode=WaypointMissionHeadingMode.USING_WAYPOINT_HEADING;
                }
            }
        });
        new AlertDialog.Builder(getContext()).setTitle("")
                .setView(wayPointSettings)
                .setPositiveButton("finish", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String altitudeStr = tv_wayPointAltitude.getText().toString();
                        currentMission.altitude = Integer.parseInt(nulltoIntgerDefault(altitudeStr));
                        saveMission();
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
    public WaypointMissionOperator getWaypointMissionOperator(){
        if(instance == null){
            instance = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
        }
        return instance;
    }
    private boolean saveMission(){
        boolean flag=currentMission.saveMisson();
        //update mission list
        if(!flag){
            setResultToToast("can't save current mission");
            return false;
        }
        setResultToToast("success");
        creatable=false;
        sendMissionChange();
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
    private void addWaypointMission(){
        AlertDialog.Builder adBuilder=new AlertDialog.Builder(getContext());
        adBuilder.setTitle("请输入任务名称");
        final EditText input=new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
        input.selectAll();
        input.setFocusable(true);
        input.setFocusableInTouchMode(true);
        input.requestFocus();
        adBuilder.setView(input);
        adBuilder.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        adBuilder.setPositiveButton("confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {}
        });
        final AlertDialog alertDialog= adBuilder.create();
        alertDialog.show();
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String text=input.getText().toString().trim();
                        if(text.length()>0) {
                            arrayAdapter.insert(text, 0);
                            currentMission = new WaypointsMission(arrayAdapter.getItem(0).toString());
                            creatable = true;
                            alertDialog.dismiss();
                        }else {
                            setResultToToast("任务名不能为空");
                        }
                    }
                });

        alertDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
    }
    //single waypoint setting,infowindow
    private class myInfoWindowAdapter implements AMap.InfoWindowAdapter,View.OnClickListener{
        @Override
        public View getInfoContents(Marker marker){
           return null;
        }
        View infoWindow=null;
        @Override
        public View getInfoWindow(Marker marker){
            if(infoWindow == null){
                infoWindow=LayoutInflater.from(getContext()).inflate(R.layout.waypoint_infowindow,null);
                render(marker,infoWindow);
            }
            return infoWindow;
        }
        public void render(Marker marker,View view){

        }
        @Override
        public void onClick(View view){

        }
    }


    private void setResultToToast(final String msg){
        Toast.makeText(getActivity(),msg,Toast.LENGTH_LONG).show();
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
    String nulltoIntgerDefault(String str){
        if (!isInteger(str)){
            str="0";
        }
        return str;
    }
    private void sendMissionChange(){
        Intent intent=new Intent("MISSION_ITEMS_CHANGE");
        getActivity().sendBroadcast(intent);
    }
}
