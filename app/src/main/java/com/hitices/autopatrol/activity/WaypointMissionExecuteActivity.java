package com.hitices.autopatrol.activity;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.SeekBar;
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
import com.amap.api.maps2d.model.PolylineOptions;
import com.hitices.autopatrol.AutoPatrolApplication;
import com.hitices.autopatrol.R;
import com.hitices.autopatrol.algorithm.AntColonyAlgorithm;
import com.hitices.autopatrol.helper.FlightRecordHelper;
import com.hitices.autopatrol.missions.WaypointsMission;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.gimbal.GimbalMode;
import dji.common.gimbal.Rotation;
import dji.common.gimbal.RotationMode;
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

/**
 * WaypointMissionExecuteActivity 用于执行waypoint类型任务
 * created by Rhys
 */
public class WaypointMissionExecuteActivity extends Activity implements View.OnClickListener {
    public static WaypointMission.Builder builder;
    private WaypointsMission waypointsMission;
    private FlightController mFlightController;
    private Waypoint currentWaypoint;
    AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            showWaypointDetail(i);
        }
    };
    private WaypointMissionOperator insatnce;
    private MapView mapView;
    private AMap aMap;
    private Marker droneMarker;
    private LatLng droneLocation, locationLatlng;
    private Marker location;//photo location
    private Date startTime;
    AMapLocationListener aMapLocationListener = new AMapLocationListener() {
        @Override
        public void onLocationChanged(AMapLocation amapLocation) {
            if (amapLocation != null) {
                if (amapLocation.getErrorCode() == 0) {
//
                    locationLatlng = new LatLng(amapLocation.getLatitude(), amapLocation.getLongitude());
//                    LatLng harbin = new LatLng(126.640692,45.748065);-
                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(locationLatlng);
                    markerOptions.title("marker");
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
                    if (location != null)
                        location.destroy();
                    location = aMap.addMarker(markerOptions);
                } else {
                    //显示错误信息ErrCode是错误码，errInfo是错误信息，详见错误码表。
                    Log.e("AmapError", "location Error, ErrCode:"
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
                FlightRecordHelper.SaveRecord(waypointsMission.missionName, startTime, new Date());
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_execute);
        Intent intent = getIntent();
        String path = AutoPatrolApplication.MISSION_DIR + "/" + intent.getStringExtra("missionName") + ".xml";
        waypointsMission = readMission(path);

        initUI();
        initMapView(savedInstanceState);
        addListener();
        initFlightController();
        showMissionChangeDialog();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
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
        UiSettings settings = aMap.getUiSettings();
        settings.setZoomControlsEnabled(false);

        //use amap location
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
                            updateDroneLocation(AutoPatrolApplication.WGS84ConvertToAmap(droneLocation));
                            //  setResultToToast("after坐标" + AutoPatrolApplication.WGS84ConvertToAmap(droneLocation).latitude + "," + AutoPatrolApplication.WGS84ConvertToAmap(droneLocation).longitude);
                        }
                    });
            setResultToToast("in flight control");
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
    private void markWaypoint() {
        List<LatLng> lines = new ArrayList<>();
        //lines.add(droneLocation);
        for (int i = 0; i < waypointsMission.getWaypointList().size(); i++) {
            MarkerOptions markerOptions = new MarkerOptions();
            LatLng ll = new LatLng(waypointsMission.getWaypointList().get(i).coordinate.getLatitude(),
                    waypointsMission.getWaypointList().get(i).coordinate.getLongitude());
            lines.add(ll);
            markerOptions.position(ll);
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
            aMap.addMarker(markerOptions);
            cameraUpdate(ll);
        }
        //lines.add(droneLocation);
        aMap.addPolyline(new PolylineOptions().addAll(lines).color(Color.argb(255, 1, 1, 1)));
    }

    private void markWaypointbyIndex(List<Integer> sortedIndex) {
        List<LatLng> lines = new ArrayList<>();
        //lines.add(droneLocation);
        for (int i = 0; i < sortedIndex.size(); i++) {
            MarkerOptions markerOptions = new MarkerOptions();
            int index = sortedIndex.get(i);
            LatLng ll = new LatLng(waypointsMission.getWaypointList().get(index).coordinate.getLatitude(),
                    waypointsMission.getWaypointList().get(index).coordinate.getLongitude());
            lines.add(ll);
            markerOptions.position(ll);
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
            aMap.addMarker(markerOptions);
            cameraUpdate(ll);
        }
        //lines.add(droneLocation);
        aMap.addPolyline(new PolylineOptions().addAll(lines).color(Color.argb(255, 1, 1, 1)));
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
        if (insatnce == null) {
            insatnce = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
        }
        if (insatnce == null) {
            setResultToToast("waypointMissionOperator is null");
        }
        return insatnce;
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
    private void setCameraMode() {
        if (AutoPatrolApplication.getGimbalInstance() != null) {
            AutoPatrolApplication.getGimbalInstance().setMode(GimbalMode.FPV, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                }
            });
            Rotation.Builder b = new Rotation.Builder();
            b.mode(RotationMode.ABSOLUTE_ANGLE);
            b.pitch(-90f);
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

    /**
     * 任务概览，可修改部分参数
     * 显示飞行器类型，相机类型，当前坐标，
     * 高度、速度、任务执行完成动作、机头朝向等
     * 显示航点，航点部分参数支持修改
     */
    private void showMissionChangeDialog() {
        LinearLayout wpPreview = (LinearLayout) getLayoutInflater().inflate(R.layout.activity_preview_waypoint, null);
        //ui
        final TextView name, aircraft, camera, time, startpoint, seekBar_text, altitude;
        SeekBar sb_speed;
        RadioGroup rg_actionAfterFinished;
        RadioGroup rg_heading;
        GridView gv_missions;

        name = wpPreview.findViewById(R.id.preview_current_mission_name);
        aircraft = wpPreview.findViewById(R.id.preview_aircraft_name);
        camera = wpPreview.findViewById(R.id.preview_camera);
        time = wpPreview.findViewById(R.id.preview_time);
        startpoint = wpPreview.findViewById(R.id.preview_start_point);
        altitude = wpPreview.findViewById(R.id.preview_altitude);
        seekBar_text = wpPreview.findViewById(R.id.preview_seekBar_text);
        //final TextView tv_speed=findViewById(R.id.preview_seekBar_text);
        sb_speed = wpPreview.findViewById(R.id.preview_speed);
        rg_actionAfterFinished = wpPreview.findViewById(R.id.preview_actionAfterFinished);
        rg_heading = wpPreview.findViewById(R.id.preview_heading);
        gv_missions = wpPreview.findViewById(R.id.preview_gv_action);
        if (waypointsMission != null) {
            sb_speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    float tSpeed = (((float) i) / 100) * 15;
                    seekBar_text.setText(String.valueOf((int) (tSpeed + 0.5)) + "m/s");
                    waypointsMission.setSpeed(tSpeed);
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
                    if (i == R.id.finishNone) {
                        waypointsMission.setFinishedAction(WaypointMissionFinishedAction.NO_ACTION);
                    } else if (i == R.id.finishGoHome) {
                        waypointsMission.setFinishedAction(WaypointMissionFinishedAction.GO_HOME);
                    } else if (i == R.id.finishAutoLanding) {
                        waypointsMission.setFinishedAction(WaypointMissionFinishedAction.AUTO_LAND);
                    } else if (i == R.id.finishToFirst) {
                        waypointsMission.setFinishedAction(WaypointMissionFinishedAction.GO_FIRST_WAYPOINT);
                    }
                }
            });
            rg_heading.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup radioGroup, int i) {
                    if (i == R.id.headingNext) {
                        waypointsMission.setHeadingMode(WaypointMissionHeadingMode.AUTO);
                    } else if (i == R.id.headingInitDirec) {
                        waypointsMission.setHeadingMode(WaypointMissionHeadingMode.USING_INITIAL_DIRECTION);
                    } else if (i == R.id.headingRC) {
                        waypointsMission.setHeadingMode(WaypointMissionHeadingMode.CONTROL_BY_REMOTE_CONTROLLER);
                    } else if (i == R.id.headingWP) {
                        waypointsMission.setHeadingMode(WaypointMissionHeadingMode.USING_WAYPOINT_HEADING);
                    }
                }
            });
            gv_missions.setAdapter(new WPGridviewAdapter(this));
            gv_missions.setOnItemClickListener(onItemClickListener);
            //update text
            name.setText("任务名称:" + waypointsMission.missionName);
            time.setText("速度:" + String.valueOf(waypointsMission.getSpeed()));
            altitude.setText("默认高度:" + String.valueOf(waypointsMission.getAltitude() + " m"));
            //convert between float and int
            //rate=waypointsMission.speed/15
            sb_speed.setProgress((int) (waypointsMission.getSpeed() / 15 * 100 + 0.5));
            seekBar_text.setText(String.valueOf((int) (waypointsMission.getSpeed() + 0.5)) + "m/s");

            rg_actionAfterFinished.check(getFinishCheckedId(waypointsMission.getFinishedAction()));
            rg_heading.check(getHeadingCheckedId(waypointsMission.getHeadingMode()));
        } else {
            name.setText("XXXXXXXXXXXXXXXXXXXXXXXXXXX");
        }
        Aircraft myAircraft = AutoPatrolApplication.getAircraftInstance();
        Camera myCamera = AutoPatrolApplication.getCameraInstance();
        if (myAircraft != null) {
            aircraft.setText("飞行器型号:" + myAircraft.getModel().getDisplayName());
        } else {
            aircraft.setText("飞行器型号:xxx");
        }
        if (myCamera != null) {
            camera.setText("相机型号:" + myCamera.getDisplayName());
        } else {
            camera.setText("相机型号：xxx");
        }
        if (droneLocation != null) {
            startpoint.setText("无人机坐标 [" + String.valueOf(droneLocation.latitude) + "," + String.valueOf(droneLocation.longitude) + "] ");
        } else {
            startpoint.setText("null");
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("")
                .setView(wpPreview)
                .setPositiveButton("upload", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
        final AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                        missionProcessing();
                    }
                });
        Window window = dialog.getWindow();
        //window.getDecorView().setPadding(0,0,0,0);
        WindowManager.LayoutParams params = window.getAttributes();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.MATCH_PARENT;
        window.setAttributes(params);
    }

    /**
     * 调用蚁群算法对航点排序
     * 需使用无人机当前位置，影响算法
     */
    private void sortingWaypoints() {
        if (waypointsMission != null) {
            if (droneLocation == null) {
//                droneLocation = new LatLng(waypointsMission.waypointList.get(0).coordinate.getLatitude(),
//                        waypointsMission.waypointList.get(0).coordinate.getLongitude());
                droneLocation = AutoPatrolApplication.AmapConvertToWGS84(locationLatlng);
            }
            AntColonyAlgorithm antColonyAlgorithm = new AntColonyAlgorithm(waypointsMission.getWaypointList(), droneLocation);
            List<Integer> sortedIndex = antColonyAlgorithm.getSortedWaypoints();
            //markwaypoint
            markWaypointbyIndex(sortedIndex);
            List<Waypoint> newWaypointList = new ArrayList<>();
            //built new waypoint list,include convert
            for (int i = 0; i < sortedIndex.size(); i++) {
                newWaypointList.add(waypointConvert(waypointsMission.getWaypointList().get(sortedIndex.get(i))));
            }
            waypointsMission.setWaypointList(newWaypointList);
        }
    }

    /**
     * 确认任务后的操作流程
     * 排序
     * load mission
     * 在地图标记航线
     */
    private void missionProcessing() {
        sortingWaypoints();
        loadMission();
        //markWaypoint();
        setCameraMode();
    }

    /**
     * loadMission
     * 航点任务执行流程第一步
     */
    private void loadMission() {
        setResultToToast("on load");
        //以无人机起飞位置作为返航点
        if (droneLocation != null) {
            waypointsMission.addWaypointList(droneLocation);
        }
        //以当前人的位置作返航点
        else {
            if (locationLatlng != null)
                waypointsMission.addWaypointList(AutoPatrolApplication.AmapConvertToWGS84(locationLatlng));
        }
        builder = waypointsMission.getMissionBuilder();
        if (builder != null) {
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

    /**
     * 坐标转换，高德->GPS
     */
    private Waypoint waypointConvert(Waypoint w) {
        LatLng l = AutoPatrolApplication.AmapConvertToWGS84(
                new LatLng(w.coordinate.getLatitude(), w.coordinate.getLongitude()));
        Waypoint waypoint = new Waypoint(l.latitude, l.longitude, w.altitude);
        for (int i = 0; i < w.waypointActions.size(); i++) {
            waypoint.addAction(w.waypointActions.get(i));
        }
        return waypoint;
    }

    /**
     * uploadMission
     * 航点任务执行流程第二步
     * 上传任务到飞机
     */
    private void uploadMission() {
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

    /**
     * 任务开始执行
     */
    private void startMission() {
        setResultToToast("on start");
        getWaypointMissionOperator().startMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error3) {
                if (error3 == null) {
                    startTime = new Date();
                }
                setResultToToast("Mission Start: " + (error3 == null ? "Successfully" : error3.getDescription()));
            }
        });
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
     * 显示航点具体信息，不支持修改
     *
     * @param i
     */
    private void showWaypointDetail(int i) {
        //preview_wp_setting
        LinearLayout waypointDetail = (LinearLayout) getLayoutInflater().inflate(R.layout.waypoint_preview_waypoint_detail, null);
        GridView detail = waypointDetail.findViewById(R.id.preview_wp_setting);
        final EditText altitude = waypointDetail.findViewById(R.id.preview_wp_altitude);
        currentWaypoint = waypointsMission.getWaypointList().get(i);
        final WPWaypointGridviewAdapter adapter = new WPWaypointGridviewAdapter(currentWaypoint.waypointActions, this);
        detail.setAdapter(adapter);
        altitude.setInputType(InputType.TYPE_CLASS_NUMBER);
        altitude.setText(String.valueOf(currentWaypoint.altitude));
        new AlertDialog.Builder(this).setTitle("")
                .setView(waypointDetail)
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        currentWaypoint.altitude = Float.valueOf(altitude.getText().toString());
                        currentWaypoint.removeAllAction();
                        for (int j = 0; j < adapter.getSelectedAction().size(); j++) {
                            currentWaypoint.addAction(new WaypointAction(adapter.getSelectedAction().get(j), j));
                        }
                    }
                })
                .create().show();
    }

    /**
     * readMission
     * 从文件中导入任务
     *
     * @param path
     * @return
     */
    private WaypointsMission readMission(String path) {
        try {
            File file = new File(path);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            //name
            NodeList nodes = doc.getElementsByTagName("missionName");
            if (nodes.item(0) == null) {
                return null;
            }
            WaypointsMission newMission = new WaypointsMission(nodes.item(0).getTextContent());

            nodes = doc.getElementsByTagName("speed");
            if (nodes.item(0) != null) {
                newMission.setSpeed(Float.parseFloat(nodes.item(0).getTextContent()));
            }
            nodes = doc.getElementsByTagName("altitude");
            if (nodes.item(0) != null) {
                newMission.setAltitude(Float.parseFloat(nodes.item(0).getTextContent()));
            }
            //finishedAction
            nodes = doc.getElementsByTagName("finishedAction");
            if (nodes.item(0) != null) {
                newMission.setFinishedAction(getFinishedAction(nodes.item(0).getTextContent()));
            }
            //headingMode
            nodes = doc.getElementsByTagName("headingMode");
            if (nodes.item(0) != null) {
                newMission.setHeadingMode(getHeadingMode(nodes.item(0).getTextContent()));
            }
            //Waypoints
            nodes = doc.getElementsByTagName("Waypoints");
            Node node = nodes.item(0);
            //single waypoint
            NodeList nWaypointList = ((Element) node).getElementsByTagName("waypoint");
            for (int temp = 0; temp < nWaypointList.getLength(); temp++) {
                Node nNode = nWaypointList.item(temp);
                System.out.println("\nCurrent Element :" + nNode.getNodeName());
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    LatLng ll = new LatLng(
                            Double.parseDouble(eElement.getElementsByTagName("latitude").item(0).getTextContent()),
                            Double.parseDouble(eElement.getElementsByTagName("longitude").item(0).getTextContent()));
                    Waypoint w = new Waypoint(
                            ll.latitude, ll.longitude,
                            Float.parseFloat(eElement.getElementsByTagName("altitude").item(0).getTextContent()));
                    NodeList eActions = eElement.getElementsByTagName("actions");
                    for (int j = 0; j < eActions.getLength(); j++) {
                        Node n = eActions.item(j);
                        if (n.getNodeType() == Node.ELEMENT_NODE) {
                            Element e = (Element) n;
                            NodeList t = e.getChildNodes();
                            for (int k = 0; k < t.getLength(); k++) {
                                WaypointActionType type = getAction(t.item(k).getNodeName());
                                w.addAction(new WaypointAction(type, k));
                            }
                        }
                    }
                    newMission.addWaypointToList(w);
                    newMission.getWaypoints().put(ll, w);
                    System.out.println("\nlat and lng" + String.valueOf(ll.latitude) + " : " + String.valueOf(ll.longitude));
                }
            }
            newMission.FLAG_ISSAVED = true;
            return newMission;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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

    /**
     * 工具类函数
     * 航点动作字符串与动作类的对应关系
     *
     * @param s
     * @return
     */
    @Nullable
    private WaypointActionType getAction(String s) {
        if (WaypointActionType.STAY.toString().equals(s))
            return WaypointActionType.STAY;
        else if (WaypointActionType.CAMERA_ZOOM.toString().equals(s))
            return WaypointActionType.CAMERA_ZOOM;
        else if (WaypointActionType.CAMERA_FOCUS.toString().equals(s))
            return WaypointActionType.CAMERA_FOCUS;
        else if (WaypointActionType.GIMBAL_PITCH.toString().equals(s))
            return WaypointActionType.GIMBAL_PITCH;
        else if (WaypointActionType.START_RECORD.toString().equals(s))
            return WaypointActionType.START_RECORD;
        else if (WaypointActionType.STOP_RECORD.toString().equals(s))
            return WaypointActionType.STOP_RECORD;
        else if (WaypointActionType.ROTATE_AIRCRAFT.toString().equals(s))
            return WaypointActionType.ROTATE_AIRCRAFT;
        else if (WaypointActionType.START_TAKE_PHOTO.toString().equals(s))
            return WaypointActionType.START_TAKE_PHOTO;
        else return null;
    }

    private int getFinishCheckedId(WaypointMissionFinishedAction action) {
        if (action == WaypointMissionFinishedAction.NO_ACTION) {
            return R.id.preview_finishNone;
        } else if (action == WaypointMissionFinishedAction.GO_HOME) {
            return R.id.preview_finishGoHome;
        } else if (action == WaypointMissionFinishedAction.AUTO_LAND) {
            return R.id.preview_finishAutoLanding;
        } else if (action == WaypointMissionFinishedAction.GO_FIRST_WAYPOINT) {
            return R.id.preview_finishToFirst;
        } else
            return R.id.preview_finishNone;
    }

    private int getHeadingCheckedId(WaypointMissionHeadingMode action) {
        if (action == WaypointMissionHeadingMode.CONTROL_BY_REMOTE_CONTROLLER) {
            return R.id.preview_headingRC;
        } else if (action == WaypointMissionHeadingMode.USING_INITIAL_DIRECTION) {
            return R.id.preview_headingInitDirec;
        } else if (action == WaypointMissionHeadingMode.USING_WAYPOINT_HEADING) {
            return R.id.preview_headingWP;
        } else
            return R.id.preview_headingNext;
    }

    private void setResultToToast(final String msg) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getActionChinese(WaypointActionType action) {
        String chinese = "";
        switch (action) {
            case STAY:
                chinese = "停下";
                break;
            case CAMERA_ZOOM:
                chinese = "相机变焦";
                break;
            case CAMERA_FOCUS:
                chinese = "相机焦点";
                break;
            case GIMBAL_PITCH:
                chinese = "云台调整";
                break;
            case START_RECORD:
                chinese = "开始录像";
                break;
            case STOP_RECORD:
                chinese = "停止录像";
                break;
            case ROTATE_AIRCRAFT:
                chinese = "旋转";
                break;
            case START_TAKE_PHOTO:
                chinese = "拍照";
                break;
            default:
                break;
        }
        return chinese;
    }

    private class WPGridviewAdapter extends BaseAdapter {
        private LayoutInflater inflater;

        WPGridviewAdapter(Context context) {
            inflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return waypointsMission.getWaypointList().size();
        }

        @Override
        public Object getItem(int position) {
            return waypointsMission.getWaypoints().get(position);
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
                convertView = inflater.inflate(R.layout.w_p_gv_item, null);
                holder.id = convertView.findViewById(R.id.preview_gvitem_id);
                holder.lat = convertView.findViewById(R.id.preview_gvitem_lat);
                holder.lng = convertView.findViewById(R.id.preview_gvitem_lng);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.id.setText(String.valueOf(position));
            holder.lat.setText(String.valueOf(waypointsMission.getWaypointList().get(position).coordinate.getLatitude()));
            holder.lng.setText(String.valueOf(waypointsMission.getWaypointList().get(position).coordinate.getLongitude()));
            return convertView;
        }

        class ViewHolder {
            TextView id, lat, lng;
        }

    }

    private class WPWaypointGridviewAdapter extends BaseAdapter {
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
            ViewHolder holder;
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
                    CheckBox cb = (CheckBox) view;
                    int position = cb.getId();
                    boolean b = cb.isChecked();
                    if (b) {
                        if (!selectedAction.contains(allAction.get(position)))
                            selectedAction.add(allAction.get(position));
                    } else {
                        selectedAction.remove(allAction.get(position));
                    }
                }
            });
            return convertView;
        }

        private boolean isSelected(WaypointActionType wat) {
            for (int i = 0; i < actions.size(); i++) {
                if (actions.get(i).actionType.equals(wat))
                    return true;
            }
            return false;
        }

        public List<WaypointActionType> getSelectedAction() {
            return selectedAction;
        }

        private void initData() {
            allAction = new ArrayList<>();
            allAction.add(WaypointActionType.CAMERA_FOCUS);
            allAction.add(WaypointActionType.CAMERA_ZOOM);
            allAction.add(WaypointActionType.GIMBAL_PITCH);
            allAction.add(WaypointActionType.START_RECORD);
            allAction.add(WaypointActionType.STOP_RECORD);
            allAction.add(WaypointActionType.START_TAKE_PHOTO);
            allAction.add(WaypointActionType.STAY);
            allAction.add(WaypointActionType.ROTATE_AIRCRAFT);
            selectedAction = new ArrayList<>();
            for (int i = 0; i < actions.size(); i++) {
                selectedAction.add(actions.get(i).actionType);
            }
        }

        class ViewHolder {
            CheckBox cb;
            TextView name;
        }
    }
}
