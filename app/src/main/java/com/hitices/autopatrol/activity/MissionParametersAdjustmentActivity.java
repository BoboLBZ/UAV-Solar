package com.hitices.autopatrol.activity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
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
import com.amap.api.maps2d.model.PolylineOptions;
import com.hitices.autopatrol.AutoPatrolApplication;
import com.hitices.autopatrol.R;
import com.hitices.autopatrol.entity.dataSupport.PatrolMission;
import com.hitices.autopatrol.entity.missions.BaseModel;
import com.hitices.autopatrol.entity.missions.FlatlandModel;
import com.hitices.autopatrol.entity.missions.MultiPointsModel;
import com.hitices.autopatrol.entity.missions.SlopeModel;
import com.hitices.autopatrol.helper.GoogleMapHelper;
import com.hitices.autopatrol.helper.MissionHelper;
import com.hitices.autopatrol.helper.ToastHelper;

import org.litepal.crud.DataSupport;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.gimbal.GimbalState;
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

public class MissionParametersAdjustmentActivity extends Activity implements View.OnClickListener {
    private static final String TAG = MissionParametersAdjustmentActivity.class.getName();
    List<Waypoint> waypoints;
    private WaypointMission.Builder builder;
    private BaseModel adjustModel;
    private List<BaseModel> modelList;
    private FlightController mFlightController;
    private WaypointMissionOperator operatorInstance;
    //mission about
    private PatrolMission patrolMission;
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
                    Log.d("AmapError", "humanLocationMarker Error, ErrCode:"
                            + amapLocation.getErrorCode() + ", errInfo:"
                            + amapLocation.getErrorInfo());
                }
            }
        }
    };
    //ui
    private MapView mapView;
    private ImageButton upload, start, pause;
    private Button btnSaveLocation, btnSaveAltitude, btnSaveHeading, btnSaveCameraAngle, btnSave;
    private int index = -1;
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
            if (error == null) {
                setResultToToast("可以开始调整");
                index = index + 1;
            }
        }
    };
    private float cameraAngle = 0;
    private float currentAltitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mission_parameters_adjustment);
        initUI();
        initMapView(savedInstanceState);
        addListener();
        initFlightController();
        initContent();
        showSelectDialog();
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
        upload = findViewById(R.id.adjustment_uploadMission);
        pause = findViewById(R.id.adjustment_stopMission);
//        exit = findViewById(R.id.adjustment_exitMission);
        start = findViewById(R.id.adjustment_startMission);

        btnSaveLocation = findViewById(R.id.adjustment_location);
        btnSaveAltitude = findViewById(R.id.adjustment_altitude);
        btnSaveHeading = findViewById(R.id.adjustment_heading);
        btnSaveCameraAngle = findViewById(R.id.adjustment_cameraAngle);
        btnSave = findViewById(R.id.adjustment_saveMission);

        upload.setOnClickListener(this);
        start.setOnClickListener(this);
//        exit.setOnClickListener(this);
        pause.setOnClickListener(this);
        btnSaveCameraAngle.setOnClickListener(this);
        btnSaveHeading.setOnClickListener(this);
        btnSaveAltitude.setOnClickListener(this);
        btnSaveLocation.setOnClickListener(this);
        btnSave.setOnClickListener(this);
    }

    private void initMapView(Bundle savedInstanceState) {
        mapView = findViewById(R.id.adjustment_mapview);
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
                        public void onUpdate(final FlightControllerState
                                                     djiFlightControllerCurrentState) {
                            //update drone location
                            currentAltitude = djiFlightControllerCurrentState.getAircraftLocation().getAltitude();
                            droneLocation = new LatLng(djiFlightControllerCurrentState.getAircraftLocation().getLatitude(),
                                    djiFlightControllerCurrentState.getAircraftLocation().getLongitude());
                            //gps 坐标转换成 高德坐标系
                            updateDroneLocation(GoogleMapHelper.WGS84ConvertToAmap(droneLocation));
                        }
                    });
//            ToastHelper.getInstance().showShortToast("in flight control");
        }
        if (AutoPatrolApplication.getGimbalInstance() != null) {
            AutoPatrolApplication.getGimbalInstance().setStateCallback(new GimbalState.Callback() {
                @Override
                public void onUpdate(GimbalState gimbalState) {
                    cameraAngle = -gimbalState.getAttitudeInDegrees().getPitch();
                }
            });
        }

    }
    private void missionProcessingWithMark(){
        generateExecutableWaypoints();
        markWaypoints(waypoints);
        List<Waypoint> points = new ArrayList<>();
        points.add(waypoints.get(index+1));
        loadMission(WaypointListConvert(points), getAltitude(waypoints));
    }
    private void missionProcessing() {
        generateExecutableWaypoints();
        List<Waypoint> points = new ArrayList<>();
        points.add(waypoints.get(index+1));
        loadMission(WaypointListConvert(points), getAltitude(waypoints));
    }

    /**
     * loadMission
     * 航点任务执行流程第一步
     */
    private void loadMission(@NonNull List<Waypoint> list, @NonNull float altitude) {
//        ToastHelper.getInstance().showShortToast("on load");
        List<Waypoint> points=new ArrayList<>();
        if (droneLocation != null) {
            points.add(new Waypoint(droneLocation.latitude, droneLocation.longitude, altitude));
        }
        points.addAll(list);
//        setResultToToast("points num:"+String.valueOf(points.size()));
        builder = new WaypointMission.Builder();
        builder.waypointList(points);
        builder.waypointCount(points.size());

        float speed = getSpeed(adjustModel);
        builder.autoFlightSpeed(speed);
        builder.maxFlightSpeed(speed);

        builder.finishedAction(WaypointMissionFinishedAction.NO_ACTION);
        builder.headingMode(WaypointMissionHeadingMode.USING_WAYPOINT_HEADING);
        builder.flightPathMode(WaypointMissionFlightPathMode.NORMAL);

        DJIError error = getWaypointMissionOperator().loadMission(builder.build());
        if (error == null) {
            ToastHelper.getInstance().showShortToast("生成任务成功");
        } else {
            ToastHelper.getInstance().showShortToast("l生成任务失败:" + error.getDescription());
        }

    }

    /**
     * 根据传入的任务信息读取子任务列表
     */
    private void initContent() {
        Intent intent = getIntent();
        String path = intent.getStringExtra("PATH");
        int id = intent.getIntExtra("ID", -1);
        patrolMission = DataSupport.find(PatrolMission.class, id);
        if (patrolMission != null) {
            Log.d("debug", path);
            MissionHelper helper = new MissionHelper(path, patrolMission);
            modelList = helper.getModelList();
        }
    }

    /**
     * uploadMission
     * 航点任务执行流程第二步
     * 上传任务到飞机
     */
    private void uploadMission() {
//        ToastHelper.getInstance().showShortToast("on upload");
        if (getWaypointMissionOperator().getCurrentState() == WaypointMissionState.READY_TO_UPLOAD) {
            getWaypointMissionOperator().uploadMission(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error2) {
                    if (error2 == null) {
                        setResultToToast("任务上传成功!");
                    } else {
                        setResultToToast("任务上传失败: " + error2.getDescription() + " 重传中...");
                        getWaypointMissionOperator().retryUploadMission(null);
                    }
                }
            });
        } else {
            setResultToToast("任务已上传或未制订任务，请重试");
        }
    }

    /**
     * 任务开始执行
     */
    private void startMission() {
//        ToastHelper.getInstance().showShortToast("on start");
        if (getWaypointMissionOperator().getCurrentState() == WaypointMissionState.READY_TO_EXECUTE) {
            getWaypointMissionOperator().startMission(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error3) {
                    setResultToToast("任务开始:" + (error3 == null ? "成功" : error3.getDescription()));
                }
            });
        } else {
            setResultToToast("请稍后重试:" + getWaypointMissionOperator().getCurrentState().getName() + "");
            getWaypointMissionOperator().retryUploadMission(null);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.adjustment_uploadMission:
                uploadMission();
                break;
            case R.id.adjustment_startMission:
                startMission();
                break;
            case R.id.adjustment_stopMission:
                missionProcessing();
                break;
            case R.id.adjustment_location:
                saveLocation();
                break;
            case R.id.adjustment_cameraAngle:
                saveCameraAngle();
                break;
            case R.id.adjustment_heading:
                saveHeading();
                break;
            case R.id.adjustment_altitude:
                saveAltitude();
                break;
            case R.id.adjustment_saveMission:
                saveMission();
                break;
        }
    }

    private void saveLocation() {
        boolean flag = false;
        switch (adjustModel.getModelType()) {
            case Flatland:
                flag = ((FlatlandModel) adjustModel).adjustVertes(index, GoogleMapHelper.WGS84ConvertToAmap(droneLocation));
                break;
            case MultiPoints:
                flag = ((MultiPointsModel) adjustModel).updateWaypointLocation(index, GoogleMapHelper.WGS84ConvertToAmap(droneLocation));
                break;
            case Slope:
                flag = ((SlopeModel) adjustModel).updateDroneLocation(index, GoogleMapHelper.WGS84ConvertToAmap(droneLocation));
                break;
        }
        if (flag) {
            markHomePoint(GoogleMapHelper.WGS84ConvertToAmap(droneLocation));
            ToastHelper.getInstance().showShortToast("成功记录位置");
        } else {
            ToastHelper.getInstance().showShortToast("调整位置失败");
        }
    }

    private void saveAltitude() {
        boolean result = false;
        switch (adjustModel.getModelType()) {
            case Flatland:
                float oldAltitude = ((FlatlandModel) adjustModel).getAltitude();
                result = ((FlatlandModel) adjustModel).setDistanceToPanel(currentAltitude - oldAltitude);
                break;
            case MultiPoints:
                result = ((MultiPointsModel) adjustModel).updateWaypointAltitude(index, currentAltitude);
                break;
            case Slope:
                result = ((SlopeModel) adjustModel).updateAltitude(index, currentAltitude);
                break;
        }
        if (result) {
            ToastHelper.getInstance().showShortToast("成功记录高度");
        } else {
            ToastHelper.getInstance().showShortToast("记录高度失败");
        }
    }

    private void saveHeading() {
        float heading = mFlightController.getCompass().getHeading();
        adjustModel.setHeadingAngle((int) heading);
        ToastHelper.getInstance().showShortToast("成功记录拍照朝向:" + String.valueOf(heading));

    }

    private void saveCameraAngle() {
        adjustModel.setCameraAngle((int) cameraAngle);
//        Attitude attitude=mFlightController.getState().getAttitude();
//        double pitch=-attitude.pitch;
//        adjustModel.setCameraAngle((int)pitch);
        ToastHelper.getInstance().showShortToast("成功记录相机俯角:" + String.valueOf((int) cameraAngle));
    }

    private void saveMission() {
        //update database
        int id = patrolMission.getId();
        Date date = new Date();
        PatrolMission temp = DataSupport.find(PatrolMission.class, id);
        if (temp != null) {
            temp.setLastModifiedTime(date);
            if (modelList != null) {
                temp.setChildNums(modelList.size());
            }
            temp.save();
        } else {
            patrolMission.setLastModifiedTime(date);
            if (modelList != null) {
                patrolMission.setChildNums(modelList.size());
            }
            patrolMission.save();
        }
        //update file
        if (!MissionHelper.saveMissionToFile(patrolMission, modelList)) {
            setResultToToast("保存任务失败");
        } else {
//            saveFlag = true;
            setResultToToast("保存任务成功");
        }
    }

    /**
     * 任务暂停
     */
    private void pauseMission() {
//        ToastHelper.getInstance().showShortToast("on pause");

        getWaypointMissionOperator().pauseMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error3) {
                setResultToToast("任务暂停成功: " + (error3 == null ? "成功" : error3.getDescription()));
            }
        });
    }

    private void continueMission() {
        if (getWaypointMissionOperator().getCurrentState() == WaypointMissionState.EXECUTION_PAUSED) {
            getWaypointMissionOperator().resumeMission(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error3) {
                    setResultToToast("任务继续执行: " + (error3 == null ? "成功" : error3.getDescription()));
                }
            });
        }

    }

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
        ToastHelper.getInstance().showShortToast("任务点个数:" + String.valueOf(waypoints.size()));
    }

    private void markHomePoint(LatLng latLng) {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(),
                R.drawable.ic_home_point_marker)));
        aMap.addMarker(markerOptions);
        // cameraUpdate(ll);
    }

    private float getSpeed(BaseModel model) {
        float speed = 6f;
        switch (model.getModelType()) {
            case Slope:
                speed = ((SlopeModel) model).getSpeed();
                break;
            case MultiPoints:
                speed = ((MultiPointsModel) model).getSpeed();
                break;
            case Flatland:
                speed = ((FlatlandModel) model).getSpeed();
                break;
        }
        Log.d(TAG, "speed is " + String.valueOf(speed));
        ToastHelper.getInstance().showShortToast("飞行速度:" + String.valueOf(speed));
        return speed;
    }

    private float getAltitude(List<Waypoint> points) {
        //选择所有任务安全高度之最作为返航高度
        float altitude = 12f;
        for (int i = 0; i < points.size(); i++) {
            if (points.get(i).altitude > altitude) {
                altitude = points.get(i).altitude;
            }
        }
        Log.d(TAG, "safe altitude is " + String.valueOf(altitude));
        ToastHelper.getInstance().showShortToast("安全飞行高度:" + String.valueOf(altitude));
        return altitude;
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

    private List<Waypoint> WaypointListConvert(List<Waypoint> list) {
        List<Waypoint> temp = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            temp.add(waypointConvert(list.get(i)));
        }
        return temp;
    }

    private List<Waypoint> generateExecutableWaypoints() {
        waypoints = new ArrayList<>();
        if (adjustModel != null) {
            switch (adjustModel.getModelType()) {
                case Slope:
                    waypoints = ((SlopeModel) adjustModel).getAdjustPoints();
                    break;
                case Flatland:
                    waypoints = ((FlatlandModel) adjustModel).getAdjustPoints();
                    break;
                case MultiPoints:
                    waypoints = ((MultiPointsModel) adjustModel).getWaypointList();
                    break;
            }
        }
        return waypoints;
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
            ToastHelper.getInstance().showShortToast("waypointMissionOperator is null");
        }
        return operatorInstance;
    }

    /**
     * Add Listener for WaypointMissionOperator
     */
    private void addListener() {
        if (getWaypointMissionOperator() != null) {
            getWaypointMissionOperator().addListener(eventNotificationListener);
//            ToastHelper.getInstance().showShortToast("add listener");
        }
    }

    /**
     * remove Listener for WaypointMissionOperator
     */
    private void removeListener() {

        if (getWaypointMissionOperator() != null) {
            getWaypointMissionOperator().removeListener(eventNotificationListener);
//            ToastHelper.getInstance().showShortToast("remove listener");
        }
    }

    private void showSelectDialog() {
        List<String> name = new ArrayList<>();
        for (int i = 0; i < modelList.size(); i++) {
            name.add(modelList.get(i).getMissionName());
        }
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("选择需要调整参数的子任务")
                .setItems(name.toArray(new String[0]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        adjustModel = modelList.get(which);
                        Log.d(TAG, "onClick: " + String.valueOf(which));
                        missionProcessingWithMark();
                    }
                })
                .setPositiveButton("确认", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .create();
        dialog.show();
    }

    private void setResultToToast(final String msg) {
        this.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
