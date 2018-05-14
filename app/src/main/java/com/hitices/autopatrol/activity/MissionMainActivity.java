package com.hitices.autopatrol.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import com.amap.api.maps2d.model.Polygon;
import com.amap.api.maps2d.model.PolygonOptions;
import com.amap.api.maps2d.model.Polyline;
import com.amap.api.maps2d.model.PolylineOptions;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.hitices.autopatrol.R;
import com.hitices.autopatrol.adapter.FlatlandSettingGridviewAdapter;
import com.hitices.autopatrol.entity.dataSupport.PatrolMission;
import com.hitices.autopatrol.entity.missions.BaseModel;
import com.hitices.autopatrol.entity.missions.FlatlandModel;
import com.hitices.autopatrol.entity.missions.ModelType;
import com.hitices.autopatrol.entity.missions.MultiPointsModel;
import com.hitices.autopatrol.entity.missions.SlopeModel;
import com.hitices.autopatrol.helper.ContextHelper;
import com.hitices.autopatrol.helper.GoogleMapHelper;
import com.hitices.autopatrol.helper.MissionConstraintHelper;
import com.hitices.autopatrol.helper.MissionHelper;
import com.hitices.autopatrol.helper.ToastHelper;

import org.litepal.crud.DataSupport;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import dji.common.mission.waypoint.Waypoint;


public class MissionMainActivity extends AppCompatActivity implements View.OnClickListener {
    AMap.OnInfoWindowClickListener onInfoWindowClickListener = new AMap.OnInfoWindowClickListener() {
        @Override
        public void onInfoWindowClick(Marker marker) {
        }
    };
    private ChildMissionListAdapter adapter;
    //mission about
    private PatrolMission mission;
    private List<BaseModel> modelList;
    private BaseModel currentModel;
    private boolean saveFlag = true;
    //multiPoint
    private List<Marker> mMarkers = new ArrayList<>();
    //ui
    private AMap aMap;
    private MapView mapView;
    private Button btn_childlist;
    private FloatingActionsMenu menu;
    private FloatingActionButton btn_add, btn_save, btn_show;
    private RecyclerView recyclerView;
    private TextView currentChildMissionName;
    private ImageButton child_setting;
    //location about
    private LatLng locationLatlng;
    private Marker location;
    AMapLocationListener aMapLocationListener = new AMapLocationListener() {
        @Override
        public void onLocationChanged(AMapLocation amapLocation) {
            if (amapLocation != null) {
                if (amapLocation.getErrorCode() == 0) {
                    locationLatlng = new LatLng(amapLocation.getLatitude(), amapLocation.getLongitude());
                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(locationLatlng);
                    markerOptions.title("marker");
                    markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_human_location_marker)));
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
    private Marker currentMarker;
    private boolean flag_isShow = false;
    AMap.OnMarkerClickListener markerClickListener = new AMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {
            if (currentMarker == null)
                currentMarker = marker;
            if (currentMarker.equals(marker)) {
                if (!flag_isShow) {
                    marker.showInfoWindow();
                    flag_isShow = true;
                } else {
                    marker.hideInfoWindow();
                    flag_isShow = false;
                }
            } else {
                currentMarker.hideInfoWindow();
                marker.showInfoWindow();
                flag_isShow = true;
            }
            currentMarker = marker;
            return true;
        }
    };
    //flatland
    private Polyline f_polyline;
    private Polygon f_polygon;
    private Marker f_startPoint;
    //slope model
    private Polyline s_polyline;
    private Polygon s_polygon;
    private Marker s_startPoint;
    private Marker s_base_A;
    private Marker s_base_B;
    private Polyline s_baseline;
    AMap.OnMarkerDragListener markerDragListener = new AMap.OnMarkerDragListener() {
        String type;
        String id;

        @Override
        public void onMarkerDragStart(Marker arg0) {
            type = arg0.getTitle();
            saveFlag = false;
        }

        @Override
        public void onMarkerDragEnd(Marker arg0) {
            if (currentModel.getModelType() == ModelType.Slope) {
                if (type.equals("baseA")) {
                    s_baseline.remove();
                    //s_base_A=arg0;
                    id = s_base_A.getId();
                } else if (type.equals("baseB")) {
                    s_baseline.remove();
                    //s_base_B=arg0;
                    id = s_base_B.getId();
                }
            }
            drawBaseline(s_base_A, s_base_B);
//            System.out.println("debug:ID0:"+arg0.getId());
//            System.out.println("debug:IDA:"+s_base_A.getId());
//            System.out.println("debug:IDB:"+s_base_B.getId());
//            System.out.println("debug:arg0:"+arg0.getTitle().toString());
//            System.out.println("debug:A:"+s_base_A.getTitle().toString());
//            System.out.println("debug:B:"+s_base_B.getTitle().toString());
            //removerRedundancy();

        }

        @Override
        public void onMarkerDrag(Marker arg0) {
        }

        private void removerRedundancy() {
            if (!id.isEmpty()) {
                List<Marker> list = aMap.getMapScreenMarkers();
                System.out.println("debug:" + String.valueOf(list.size()));
                for (int i = 0; i < list.size(); i++) {
                    Marker temp = list.get(i);
                    if (temp.getTitle().equals(type)) {
                        System.out.println("debug:" + id + ":" + temp.getId());
                        if (!temp.getId().equals(id)) {
                            System.out.println("debug:aa" + id + ":" + temp.getId());
                            temp.destroy();

                        }
                    }
                }
            }
        }
    };
    private int status_of_slopemodel = 1;//设置不同状态，主要用于标记baseline(1:添加边界; 2:绘制baseline)
    //listener
    AMap.OnMapClickListener onMapClickListener = new AMap.OnMapClickListener() {
        @Override
        public void onMapClick(LatLng latLng) {
            switch (currentModel.getModelType()) {
                case MultiPoints:
                    markWaypoint(latLng);
                    getMultipointsModel().addPointToList(latLng);
                    saveFlag = false;
                    break;
                case Flatland:
                    drawFlatlandBorder(latLng);
                    saveFlag = false;
                    break;
                case Slope:
                    switch (status_of_slopemodel) {
                        case 1:
                            drawSlopeBorder(latLng);
                            saveFlag = false;
                            break;
                        case 2:
                            //drawBaseline();
                            drawBaselineMarker(latLng);
                            saveFlag = false;
                            break;
                    }
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mission_main);
        initMission();
        initMapView(savedInstanceState);
        initUI();
        refreshView();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            //;need to add new save logic
            if (!saveFlag) {
                new AlertDialog.Builder(this).setTitle("提醒")
                        .setMessage("当前任务可能已经被修改，是否保存这些变化？")
                        .setPositiveButton("是", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                saveMission();
                                finish();
                            }
                        })
                        .setNegativeButton("否", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                finish();
                            }
                        }).create().show();
            } else {
                finish();
            }
            return true;
        }
        return super.onKeyDown(keyCode, keyEvent);
    }

    @Override
    public void onClick(View view) {
        menu.collapse();
        switch (view.getId()) {
            case R.id.btn_child_mission_main:
                changeMissionListStatus();
                break;
            case R.id.add_mdoel_mission_main:
                addChildMissionDialog();
                break;
            case R.id.save_mission_main:
                saveMission();
                break;
            case R.id.btn_setting_mission_main:
                showSettingDialog();
                break;
            case R.id.show_all_mdoel_mission_main:
                showAllModel();
                break;
        }
    }

    private Context getContext() {
        return this;
    }
    private void initMapView(Bundle savedInstanceState) {
        mapView = findViewById(R.id.map_mission_main);
        mapView.onCreate(savedInstanceState);
        if (aMap == null) {
            aMap = mapView.getMap();
            //use google map satellite data
        }
        GoogleMapHelper.useGoogleMapSatelliteData(aMap);
        aMap.setOnMapClickListener(onMapClickListener);
        aMap.setOnMarkerClickListener(markerClickListener);
        aMap.setOnMarkerDragListener(markerDragListener);

        UiSettings settings = aMap.getUiSettings();
        settings.setZoomControlsEnabled(false);
        settings.setScaleControlsEnabled(true);
        //use gps location
        AMapLocationClientOption mLocationOption = new AMapLocationClientOption();
        AMapLocationClient mlocationClient = new AMapLocationClient(this);
        mlocationClient.setLocationListener(aMapLocationListener);
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        mLocationOption.setInterval(1000);
        mlocationClient.setLocationOption(mLocationOption);
        mlocationClient.startLocation();
        //info
        aMap.setOnInfoWindowClickListener(onInfoWindowClickListener);
        MyInfoWindowAdapter MyInfoWindowAdapter = new MyInfoWindowAdapter();
        aMap.setInfoWindowAdapter(MyInfoWindowAdapter);
        //first zoom update
        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(mlocationClient.getLastKnownLocation().getLatitude(),
                        mlocationClient.getLastKnownLocation().getLongitude()),
                18f));
    }

    private void initUI() {
        btn_childlist = findViewById(R.id.btn_child_mission_main);
        child_setting = findViewById(R.id.btn_setting_mission_main);
        btn_add = findViewById(R.id.add_mdoel_mission_main);
        btn_save = findViewById(R.id.save_mission_main);
        btn_show = findViewById(R.id.show_all_mdoel_mission_main);
        menu = findViewById(R.id.menu_mission_main);
        recyclerView = findViewById(R.id.view_child_mission_main);
        currentChildMissionName = findViewById(R.id.tv_child_name_mission_main);

        LinearLayoutManager manager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(manager);
        adapter = new ChildMissionListAdapter(modelList);
        recyclerView.setAdapter(adapter);

        child_setting.setOnClickListener(this);
        btn_childlist.setOnClickListener(this);
        btn_save.setOnClickListener(this);
        btn_add.setOnClickListener(this);
        btn_show.setOnClickListener(this);
    }

    private void initMission() {
        modelList = new ArrayList<>();
        currentModel = null;
        String type = getIntent().getStringExtra("TYPE");
        if (type.equals("modify")) {
            String path = getIntent().getStringExtra("PATH");
            int id = getIntent().getIntExtra("ID", -1);
            //read mission
            mission = DataSupport.find(PatrolMission.class, id);
            if (mission == null) {
                ToastHelper.getInstance().showShortToast("can not open current mission");
                finish();
            }
            if (path == null) {
                mission.setFilePath(MissionConstraintHelper.MISSION_DIR + "/" + mission.getName() + ".xml");
            }
            MissionHelper helper = new MissionHelper(mission.getFilePath(), mission);
            modelList = helper.getModelList();
            if (modelList.size() > 0) {
                currentModel = modelList.get(0);
            }
        } else {
            mission = new PatrolMission();
            showCreateDialog();
        }
    }

    private void changeMissionListStatus() {
        adapter.setModelList(modelList);
        int i = recyclerView.getVisibility();
        switch (i) {
            case View.VISIBLE:
                recyclerView.setVisibility(View.INVISIBLE);
                break;
            case View.INVISIBLE:
                recyclerView.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void showCreateDialog() {
        AlertDialog.Builder adBuilder = new AlertDialog.Builder(this);
        adBuilder.setTitle("请输入任务名称");
        final EditText input = new EditText(this);
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
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        final AlertDialog alertDialog = adBuilder.create();
        alertDialog.show();
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String text = input.getText().toString().trim();
                        if (text.length() > 0) {
                            mission.setName(text);
                            mission.setLastModifiedTime(new Date());
                            refreshView();
                            alertDialog.dismiss();

                        } else {
                            ToastHelper.getInstance().showShortToast("任务名不能为空");
                        }
                    }
                });

        alertDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
    }

    private void addChildMissionDialog() {
        LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_add_child_mission, null);
        final EditText name = layout.findViewById(R.id.edit_name_mission_main);
        final RadioGroup type = layout.findViewById(R.id.child_type_mission_main);
        //init
        type.check(R.id.multipoints_mission_main);
        type.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
            }
        });
        //dialog init
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("添加子任务")
                .setPositiveButton("确认", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String n = name.getText().toString();
                        if (n.length() < 1) {
                            setResultToToast("添加任务失败");
                        } else {
                            switch (type.getCheckedRadioButtonId()) {
                                case R.id.multipoints_mission_main:
                                    MultiPointsModel model = new MultiPointsModel(n);
                                    currentModel = model;
                                    modelList.add(model);
                                    break;
                                case R.id.flatland_mission_main:
                                    FlatlandModel model1 = new FlatlandModel(n);
                                    currentModel = model1;
                                    modelList.add(model1);
                                    break;
                                case R.id.slope_mission_main:
                                    SlopeModel mode2 = new SlopeModel(n);
                                    currentModel = mode2;
                                    status_of_slopemodel = 2;
                                    modelList.add(mode2);
                                    break;
                            }
                            //test
                            System.out.println("num of models:" + String.valueOf(modelList.size()));
                            refreshView();
                            saveFlag = false;
                        }
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                })
                .setView(layout);
        builder.create().show();
    }

    private void showSettingDialog() {
        if (currentModel != null) {
            switch (currentModel.getModelType()) {
                case Slope:
                    showSlopeModelSettingDialog();
                    break;
                case Flatland:
                    showFlatlandModelSettingDialog();
                    break;
                case MultiPoints:
                    showMultiPointsModelSettingDialog();
                    break;
            }
        }
    }

    private void showAllModel() {
        if (currentModel != null) {
            switch (currentModel.getModelType()) {
                case MultiPoints:
                    clearMultiPointDisplay();
                    break;
                case Flatland:
                    clearFlatlandDisplay();
                    break;
                case Slope:
                    clearSlopeDisplay();
                    break;
            }
        }
        if (!modelList.isEmpty()) {
            for (int i = 0; i < modelList.size(); i++) {

                switch (modelList.get(i).getModelType()) {
                    case Slope:
                        showSlopeView((SlopeModel) modelList.get(i));
                        break;
                    case Flatland:
                        showFlatlandView((FlatlandModel) modelList.get(i));
                        break;
                    case MultiPoints:
                        showMultiPointsView((MultiPointsModel) modelList.get(i));
                        break;
                }
            }
        }
    }

    private void showMultiPointsModelSettingDialog() {
        LinearLayout multipointdSetiing = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_multipoints_setting, null);
        final TextView altitude_text = multipointdSetiing.findViewById(R.id.altitude_text);
        final TextView speed_text = multipointdSetiing.findViewById(R.id.seekBar_text);
        final TextView mName = multipointdSetiing.findViewById(R.id.setting_mName);
        final TextView pitchText = multipointdSetiing.findViewById(R.id.pitch_text);
        final TextView headingText = multipointdSetiing.findViewById(R.id.heading_text);

        final SeekBar sb_speed = multipointdSetiing.findViewById(R.id.speed);
        final SeekBar sb_altitude = multipointdSetiing.findViewById(R.id.altitude);
        final SeekBar sb_pitch = multipointdSetiing.findViewById(R.id.pitch);
        final SeekBar sb_heading = multipointdSetiing.findViewById(R.id.heading);
        //init seekbar
        sb_altitude.setMax((int) (MissionConstraintHelper.getMaxAltitude() + 0.5));
        sb_speed.setMax((int) (MissionConstraintHelper.getMaxSpeed() + 0.5));
        sb_pitch.setMax(MissionConstraintHelper.getMaxPitch());
        sb_heading.setMax(360);
        //update data
        //通用高度
        sb_altitude.setProgress((int) (getMultipointsModel().getAltitude()));
        altitude_text.setText(String.valueOf(getMultipointsModel().getAltitude()) + " m");
        //任务名称
        mName.setText(getMultipointsModel().getMissionName());
        //通用速度
        sb_speed.setProgress((int) (getMultipointsModel().getSpeed() + 0.5));
        speed_text.setText(String.valueOf(getMultipointsModel().getSpeed()) + " m/s");
        //朝向
        sb_heading.setProgress(getMultipointsModel().getHeadingAngle() + 180);
        headingText.setText(String.valueOf(getMultipointsModel().getHeadingAngle()));
        //俯仰角
        sb_pitch.setProgress(getMultipointsModel().getCameraAngle());
        pitchText.setText(String.valueOf(getMultipointsModel().getCameraAngle()));
        //设置监听器
        sb_pitch.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                pitchText.setText(String.valueOf(i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        sb_speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                speed_text.setText(String.valueOf(i) + " m/s");
                //getMultipointsModel().setSpeed(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        sb_altitude.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                //getMultipointsModel().setAltitude(i);
                altitude_text.setText(String.valueOf(i) + " m");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        sb_heading.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                headingText.setText(String.valueOf(i - 180));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("")
                .setView(multipointdSetiing)
                .setPositiveButton("确认", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //change commit
                        getMultipointsModel().setSpeed(sb_speed.getProgress());
                        getMultipointsModel().setAltitude(sb_altitude.getProgress());
                        getMultipointsModel().setCameraAngle(sb_pitch.getProgress());
                        int angle = sb_heading.getProgress();
                        if (angle >= 0 && angle <= 360) {
                            getMultipointsModel().setHeadingAngle(angle - 180);
                            getMultipointsModel().changeActionOfAllPoints(angle - 180);
                        } else {
                            getMultipointsModel().setHeadingAngle(0); //使用默认值0，正北方向
                        }
                        //change flag
                        saveFlag = false;
                    }
                })
                .setNegativeButton("取消更改", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
        builder.create().show();

    }

    private void showFlatlandModelSettingDialog() {

        LinearLayout settingView = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_flatland_setting, null);

        final SeekBar mAltitude = settingView.findViewById(R.id.polygon_setting_altitude);
        final SeekBar mSpeed = settingView.findViewById(R.id.polygon_setting_speed);
        final SeekBar mOverate = settingView.findViewById(R.id.polygon_setting_overrate);
        final SeekBar mWidth = settingView.findViewById(R.id.polygon_setting_width);
        final SeekBar mPitch = settingView.findViewById(R.id.polygon_setting_pitch);
        final SeekBar mHeading = settingView.findViewById(R.id.polygon_setting_heading);
        final SeekBar mDis=settingView.findViewById(R.id.polygon_setting_dis);

        final TextView mSpeedText = settingView.findViewById(R.id.polygon_setting_speed_text);
        final TextView mOverateText = settingView.findViewById(R.id.polygon_setting_overrate_text);
        final TextView mWidthText = settingView.findViewById(R.id.polygon_setting_width_text);
        final TextView mAltitudeText = settingView.findViewById(R.id.polygon_setting_altitude_text);
        final TextView mPitchText = settingView.findViewById(R.id.polygon_setting_pitch_text);
        final TextView mName = settingView.findViewById(R.id.polygon_setting_name);
        final TextView mHeadingText = settingView.findViewById(R.id.polygon_setting_heading_text);
        final TextView mDisText=settingView.findViewById(R.id.polygon_setting_dis_text);

        GridView vertexs = settingView.findViewById(R.id.polygon_setting_vertexs);
        //init
        mName.setText(getFlatlandModel().getMissionName());

        mAltitude.setMax((int) MissionConstraintHelper.getMaxAltitude());
        mAltitude.setProgress((int) (getFlatlandModel().getAltitude() + 0.5));
        mAltitudeText.setText(String.valueOf((int) (getFlatlandModel().getAltitude() + 0.5)) + " m");

        mDis.setMax(20);
        mDis.setProgress((int)(getFlatlandModel().getDistanceToPanel() + 0.5));
        mDisText.setText(String.valueOf((int) (getFlatlandModel().getDistanceToPanel() + 0.5)) + " m");

        mSpeed.setMax((int) MissionConstraintHelper.getMaxSpeed());
        mSpeed.setProgress((int) (getFlatlandModel().getSpeed()));
        mSpeedText.setText(String.valueOf((int) (getFlatlandModel().getSpeed() + 0.5)) + " m/s");

        mOverate.setMax(MissionConstraintHelper.getMaxFlatlandOverRate());
        //mOverate.setMin(10);
        mOverate.setProgress(getFlatlandModel().getOverlapRate());
        mOverateText.setText(String.valueOf(getFlatlandModel().getOverlapRate()) + "%");

        mWidth.setMax(MissionConstraintHelper.getMaxFlatlandWidth());
        mWidth.setProgress((int) (getFlatlandModel().getWidth() + 0.5));
        mWidthText.setText(String.valueOf((int) (getFlatlandModel().getWidth() + 0.5)) + " m");

        mPitch.setMax(MissionConstraintHelper.getMaxPitch());
        mPitch.setProgress(getFlatlandModel().getCameraAngle());
        mPitchText.setText(String.valueOf(getFlatlandModel().getCameraAngle()));

        mHeading.setMax(360);
        mHeading.setProgress(getFlatlandModel().getHeadingAngle() + 180);
        mHeadingText.setText(String.valueOf(getFlatlandModel().getHeadingAngle()));

        mSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mSpeedText.setText(String.valueOf(i) + "m/s");
                // getFlatlandModel().setSpeed(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mOverate.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mOverateText.setText(String.valueOf(i) + " %");
//                getFlatlandModel().setOverlapRate(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mWidth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mWidthText.setText(String.valueOf(i) + " m");
                // getFlatlandModel().setWidth(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mAltitude.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mAltitudeText.setText(String.valueOf(i) + " m");
                // getFlatlandModel().setAltitude(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mPitch.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mPitchText.setText(String.valueOf(i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mHeading.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mHeadingText.setText(String.valueOf(i - 180));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mDis.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mDisText.setText(String.valueOf(progress)+" m");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        vertexs.setAdapter(new FlatlandSettingGridviewAdapter(this, getFlatlandModel().getVertexs()));
        //dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(settingView);
        builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //change commit
                getFlatlandModel().setAltitude(mAltitude.getProgress());
                getFlatlandModel().setOverlapRate(mOverate.getProgress());
                getFlatlandModel().setSpeed(mSpeed.getProgress());
                getFlatlandModel().setWidth(mWidth.getProgress());
                getFlatlandModel().setCameraAngle(mPitch.getProgress());
                getFlatlandModel().setDistanceToPanel(mDis.getProgress());
                int angle = mHeading.getProgress();
                if (angle >= 0 && angle <= 360) {
                    getFlatlandModel().setHeadingAngle(angle - 180);
                } else {
                    getFlatlandModel().setHeadingAngle(0); //使用默认值0，正北方向
                }
                //change flag
                saveFlag = false;
            }
        });
        builder.setNeutralButton("取消修改", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        builder.create().show();
    }

    private void showSlopeModelSettingDialog() {
        LinearLayout settingView = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_slope_setting, null);

        final SeekBar mAltitude = settingView.findViewById(R.id.slope_setting_altitude);
        final SeekBar mSpeed = settingView.findViewById(R.id.slope_setting_speed);
        final SeekBar mOverate = settingView.findViewById(R.id.slope_setting_overrate);
        final SeekBar mWidth = settingView.findViewById(R.id.slope_setting_width);
        final SeekBar mPitch = settingView.findViewById(R.id.slope_setting_pitch);
        final SeekBar mDistance = settingView.findViewById(R.id.slope_setting_distance);
        final SeekBar mHeading = settingView.findViewById(R.id.slope_setting_heading);

        final TextView mSpeedText = settingView.findViewById(R.id.slope_setting_speed_text);
        final TextView mOverateText = settingView.findViewById(R.id.slope_setting_overrate_text);
        final TextView mWidthText = settingView.findViewById(R.id.slope_setting_width_text);
        final TextView mAltitudeText = settingView.findViewById(R.id.slope_setting_altitude_text);
        final TextView mPitchText = settingView.findViewById(R.id.slope_setting_pitch_text);
        final TextView mDistanceText = settingView.findViewById(R.id.slope_setting_distance_text);
        final TextView mName = settingView.findViewById(R.id.slope_setting_name);
        final TextView mHeadingText = settingView.findViewById(R.id.slope_setting_heading_text);

        //base line
        final Button set = settingView.findViewById(R.id.slope_setting_set);
        final SeekBar mA = settingView.findViewById(R.id.slope_setting_A);
        final SeekBar mB = settingView.findViewById(R.id.slope_setting_B);
        final TextView mAText = settingView.findViewById(R.id.slope_setting_A_text);
        final TextView mBText = settingView.findViewById(R.id.slope_setting_B_text);

        GridView vertexs = settingView.findViewById(R.id.slope_setting_vertexs);
        //init
        mName.setText(getSlopeModel().getMissionName());

        mDistance.setMax(MissionConstraintHelper.getMaxShotDistance());
        //mDistance.setMin(MissionConstraintHelper.getMinShotDistance());
        mDistance.setProgress((int) (getSlopeModel().getDistanceToPanel() + 0.5));
        mDistanceText.setText(String.valueOf((int) (getSlopeModel().getDistanceToPanel() + 0.5)) + " m");

        mAltitude.setMax((int) MissionConstraintHelper.getMaxAltitude());
        mAltitude.setProgress((int) (getSlopeModel().getAltitude() + 0.5));
        mAltitudeText.setText(String.valueOf((int) (getSlopeModel().getAltitude() + 0.5)) + " m");

        mSpeed.setMax((int) MissionConstraintHelper.getMaxSpeed());
        mSpeed.setProgress((int) (getSlopeModel().getSpeed()));
        mSpeedText.setText(String.valueOf((int) (getSlopeModel().getSpeed() + 0.5)) + " m/s");

        mOverate.setMax(MissionConstraintHelper.getMaxFlatlandOverRate());
        //mOverate.setMin(10);
        mOverate.setProgress(getSlopeModel().getOverlapRate());
        mOverateText.setText(String.valueOf(getSlopeModel().getOverlapRate()) + " %");

        mWidth.setMax(MissionConstraintHelper.getMaxFlatlandWidth());
        mWidth.setProgress((int) (getSlopeModel().getWidth() + 0.5));
        mWidthText.setText(String.valueOf((int) (getSlopeModel().getWidth() + 0.5)) + " m");

        mPitch.setMax(MissionConstraintHelper.getMaxPitch());
        mPitch.setProgress(getSlopeModel().getCameraAngle());
        mPitchText.setText(String.valueOf(getSlopeModel().getCameraAngle()));

        mHeading.setMax(360);
        mHeading.setProgress(getSlopeModel().getHeadingAngle() + 180);
        mHeadingText.setText(String.valueOf(getSlopeModel().getHeadingAngle()));

        mA.setMax((int) MissionConstraintHelper.getMaxAltitude());
        mB.setMax((int) MissionConstraintHelper.getMaxAltitude());
        if (getSlopeModel().getBaselineA() != null && getSlopeModel().getBaselineB() != null) {
            mA.setProgress((int) getSlopeModel().getBaselineA().altitude);
            mAText.setText(String.valueOf((int) getSlopeModel().getBaselineA().altitude) + " m");

            mB.setProgress((int) getSlopeModel().getBaselineB().altitude);
            mBText.setText(String.valueOf((int) getSlopeModel().getBaselineB().altitude) + " m");
        } else {
            mA.setProgress(0);
            mAText.setText(String.valueOf(0) + " m");

            mB.setProgress(0);
            mBText.setText(String.valueOf(0) + " m");
        }
        mDistance.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mDistanceText.setText(String.valueOf(i) + " m");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mSpeedText.setText(String.valueOf(i) + "m/s");
                // getSlopeModel().setSpeed(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mOverate.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mOverateText.setText(String.valueOf(i) + " %");
//                getSlopeModel().setOverlapRate(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mWidth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mWidthText.setText(String.valueOf(i) + " m");
                // getSlopeModel().setWidth(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mAltitude.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mAltitudeText.setText(String.valueOf(i) + " m");
                // getSlopeModel().setAltitude(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mPitch.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mPitchText.setText(String.valueOf(i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mA.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mAText.setText(String.valueOf(i) + " m");

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mBText.setText(String.valueOf(i) + " m");

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mHeading.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mHeadingText.setText(String.valueOf(i - 180));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        vertexs.setAdapter(new FlatlandSettingGridviewAdapter(this, getSlopeModel().getVertexs()));
        //dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(settingView);
        builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //change commit
                getSlopeModel().setAltitude(mAltitude.getProgress());
                getSlopeModel().setOverlapRate(mOverate.getProgress());
                getSlopeModel().setSpeed(mSpeed.getProgress());
                getSlopeModel().setWidth(mWidth.getProgress());
                getSlopeModel().setCameraAngle(mPitch.getProgress());
                getSlopeModel().setDistanceToPanel(mDistance.getProgress());
                int angle = mHeading.getProgress();
                if (angle >= 0 && angle <= 360) {
                    getSlopeModel().setHeadingAngle(angle - 180);
                } else {
                    getSlopeModel().setHeadingAngle(0); //使用默认值0，正北方向
                }
                //应该增加正确性判断
                if (getSlopeModel().getBaselineA() != null && getSlopeModel().getBaselineB() != null) {
                    getSlopeModel().getBaselineA().altitude = mA.getProgress();
                    getSlopeModel().getBaselineB().altitude = mB.getProgress();
                } else {
                    ToastHelper.getInstance().showShortToast("请进行斜面倾斜角的相关设置");
                }

                //change flag
                saveFlag = false;
            }
        });
        builder.setNeutralButton("取消修改", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        final AlertDialog dialog = builder.create();
        set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (status_of_slopemodel) {
                    case 1:
                        status_of_slopemodel = 2;
                        break;
                    case 2:
                        status_of_slopemodel = 1;
                        break;
                }
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    //multi point
    private void markWaypoint(LatLng latLng) {
        if (currentMarker != null) {
            currentMarker.hideInfoWindow();
        }
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        markerOptions.draggable(false);
        markerOptions.title("point");
        aMap.addMarker(markerOptions);
        Marker marker = aMap.addMarker(markerOptions);
        mMarkers.add(marker);
    }

    //flatland
    private void drawFlatlandBorder(LatLng latLng) {
        getFlatlandModel().addVertex(latLng);
        if (f_polyline != null) {
            f_polyline.remove();
        }
        if (f_polygon != null)
            f_polygon.remove();
        if (f_startPoint != null)
            f_startPoint.remove();
        PolylineOptions options = new PolylineOptions().addAll(getFlatlandModel().getVertexs());
        if (getFlatlandModel().getVertexs().size() > 0)
            options.add(getFlatlandModel().getVertexs().get(0));
        f_polyline = aMap.addPolyline(options);
        f_polygon = aMap.addPolygon(new PolygonOptions().addAll(getFlatlandModel().getVertexs())
                .fillColor(getResources().getColor(R.color.fillColor)));
        if (getFlatlandModel().getVertexs().size() == 1)
            drawStartPoint(getFlatlandModel().getVertexs().get(0));
        else {
            destroyStartPoint();
        }

    }

    private void drawStartPoint(LatLng latLng) {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        markerOptions.title("f_startPoint");
        aMap.addMarker(markerOptions);
        f_startPoint = aMap.addMarker(markerOptions);
    }

    private void destroyStartPoint() {
        if (f_startPoint != null)
            f_startPoint.destroy();
    }

    //slope model about
    private void drawSlopeBorder(LatLng latLng) {
        getSlopeModel().addVertex(latLng);
        if (s_polyline != null) {
            s_polyline.remove();
        }
        if (s_polygon != null)
            s_polygon.remove();
        if (s_startPoint != null)
            s_startPoint.remove();
        PolylineOptions options = new PolylineOptions().addAll(getSlopeModel().getVertexs());
        if (getSlopeModel().getVertexs().size() > 0)
            options.add(getSlopeModel().getVertexs().get(0));
        s_polyline = aMap.addPolyline(options);
        s_polygon = aMap.addPolygon(new PolygonOptions().addAll(getSlopeModel().getVertexs())
                .fillColor(getResources().getColor(R.color.fillColorSlope)));
        if (getSlopeModel().getVertexs().size() == 1)
            drawStartPointSlope(getSlopeModel().getVertexs().get(0));
        else {
            destroyStartPointSlope();
        }
    }

    private void drawStartPointSlope(LatLng latLng) {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        markerOptions.title("f_startPoint");
        aMap.addMarker(markerOptions);
        s_startPoint = aMap.addMarker(markerOptions);
    }

    private void destroyStartPointSlope() {
        if (s_startPoint != null)
            s_startPoint.destroy();
    }

    private void drawBaseline(Marker a, Marker b) {
//         if(s_base_A == null){
//             MarkerOptions markerOptions = new MarkerOptions();
//             markerOptions.position(latLng);
//             markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_baseline_a)));
//             markerOptions.draggable(true);
//             markerOptions.title("base");
//             aMap.addMarker(markerOptions);
//            s_base_A = aMap.addMarker(markerOptions);
//         }else if(s_base_B == null){
//             MarkerOptions markerOptions = new MarkerOptions();
//             markerOptions.position(latLng);
//             markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_baseline_b)));
//             markerOptions.draggable(true);
//             markerOptions.title("base");
//             aMap.addMarker(markerOptions);
//             s_base_B = aMap.addMarker(markerOptions);
//         }else {
//             s_baseline.remove();
//             s_baseline=aMap.addPolyline(
//                     new PolylineOptions().add(s_base_A.getPosition(),s_base_B.getPosition())
//                             .color(getResources().getColor(R.color.baseline)));
//         }

        //s_baseline.remove();
        s_baseline = aMap.addPolyline(
                new PolylineOptions().add(a.getPosition(), b.getPosition())
                        .color(getResources().getColor(R.color.baseline)));
        if (currentModel == null)
            return;
        if (currentModel.getModelType() != ModelType.Slope)
            return;
        if (getSlopeModel().getBaselineA() != null && getSlopeModel().getBaselineB() != null) {
            Waypoint baseA = new Waypoint(a.getPosition().latitude, a.getPosition().longitude, getSlopeModel().getBaselineA().altitude);
            Waypoint baseB = new Waypoint(b.getPosition().latitude, b.getPosition().longitude, getSlopeModel().getBaselineB().altitude);
            getSlopeModel().setBaselineA(baseA);
            getSlopeModel().setBaselineB(baseB);
        } else {
            Waypoint baseA = new Waypoint(a.getPosition().latitude, a.getPosition().longitude, 22);
            Waypoint baseB = new Waypoint(b.getPosition().latitude, b.getPosition().longitude, 22);
            getSlopeModel().setBaselineA(baseA);
            getSlopeModel().setBaselineB(baseB);
        }


    }

    private void drawBaselineMarker(LatLng latLng) {
        if (s_base_A == null) {
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_baseline_a)));
            markerOptions.draggable(true);
            markerOptions.title("baseA");
            aMap.addMarker(markerOptions);
            s_base_A = aMap.addMarker(markerOptions);
        } else if (s_base_B == null) {
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_baseline_b)));
            markerOptions.draggable(true);
            markerOptions.title("baseB");
            aMap.addMarker(markerOptions);
            s_base_B = aMap.addMarker(markerOptions);
        }
        if (s_base_A != null && s_base_B != null) {
            //s_baseline.remove();
            drawBaseline(s_base_A, s_base_B);
        }
    }

    //child model change about
    private void refreshView() {
        btn_childlist.setText(mission.getName());
        if (currentModel != null) {
            currentChildMissionName.setText(currentModel.getMissionName());
        }
        clearFlatlandDisplay();
        clearMultiPointDisplay();
        clearSlopeDisplay();
        if (currentModel != null) {
            switch (currentModel.getModelType()) {
                case Slope:
                    showSlopeView(getSlopeModel());
                    break;
                case Flatland:
                    showFlatlandView(getFlatlandModel());
                    break;
                case MultiPoints:
                    showMultiPointsView(getMultipointsModel());
                    break;
            }
        } else {
            cameraUpdate(locationLatlng, 18);
        }

    }

    private void clearMultiPointDisplay() {
        for (int i = 0; i < mMarkers.size(); i++) {
            if (!mMarkers.get(i).getTitle().equals("marker"))
                mMarkers.get(i).destroy();
        }
        mMarkers.clear();

    }

    private void clearFlatlandDisplay() {
        if (f_polygon != null)
            f_polygon.remove();
        if (f_polyline != null)
            f_polyline.remove();
        if (f_startPoint != null)
            f_startPoint.destroy();
    }

    private void clearSlopeDisplay() {
        if (s_polygon != null)
            s_polygon.remove();
        if (s_polyline != null)
            s_polyline.remove();
        if (s_startPoint != null)
            s_startPoint.destroy();
        if (s_base_A != null) {
            s_base_A.remove();
            s_base_A.destroy();
            s_base_A = null;
        }
        if (s_base_B != null) {
            s_base_B.remove();
            s_base_B.destroy();
            s_base_B = null;
        }
        if (s_baseline != null)
            s_baseline.remove();
    }

    private void showMultiPointsView(MultiPointsModel multiPointsModel) {
        for (int i = 0; i < multiPointsModel.getWaypointList().size(); i++) {
            MarkerOptions markerOptions = new MarkerOptions();
            LatLng ll = new LatLng(
                    multiPointsModel.getWaypointList().get(i).coordinate.getLatitude(),
                    multiPointsModel.getWaypointList().get(i).coordinate.getLongitude());
            markerOptions.position(ll);
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
            markerOptions.draggable(false);
            markerOptions.title("waypoint");
            aMap.addMarker(markerOptions);
            cameraUpdate(ll, aMap.getCameraPosition().zoom);
            Marker marker = aMap.addMarker(markerOptions);
            mMarkers.add(marker);
        }
    }

    private void showFlatlandView(FlatlandModel flatlandModel) {
        PolylineOptions options = new PolylineOptions().addAll(flatlandModel.getVertexs());
        if (flatlandModel.getVertexs().size() > 0)
            options.add(flatlandModel.getVertexs().get(0));
        f_polyline = aMap.addPolyline(options);
        f_polygon = aMap.addPolygon(new PolygonOptions().addAll(flatlandModel.getVertexs())
                .fillColor(getResources().getColor(R.color.fillColor)));
        if (flatlandModel.getVertexs().size() > 0) {
            cameraUpdate(flatlandModel.getVertexs().get(0), aMap.getCameraPosition().zoom);
        } else {
            cameraUpdate(locationLatlng, 18f);
        }

    }

    private void showSlopeView(SlopeModel slopeModel) {
        //参数调整
        status_of_slopemodel = 1;
        //show border
        PolylineOptions options = new PolylineOptions().addAll(slopeModel.getVertexs());
        if (slopeModel.getVertexs().size() > 0)
            options.add(slopeModel.getVertexs().get(0));
        s_polyline = aMap.addPolyline(options);
        s_polygon = aMap.addPolygon(new PolygonOptions().addAll(slopeModel.getVertexs())
                .fillColor(getResources().getColor(R.color.fillColor)));
        if (slopeModel.getVertexs().size() > 0) {
            cameraUpdate(slopeModel.getVertexs().get(0), aMap.getCameraPosition().zoom);
        } else {
            cameraUpdate(locationLatlng, 18f);
        }
        //show baseline
        if (slopeModel.getBaselineA() != null && slopeModel.getBaselineB() != null) {
            drawBaselineMarker(new LatLng(slopeModel.getBaselineA().coordinate.getLatitude(),
                    slopeModel.getBaselineA().coordinate.getLongitude()));
            drawBaselineMarker(new LatLng(slopeModel.getBaselineB().coordinate.getLatitude(),
                    slopeModel.getBaselineB().coordinate.getLongitude()));
        }
    }

    //general tools
    private MultiPointsModel getMultipointsModel() {
        return (MultiPointsModel) currentModel;
    }

    private FlatlandModel getFlatlandModel() {
        return (FlatlandModel) currentModel;
    }

    private SlopeModel getSlopeModel() {
        return (SlopeModel) currentModel;
    }

    private void cameraUpdate(LatLng pos, float zoomLevel) {
        CameraUpdate cameraupdate = CameraUpdateFactory.newLatLngZoom(pos, zoomLevel);
        aMap.moveCamera(cameraupdate);
    }

    /**
     * save mission to database and files
     */
    private void saveMission() {
        //update database
        int id = mission.getId();
        Date date = new Date();
        PatrolMission temp = DataSupport.find(PatrolMission.class, id);
        if (temp != null) {
            temp.setLastModifiedTime(date);
            if (modelList != null) {
                temp.setChildNums(modelList.size());
            }
            temp.save();
        } else {
            mission.setLastModifiedTime(date);
            if (modelList != null) {
                mission.setChildNums(modelList.size());
            }
            mission.save();
        }
        //update file
        if (!MissionHelper.saveMissionToFile(mission, modelList)) {
            setResultToToast("save failed");
        } else {
            saveFlag = true;
            setResultToToast("success");
        }
    }

    private void setResultToToast(final String msg) {
        this.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        View view;
        ImageView missionImg;
        TextView nameText;
        TextView typeText;

        public ViewHolder(View view) {
            super(view);
            this.view = view;
            missionImg = view.findViewById(R.id.main_list_mission_image);
            nameText = view.findViewById(R.id.main_list_mission_name);
            typeText = view.findViewById(R.id.main_list_mission_type);
        }
    }

    private class ChildMissionListAdapter extends RecyclerView.Adapter<ViewHolder> {
        private List<BaseModel> modelList;
        private int rowIndex = 0;
        public ChildMissionListAdapter(List<BaseModel> list) {

            this.modelList = list;
        }

        public void setModelList(List<BaseModel> list) {
            this.modelList = list;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.mission_main_list_item, parent, false);
            final ViewHolder holder = new ViewHolder(view);

            return holder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {
            final BaseModel model = modelList.get(position);
            final ModelType type = model.getModelType();
            holder.nameText.setText(model.getMissionName());
            holder.typeText.setText(type.toString());
            switch (type) {
                case Slope:
                    Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.slope);
                    holder.missionImg.setImageBitmap(bitmap);
                    break;
                case Flatland:
                    bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.flatland);
                    holder.missionImg.setImageBitmap(bitmap);
                    break;
                case MultiPoints:
                    bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.multipoint);
                    holder.missionImg.setImageBitmap(bitmap);
                    break;
                default:
                    break;
            }
            holder.view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //remind to finish
                    currentModel = modelList.get(position);
                    currentChildMissionName.setText(currentModel.getMissionName());
                    rowIndex = position;
                    notifyDataSetChanged();
                    refreshView();
                }
            });
            holder.view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    new android.app.AlertDialog.Builder(getContext())
                            .setTitle("提醒")
                            .setMessage("确认删除子任务:" + model.getMissionName())
                            .setPositiveButton("是", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    deletechildMission(position);
                                }
                            })
                            .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                }
                            }).create().show();
                    return false;
                }
            });
            if (rowIndex == position) {
                holder.nameText.setTextColor(getResources().getColor(R.color.red));
                holder.typeText.setTextColor(getResources().getColor(R.color.red));
            } else {
                holder.nameText.setTextColor(getResources().getColor(R.color.black_half));
                holder.typeText.setTextColor(getResources().getColor(R.color.black_half));
            }
        }

        private void deletechildMission(int position) {
            modelList.remove(position);
            saveFlag = false;
            notifyDataSetChanged();
            if (!modelList.isEmpty()) {
                currentModel = modelList.get(0);
                currentChildMissionName.setText(currentModel.getMissionName());
            } else {
                currentModel = null;
                currentChildMissionName.setText("null");
            }
        }

        @Override
        public int getItemCount() {
            return modelList.size();
        }
    }

    private class MyInfoWindowAdapter implements AMap.InfoWindowAdapter {
        View infoWindow = null;
        TextView tv_lat;
        TextView tv_lng;
        TextView tv_altitude;
        SeekBar sb_altitude;
        Waypoint waypoint;
        Marker mMarker;
        Button btn_deleteWaypoint;

        @Override
        public View getInfoContents(Marker marker) {
            return null;
        }

        public void getWaypoint(Marker marker) {
            mMarker = marker;
            LatLng latLng = marker.getPosition();
            waypoint = getMultipointsModel().getWaypoint(latLng);
        }

        @NonNull
        public View initView(final LatLng latLng) {
            View view = LayoutInflater.from(ContextHelper.getApplicationContext()).inflate(R.layout.waypoint_infowindow, null);
            tv_altitude = view.findViewById(R.id.info_altitude_text);
            sb_altitude = view.findViewById(R.id.info_altitude);
            tv_lat = view.findViewById(R.id.waypoint_lat);
            tv_lng = view.findViewById(R.id.waypoint_lng);
            btn_deleteWaypoint = view.findViewById(R.id.deleteWaypoint);
            //sb init
            sb_altitude.setMax((int) (MissionConstraintHelper.getMaxAltitude() + 0.5));
            sb_altitude.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    getMultipointsModel().updatePoint(latLng, i);
                    tv_altitude.setText(String.valueOf(i) + " m");
                    saveFlag = false;
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
            btn_deleteWaypoint.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    deleteCurrentPoint();
                }
            });
            //init data
            if (waypoint != null) {
                sb_altitude.setProgress((int) (waypoint.altitude + 0.5));
                tv_altitude.setText(String.valueOf(sb_altitude.getProgress()) + " m");
                //tv_index.setText("编号："+String.valueOf());
                tv_lat.setText("纬度:" + String.valueOf(waypoint.coordinate.getLatitude()));
                tv_lng.setText("经度：" + String.valueOf(waypoint.coordinate.getLongitude()));
            }
            return view;
        }


        private void deleteCurrentPoint() {
            getMultipointsModel().removeWaypoint(mMarker.getPosition());
            mMarkers.remove(mMarker);
            mMarker.hideInfoWindow();
            mMarker.destroy();
            saveFlag = false;
        }

        @Override
        public View getInfoWindow(Marker marker) {
            getWaypoint(marker);
            infoWindow = initView(marker.getPosition());
            return infoWindow;
        }
    }
}
