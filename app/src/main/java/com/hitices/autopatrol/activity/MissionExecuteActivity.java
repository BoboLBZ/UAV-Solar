package com.hitices.autopatrol.activity;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
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
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.AMapUtils;
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
import com.hitices.autopatrol.helper.MissionConstraintHelper;
import com.hitices.autopatrol.helper.MissionHelper;
import com.hitices.autopatrol.helper.ToastHelper;

import org.litepal.crud.DataSupport;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import dji.common.battery.BatteryState;
import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.mission.waypoint.WaypointMissionState;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.model.LocationCoordinate2D;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.battery.Battery;
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
    private static final String TAG = MissionExecuteActivity.class.getName();
    public WaypointMission.Builder builder;
    private List<BaseModel> modelList;
    private List<BaseModel> executeModelList;
    private FlightController mFlightController;
    private FlightRecord record;
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
    private ImageButton upload, start, exit, pause;
    private List<Waypoint> unfinishedPoints = new ArrayList<>();
    private int index = -1;
    private LatLng homePointGPS;
    //use to save the status of mission(mission stop)
    private List<Marker> markers = new ArrayList<>();
    //    private int savedHeadingDegree;
    //safe
    private boolean returnHomeSettingFlag = true;
    private int savedCameraPitch;
    private int homeThreshold;
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
            setResultToToast("任务终止: " + (error == null ? "成功!" : error.getDescription()));
            if (error == null) {
                record.setEndTime(new Date());
                record.setHasVisible(true); //设置照片为可见光图像
                record.save();
                // setResultToToast("");
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
        upload = findViewById(R.id.execute_uploadMission);
        pause = findViewById(R.id.execute_stopMission);
        exit = findViewById(R.id.execute_exitMission);
        start = findViewById(R.id.execute_startMission);
        upload.setOnClickListener(this);
        start.setOnClickListener(this);
        exit.setOnClickListener(this);
        pause.setOnClickListener(this);
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
                        public void onUpdate(final FlightControllerState
                                                     djiFlightControllerCurrentState) {
                            //update drone location
                            droneLocation = new LatLng(djiFlightControllerCurrentState.getAircraftLocation().getLatitude(),
                                    djiFlightControllerCurrentState.getAircraftLocation().getLongitude());
                            //gps 坐标转换成 高德坐标系
                            updateDroneLocation(GoogleMapHelper.WGS84ConvertToAmap(droneLocation));
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (djiFlightControllerCurrentState.isFlying() &&
                                            Math.abs(djiFlightControllerCurrentState.getVelocityX()) < 0.001 &&
                                            Math.abs(djiFlightControllerCurrentState.getVelocityY()) < 0.001 &&
                                            Math.abs(djiFlightControllerCurrentState.getVelocityZ()) < 0.001) {
                                        processOfMission(GoogleMapHelper.WGS84ConvertToAmap(droneLocation));
                                    }
                                }
                            });

                        }
                    });
            // ToastHelper.getInstance().showShortToast("in flight control");
        }

    }

    /**
     * 确认任务后的操作流程
     * 生成可执行航点
     * 地图标记
     * 安全措施设置
     * load mission，生成可执行的waypoint mission
     */
    private void missionProcessing() {
        List<Waypoint> waypoints = generateExecutableWaypoints(executeModelList);
        markWaypoints(waypoints);
        markRangeOfExecuteModel();
        loadMission(WaypointListConvert(waypoints), getAltitude());
        simpleSafetyMeasures(mFlightController);
    }

    private void simpleSafetyMeasures(FlightController controller) {
        if (!setSmartReturnHome(controller)) {
            ToastHelper.getInstance().showLongToast("设置智能返航失败，请注意飞行安全");
        } else {
            ToastHelper.getInstance().showLongToast("成功设置智能返航");

        }
    }

    private boolean continueProcess() {
        //检查无人机连接状态
        if (!checkStatusOfDrone()) {
            ToastHelper.getInstance().showLongToast("无人机未连接");
            return false;
        }
        //不确定是否需要重新初始化
        //initFlightController();
        //剩余任务点重新制订飞行任务
        if (missionReload()) {
            ToastHelper.getInstance().showLongToast("重新制订任务成功，请选择上传任务");
            return true;
        } else {
            ToastHelper.getInstance().showLongToast("重新制订任务失败");
        }
        refreshMapview();
        return false;
    }

    private boolean checkStatusOfDrone() {
        BaseProduct mProduct = AutoPatrolApplication.getProductInstance();
        if (null != mProduct && mProduct.isConnected()) {
            if (null != mProduct.getModel() && null != mProduct.getCamera()) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean missionReload() {
        if (unfinishedPoints.isEmpty()) {
            ToastHelper.getInstance().showShortToast("任务已经完成，无需继续执行");
            return false;
        }
        if (builder != null) {
            List<Waypoint> list = WaypointListConvert(unfinishedPoints);
            float alt = unfinishedPoints.get(0).altitude;
            for (int i = 1; i < unfinishedPoints.size(); i++) {
                float temp = unfinishedPoints.get(i).altitude;
                if (temp > alt) {
                    alt = temp;
                }
            }
            //设置自动降落点
            if (homePointGPS != null) {
                list.add(new Waypoint(homePointGPS.latitude, homePointGPS.longitude, alt));
            } else {
                if (humanLocation != null) {
                    list.add(waypointConvert(new Waypoint(humanLocation.latitude, humanLocation.longitude, alt)));
                    markHomePoint(humanLocation);
                }
            }
            //增加航点调整飞机姿态，坐标为无人机当前位置,高度为安全位置
//            if(savedHeadingDegree && savedCameraPitch )
            if (droneLocation != null) {
                Waypoint w = new Waypoint(droneLocation.latitude, droneLocation.longitude, alt);
//                WaypointAction actionRotate = new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, savedHeadingDegree);
                WaypointAction actionPitch = new WaypointAction(WaypointActionType.GIMBAL_PITCH, -savedCameraPitch);
//                w.addAction(actionRotate);
                w.addAction(actionPitch);
                list.add(0, w);
            }
            builder.waypointList(list);
            builder.waypointCount(list.size());
            Log.d(TAG, "num of unfinished points:" + String.valueOf(unfinishedPoints.size()));
            DJIError error = getWaypointMissionOperator().loadMission(builder.build());
            if (error == null) {
                ToastHelper.getInstance().showShortToast("任务继续:制订任务成功");
            } else {
                ToastHelper.getInstance().showShortToast("任务继续:制订任务失败:" + error.getDescription());
                return false;
            }
            //check smart return home
            mFlightController.getSmartReturnToHomeEnabled(new CommonCallbacks.CompletionCallbackWith<Boolean>() {
                @Override
                public void onSuccess(Boolean aBoolean) {
                    setResultToToast("成功设置智能返航");
                }

                @Override
                public void onFailure(DJIError djiError) {
                    setResultToToast("设置智能返航失败，注意飞行安全");
                }
            });
            return true;
        } else {
            return false;
        }
    }

    private void refreshMapview() {

    }

    /**
     * 设置智能返航，返航条件：信号丢失，电量低于设定的阈值
     *
     * @param controller
     * @return
     */
    private boolean setSmartReturnHome(@NonNull FlightController controller) {
        //set home location (use drone take off location)
        LocationCoordinate2D coordinate2D = new LocationCoordinate2D(homePointGPS.latitude, homePointGPS.longitude);
        controller.setHomeLocation(coordinate2D, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError != null) {
                    Log.d(TAG, "setSmartReturnHome onResult: " + djiError.getDescription());
                    returnHomeSettingFlag = false;
                }
            }
        });
        //set return home altitude(Range 20-500)use max safe altitude,
        int alt = MissionConstraintHelper.getDefaultReturnHomeAltitude();
        float myAlt = getAltitude();
        if (myAlt > 20 && myAlt < 500) {
            alt = (int) myAlt;
        }
        controller.setGoHomeHeightInMeters(alt, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError != null) {
                    Log.d(TAG, "setSmartReturnHome onResult: " + djiError.getDescription());
                    returnHomeSettingFlag = false;
                }
            }
        });
        //enable smart return home
        controller.setSmartReturnToHomeEnabled(true, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError != null) {
                    Log.d(TAG, "setSmartReturnHome onResult: " + djiError.getDescription());
                    returnHomeSettingFlag = false;
                }
            }
        });
        //set
        //这个时候遥控器会有警报，在设置smart RTH的情况下不一定会返航
        controller.setLowBatteryWarningThreshold(30, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError != null) {
                    returnHomeSettingFlag = false;
                }
            }
        });
        controller.setSeriousLowBatteryWarningThreshold(homeThreshold, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError != null) {
                    returnHomeSettingFlag = false;
                }
            }
        });
        //在低电量返航
        returnHomeViaBattery(homeThreshold);
        return returnHomeSettingFlag;
    }

    private void returnHomeViaBattery(@NonNull final int percent) {
        //不支持
        final Battery battery = AutoPatrolApplication.getBattery();
        if (battery != null) {
            battery.setStateCallback(new BatteryState.Callback() {
                boolean flag = true;

                @Override
                public void onUpdate(BatteryState batteryState) {
                    if (flag) {
//                        Log.d(TAG, "battery: " + String.valueOf(batteryState.getChargeRemainingInPercent()));
                        if (batteryState.getChargeRemainingInPercent() <= percent) {
                            flag = false;
                            startReturnHome();
                        }
                    }
                }
            });
        } else {
            setResultToToast("连接电池失败");
        }
    }

    private void startReturnHome() {
        mFlightController.startGoHome(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                setResultToToast("开始返航");
            }
        });
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
            Log.d("debug", path);
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
                changeStatusOfPause();
                break;
            case R.id.execute_exitMission:
                changeStatusOfStop();
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
            markers.add(aMap.addMarker(markerOptions));
            cameraUpdate(ll);
        }
        //lines.add(droneLocation);
        aMap.addPolyline(new PolylineOptions().addAll(lines).color(Color.argb(125, 1, 1, 1)));
        ToastHelper.getInstance().showShortToast("任务点数量:" + String.valueOf(waypoints.size()));
    }

    private void processOfMission(LatLng latLng) {
        if (!unfinishedPoints.isEmpty()) {
            Waypoint waypoint = unfinishedPoints.get(0);
            LatLng temp = new LatLng(waypoint.coordinate.getLatitude(), waypoint.coordinate.getLongitude());
            //判断是否到达下一个点
            float dis = AMapUtils.calculateLineDistance(temp, latLng);
            Log.d(TAG, "processOfMission: " + String.valueOf(dis));
            if (dis < 3) {
                index = index + 1;
                changeWaypointsMarker(index);
                unfinishedPoints.remove(0);
            }
        }

    }

    private void changeWaypointsMarker(int index) {
        if (index >= 0 && index < markers.size()) {
            Marker marker = markers.get(index);
            Log.d(TAG, "index is " + String.valueOf(index));
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(marker.getPosition());
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET));
            marker.remove();
            markers.set(index, aMap.addMarker(markerOptions));
            //Log.d(TAG, "changeWaypointsMarker: num of markers:"+String.valueOf(aMap.getMapScreenMarkers().size()));
        }
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

    private void markHomePoint(LatLng latLng) {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(),
                R.drawable.ic_home_point_marker)));
        aMap.addMarker(markerOptions);
        // cameraUpdate(ll);
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
                    showSlopeView((SlopeModel) executeModelList.get(i));
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
        PolylineOptions options = new PolylineOptions().addAll(model.getVertexs());
        if (model.getVertexs().size() > 0)
            options.add(model.getVertexs().get(0));
        aMap.addPolyline(options);
        aMap.addPolygon(new PolygonOptions().addAll(model.getVertexs())
                .fillColor(getResources().getColor(R.color.fillColorSlope)));
        if (model.getVertexs().size() > 0) {
            cameraUpdate(model.getVertexs().get(0));
        } else {
            cameraUpdate(humanLocation);
        }
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
            ToastHelper.getInstance().showShortToast("系统错误，请重试");
        }
        return operatorInstance;
    }

    /**
     * Add Listener for WaypointMissionOperator
     */
    private void addListener() {
        if (getWaypointMissionOperator() != null) {
            getWaypointMissionOperator().addListener(eventNotificationListener);
            // ToastHelper.getInstance().showShortToast("add listener");
        }
    }

    /**
     * remove Listener for WaypointMissionOperator
     */
    private void removeListener() {

        if (getWaypointMissionOperator() != null) {
            getWaypointMissionOperator().removeListener(eventNotificationListener);
            // ToastHelper.getInstance().showShortToast("remove listener");
        }
    }

    private void showchildSeleteDialog() {
        LinearLayout view = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_execute_select_model, null);
        GridView gridView = view.findViewById(R.id.gridview_execute_select_child);
        final SeekBar sb_battery = view.findViewById(R.id.execute_battery);
        final TextView battery_text = view.findViewById(R.id.execute_battery_text);
        //init gridview GridviewSeleteAdapter
        final GridviewSeleteAdapter adapter = new GridviewSeleteAdapter(this, modelList);
        Log.d("debug", String.valueOf(modelList.size()));
        gridView.setAdapter(adapter);

        sb_battery.setMax(90);
        homeThreshold = MissionConstraintHelper.getReturnHomeThreshold();
        sb_battery.setProgress(homeThreshold);
        battery_text.setText(String.valueOf(homeThreshold) + "%");

        sb_battery.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                battery_text.setText(String.valueOf(i) + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        new AlertDialog.Builder(this)
                .setTitle("")
                .setView(view)
                .setPositiveButton("确认", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        executeModelList = adapter.getExecuteList();
                        Log.d(TAG, "onClick:exec num:" + String.valueOf(executeModelList.size()));
                        int process = sb_battery.getProgress();
                        if (process > 10 && process < 90) {
                            homeThreshold = process;
                        }
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
                formerPoint = GoogleMapHelper.WGS84ConvertToAmap(droneLocation);
            }
            for (int i = 0; i < executeModelList.size(); i++) {
                BaseModel model = executeModelList.get(i);
                model.generateExecutablePoints(formerPoint);
                waypoints.addAll(model.getExecutePoints());
                formerPoint = model.getEndPoint();
            }
        }
        unfinishedPoints.addAll(waypoints);
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
     * loadMission
     * 航点任务执行流程第一步
     */
    private void loadMission(@NonNull List<Waypoint> list, @NonNull float altitude) {
        // ToastHelper.getInstance().showShortToast("on load");
        //以无人机起飞位置作为返航点
        if (droneLocation != null) {
            list.add(new Waypoint(droneLocation.latitude, droneLocation.longitude, altitude));
            homePointGPS = new LatLng(droneLocation.latitude, droneLocation.longitude);
            markHomePoint(GoogleMapHelper.WGS84ConvertToAmap(droneLocation));
        }
        //以当前人的位置作返航点
        else {
            if (humanLocation != null) {
                list.add(waypointConvert(new Waypoint(humanLocation.latitude, humanLocation.longitude, altitude)));
                homePointGPS = GoogleMapHelper.AmapConvertToWGS84(humanLocation);

            }
        }
        builder = new WaypointMission.Builder();

        builder.waypointList(list);
        builder.waypointCount(list.size());

        float speed = getSpeed();
        builder.autoFlightSpeed(speed);
        builder.maxFlightSpeed(speed);

        builder.finishedAction(WaypointMissionFinishedAction.AUTO_LAND);
        builder.headingMode(WaypointMissionHeadingMode.USING_WAYPOINT_HEADING);
        builder.flightPathMode(WaypointMissionFlightPathMode.NORMAL);

        DJIError error = getWaypointMissionOperator().loadMission(builder.build());
        if (error == null) {
            ToastHelper.getInstance().showShortToast("制订任务成功");
        } else {
            ToastHelper.getInstance().showShortToast("制订任务失败:" + error.getDescription());
        }

    }

    private float getSpeed() {
        float speed = 3f;
        if (!executeModelList.isEmpty()) {
            for (int i = 0; i < executeModelList.size(); i++) {
                BaseModel model = executeModelList.get(i);
                switch (model.getModelType()) {
                    case Slope:
                        if (((SlopeModel) model).getSpeed() > speed)
                            speed = ((SlopeModel) model).getSpeed();
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
        Log.d(TAG, "speed is " + String.valueOf(speed));
        ToastHelper.getInstance().showShortToast("飞行速度:" + String.valueOf(speed));
        return speed;
    }

    private float getAltitude() {
        //选择所有任务安全高度之最作为返航高度
        float altitude = 12f;
        if (!executeModelList.isEmpty()) {
            for (int i = 0; i < executeModelList.size(); i++) {
                BaseModel model = executeModelList.get(i);
                switch (model.getModelType()) {
                    case Slope:
                        if (((SlopeModel) model).getSafeAltitude() > altitude) {
                            altitude = ((SlopeModel) model).getSafeAltitude();
                        }
                        break;
                    case Flatland:
                        if (((FlatlandModel) model).getSafeAltitude() > altitude) {
                            altitude = ((FlatlandModel) model).getSafeAltitude();
                        }
                        break;
                    case MultiPoints:
                        if (((MultiPointsModel) model).getSafeAltitude() > altitude) {
                            altitude = ((MultiPointsModel) model).getSafeAltitude();
                        }
                        break;
                }
            }
        }
        Log.d(TAG, "safe altitude is " + String.valueOf(altitude));
        ToastHelper.getInstance().showShortToast("安全飞行高度:" + String.valueOf(altitude));
        return altitude;
    }

    private void startAuto() {
        uploadMission();
        while (getWaypointMissionOperator().getCurrentState() == WaypointMissionState.UPLOADING) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {

                }
            }, 1000);
        }
        startMission();
    }

    /**
     * uploadMission
     * 航点任务执行流程第二步
     * 上传任务到飞机
     */
    private void uploadMission() {
        // ToastHelper.getInstance().showShortToast("on upload");
        if (getWaypointMissionOperator().getCurrentState() == WaypointMissionState.READY_TO_UPLOAD) {
            getWaypointMissionOperator().uploadMission(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error2) {
                    if (error2 == null) {
                        setResultToToast("任务上传成功");
                    } else {
                        setResultToToast("任务上传失败: " + error2.getDescription() + " 重试中...");
                        getWaypointMissionOperator().retryUploadMission(null);
                    }
                }
            });
        } else {
            setResultToToast("无法上传或任务已经上传");
        }
    }

    /**
     * 任务开始执行
     */
    private void startMission() {
        // ToastHelper.getInstance().showShortToast("on start");
        record = new FlightRecord();
        record.setMissionName(patrolMission.getName());
//        record.setPowerStationName(patrolMission.getPowerStation().getName());
//        record.setPowerStationThumb(patrolMission.getPowerStation().getThumbUrl());
        if (getWaypointMissionOperator().getCurrentState() == WaypointMissionState.READY_TO_EXECUTE) {
            getWaypointMissionOperator().startMission(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error3) {
                    if (error3 == null) {
                        record.setStartTime(new Date());
                    }
                    setResultToToast("任务开始执行:" + (error3 == null ? "成功" : error3.getDescription()));
                }
            });
        } else {
            setResultToToast("任务状态:" + getWaypointMissionOperator().getCurrentState().getName());
            getWaypointMissionOperator().retryUploadMission(null);
        }
    }

    /**
     * 任务终止
     * mf,call wrong function,dashazi
     */
    private void stopMission() {
        // ToastHelper.getInstance().showShortToast("on stop");

        getWaypointMissionOperator().stopMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error3) {
                setResultToToast("任务终止: " + (error3 == null ? "成功" : error3.getDescription()));
            }
        });
        saveDroneStatus();
    }

    /**
     * 记录飞行器朝向、云台俯仰角
     */
    private void saveDroneStatus() {
//        savedHeadingDegree = (int) mFlightController.getCompass().getHeading();
        if (unfinishedPoints != null && !executeModelList.isEmpty()) {
            int count = unfinishedPoints.size();
            for (int i = executeModelList.size() - 1; i >= 0; i--) {
                int pointNum = executeModelList.get(i).getExecutePoints().size();
                if (count > pointNum) {
                    count = count - pointNum;
                } else {
                    savedCameraPitch = executeModelList.get(i).getCameraAngle();
                }
            }
        }
    }

    private void changeStatusOfPause() {
        WaypointMissionState state = getWaypointMissionOperator().getCurrentState();
        if (state != null) {
            if (state == WaypointMissionState.EXECUTING) {
                pauseMission();
                pause.setImageDrawable(getResources().getDrawable(R.drawable.ic_mission_resume));
            } else if (state == WaypointMissionState.EXECUTION_PAUSED) {
                continueMission();
                pause.setImageDrawable(getResources().getDrawable(R.drawable.ic_mission_stop));

            } else {
                pause.setImageDrawable(getResources().getDrawable(R.drawable.ic_mission_stop));
            }
        }
    }

    private void changeStatusOfStop() {
        WaypointMissionState state = getWaypointMissionOperator().getCurrentState();
        if (state != null) {
            if (state == WaypointMissionState.EXECUTING) {
                stopMission();
                exit.setImageDrawable(getResources().getDrawable(R.drawable.ic_mission_continue));
            } else {
                if (continueProcess()) {
                    exit.setImageDrawable(getResources().getDrawable(R.drawable.ic_mission_exit));
                }
            }
        }
    }

    /**
     * 任务暂停
     */
    private void pauseMission() {
        // ToastHelper.getInstance().showShortToast("on pause");

        getWaypointMissionOperator().pauseMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error3) {
                setResultToToast("任务暂停: " + (error3 == null ? "成功" : error3.getDescription()));
            }
        });
    }

    private void continueMission() {
        if (getWaypointMissionOperator().getCurrentState() == WaypointMissionState.EXECUTION_PAUSED) {
            getWaypointMissionOperator().resumeMission(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error3) {
                    setResultToToast("任务继续: " + (error3 == null ? "成功" : error3.getDescription()));
                }
            });
        }

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
            public void run() {
                Toast.makeText(getApplication(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private class GridviewSeleteAdapter extends BaseAdapter {
        List<BaseModel> oldModelList;
        private LayoutInflater inflater;
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
