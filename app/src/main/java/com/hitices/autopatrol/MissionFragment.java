package com.hitices.autopatrol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
import com.amap.api.maps2d.UiSettings;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;
import com.amap.api.maps2d.model.Polygon;
import com.amap.api.maps2d.model.PolygonOptions;
import com.amap.api.maps2d.model.Polyline;
import com.amap.api.maps2d.model.PolylineOptions;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.hitices.autopatrol.missions.BaseMission;
import com.hitices.autopatrol.missions.MissionType;
import com.hitices.autopatrol.missions.PolygonMission;
import com.hitices.autopatrol.missions.WaypointsMission;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.sql.StatementEvent;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.sdkmanager.DJISDKManager;

public class MissionFragment extends Fragment implements View.OnClickListener{
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
    private BaseMission currentMission;
    private boolean creatable;
    private String lastSelectedMissionName="";
    //waypoint mission
    private List<Marker> mMarkers = new ArrayList<>();
    //polygon mission
    private Polyline polyline;
    private Polygon polygon;
    private Marker startPoint;

    //location
    private AMapLocationClient mlocationClient;
    //info window
    private boolean flag_isShow=false;
    private Marker currentMarker;
    private Marker location;
    public MissionFragment() {
        // Required empty public constructor
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
        super.onAttach(context);
        IntentFilter filter = new IntentFilter();
        filter.addAction(AutoPatrolApplication.FLAG_CONNECTION_CHANGE);
        context.registerReceiver(mReceiver, filter);
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
    @Override
    public void onClick(View view){
        menu.collapse();
        switch (view.getId()){
            case R.id.add_polygon_mission:
                addPolygonMoission();
                break;
            case R.id.add_waypoint_mission:
                addWaypointMission();
                break;
            case R.id.import_mission:
                //test
//                WaypointsMission w=new WaypointsMission("test");
//                w.readMission(AutoPatrolApplication.missionDir+"/121212.xml");
                break;
            case R.id.adjust_mission:
                if(currentMission != null) {
                    creatable = !creatable;
                    if(creatable)
                        currentMission.FLAG_ISSAVED=false;
                }else setResultToToast("当前无任务，请先新建或导入任务");
                break;
            case R.id.mission_setting:
                    if (creatable) {
                        if(currentMission.missionType == MissionType.WaypointMission)
                           showWaypointsSettingDialog();
                        else if(currentMission.missionType == MissionType.PolygonMission)
                            showPolygonSettingDialog();
                    }
            default:
                break;
        }
    }

    private void initMapView(){
        if(aMap == null){
            aMap=mapView.getMap();
            aMap.setOnMapClickListener(onMapClickListener);
            aMap.setOnMarkerClickListener(markerClickListener);
            aMap.setOnMarkerDragListener(markerDragListener);
        }
        UiSettings settings=aMap.getUiSettings();
        settings.setZoomControlsEnabled(false);
        //use gps location
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
    protected void initSpinner(){
        arrayAdapter=new ArrayAdapter<>(getContext(),android.R.layout.simple_spinner_item,AutoPatrolApplication.getMissionList());
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(arrayAdapter);
        spinner.setOnItemSelectedListener(onItemSelectedListener);
    }
    private WaypointsMission getCurrentWaypointsMission(){
        if(currentMission.missionType == MissionType.WaypointMission)
            return (WaypointsMission)currentMission;
        else return null;
    }
    private PolygonMission getCurrentPolygonMission(){
        if(currentMission.missionType == MissionType.PolygonMission)
            return (PolygonMission)currentMission;
        else return null;
    }
    private void markWaypoint(LatLng latLng){
        if(currentMarker != null)
            currentMarker.hideInfoWindow();
        //amap about
        if(creatable){
            MarkerOptions markerOptions =new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
            markerOptions.draggable(true);
            markerOptions.title("waypoint");
            aMap.addMarker(markerOptions);
            Marker marker =aMap.addMarker(markerOptions);
            mMarkers.add(marker);
         }else {
            setResultToToast("请选择修改任务");
        }
    }
    private void setWaypointList(LatLng latLng){
        //dji mission about
        //直接加到class里
        if(creatable && getCurrentWaypointsMission() != null){
            getCurrentWaypointsMission().addWaypointList(latLng);
        }
    }
    private void drawPolygon(LatLng latLng){
        getCurrentPolygonMission().addVertex(latLng);
        if(polyline != null){
            polyline.remove();
        }
        if(polygon != null)
            polygon.remove();
        if(startPoint != null)
            startPoint.remove();
        PolylineOptions options=new PolylineOptions().addAll(getCurrentPolygonMission().getVertexs());
        if(getCurrentPolygonMission().getVertexs().size()>0)
            options.add(getCurrentPolygonMission().getVertexs().get(0));
        polyline=aMap.addPolyline(options);
        polygon=aMap.addPolygon(new PolygonOptions().addAll(getCurrentPolygonMission().getVertexs())
                                                    .fillColor(getResources().getColor(R.color.fillColor)));
        if(getCurrentPolygonMission().getVertexs().size()==1)
            drawStartPoint(getCurrentPolygonMission().getVertexs().get(0));

    }
    private void drawStartPoint(LatLng latLng){
        MarkerOptions markerOptions =new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        markerOptions.title("startPoint");
        aMap.addMarker(markerOptions);
        startPoint =aMap.addMarker(markerOptions);
    }
    private void backPolygonVertex(){
//        int size=mVertexs.size();
//        if(size>0){
//            mVertexs.remove(size-1);
//        }
//        if(polyline != null){
//            polyline.remove();
//        }
//        if(polygon != null)
//            polygon.remove();
//        PolylineOptions options=new PolylineOptions().addAll(mVertexs);
//        if(mVertexs.size()>0)
//            options.add(mVertexs.get(0));
//        polyline=aMap.addPolyline(options);
//        polygon=aMap.addPolygon(new PolygonOptions().addAll(mVertexs)
//                .fillColor(getResources().getColor(R.color.pink_pressed)));
    }
    private void refreshMapView(){
        for(int i=0;i<mMarkers.size();i++) {
            if ( !mMarkers.get(i).getTitle().equals("marker"))
                mMarkers.get(i).destroy();
        }
        if(polygon != null)
            polygon.remove();
        if(polyline != null)
            polyline.remove();
        if(startPoint!= null)
            startPoint.remove();
        mMarkers.clear();
        if(aMap == null)
            initMapView();
        //depend on missiontype
        if(currentMission.missionType == MissionType.WaypointMission) {
            //update new markers

            for (int i = 0; i < ((WaypointsMission)currentMission).waypointList.size(); i++) {
                MarkerOptions markerOptions = new MarkerOptions();
                LatLng ll = new LatLng(((WaypointsMission)currentMission).waypointList.get(i).coordinate.getLatitude(),
                        ((WaypointsMission)currentMission).waypointList.get(i).coordinate.getLongitude());
                markerOptions.position(ll);
                System.out.println("\nnewtest" + String.valueOf(ll.latitude) + " : " + String.valueOf(ll.longitude));
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                markerOptions.draggable(true);
                markerOptions.title("waypoint");
                aMap.addMarker(markerOptions);
                aMap.moveCamera(CameraUpdateFactory.newLatLng(ll));
                cameraUpdate(ll);
                Marker marker = aMap.addMarker(markerOptions);
                mMarkers.add(marker);
//                for (int i = 0; i < currentWaypointsMission.waypointList.size(); i++) {
//                MarkerOptions markerOptions = new MarkerOptions();
//                LatLng ll = new LatLng(currentWaypointsMission.waypointList.get(i).coordinate.getLatitude(),
//                        currentWaypointsMission.waypointList.get(i).coordinate.getLongitude());
//                markerOptions.position(ll);
//                System.out.println("\nnewtest" + String.valueOf(ll.latitude) + " : " + String.valueOf(ll.longitude));
//                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
//                markerOptions.draggable(true);
//                markerOptions.title("waypoint");
//                aMap.addMarker(markerOptions);
//                aMap.moveCamera(CameraUpdateFactory.newLatLng(ll));
//                cameraUpdate(ll);
//                Marker marker = aMap.addMarker(markerOptions);
//                mMarkers.add(marker);
            }
        }else if(currentMission.missionType == MissionType.PolygonMission){
            //polygon mission,draw polygon
            PolylineOptions options=new PolylineOptions().addAll(getCurrentPolygonMission().getVertexs());
            if(getCurrentPolygonMission().getVertexs().size()>0)
                options.add(getCurrentPolygonMission().getVertexs().get(0));
            polyline=aMap.addPolyline(options);
            polygon=aMap.addPolygon(new PolygonOptions().addAll(getCurrentPolygonMission().getVertexs())
                    .fillColor(getResources().getColor(R.color.fillColor)));
        }

    }
    private void refreshSpinnerColor(){
        switch (currentMission.missionType){
            case PolygonMission:
                spinner.setBackgroundColor(getResources().getColor(R.color.selected));
                break;
            case WaypointMission:
                spinner.setBackgroundColor(getResources().getColor(R.color.pink_pressed));
                break;
            default:
                break;
        }
    }
    private void cameraUpdate(LatLng pos){
//        LatLng pos =new LatLng(droneLocationLat,droneLocationLng);
        float zoomLevel = (float)18.0;
        CameraUpdate cameraupdate =CameraUpdateFactory.newLatLngZoom(pos,zoomLevel);
        aMap.moveCamera(cameraupdate);
    }
    private void showWaypointsSettingDialog(){
        final List<WaypointAction> actions=getCurrentWaypointsMission().currentGeneralActions;
        LinearLayout wayPointSettings = (LinearLayout)getLayoutInflater().inflate(R.layout.dialog_waypointsetting,null);
        final TextView tv_wayPointAltitude= wayPointSettings.findViewById(R.id.altitude);
        final TextView seekBar_speed=wayPointSettings.findViewById(R.id.seekBar_text);
        final TextView mName=wayPointSettings.findViewById(R.id.setting_mName);
        Button mSave=wayPointSettings.findViewById(R.id.dialog_save);
        Button mDelete=wayPointSettings.findViewById(R.id.dialog_delete);
        GridView gv_missions=wayPointSettings.findViewById(R.id.gv_general_actions);
        SeekBar sb_speed=wayPointSettings.findViewById(R.id.speed);
        RadioGroup rg_actionAfterFinished =wayPointSettings.findViewById(R.id.actionAfterFinished);
        RadioGroup rg_heading =wayPointSettings.findViewById(R.id.heading);

        //init
        tv_wayPointAltitude.setText(String.valueOf(50));
       // mName.setText(arrayAdapter.getItem(0).toString());
        mName.setText(getCurrentWaypointsMission().missionName);
        sb_speed.setMax(15);
        sb_speed.setProgress(10);
        seekBar_speed.setText(String.valueOf(10)+" m/s");
        rg_actionAfterFinished.check(R.id.finishGoHome);
        rg_heading.check(R.id.headingWP);
        final GridviewAdapter gridviewAdapter=new GridviewAdapter(actions,getContext());
        gv_missions.setAdapter(gridviewAdapter);
        sb_speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                seekBar_speed.setText(String.valueOf(i)+" m/s");
                getCurrentWaypointsMission().speed=i;
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
                    getCurrentWaypointsMission().finishedAction=WaypointMissionFinishedAction.NO_ACTION;
                }else if(i == R.id.finishGoHome){
                    getCurrentWaypointsMission().finishedAction=WaypointMissionFinishedAction.GO_HOME;
                }else if (i == R.id.finishAutoLanding){
                    getCurrentWaypointsMission().finishedAction=WaypointMissionFinishedAction.AUTO_LAND;
                }else if (i == R.id.finishToFirst){
                    getCurrentWaypointsMission().finishedAction=WaypointMissionFinishedAction.GO_FIRST_WAYPOINT;
                }
            }
        });
        rg_heading.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if(i == R.id.headingNext){
                    getCurrentWaypointsMission().headingMode=WaypointMissionHeadingMode.AUTO;
                }else if(i == R.id.headingInitDirec){
                    getCurrentWaypointsMission().headingMode=WaypointMissionHeadingMode.USING_INITIAL_DIRECTION;
                }else if (i == R.id.headingRC){
                    getCurrentWaypointsMission().headingMode=WaypointMissionHeadingMode.CONTROL_BY_REMOTE_CONTROLLER;
                }else if (i == R.id.headingWP){
                    getCurrentWaypointsMission().headingMode=WaypointMissionHeadingMode.USING_WAYPOINT_HEADING;
                }
            }
        });
        final AlertDialog dialog;
        AlertDialog.Builder builder=new AlertDialog.Builder(getContext());
        builder .setTitle("")
                .setView(wayPointSettings)
                .setPositiveButton("finish", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String altitudeStr = tv_wayPointAltitude.getText().toString();
                        getCurrentWaypointsMission().genernalWaypointSetting(
                                Integer.parseInt(nulltoIntgerDefault(altitudeStr)),
                                gridviewAdapter.getSelectedAction());
                    }
                })
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //test
                        setResultToToast(String.valueOf(gridviewAdapter.getSelectedAction().size()));
                        dialogInterface.cancel();

                    }
                });
        dialog=builder.create();
        mSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String altitudeStr = tv_wayPointAltitude.getText().toString();
                getCurrentWaypointsMission().genernalWaypointSetting(
                        Integer.parseInt(nulltoIntgerDefault(altitudeStr)),
                        gridviewAdapter.getSelectedAction());
                if(saveMission()){
                    dialog.cancel();
                }else {
                    setResultToToast("保存任务失败");
                }
            }
        });
        mDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(deleteMission()){
                    arrayAdapter.remove(getCurrentWaypointsMission().missionName);
                    currentMission=null;
                    if(arrayAdapter.getCount()>0)
                       spinner.setSelection(0,true);
                    else
                        spinner.setSelected(false);
                    sendMissionChange();
                    dialog.cancel();
                }else {
                    setResultToToast("删除任务失败");
                }

            }
        });
        dialog.show();
    }
    private void showPolygonSettingDialog(){
        LinearLayout settingView=(LinearLayout)getLayoutInflater().inflate(R.layout.dialog_polygon_setting,null);
        TextView mName=settingView.findViewById(R.id.polygon_setting_mName);
        GridView Vertexs=settingView.findViewById(R.id.polygon_setting_vertexs);
        //init
        mName.setText(getCurrentPolygonMission().missionName);
        Vertexs.setAdapter(new PSGridviewAdapter(getContext()));
        AlertDialog.Builder builder=new AlertDialog.Builder(getContext());
        builder.setView(settingView);
        builder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                getCurrentPolygonMission().saveMission();
            }
        });
        AlertDialog alertDialog=builder.create();
        alertDialog.show();
    }
    private boolean saveMission(){
        boolean flag=currentMission.saveMission();
        //update mission list
        if(!flag){
            setResultToToast("can't save current mission");
            return false;
        }
        setResultToToast("success");
        creatable=false;
        sendMissionChange();
        //currentWaypointsMission.readMission(AutoPatrolApplication.missionDir+"/"+currentWaypointsMission.missionName+".xml");
        return true;
    }
    private boolean deleteMission(){
        File f=new File(AutoPatrolApplication.missionDir+"/"+getCurrentWaypointsMission().missionName+".xml");
        if(f.exists()){
            f.delete();
            return true;
        }
        return false;
    }
    private void addWaypointMission(){
        AlertDialog.Builder adBuilder=new AlertDialog.Builder(getContext());
        adBuilder.setTitle("请输入航点任务名称");
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
                            spinner.setSelection(0,true);
                            currentMission = new WaypointsMission(arrayAdapter.getItem(0).toString());
                            lastSelectedMissionName=getCurrentWaypointsMission().missionName;
                            creatable = true;
                            alertDialog.dismiss();
                            refreshMapView();
                        }else {
                            setResultToToast("任务名不能为空");
                        }
                    }
                });

        alertDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
    }
    private void addPolygonMoission(){
        AlertDialog.Builder adBuilder=new AlertDialog.Builder(getContext());
        adBuilder.setTitle("请输入区域任务名称");
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
                            spinner.setSelection(0,true);
//                            currentWaypointsMission = new WaypointsMission(arrayAdapter.getItem(0).toString());
//                            lastSelectedMissionName=currentWaypointsMission.missionName;
                            currentMission=new PolygonMission(arrayAdapter.getItem(0).toString());
                            //BaseMission bs=currentPolygonMission;
                            creatable = true;
                            alertDialog.dismiss();
                            refreshMapView();
                        }else {
                            setResultToToast("任务名不能为空");
                        }
                    }
                });

        alertDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
    }
    private void importMission(){
        //注意修改lastSelectedMissionName的值
    }


    public void readWaypointsMission(Document doc,WaypointsMission newWaypointsMission){
        try {
            NodeList nodes=doc.getElementsByTagName("speed");
            if(nodes.item(0) != null){
                newWaypointsMission.speed=Float.parseFloat(nodes.item(0).getTextContent());
            }
            //finishedAction
            nodes=doc.getElementsByTagName("finishedAction");
            if(nodes.item(0) != null){
                newWaypointsMission.finishedAction=getFinishedAction(nodes.item(0).getTextContent());
            }
            //headingMode
            nodes=doc.getElementsByTagName("headingMode");
            if(nodes.item(0) != null){
                newWaypointsMission.headingMode=getHeadingMode(nodes.item(0).getTextContent());
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
                    newWaypointsMission.waypointList.add(w);
                    newWaypointsMission.waypoints.put(ll,w);
                    System.out.println("\nlat and lng" + String.valueOf(ll.latitude)+" : "+String.valueOf(ll.longitude));
                }
            }
            newWaypointsMission.FLAG_ISSAVED=true;
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void readPolygonMission(Document doc,PolygonMission newPolygonMission){
        NodeList nodes=doc.getElementsByTagName("Vertexs");
        Node node=nodes.item(0);

        NodeList nVertexList = ((Element)node).getElementsByTagName("vertex");
        for (int temp = 0; temp < nVertexList.getLength(); temp++) {
            Node nNode = nVertexList.item(temp);
            System.out.println("\nvertex :" + nNode.getNodeName());
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                Element eElement = (Element) nNode;
                LatLng ll=new LatLng(
                        Double.parseDouble(eElement.getElementsByTagName("latitude").item(0).getTextContent()),
                        Double.parseDouble(eElement.getElementsByTagName("longitude").item(0).getTextContent()));
                newPolygonMission.addVertex(ll);
                System.out.println("\npolygon" + String.valueOf(ll.latitude)+" : "+String.valueOf(ll.longitude));
            }
        }
        newPolygonMission.FLAG_ISSAVED=true;
    }
    public BaseMission readBaseMission(String path){
        try {
            //need test,
            File file = new File(path);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
//            //root
            doc.getDocumentElement().normalize();
            String type=doc.getDocumentElement().getNodeName();
            System.out.println("Root element :" + type);
            //name
            BaseMission newMission;

            NodeList nodes=doc.getElementsByTagName("missionName");
            if(nodes.item(0) == null){
                return null;
            }else {
                if(type.equals("PolygonMission"))
                {
                    newMission=new PolygonMission(nodes.item(0).getTextContent());
                    readPolygonMission(doc,(PolygonMission)newMission);
                }else if(type.equals("WaypointsMission")){
                    newMission=new WaypointsMission(nodes.item(0).getTextContent());
                    readWaypointsMission(doc,(WaypointsMission)newMission);
                }else {
                    return null;
                }
            }
            newMission.FLAG_ISSAVED=true;
            return newMission;
        }catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private class myInfoWindowAdapter implements AMap.InfoWindowAdapter,View.OnClickListener{
        @Override
        public View getInfoContents(Marker marker){
            return null;
        }
        View infoWindow=null;
        TextView tv_index;
        TextView tv_lat;
        TextView tv_lng;
        EditText tv_altitude;
        GridView gv_actions;
        Waypoint waypoint;
        Marker mMarker;
        Button btn_deleteWaypoint,btn_save;
        List<WaypointAction> actions;
        GridviewAdapter gva;
        public void getWaypoint(Marker marker){
            mMarker=marker;
            LatLng latLng=marker.getPosition();
            waypoint=getCurrentWaypointsMission().getWaypoint(latLng);
            if(waypoint == null){
                actions=new ArrayList<>();
                Log.e("rhys","can't find waypoint");
            }else {
                actions=waypoint.waypointActions;
                Log.e("rhys","actions nums:"+String.valueOf(actions.size()));
            }
        }
        @NonNull
        public View initView(LatLng latLng){
            View view=LayoutInflater.from(getContext()).inflate(R.layout.waypoint_infowindow,null);
            tv_altitude=view.findViewById(R.id.waypoint_altitude);
            tv_index=view.findViewById(R.id.waypoint_index);
            tv_lat=view.findViewById(R.id.waypoint_lat);
            tv_lng=view.findViewById(R.id.waypoint_lng);
            btn_deleteWaypoint=view.findViewById(R.id.deleteWaypoint);
            btn_deleteWaypoint.setOnClickListener(this);
            btn_save=view.findViewById(R.id.Waypoint_savechange);
            btn_save.setOnClickListener(this);
            gv_actions=view.findViewById(R.id.waypointActions);
            //init data
            if(waypoint != null){
                tv_altitude.setText(String.valueOf(waypoint.altitude));//remain to modify
                //tv_index.setText("编号："+String.valueOf());
                tv_lat.setText("纬度："+String.valueOf(waypoint.coordinate.getLatitude()));
                tv_lng.setText("经度："+String.valueOf(waypoint.coordinate.getLongitude()));
            }
            //gv_actions.setAdapter(myActionsAdapter);
            gva=new GridviewAdapter(actions,getContext());
            gv_actions.setAdapter(gva);
            return view;
        }
        @Override
        public void onClick(View view){
            switch (view.getId())
            {
                case R.id.deleteWaypoint:
                    deleteCurrentPoint();
                    break;
                case R.id.Waypoint_savechange:
                    saveChanges();
                    break;
                default:
                    break;
            }
        }
        private void deleteCurrentPoint(){
            if(creatable) {
                getCurrentWaypointsMission().removeWaypoint(mMarker.getPosition());
                mMarkers.remove(mMarker);
                mMarker.hideInfoWindow();
                mMarker.destroy();
            }
            else
                setResultToToast("不可删除，请选择修改任务");
        }
        private void saveChanges(){
            if(creatable) {
                waypoint.altitude=Float.valueOf(tv_altitude.getText().toString());
                waypoint.removeAllAction();
                for(int j=0;j<gva.getSelectedAction().size();j++){
                    waypoint.addAction(new WaypointAction(gva.getSelectedAction().get(j),j));
                }
                mMarker.hideInfoWindow();
            }
            else
                setResultToToast("不可修改任务");
        }
        @Override
        public View getInfoWindow(Marker marker){
                getWaypoint(marker);
                infoWindow=initView(marker.getPosition());
            return infoWindow;
        }
    }
    public class GridviewAdapter extends BaseAdapter {
        private List<WaypointAction> actions;
        private List<WaypointActionType> allAction;
        private List<WaypointActionType> selectedAction;
        private LayoutInflater inflater = null;
        GridviewAdapter(List<WaypointAction> list, Context context) {
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
    private class PSGridviewAdapter extends BaseAdapter {
        private LayoutInflater inflater;
        PSGridviewAdapter(Context context){
            inflater=LayoutInflater.from(context);
        }
        @Override
        public int getCount() {
            return getCurrentPolygonMission().getVertexs().size();
        }
        @Override
        public Object getItem(int position) {
            return getCurrentPolygonMission().getVertexs().get(position);
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
            holder.lat.setText(String.valueOf(getCurrentPolygonMission().getVertexs().get(position).latitude));
            holder.lng.setText(String.valueOf(getCurrentPolygonMission().getVertexs().get(position).longitude));
            return convertView;
        }
        class ViewHolder{
            TextView id,lat,lng;
        }

    }


    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
    AMap.OnMarkerDragListener markerDragListener = new AMap.OnMarkerDragListener() {
        LatLng tempLatlng;
        @Override
        public void onMarkerDragStart(Marker arg0) {
            tempLatlng=arg0.getPosition();
        }
        @Override
        public void onMarkerDragEnd(Marker arg0) {
            getCurrentWaypointsMission().waypointList.remove(new Waypoint(tempLatlng.latitude,tempLatlng.longitude,getCurrentWaypointsMission().altitude));
            getCurrentWaypointsMission().addWaypointList(arg0.getPosition());
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
    AMap.OnMapClickListener onMapClickListener=new AMap.OnMapClickListener() {
        @Override
        public void onMapClick(LatLng latLng) {
            switch (currentMission.missionType){
                case WaypointMission:
                    markWaypoint(latLng);
                    setWaypointList(latLng);
                    break;
                case PolygonMission:
                    if(creatable) {
                        drawPolygon(latLng);
                    }
                    else {
                        setResultToToast("请选择修改任务");
                    }
                    break;
            }
        }
    };
    AdapterView.OnItemSelectedListener onItemSelectedListener =new AdapterView.OnItemSelectedListener() {
        private String currentindex="";
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
            currentindex=arrayAdapter.getItem(pos);
            //setResultToToast(String.valueOf(lastSelectedMissionName)+"now:"+String.valueOf(currentindex));
            if(currentMission != null) {
                if (!lastSelectedMissionName.equals(currentindex)) {  //for init,remind me to delete
                    if (!currentMission.FLAG_ISSAVED) {
                        //unsaved
                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        builder.setTitle("Warning");
                        builder.setMessage("当前任务未保存，继续则丢失当前任务");
                        builder.setPositiveButton("继续", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                arrayAdapter.remove(lastSelectedMissionName);
                                changeMission(currentindex);
                            }
                        });
                        builder.setNeutralButton("保存后继续", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //currentWaypointsMission.saveMission();
                                currentMission.saveMission();
                                changeMission(currentindex);
                            }
                        });
                        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.cancel();
                                spinner.setSelection(arrayAdapter.getPosition(lastSelectedMissionName), true);
                            }
                        });
                        builder.create().show();
                    } else {
                        changeMission(arrayAdapter.getItem(pos));
                    }
                }
            }else {
                changeMission(currentindex);
            }

        }
        private void changeMission(String name){
            if(name.length() >0 ){
//                currentWaypointsMission=readMission(AutoPatrolApplication.missionDir+"/"+name+".xml");
//                lastSelectedMissionName=currentWaypointsMission.missionName;
                currentMission=readBaseMission(AutoPatrolApplication.missionDir+"/"+name+".xml");
                lastSelectedMissionName=currentMission.missionName;
                spinner.setSelection(arrayAdapter.getPosition(lastSelectedMissionName), true);
                refreshMapView();
                refreshSpinnerColor();
                creatable=false;
                setResultToToast("当前任务为:"+currentMission.missionName);
            }
        }
        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
//            currentWaypointsMission=null;
            aMap.clear();
            currentMission=null;
        }

    };
    AMap.OnMarkerClickListener markerClickListener = new  AMap.OnMarkerClickListener() {
        @Override public boolean onMarkerClick(Marker marker)
        {
        if(currentMarker == null)
            currentMarker=marker;
        if(currentMarker.equals(marker))
        {
            if (!flag_isShow) {
                marker.showInfoWindow();
                flag_isShow = true;
            } else {
                marker.hideInfoWindow();
                flag_isShow = false;
            }
        }else {
            currentMarker.hideInfoWindow();
            marker.showInfoWindow();
            flag_isShow = true;
        }
        currentMarker=marker;
        return true;
        }
    };
    AMapLocationListener aMapLocationListener=new AMapLocationListener() {
        @Override
        public void onLocationChanged(AMapLocation amapLocation) {
            if (amapLocation != null) {
              if (amapLocation.getErrorCode() == 0) {
//
                   LatLng harbin = new LatLng(amapLocation.getLatitude(),amapLocation.getLongitude());
//                    LatLng harbin = new LatLng(126.640692,45.748065);
                    MarkerOptions markerOptions=  new MarkerOptions();
                    markerOptions.position(harbin);
                    markerOptions.title("marker");
                    markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(),R.drawable.ic_location_marker)));
                    if(location != null)
                        location.destroy();
                    location=aMap.addMarker(markerOptions);
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
}
