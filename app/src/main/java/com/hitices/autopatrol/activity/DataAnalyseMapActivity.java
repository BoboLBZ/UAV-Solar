package com.hitices.autopatrol.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
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
import com.hitices.autopatrol.entity.imageData.AnalyzedImageBean;
import com.hitices.autopatrol.entity.imageData.MyRecognition;
import com.hitices.autopatrol.helper.ContextHelper;
import com.hitices.autopatrol.helper.GoogleMapHelper;
import com.hitices.autopatrol.helper.RecordImageHelper;
import com.hitices.autopatrol.helper.ToastHelper;
import com.hitices.autopatrol.tfObjectDetection.Classifier;
import com.hitices.autopatrol.tfObjectDetection.ImageUtils;
import com.hitices.autopatrol.tfObjectDetection.TensorFlowObjectDetectionAPIModel;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class DataAnalyseMapActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = DataAnalyseMapActivity.class.getName();

    private static final int TFOD_SUCCESS = 0;
    private static final int TFOD_COMPLETED = 1;
    private static final int TFOD_UPDATE_MARKER = 2;

    private static final String VISIBLE_IMAGES = "VISIBLE_IMAGES";
    private static final String INFRARED_IMAGES = "INFRARED_IMAGES";

    private static final int BOTH_IMAGES = 0;
    private static final int ONLY_VISIBLE = 1;
    private static final int ONLY_INFRARED = 2;
    private static final int NO_IMAGES = 3;

    private View visibleFrame, infraredFrame, showBtnsLayout;
    private Button showVisibleBtn, showInfraredBtn;
    private Button genReportBtn, runAnalysisBtn;
    private MapView visibleMapView, infraredMapView;
    private AMap visibleAMap, infraredAMap;

    private boolean needInitComplexThing;

    private List<AnalyzedImageBean> visibleAnalyzedImageBeans = new ArrayList<>();
    private List<AnalyzedImageBean> infraredAnalyzedImageBeans = new ArrayList<>();

    private String imageNow = VISIBLE_IMAGES;
    private FlightRecord mFlightRecord;
    private int imagesType = BOTH_IMAGES;

    private ProgressDialog analysisProgressDialog;

    // object_detection

    // 模型及Label位置
    private static final String SSD_MOBILENET1_MODEL_FILE = "file:///android_asset/ssd_mobilenet1.pb";
    private static final String FASTERRCNN_RESNET101_MODEL_FILE = "file:///android_asset/fasterrcnn_resnet101.pb";
    private static final String TF_OD_API_MODEL_FILE = SSD_MOBILENET1_MODEL_FILE;
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/solar_labels_list.txt";
    // API输入图片的尺寸（要压缩处理）
    private static final int TF_OD_API_INPUT_SIZE = 450;
    // Minimum detection confidence to track a detection.
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.6f;

    private static final boolean SAVE_PREVIEW_BITMAP = false;

    // API主体
    private Classifier detector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_analyse_map);

        Intent intent = getIntent();
        mFlightRecord = (FlightRecord) intent.getSerializableExtra(getResources().getString(R.string.selected_flight_record));

        analyseRecordData();

        initUI(this, savedInstanceState);

        needInitComplexThing = true;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (needInitComplexThing) {
            markImageOnMap();
            initTFObjectDetection();
            needInitComplexThing = false;
        }

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
                imageNow = VISIBLE_IMAGES;
                visibleFrame.setVisibility(View.VISIBLE);
                infraredFrame.setVisibility(View.GONE);
                break;
            case R.id.btn_show_infrared:
                // 切换到红外光图像
                imageNow = INFRARED_IMAGES;
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
                if (imageNow.equals(VISIBLE_IMAGES)) {
                    runVisibleDataAnalyse();
                } else if (imageNow.equals(INFRARED_IMAGES)) {
                    runInfraredDataAnalyse();
                }
                break;
        }
    }

    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case TFOD_SUCCESS:
                    analysisProgressDialog.setProgress(msg.arg1);
                    break;
                case TFOD_UPDATE_MARKER:
                    AnalyzedImageBean image = (AnalyzedImageBean) msg.obj;
                    LatLng location = image.getMapMarker().getPosition();
                    image.getMapMarker().remove();
                    MarkerOptions markerOptions = genWarnMarkerOptions(visibleAnalyzedImageBeans.indexOf(image),
                            location);
                    Marker marker = visibleAMap.addMarker(markerOptions);
                    image.setMapMarker(marker);
                    break;
                case TFOD_COMPLETED:
                    hideAnalysisProgressDialog();
                    break;
            }
        }
    };

    private void initUI(Context context, Bundle savedInstanceState) {

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

        initDialog(context);
        initMapView(savedInstanceState);
    }

    private void initDialog(Context context) {

        //Init runAnalyse Dialog
        analysisProgressDialog = new ProgressDialog(context);
        analysisProgressDialog.setTitle(ContextHelper.getApplicationContext()
                .getResources().getString(R.string.run_analysis_title));
        analysisProgressDialog.setIcon(android.R.drawable.ic_dialog_info);
        analysisProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        analysisProgressDialog.setCanceledOnTouchOutside(false);
        analysisProgressDialog.setCancelable(true);
        analysisProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                // 中止分析
            }
        });
    }

    private void analyseRecordData() {
        if (mFlightRecord.isHasVisible() & mFlightRecord.isHasInfrared()) {
            imagesType = BOTH_IMAGES;
        } else if (mFlightRecord.isHasVisible()) {
            imagesType = ONLY_VISIBLE;
        } else if (mFlightRecord.isHasInfrared()) {
            imagesType = ONLY_INFRARED;
        } else {
            imagesType = NO_IMAGES;
        }
        initImages();
    }

    private void initImages() {
        visibleAnalyzedImageBeans.clear();
        infraredAnalyzedImageBeans.clear();

        List<String> visibleImageUrls = new ArrayList<>();
        List<String> infraredImageUrls = new ArrayList<>();

        switch (imagesType) {
            case BOTH_IMAGES:
                // 默认可见光、红外光都有
                visibleImageUrls = getImageUrls(true);
                infraredImageUrls = getImageUrls(false);
                break;
            case ONLY_VISIBLE:
                // 没有红外光，有可见光
                visibleImageUrls = getImageUrls(true);
                break;
            case ONLY_INFRARED:
                // 没有可见光图集，有红外
                infraredImageUrls = getImageUrls(false);
                break;
            case NO_IMAGES:
                break;
        }

        if (null != visibleImageUrls && visibleImageUrls.size() != 0) {
            for (String url : visibleImageUrls) {
                AnalyzedImageBean image = new AnalyzedImageBean(url, AnalyzedImageBean.IS_VISIBLE);
                visibleAnalyzedImageBeans.add(image);
            }
        }
        if (null != infraredImageUrls && infraredImageUrls.size() != 0) {
            for (String url : infraredImageUrls) {
                AnalyzedImageBean image = new AnalyzedImageBean(url, AnalyzedImageBean.IS_INFRARED);
                infraredAnalyzedImageBeans.add(image);
            }
        }
    }

    private void initMapView(Bundle savedInstanceState) {

        // 初始化地图部分
        visibleFrame = findViewById(R.id.fl_record_visible_map);
        infraredFrame = findViewById(R.id.fl_record_infrared_map);
        visibleFrame.setVisibility(View.VISIBLE);
        infraredFrame.setVisibility(View.GONE);

        visibleMapView = findViewById(R.id.mv_record_visible);
        infraredMapView = findViewById(R.id.mv_record_infrared);

        switch (imagesType) {
            case BOTH_IMAGES:
                // 默认可见光、红外光都有
                imageNow = VISIBLE_IMAGES;

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

                break;
            case ONLY_VISIBLE:
                // 没有红外光，有可见光
                imageNow = VISIBLE_IMAGES;

                // 去掉上方的选择区域
                showBtnsLayout.setVisibility(View.GONE);

                visibleMapView.onCreate(savedInstanceState);
                if (visibleAMap == null) {
                    visibleAMap = visibleMapView.getMap();
                }
                visibleAMap.setOnMarkerClickListener(visibleMarkerClickListener);
                GoogleMapHelper.useGoogleMapSatelliteData(visibleAMap);

                UiSettings settings3 = visibleAMap.getUiSettings();
                settings3.setZoomControlsEnabled(false);
                settings3.setScaleControlsEnabled(true);

                break;
            case ONLY_INFRARED:
                // 没有可见光图集，有红外
                imageNow = INFRARED_IMAGES;

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

                UiSettings settings4 = infraredAMap.getUiSettings();
                settings4.setZoomControlsEnabled(false);
                settings4.setScaleControlsEnabled(true);

                break;
            case NO_IMAGES:
                break;
        }
    }

    private void markImageOnMap() {
        switch (imagesType) {
            case BOTH_IMAGES:
                // 默认可见光、红外光都有
                // 标记照片位置点
                markVisibleImageLocation();
                markInfraredImageLocation();
                break;
            case ONLY_VISIBLE:
                // 没有红外光，有可见光
                // 标记照片位置点
                markVisibleImageLocation();
                break;
            case ONLY_INFRARED:
                // 没有可见光图集，有红外
                // 标记照片位置点
                markInfraredImageLocation();
                break;
            case NO_IMAGES:
                break;
        }
    }

    private void initTFObjectDetection() {
        try {
            detector = TensorFlowObjectDetectionAPIModel.create(
                    getAssets(), TF_OD_API_MODEL_FILE, TF_OD_API_LABELS_FILE, TF_OD_API_INPUT_SIZE);
            if (null == detector) {
                ToastHelper.getInstance().showShortToast("模型初始化失败");
            }
        } catch (final IOException e) {
            Log.e(TAG, "Exception initializing classifier!" + e);
            finish();
        }
    }

    private void runVisibleDataAnalyse() {
        if (null == visibleAnalyzedImageBeans || visibleAnalyzedImageBeans.size() == 0) {
            // 无照片
            return;
        }

        showAnalysisProgressDialog();

        Thread runTFODThread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int index = 0; index < visibleAnalyzedImageBeans.size(); index++) {

                    final AnalyzedImageBean image = visibleAnalyzedImageBeans.get(index);

                    final int nowNum = index + 1;
                    final int nowProgress = (int) (1.0 * nowNum / visibleAnalyzedImageBeans.size() * 100);

                    runTFObjectDetection(image, new OneAnalysisListener() {
                        @Override
                        public void onStart() {

                        }

                        @Override
                        public void onSuccess(int imageState) {
                            Log.d(TAG, "now success nums: " + nowNum);
                            image.setImageState(imageState);
                            if (imageState != AnalyzedImageBean.IS_NORMAL) {
                                Message message = new Message();
                                message.what = TFOD_UPDATE_MARKER;
                                message.obj = image;
                                handler.sendMessage(message);
                            }

                            Message message = new Message();
                            message.what = TFOD_SUCCESS;
                            message.arg1 = nowProgress;
                            handler.sendMessage(message);

                            if (nowNum == visibleAnalyzedImageBeans.size()) {
                                ToastHelper.getInstance().showShortToast("分析完成： " + nowNum + " 张照片");
                                Message message1 = new Message();
                                message1.what = TFOD_COMPLETED;
                                handler.sendMessage(message1);
                            }

                        }

                        @Override
                        public void onFailure() {

                        }
                    });


                }
            }
        });

        runTFODThread.start();

    }

    private void runInfraredDataAnalyse() {

    }

    private void runTFObjectDetection(AnalyzedImageBean image, OneAnalysisListener oneAnalysisListener) {
        // read image
        FileInputStream solarImageFS;
        try {
            solarImageFS = new FileInputStream(image.getImagePath());
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        Bitmap solarImageBitmap = BitmapFactory.decodeStream(solarImageFS);

        // pre-process image(prepare to do something)
        int previewWidth = solarImageBitmap.getWidth();
        int previewHeight = solarImageBitmap.getHeight();
        int cropSize = TF_OD_API_INPUT_SIZE;
        Log.d(TAG, "prewW: " + previewWidth + ", prewH: " + previewHeight +
                ",  tfod_api_input_size: " + cropSize);

        // 创建压缩图像
        Bitmap rgbFrameBitmap = Bitmap.createBitmap(solarImageBitmap);
        Bitmap croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);

        Matrix frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropSize, cropSize,
                        0, false); // 旋转为0？？？
        Matrix cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        // For examining the actual TF input.
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap);
        }

        // tfodapi 分析
        // detector recognizeImage
        final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);

        // recognize small size test
        Bitmap copyCroppedImageBitmap = Bitmap.createBitmap(croppedBitmap);
        final Canvas testCanvas = new Canvas(copyCroppedImageBitmap);
        final Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.0f);

        // 处理结果
        boolean hasCovered = false;
        boolean hasBroken = false;
        final List<MyRecognition> mappedRecognitions = new LinkedList<MyRecognition>();

        for (final Classifier.Recognition result : results) {

            final RectF location = result.getLocation();
            if (location != null && result.getConfidence() >= MINIMUM_CONFIDENCE_TF_OD_API) {

                if (result.getTitle().equals(getResources().getString(R.string.covered_panel_title))) {
                    hasCovered = true;
                }
                if (result.getTitle().equals(getResources().getString(R.string.broken_panel_title))) {
                    hasBroken = true;
                }

                testCanvas.drawRect(location, paint);

                // for test
                ImageUtils.saveBitmap(copyCroppedImageBitmap);

                // location change
                cropToFrameTransform.mapRect(location);
                result.setLocation(location);
                mappedRecognitions.add(new MyRecognition(result));
            }
        }

        // 保存结果
        image.setRecognitions(mappedRecognitions);

        int thisImageState = AnalyzedImageBean.IS_NORMAL;
        if (hasCovered) {
            thisImageState = AnalyzedImageBean.HAS_COVERED;
        }
        if (hasBroken) {
            thisImageState = AnalyzedImageBean.HAS_BROKEN;
        }
        oneAnalysisListener.onSuccess(thisImageState);
    }

    /**
     * 在地图上标记可见光图像
     */
    private void markVisibleImageLocation() {

        if (visibleAnalyzedImageBeans.size() == 0) {
            // 读取照片异常
        }

        for (int i = 0; i < visibleAnalyzedImageBeans.size(); i++) {
            AnalyzedImageBean image = visibleAnalyzedImageBeans.get(i);
            LatLng location = RecordImageHelper.getImageLatlng(image.getImagePath());
            location = GoogleMapHelper.WGS84ConvertToAmap(location);
            MarkerOptions newMarkerOptions = genNormalMarkerOptions(i, location);
            Marker newMarker = visibleAMap.addMarker(newMarkerOptions);
            image.setMapMarker(newMarker);

            // 调整地图视角
            CameraUpdate cameraupdate = CameraUpdateFactory.newLatLngZoom(location, 18f);
            visibleAMap.moveCamera(cameraupdate);
        }
    }

    /**
     * 在地图上标记红外光图像
     */
    private void markInfraredImageLocation() {

        if (infraredAnalyzedImageBeans.size() == 0) {
            // 读取照片异常
        }

        for (int i = 0; i < infraredAnalyzedImageBeans.size(); i++) {
            AnalyzedImageBean image = infraredAnalyzedImageBeans.get(i);
            LatLng location = RecordImageHelper.getImageLatlng(image.getImagePath());
            location = GoogleMapHelper.WGS84ConvertToAmap(location);
            MarkerOptions newMarkerOptions = genNormalMarkerOptions(i, location);
            Marker newMarker = infraredAMap.addMarker(newMarkerOptions);
            image.setMapMarker(newMarker);

            // 调整地图视角
            CameraUpdate cameraupdate = CameraUpdateFactory.newLatLngZoom(location, 18f);
            infraredAMap.moveCamera(cameraupdate);
        }
    }

    /**
     * 获得图片地址列表
     *
     * @param isVisible
     * @return
     */
    private List<String> getImageUrls(boolean isVisible) {
        List<String> urls = new ArrayList<>();

        File imagePath;
        if (isVisible) {
            imagePath = RecordImageHelper.getRecordTestImagePath();
//            imagePath = RecordImageHelper.getRecordVisibleImagePath(mFlightRecord);
        } else {
            imagePath = RecordImageHelper.getRecordTestImagePath();
//            imagePath = RecordImageHelper.getRecordVisibleImagePath(mFlightRecord);
        }

        if (!imagePath.exists()) {
            ToastHelper.getInstance().showLongToast("任务照片不存在");
        } else {
            File[] images = imagePath.listFiles();
            if (images == null) {//判断权限
                ToastHelper.getInstance().showLongToast("任务照片不存在");
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
        AnalyzedImageBean showImage;
        if (isVisible) {
            showImage = visibleAnalyzedImageBeans.get(index);
            Intent intent = new Intent(this, AnalyzedImageActivity.class);
            intent.putExtra(AnalyzedImageActivity.EXTRA_IMAGE, showImage);
            startActivity(intent);
        } else {
            showImage = infraredAnalyzedImageBeans.get(index);
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
     * @return
     */
    private MarkerOptions genNormalMarkerOptions(int index, LatLng location) {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.title(String.valueOf(index));
        markerOptions.position(location);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));

        return markerOptions;
    }

    private MarkerOptions genWarnMarkerOptions(int index, LatLng location) {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.title(String.valueOf(index));
        markerOptions.position(location);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

        return markerOptions;
    }

    private void showAnalysisProgressDialog() {
        if (analysisProgressDialog != null) {
            analysisProgressDialog.incrementProgressBy(-analysisProgressDialog.getProgress());
            analysisProgressDialog.show();
        }
    }

    private void hideAnalysisProgressDialog() {
        if (null != analysisProgressDialog && analysisProgressDialog.isShowing()) {
            analysisProgressDialog.dismiss();
        }
    }

    private interface OneAnalysisListener {
        void onStart();

        void onSuccess(int imageType);

        void onFailure();
    }
}
