package com.hitices.autopatrol.activity;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
import com.amap.api.maps2d.model.PolygonOptions;
import com.amap.api.maps2d.model.PolylineOptions;
import com.hitices.autopatrol.AutoPatrolApplication;
import com.hitices.autopatrol.R;
import com.hitices.autopatrol.entity.dataSupport.FlightRecord;
import com.hitices.autopatrol.entity.dataSupport.PatrolMission;
import com.hitices.autopatrol.entity.missions.BaseModel;
import com.hitices.autopatrol.entity.missions.FlatlandModel;
import com.hitices.autopatrol.entity.missions.MultiPointsModel;
import com.hitices.autopatrol.entity.missions.SlopeModel;
import com.hitices.autopatrol.helper.GoogleMapHelper;
import com.hitices.autopatrol.helper.MissionHelper;

import org.litepal.crud.DataSupport;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.gimbal.GimbalMode;
import dji.common.gimbal.Rotation;
import dji.common.gimbal.RotationMode;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.mission.waypoint.WaypointMissionState;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

/**
 * MissionExecuteActivity 用于执行waypoint类型任务
 * created by Rhys
 */
public class MissionExecuteActivity extends Activity implements View.OnClickListener {
    public WaypointMission.Builder builder;
    private PatrolMission patrolMission;
    private List<BaseModel> modelList;
    private List<BaseModel> executeModelList;
    private FlightController mFlightController;
    private FlightRecord record;

    private WaypointMissionOperator operatorInstance;
    private MapView mapView;
    private AMap aMap;
    private Marker droneMarker;
    private LatLng droneLocation;
    private LatLng humanLocation;
    private Marker humanLocationMarker;



    AMapLocationListener aMapLocationListener = new AMapLocationListener() {
        @Override
        public void onLocationChanged(AMapLocation amapLocation) {
            if (amapLocation != null) {
                if (amapLocation.getErrorCode() == 0) {
                    humanLocation = new LatLng(amapLocation.getLatitude(), amapLocation.getLongitude());
//                    LatLng harbin = new LatLng(126.640692,45.748065);-
                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(humanLocation);
                    markerOptions.title("marker");
                    markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(),
                            R.drawable.ic_human_location_marker)));
                    if (humanLocationMarker != null)
                        humanLocationMarker.destroy();
                    humanLocationMarker = aMap.addMarker(markerOptions);
                } else {
                    //显示错误信息ErrCode是错误码，errInfo是错误信息，详见错误码表。
                    Log.e("AmapError", "humanLocationMarker Error, ErrCode:"
                            + amapLocation.getErrorCode() + ", errInfo:"
                            + amapLocation.getErrorInfo());
                }
            }
        }
    };
    private ImageButton uplaod, start, stop;
    /**
     * WaypointMissionOperatorListener
     */
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
            if (error == null) {
                record.setEndTime(new Date());
                record.save();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_execute);

        initUI();
        initMapView(savedInstanceState);
        addListener();
        initFlightController();
        initContent();
        showchildSeleteDialog();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        removeListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        initFlightController();
    }

    private void initUI() {
        uplaod = findViewById(R.id.execute_uploadMission);
        stop = findViewById(R.id.execute_stopMission);
        start = findViewById(R.id.execute_startMission);
        uplaod.setOnClickListener(this);
        start.setOnClickListener(this);
        stop.setOnClickListener(this);
    }

    /**
     * 初始化 mapview，amap，
     * 高德地图使用相关设置
     * 使用GPS定位
     *
     * @param savedInstanceState
     */
    private void initMapView(Bundle savedInstanceState) {
        mapView = findViewById(R.id.execute_mapview);
        mapView.onCreate(savedInstanceState);
        if (aMap == null) {
            aMap = mapView.getMap();
        }
        GoogleMapHelper.useGoogleMapSatelliteData(aMap);

        UiSettings settings = aMap.getUiSettings();
        settings.setZoomControlsEnabled(false);

        //use amap humanLocationMarker
        AMapLocationClientOption mLocationOption = new AMapLocationClientOption();
        AMapLocationClient mlocationClient = new AMapLocationClient(this);
        mlocationClient.setLocationListener(aMapLocationListener);
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        mLocationOption.setInterval(1000);
        mlocationClient.setLocationOption(mLocationOption);
        mlocationClient.startLocation();
    }

    /**
     * 初始化FlightController，
     * 可获取飞行器相关状态
     */
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
                            droneLocation = new LatLng(djiFlightControllerCurrentState.getAircraftLocation().getLatitude(),
                                    djiFlightControllerCurrentState.getAircraftLocation().getLongitude());
                            // setResultToToast("before坐标" + droneLocation.latitude + "," + droneLocation.longitude);
                            //gps 坐标转换成 高德坐标系
                            updateDroneLocation(GoogleMapHelper.WGS84ConvertToAmap(droneLocation));
                            //  setResultToToast("after坐标" + AutoPatrolApplication.WGS84ConvertToAmap(droneLocation).latitude + "," + AutoPatrolApplication.WGS84ConvertToAmap(droneLocation).longitude);
                        }
                    });
            setResultToToast("in flight control");
        }

    }

    /**
     * 根据传入的任务信息读取子任务列表
     */
    private void initContent() {
        Intent intent = getIntent();
        //String name=intent.getStringExtra("NAME");
        String path = intent.getStringExtra("PATH");
        int id = intent.getIntExtra("ID", -1);
        patrolMission = DataSupport.find(PatrolMission.class, id);
        if (patrolMission != null) {
            Log.e("debug", path);
            MissionHelper helper = new MissionHelper(path, patrolMission);
            modelList = helper.getModelList();
        }
    }
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.execute_uploadMission:
                uploadMission();
                break;
            case R.id.execute_startMission:
                startMission();
                break;
            case R.id.execute_stopMission:
                stopMission();
                break;
        }
    }

    /**
     * 在地图上标记航线
     */
    private void markWaypoints(List<Waypoint> waypoints) {
        List<LatLng> lines = new ArrayList<>();
        //lines.add(droneLocation);
        for (int i = 0; i < waypoints.size(); i++) {
            MarkerOptions markerOptions = new MarkerOptions();
            LatLng ll = new LatLng(waypoints.get(i).coordinate.getLatitude(),
                    waypoints.get(i).coordinate.getLongitude());
            lines.add(ll);
            markerOptions.position(ll);
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
            aMap.addMarker(markerOptions);
            cameraUpdate(ll);
        }
        //lines.add(droneLocation);
        aMap.addPolyline(new PolylineOptions().addAll(lines).color(Color.argb(125, 1, 1, 1)));
        setResultToToast("num of waypoint is " + String.valueOf(waypoints.size()));
    }

    private void markRangeOfExecuteModel() {
        for (int i = 0; i < executeModelList.size(); i++) {
            switch (executeModelList.get(i).getModelType()) {
                case MultiPoints:
                    break;
                case Flatland:
                    showFlatlandView((FlatlandModel) executeModelList.get(i));
                    break;
                case Slope:
                    break;
            }
        }

    }

    private void showMultiPointsView(MultiPointsModel model) {
        for (int i = 0; i < model.getWaypointList().size(); i++) {
            MarkerOptions markerOptions = new MarkerOptions();
            LatLng ll = new LatLng(
                    model.getWaypointList().get(i).coordinate.getLatitude(),
                    model.getWaypointList().get(i).coordinate.getLongitude());
            markerOptions.position(ll);
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
            markerOptions.draggable(true);
            markerOptions.title("waypoint");
            aMap.addMarker(markerOptions);
            cameraUpdate(ll);
            aMap.addMarker(markerOptions);

        }
    }

    private void showFlatlandView(FlatlandModel model) {
        PolylineOptions options = new PolylineOptions().addAll(model.getVertexs());
        if (model.getVertexs().size() > 0)
            options.add(model.getVertexs().get(0));
        aMap.addPolyline(options);
        aMap.addPolygon(new PolygonOptions().addAll(model.getVertexs())
                .fillColor(getResources().getColor(R.color.fillColor)));
        if (model.getVertexs().size() > 0) {
            cameraUpdate(model.getVertexs().get(0));
        } else {
            cameraUpdate(humanLocation);
        }
    }

    private void showSlopeView(SlopeModel model) {

    }

    /**
     * 动态更新飞行器位置
     *
     * @param latLng
     */
    private void updateDroneLocation(LatLng latLng) {
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

    /**
     * 更改地图缩放程度和地图中心点
     *
     * @param pos
     */
    private void cameraUpdate(LatLng pos) {
//        LatLng pos =new LatLng(droneLocationLat,droneLocationLng);
        float zoomLevel = (float) 16.0;
        CameraUpdate cameraupdate = CameraUpdateFactory.newLatLngZoom(pos, zoomLevel);
        aMap.moveCamera(cameraupdate);
    }

    private WaypointMissionOperator getWaypointMissionOperator() {
        if (operatorInstance == null) {
            operatorInstance = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
        }
        if (operatorInstance == null) {
            setResultToToast("waypointMissionOperator is null");
        }
        return operatorInstance;
    }

    /**
     * Add Listener for WaypointMissionOperator
     */
    private void addListener() {
        if (getWaypointMissionOperator() != null) {
            getWaypointMissionOperator().addListener(eventNotificationListener);
            setResultToToast("add listener");
        }
    }

    /**
     * remove Listener for WaypointMissionOperator
     */
    private void removeListener() {

        if (getWaypointMissionOperator() != null) {
            getWaypointMissionOperator().removeListener(eventNotificationListener);
            setResultToToast("remove listener");
        }
    }

    /**
     * 调整云台，让相机竖直向下
     */
    private void setCameraMode(int angle) {
        if (AutoPatrolApplication.getGimbalInstance() != null) {
            AutoPatrolApplication.getGimbalInstance().setMode(GimbalMode.FPV, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                }
            });
            Rotation.Builder b = new Rotation.Builder();
            b.mode(RotationMode.ABSOLUTE_ANGLE);
            b.pitch(-angle);
            AutoPatrolApplication.getGimbalInstance().rotate(b.build(), new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError == null) {
                        Log.i("rhys", "rotateGimbal success");
                    } else {
                        Log.i("rhys", "rotateGimbal error " + djiError.getDescription());
                    }
                }
            });
        }
    }


    private void showchildSeleteDialog() {
        LinearLayout view = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_execute_select_model, null);
        GridView gridView = view.findViewById(R.id.gridview_execute_select_child);
        //init gridview GridviewSeleteAdapter
        final GridviewSeleteAdapter adapter = new GridviewSeleteAdapter(this, modelList);
        Log.e("debug", String.valueOf(modelList.size()));
        gridView.setAdapter(adapter);
        new AlertDialog.Builder(this)
                .setTitle("")
                .setView(view)
                .setPositiveButton("确认", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        executeModelList = adapter.getExecuteList();
                        System.out.println("exec num:" + String.valueOf(executeModelList.size()));
                        missionProcessing();
                    }
                })
                .create()
                .show();
    }

    private List<Waypoint> generateExecutableWaypoints(@NonNull List<BaseModel> executeModelList) {
        List<Waypoint> waypoints = new ArrayList<>();
        LatLng formerPoint;
        if (!executeModelList.isEmpty()) {
            if (droneLocation == null) {
                formerPoint = humanLocation;
            } else {
                formerPoint = droneLocation;
            }
            for (int i = 0; i < executeModelList.size(); i++) {
                BaseModel model = executeModelList.get(i);
                model.generateExecutablePoints(formerPoint);
                waypoints.addAll(model.getExecutePoints());
//                switch (model.getModelType()){
//                    case Slope:
//
//                        break;
//                    case Flatland:
//                        ((FlatlandModel)model).generateExecutablePoints(formerPoint);
//                        waypoints.addAll(((FlatlandModel)model).getExecutePoints());
//                        break;
//                    case MultiPoints:
//                        ((MultiPointsModel)model).generateExecutablePoints(formerPoint);
//                        waypoints.addAll(((MultiPointsModel)model).getExecutePoints());
//                        break;
//                }
                formerPoint = model.getEndPoint();
            }
        }
        return waypoints;
    }

    private List<Waypoint> WaypointListConvert(List<Waypoint> list) {
        List<Waypoint> temp = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            temp.add(waypointConvert(list.get(i)));
        }
        return temp;
    }

    /**
     * 确认任务后的操作流程
     * 排序
     * load mission
     * 在地图标记航线
     */
    private void missionProcessing() {
        List<Waypoint> waypoints = generateExecutableWaypoints(executeModelList);
        markWaypoints(waypoints);
        markRangeOfExecuteModel();
        setCameraMode(90);
        loadMission(WaypointListConvert(waypoints), getAltitude());
    }

    /**
     * loadMission
     * 航点任务执行流程第一步
     */
    private void loadMission(@NonNull List<Waypoint> list, @NonNull float altitude) {
        setResultToToast("on load");
        //以无人机起飞位置作为返航点
        if (droneLocation != null) {
            list.add(new Waypoint(droneLocation.latitude, droneLocation.longitude, altitude));
        }
        //以当前人的位置作返航点
        else {
            if (humanLocation != null) {
                list.add(waypointConvert(new Waypoint(humanLocation.latitude, humanLocation.longitude, altitude)));
            }
        }
        builder = new WaypointMission.Builder();

        builder.waypointList(list);
        builder.waypointCount(list.size());

        float speed = getSpeed();
        builder.autoFlightSpeed(5);
        builder.maxFlightSpeed(5);

        builder.finishedAction(WaypointMissionFinishedAction.AUTO_LAND);
        builder.headingMode(WaypointMissionHeadingMode.USING_WAYPOINT_HEADING);
        builder.flightPathMode(WaypointMissionFlightPathMode.NORMAL);

        DJIError error = getWaypointMissionOperator().loadMission(builder.build());
        if (error == null) {
            setResultToToast("load success");
        } else {
            setResultToToast("load mission failed:" + error.getDescription());
        }

    }

    private float getSpeed() {
        float speed = 6f;
        if (!executeModelList.isEmpty()) {
            for (int i = 0; i < executeModelList.size(); i++) {
                BaseModel model = executeModelList.get(i);
                switch (model.getModelType()) {
                    case Slope:

                        break;
                    case Flatland:
                        if (((FlatlandModel) model).getSpeed() > speed)
                            speed = ((FlatlandModel) model).getSpeed();
                        break;
                    case MultiPoints:
                        if (((MultiPointsModel) model).getSpeed() > speed)
                            speed = ((MultiPointsModel) model).getSpeed();
                        break;
                }
            }
        }
        return speed;
    }

    private float getAltitude() {
        float altitude = 15f;
        if (!executeModelList.isEmpty()) {
            for (int i = 0; i < executeModelList.size(); i++) {
                BaseModel model = executeModelList.get(i);
                switch (model.getModelType()) {
                    case Slope:

                        break;
                    case Flatland:
                        if (((FlatlandModel) model).getSafeAltitude() > altitude)
                            altitude = ((FlatlandModel) model).getSpeed();
                        break;
                    case MultiPoints:
                        if (((MultiPointsModel) model).getSafeAltitude() > altitude)
                            altitude = ((MultiPointsModel) model).getSpeed();
                        break;
                }
            }
        }
        return altitude;
    }
    /**
     * uploadMission
     * 航点任务执行流程第二步
     * 上传任务到飞机
     */
    private void uploadMission() {
        setResultToToast("on upload");
        if (getWaypointMissionOperator().getCurrentState() == WaypointMissionState.READY_TO_UPLOAD) {
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
        } else {
            setResultToToast("can not upload");
        }
    }

    /**
     * 任务开始执行
     */
    private void startMission() {
        setResultToToast("on start");
        record = new FlightRecord();
        record.setMission(patrolMission);
        if (getWaypointMissionOperator().getCurrentState() == WaypointMissionState.READY_TO_EXECUTE) {
            getWaypointMissionOperator().startMission(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error3) {
                    if (error3 == null) {
                        record.setStartTime(new Date());
                    }
                    setResultToToast("Mission Start:" + (error3 == null ? "Successfully" : error3.getDescription()));
                }
            });
        } else {
            setResultToToast("mission state:" + getWaypointMissionOperator().getCurrentState().getName());
            getWaypointMissionOperator().retryUploadMission(null);
        }
    }

    /**
     * 任务终止
     * mf,call wrong function,dashazi
     */
    private void stopMission() {
        setResultToToast("on stop");

        getWaypointMissionOperator().stopMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error3) {
                setResultToToast("Mission Stop: " + (error3 == null ? "Successfully" : error3.getDescription()));
            }
        });
    }

    /**
     * 任务暂停
     */
    private void pauseMission() {
        setResultToToast("on pause");

        getWaypointMissionOperator().pauseMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error3) {
                setResultToToast("Mission Pause: " + (error3 == null ? "Successfully" : error3.getDescription()));
            }
        });
    }

    /**
     * 坐标转换，高德->GPS
     */
    private Waypoint waypointConvert(Waypoint w) {
        LatLng l = GoogleMapHelper.AmapConvertToWGS84(
                new LatLng(w.coordinate.getLatitude(), w.coordinate.getLongitude()));
        Waypoint waypoint = new Waypoint(l.latitude, l.longitude, w.altitude);
        for (int i = 0; i < w.waypointActions.size(); i++) {
            waypoint.addAction(w.waypointActions.get(i));
        }
        return waypoint;
    }

    /**
     * 工具类函数
     * 完成动作的字符串与相关类的对应关系
     *
     * @param s
     * @return
     */
    private WaypointMissionFinishedAction getFinishedAction(String s) {
        if (s.equals(WaypointMissionFinishedAction.AUTO_LAND.toString())) {
            return WaypointMissionFinishedAction.AUTO_LAND;
        } else if (s.equals(WaypointMissionFinishedAction.GO_FIRST_WAYPOINT.toString())) {
            return WaypointMissionFinishedAction.GO_FIRST_WAYPOINT;
        } else if (s.equals(WaypointMissionFinishedAction.GO_HOME.toString())) {
            return WaypointMissionFinishedAction.GO_HOME;
        } else {
            return WaypointMissionFinishedAction.NO_ACTION;
        }
    }

    /**
     * 工具类函数
     * 航向的字符串与相关类的对应关系
     *
     * @param s
     * @return
     */
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

    private void setResultToToast(final String msg) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }


    private class GridviewSeleteAdapter extends BaseAdapter {
        private LayoutInflater inflater;
        List<BaseModel> oldModelList;
        private boolean[] selected;

        GridviewSeleteAdapter(Context context, List<BaseModel> list) {
            inflater = LayoutInflater.from(context);
            this.oldModelList = list;
            selected = new boolean[list.size()];
        }

        public List<BaseModel> getExecuteList() {
            List<BaseModel> list = new ArrayList<>();
            for (int i = 0; i < oldModelList.size(); i++) {
                if (selected[i]) {
                    list.add(oldModelList.get(i));
                }
            }
            return list;
        }

        @Override
        public int getCount() {
            return oldModelList.size();
        }

        @Override
        public Object getItem(int position) {
            return oldModelList.get(position);
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
                convertView = inflater.inflate(R.layout.dialog_execute_select_gridview_item, null);
                holder.checkBox = convertView.findViewById(R.id.checkbox_select_model);
                holder.name = convertView.findViewById(R.id.textview_name_select_model);
                holder.type = convertView.findViewById(R.id.textview_type_select_model);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.name.setText(oldModelList.get(position).getMissionName());
            holder.type.setText(oldModelList.get(position).getModelType().toString());
            holder.checkBox.setId(position);
            holder.checkBox.setChecked(true);
            selected[position] = true;
            holder.checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    selected[position] = !selected[position];
                }
            });
            return convertView;
        }

        class ViewHolder {
            TextView name, type;
            CheckBox checkBox;
        }

    }

}
