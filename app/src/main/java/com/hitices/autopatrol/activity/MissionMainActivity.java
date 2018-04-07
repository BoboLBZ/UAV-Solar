package com.hitices.autopatrol.activity;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.ContactsContract;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.UiSettings;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.hitices.autopatrol.R;
import com.hitices.autopatrol.entity.dataSupport.PatrolMission;
import com.hitices.autopatrol.entity.missions.BaseModel;
import com.hitices.autopatrol.entity.missions.ModelType;
import com.hitices.autopatrol.helper.GoogleMapHelper;
import com.hitices.autopatrol.helper.ToastHelper;

import org.litepal.crud.DataSupport;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class MissionMainActivity extends AppCompatActivity implements View.OnClickListener {
    //listener
    AMap.OnMapClickListener onMapClickListener = new AMap.OnMapClickListener() {
        @Override
        public void onMapClick(LatLng latLng) {
//            switch (currentMission.missionType) {
//                case WaypointMission:
//                    markWaypoint(latLng);
//                    setWaypointList(latLng);
//                    break;
//                case PolygonMission:
//                    if (creatable) {
//                        drawPolygon(latLng);
//                    } else {
//                        setResultToToast("请选择修改任务");
//                    }
//                    break;
//            }
        }
    };
    AMap.OnMarkerClickListener markerClickListener = new AMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {
//            if (currentMarker == null)
//                currentMarker = marker;
//            if (currentMarker.equals(marker)) {
//                if (!flag_isShow) {
//                    marker.showInfoWindow();
//                    flag_isShow = true;
//                } else {
//                    marker.hideInfoWindow();
//                    flag_isShow = false;
//                }
//            } else {
//                currentMarker.hideInfoWindow();
//                marker.showInfoWindow();
//                flag_isShow = true;
//            }
//            currentMarker = marker;
            return true;
        }
    };
    AMap.OnMarkerDragListener markerDragListener = new AMap.OnMarkerDragListener() {
        LatLng tempLatlng;

        @Override
        public void onMarkerDragStart(Marker arg0) {
            tempLatlng = arg0.getPosition();
        }

        @Override
        public void onMarkerDragEnd(Marker arg0) {
//            getCurrentWaypointsMission().getWaypointList().remove(new Waypoint(tempLatlng.latitude, tempLatlng.longitude, getCurrentWaypointsMission().getAltitude()));
//            getCurrentWaypointsMission().addWaypointList(arg0.getPosition());
        }

        @Override
        public void onMarkerDrag(Marker arg0) {
        }
    };
    //mission about
    private PatrolMission mission;
    private List<BaseModel> modelList;
    private BaseModel currentModel;
    //ui
    private AMap aMap;
    private MapView mapView;
    private Button btn_childlist;
    private FloatingActionsMenu menu;
    private FloatingActionButton btn_add, btn_save;
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
//
                    locationLatlng = new LatLng(amapLocation.getLatitude(), amapLocation.getLongitude());
//                    LatLng harbin = new LatLng(126.640692,45.748065);
                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(locationLatlng);
                    markerOptions.title("marker");
                    markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_location_marker)));
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mission_main);
        initMission();
        initMapView(savedInstanceState);
        initUI();
        refreshUI();
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
            saveMission();
            finish();
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
        }
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
//        aMap.setOnInfoWindowClickListener(onInfoWindowClickListener);
//        MissionFragment.myInfoWindowAdapter myInfoWindowAdapter = new MissionFragment.myInfoWindowAdapter();
//        aMap.setInfoWindowAdapter(myInfoWindowAdapter);
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
        menu = findViewById(R.id.menu_mission_main);
        recyclerView = findViewById(R.id.view_child_mission_main);
        currentChildMissionName = findViewById(R.id.tv_child_name_mission_main);

        LinearLayoutManager manager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(manager);
        ChildMissionListAdapter adapter = new ChildMissionListAdapter(modelList);
        recyclerView.setAdapter(adapter);


        child_setting.setOnClickListener(this);
        btn_childlist.setOnClickListener(this);
        btn_save.setOnClickListener(this);
        btn_add.setOnClickListener(this);
    }

    private void refreshUI() {
        btn_childlist.setText(mission.getName());
        if (currentModel != null) {
            currentChildMissionName.setText(currentModel.getMissionName());
        }
    }

    private void initMission() {
        modelList = new ArrayList<>();
        currentModel = null;
        String type = getIntent().getStringExtra("TYPE");
        if (type.equals("modify")) {
            String Path = getIntent().getStringExtra("PATH");
            int id = getIntent().getIntExtra("ID", -1);
            //read mission
            mission = DataSupport.find(PatrolMission.class, id);
            if (mission == null) {
                ToastHelper.showShortToast("can not open current mission");
                finish();
            }
        } else {
            mission = new PatrolMission();
            showCreateDialog();
        }
    }

    private void changeMissionListStatus() {
        int i = recyclerView.getVisibility();
        switch (i) {
            case View.VISIBLE:
                recyclerView.setVisibility(View.INVISIBLE);
                break;
            case View.INVISIBLE:
                recyclerView.setVisibility(View.VISIBLE);
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
                            refreshUI();
                            alertDialog.dismiss();

                        } else {
                            ToastHelper.showShortToast("任务名不能为空");
                        }
                    }
                });

        alertDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
    }

    private void addChildMissionDialog() {

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
            temp.save();
        } else {
            mission.setLastModifiedTime(date);
            mission.save();
        }
        setResultToToast("save mission");
        //update file

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

        public ChildMissionListAdapter(List<BaseModel> list) {

            this.modelList = list;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.mission_main_list_item, parent, false);
            final ViewHolder holder = new ViewHolder(view);
            holder.view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //remind to finish

                    int positon = holder.getAdapterPosition();
                    currentModel = modelList.get(positon);
                    currentChildMissionName.setText(currentModel.getMissionName());
                }
            });
            return holder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            BaseModel model = modelList.get(position);
            ModelType type = model.getModelType();
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

        }

        @Override
        public int getItemCount() {
            return modelList.size();
        }
    }

}
