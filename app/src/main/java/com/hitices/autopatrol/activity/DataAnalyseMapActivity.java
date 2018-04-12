package com.hitices.autopatrol.activity;

import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdate;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.UiSettings;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.CircleOptions;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;
import com.amap.api.maps2d.model.Tile;
import com.amap.api.maps2d.model.TileOverlayOptions;
import com.amap.api.maps2d.model.TileProvider;
import com.amap.api.maps2d.model.UrlTileProvider;
import com.hitices.autopatrol.R;
import com.hitices.autopatrol.entity.dataSupport.FlightRecord;
import com.hitices.autopatrol.helper.GoogleMapHelper;
import com.hitices.autopatrol.helper.RecordImageHelper;
import com.hitices.autopatrol.helper.ToastHelper;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class DataAnalyseMapActivity extends AppCompatActivity {

    private static final String TAG = DataAnalyseMapActivity.class.getName();

    private static final int NORMAL_MARKER_TYPE = 0;
    private static final int ERROR_MARKER_TYPE = 1;

    private AMap aMap;
    private MapView mapView;

    private List<String> imageUrls = new ArrayList<>();
    private List<MarkerOptions> markerOptions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_analyse_map);

        Intent intent = getIntent();
        FlightRecord flightRecord = (FlightRecord) intent.getSerializableExtra(getResources().getString(R.string.selected_flight_record));
        initUI(savedInstanceState, flightRecord);
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

    private void initUI(Bundle savedInstanceState, FlightRecord record) {

        // 初始化地图
        initMapView(savedInstanceState);
        // 标记照片位置点
        markImageLocation(record);
    }

    private void initMapView(Bundle savedInstanceState) {
        mapView = findViewById(R.id.mv_flight_record);
        mapView.onCreate(savedInstanceState);
        if (aMap == null) {
            aMap = mapView.getMap();
        }
        aMap.setOnMarkerClickListener(markerClickListener);
        GoogleMapHelper.useGoogleMapSatelliteData(aMap);

        UiSettings settings = aMap.getUiSettings();
        settings.setZoomControlsEnabled(false);
        settings.setScaleControlsEnabled(true);
    }

    private void markImageLocation(FlightRecord record) {
        // 获取图片路径
        imageUrls.clear();
        imageUrls = getImageUrls(record);

        if (imageUrls.size() == 0) {
            // 读取照片异常
        }

        for (int i = 0; i < imageUrls.size(); i++) {
            LatLng location = RecordImageHelper.getImageLatlng(imageUrls.get(i));
            location = GoogleMapHelper.WGS84ConvertToAmap(location);
            MarkerOptions newMarker = genNormalMarker(location, i);
            aMap.addMarker(newMarker);
            markerOptions.add(newMarker);

            CameraUpdate cameraupdate = CameraUpdateFactory.newLatLngZoom(location, 18f);
            aMap.moveCamera(cameraupdate);
        }
    }

    private List<String> getImageUrls(FlightRecord record) {
        List<String> urls = new ArrayList<>();

//        File imagePath = RecordImageHelper.getRecordVisibleImagePath(record);
        File imagePath = RecordImageHelper.getRecordTestImagePath();

        if (!imagePath.exists()) {
            ToastHelper.getInstance().showLongToast("任务照片不存在");
        } else {
            File[] images = imagePath.listFiles();
            if (images == null) {//判断权限
                return urls;
            }
            for (File image : images) {//遍历目录
                if (image.isFile()) {
                    if (image.getName().endsWith("jpg") || image.getName().endsWith("JPG")
                            || image.getName().endsWith("jpeg") || image.getName().endsWith("JPEG")
                            || image.getName().endsWith("png") || image.getName().endsWith("PNG")) {
                        urls.add(image.getAbsolutePath());//获取文件路径
                    }
                }
            }
        }

        return urls;
    }

    private void showMakerImage(int index) {
        File image = new File(imageUrls.get(index));
        ToastHelper.getInstance().showShortToast("image_index: " + index);
    }

    AMap.OnMarkerClickListener markerClickListener = new AMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {
            int index = Integer.parseInt(marker.getTitle());
            showMakerImage(index);
            return true;
        }
    };

    private MarkerOptions genNormalMarker(LatLng location, int index) {
        MarkerOptions markerOptions = new MarkerOptions();

        markerOptions.title(String.valueOf(index));
        markerOptions.position(location);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));

        return markerOptions;
    }
}
