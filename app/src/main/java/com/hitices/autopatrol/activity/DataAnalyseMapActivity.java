package com.hitices.autopatrol.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

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
import com.hitices.autopatrol.entity.dataSupport.FlightRecord;
import com.hitices.autopatrol.helper.GoogleMapHelper;
import com.hitices.autopatrol.helper.RecordImageHelper;
import com.hitices.autopatrol.helper.ToastHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DataAnalyseMapActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = DataAnalyseMapActivity.class.getName();

    private static final int NORMAL_MARKER_TYPE = 0;
    private static final int ERROR_MARKER_TYPE = 1;

    private View visibleFrame, infraredFrame, showBtnsLayout;
    private Button showVisibleBtn, showInfraredBtn;
    private Button genReportBtn, runAnalysisBtn;
    private MapView visibleMapView, infraredMapView;
    private AMap visibleAMap, infraredAMap;

    private List<String> visibleImageUrls = new ArrayList<>();
    private List<String> infraredImageUrls = new ArrayList<>();
    private List<MarkerOptions> visibleMarkerOptions = new ArrayList<>();
    private List<MarkerOptions> infraredMarkerOptions = new ArrayList<>();

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
        if (null != visibleMapView) {
            visibleMapView.onResume();
        }
        if (null != infraredMapView) {
            infraredMapView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (null != visibleMapView) {
            visibleMapView.onPause();
        }
        if (null != infraredMapView) {
            infraredMapView.onPause();
        }
    }

    @Override
    public void onDestroy() {
        if (null != visibleMapView) {
            visibleMapView.onDestroy();
        }
        if (null != infraredMapView) {
            infraredMapView.onDestroy();
        }
        super.onDestroy();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_show_visible:
                // 切换到可见光图像
                visibleFrame.setVisibility(View.VISIBLE);
                infraredFrame.setVisibility(View.GONE);
                break;
            case R.id.btn_show_infrared:
                // 切换到红外光图像
                visibleFrame.setVisibility(View.GONE);
                infraredFrame.setVisibility(View.VISIBLE);
                break;
            case R.id.btn_gen_report:
                // 生成报告
                ToastHelper.getInstance().showShortToast("生成报告");
                break;
            case R.id.btn_run_analysis:
                // 运行分析
                ToastHelper.getInstance().showShortToast("运行分析");
                break;
        }
    }

    private void initUI(Bundle savedInstanceState, FlightRecord record) {

        // 初始化界面其他部分
        showBtnsLayout = findViewById(R.id.ll_show_btns);
        showBtnsLayout.bringToFront();
        showVisibleBtn = findViewById(R.id.btn_show_visible);
        showInfraredBtn = findViewById(R.id.btn_show_infrared);
        showVisibleBtn.setOnClickListener(this);
        showInfraredBtn.setOnClickListener(this);
        genReportBtn = findViewById(R.id.btn_gen_report);
        runAnalysisBtn = findViewById(R.id.btn_run_analysis);
        genReportBtn.setOnClickListener(this);
        runAnalysisBtn.setOnClickListener(this);

        // 初始化地图部分
        visibleFrame = findViewById(R.id.fl_record_visible_map);
        infraredFrame = findViewById(R.id.fl_record_infrared_map);

        // 默认可见光、红外光都有
        if (record.isHasVisible() && record.isHasInfrared()) {
            visibleFrame.setVisibility(View.VISIBLE);
            infraredFrame.setVisibility(View.GONE);

            visibleMapView = findViewById(R.id.mv_record_visible);
            infraredMapView = findViewById(R.id.mv_record_infrared);
            visibleMapView.onCreate(savedInstanceState);
            infraredMapView.onCreate(savedInstanceState);
            if (visibleAMap == null) {
                visibleAMap = visibleMapView.getMap();
            }
            if (infraredAMap == null) {
                infraredAMap = infraredMapView.getMap();
            }
            visibleAMap.setOnMarkerClickListener(visibleMarkerClickListener);
            GoogleMapHelper.useGoogleMapSatelliteData(visibleAMap);
            infraredAMap.setOnMarkerClickListener(infraredMarkerClickListener);
            GoogleMapHelper.useGoogleMapSatelliteData(infraredAMap);

            UiSettings settings1 = visibleAMap.getUiSettings();
            settings1.setZoomControlsEnabled(false);
            settings1.setScaleControlsEnabled(true);
            UiSettings settings2 = infraredAMap.getUiSettings();
            settings2.setZoomControlsEnabled(false);
            settings2.setScaleControlsEnabled(true);

            // 标记照片位置点
            markVisibleImageLocation(record);
            markInfraredImageLocation(record);
        }

        // 没有可见光图集，有红外
        if (!record.isHasVisible()) {
            // 去掉上方的选择区域
            showBtnsLayout.setVisibility(View.GONE);

            visibleFrame.setVisibility(View.GONE);
            infraredFrame.setVisibility(View.VISIBLE);

            infraredMapView = findViewById(R.id.mv_record_infrared);
            infraredMapView.onCreate(savedInstanceState);
            if (infraredAMap == null) {
                infraredAMap = infraredMapView.getMap();
            }
            infraredAMap.setOnMarkerClickListener(infraredMarkerClickListener);
            GoogleMapHelper.useGoogleMapSatelliteData(infraredAMap);

            UiSettings settings = infraredAMap.getUiSettings();
            settings.setZoomControlsEnabled(false);
            settings.setScaleControlsEnabled(true);

            // 标记照片位置点
            markInfraredImageLocation(record);
        }

        // 没有红外光，有可见光
        if (!record.isHasInfrared()) {
            // 去掉上方的选择区域
            showBtnsLayout.setVisibility(View.GONE);

            visibleFrame.setVisibility(View.VISIBLE);
            infraredFrame.setVisibility(View.GONE);

            visibleMapView = findViewById(R.id.mv_record_visible);
            visibleMapView.onCreate(savedInstanceState);
            if (visibleAMap == null) {
                visibleAMap = visibleMapView.getMap();
            }
            visibleAMap.setOnMarkerClickListener(visibleMarkerClickListener);
            GoogleMapHelper.useGoogleMapSatelliteData(visibleAMap);

            UiSettings settings = visibleAMap.getUiSettings();
            settings.setZoomControlsEnabled(false);
            settings.setScaleControlsEnabled(true);

            // 标记照片位置点
            markVisibleImageLocation(record);
        }

        // 两个都没有，应该不可能：
        if (!record.isHasInfrared() && record.isHasVisible()) {

        }
    }

    /**
     * 在地图上标记可见光图像
     *
     * @param record 飞行记录
     */
    private void markVisibleImageLocation(FlightRecord record) {
        // 获取图片路径
        visibleImageUrls.clear();
        visibleImageUrls = getImageUrls(record, true);

        if (visibleImageUrls.size() == 0) {
            // 读取照片异常
        }

        for (int i = 0; i < visibleImageUrls.size(); i++) {
            LatLng location = RecordImageHelper.getImageLatlng(visibleImageUrls.get(i));
            location = GoogleMapHelper.WGS84ConvertToAmap(location);
            MarkerOptions newMarker = genNormalMarker(location, i);
            visibleAMap.addMarker(newMarker);
            visibleMarkerOptions.add(newMarker);

            // 调整地图视角
            CameraUpdate cameraupdate = CameraUpdateFactory.newLatLngZoom(location, 18f);
            visibleAMap.moveCamera(cameraupdate);
        }
    }

    /**
     * 在地图上标记红外光图像
     *
     * @param record 飞行记录
     */
    private void markInfraredImageLocation(FlightRecord record) {
        // 获取图片路径
        infraredImageUrls.clear();
        infraredImageUrls = getImageUrls(record, true);

        if (infraredImageUrls.size() == 0) {
            // 读取照片异常
        }

        for (int i = 0; i < infraredImageUrls.size(); i++) {
            LatLng location = RecordImageHelper.getImageLatlng(infraredImageUrls.get(i));
            location = GoogleMapHelper.WGS84ConvertToAmap(location);
            MarkerOptions newMarker = genNormalMarker(location, i);
            infraredAMap.addMarker(newMarker);
            infraredMarkerOptions.add(newMarker);

            // 调整地图视角
            CameraUpdate cameraupdate = CameraUpdateFactory.newLatLngZoom(location, 18f);
            infraredAMap.moveCamera(cameraupdate);
        }
    }

    /**
     * 获得图片地址列表
     *
     * @param record
     * @param isVisible
     * @return
     */
    private List<String> getImageUrls(FlightRecord record, boolean isVisible) {
        List<String> urls = new ArrayList<>();

        File imagePath;
        if (isVisible) {
            imagePath = RecordImageHelper.getRecordTestImagePath();
            imagePath = RecordImageHelper.getRecordVisibleImagePath(record);
        } else {
//            imagePath = RecordImageHelper.getRecordTestImagePath();
            imagePath = RecordImageHelper.getRecordVisibleImagePath(record);
        }

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

    /**
     * 点击标记点后展示图片
     *
     * @param index
     * @param isVisible
     */
    private void showMakerImage(int index, boolean isVisible) {
        ToastHelper.getInstance().showShortToast("image_index: " + index);
        File image;
        if (isVisible) {
            image = new File(visibleImageUrls.get(index));
        } else {
            image = new File(infraredImageUrls.get(index));
        }
    }

    AMap.OnMarkerClickListener visibleMarkerClickListener = new AMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {
            int index = Integer.parseInt(marker.getTitle());
            showMakerImage(index, true);
            return true;
        }
    };

    AMap.OnMarkerClickListener infraredMarkerClickListener = new AMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {
            int index = Integer.parseInt(marker.getTitle());
            showMakerImage(index, false);
            return true;
        }
    };

    /**
     * 生成一个默认的正常图片的位置标记
     *
     * @param location
     * @param index
     * @return
     */
    private MarkerOptions genNormalMarker(LatLng location, int index) {
        MarkerOptions markerOptions = new MarkerOptions();

        markerOptions.title(String.valueOf(index));
        markerOptions.position(location);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));

        return markerOptions;
    }
}
