package com.hitices.autopatrol.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdate;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.UiSettings;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;
import com.hitices.autopatrol.R;
import com.hitices.autopatrol.helper.GoogleMapHelper;
import com.hitices.autopatrol.helper.ImageInfoReadHelper;
import com.hitices.autopatrol.helper.ToastHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MissionReportActivity extends AppCompatActivity {
    AMap.OnMapClickListener onMapClickListener = new AMap.OnMapClickListener() {
        @Override
        public void onMapClick(LatLng latLng) {

        }
    };
    private TextView name, type, photo_nums;
    private AMap aMap;
    private MapView mapView;
    private List<String> imageUrls = new ArrayList<>();
    AMap.OnMarkerClickListener markerClickListener = new AMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {
            int i = Integer.parseInt(marker.getTitle());
            displayImage(imageUrls.get(i));
            return true;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mission_report);
        initUI();
        initMapView(savedInstanceState);
        //markImages();
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

    private void initUI() {
//        name = findViewById(R.id.report_mission_name);
//        type = findViewById(R.id.report_mission_type);
//        photo_nums = findViewById(R.id.report_mission_photo_nums);

    }

    private void initMapView(Bundle savedInstanceState) {
        mapView = findViewById(R.id.mission_report_map);
        mapView.onCreate(savedInstanceState);
        if (aMap == null) {
            aMap = mapView.getMap();
        }
        GoogleMapHelper.useGoogleMapSatelliteData(aMap);
        aMap.setOnMapClickListener(onMapClickListener);
        aMap.setOnMarkerClickListener(markerClickListener);
        UiSettings settings = aMap.getUiSettings();
        settings.setZoomControlsEnabled(false);
        settings.setScaleControlsEnabled(true);
    }

    private void markImages() {
        //清除当前屏幕所有marker
        aMap.clear();
        imageUrls.clear();
        //test
        getUris();
        for (int i = 0; i < imageUrls.size(); i++) {
            ImageInfoReadHelper read = new ImageInfoReadHelper(imageUrls.get(i));
            LatLng location = read.getLatlng();
            aMap.addMarker(getMarkerOptions(location, i, 2));
            CameraUpdate cameraupdate = CameraUpdateFactory.newLatLngZoom(location, 18f);
            aMap.moveCamera(cameraupdate);
        }

    }

    private MarkerOptions getMarkerOptions(LatLng location, int index, int type) {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(location);
        switch (type) {
            case 1:
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                break;
            case 2:
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                break;
            default:
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
                break;
        }
        markerOptions.title(String.valueOf(index));
        return markerOptions;
    }

    private void displayImage(final String path) {
        Intent intent = new Intent(getApplicationContext(), ShowFullImageActivity.class);
        intent.putExtra("url", "file://" + path);
        intent.putExtra("type", 0); //image
        startActivity(intent);
    }

    private void getUris() {
        imageUrls = new ArrayList<>();
        //获得intent传递的path信息
        String path = getIntent().getStringExtra("path");
        if (path.isEmpty()) {
            ToastHelper.getInstance().showLongToast("获得照片存储路径失败");
        } else {
            File f = new File(path);
            if (!f.exists()) {//判断路径是否存在
                if (!f.mkdirs()) {
                    return;
                }
            }
            File[] files = f.listFiles();
            if (files == null) {//判断权限
                return;
            }
            for (File _file : files) {//遍历目录
                if (_file.isFile()) {
                    if (_file.getName().endsWith("jpg") || _file.getName().endsWith("png") || _file.getName().endsWith("JPG")) {
                        imageUrls.add(_file.getAbsolutePath());//获取文件路径
                    }
                }
            }
        }
    }
}
