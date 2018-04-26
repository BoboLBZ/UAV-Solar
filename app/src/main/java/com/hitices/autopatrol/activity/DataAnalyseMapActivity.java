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
import android.location.Location;
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
import com.hitices.autopatrol.entity.imageData.AnalyzedImage;
import com.hitices.autopatrol.helper.ContextHelper;
import com.hitices.autopatrol.helper.GoogleMapHelper;
import com.hitices.autopatrol.helper.RecordImageHelper;
import com.hitices.autopatrol.helper.ToastHelper;
import com.hitices.autopatrol.tfObjectDetection.Classifier;
import com.hitices.autopatrol.tfObjectDetection.ImageUtils;
import com.hitices.autopatrol.tfObjectDetection.TensorFlowObjectDetectionAPIModel;
import com.nostra13.universalimageloader.utils.L;

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

    private String imageNow = VISIBLE_IMAGES;

    private View visibleFrame, infraredFrame, showBtnsLayout;
    private Button showVisibleBtn, showInfraredBtn;
    private Button genReportBtn, runAnalysisBtn;
    private MapView visibleMapView, infraredMapView;
    private AMap visibleAMap, infraredAMap;

    private List<AnalyzedImage> visibleAnalyzedImages = new ArrayList<>();
    private List<AnalyzedImage> infraredAnalyzedImages = new ArrayList<>();
//    private List<String> visibleImageUrls = new ArrayList<>();
//    private List<String> infraredImageUrls = new ArrayList<>();
//    private List<MarkerOptions> visibleMarkerOptions = new ArrayList<>();
//    private List<MarkerOptions> infraredMarkerOptions = new ArrayList<>();

    private ProgressDialog analysisProgressDialog;

    // object_detection

    // 模型及Label位置
    private static final String TF_OD_API_MODEL_FILE = "file:///android_asset/frozen_inference_graph.pb";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/solar_labels_list.txt";
    // API输入图片的尺寸（要压缩处理）
    private static final int TF_OD_API_INPUT_SIZE = 450;
    // Minimum detection confidence to track a detection.
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.6f;

    private static final boolean SAVE_PREVIEW_BITMAP = false;
//    private static final float TEXT_SIZE_DIP = 10;

    // API主体
    private Classifier detector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_analyse_map);

        Intent intent = getIntent();
        FlightRecord flightRecord = (FlightRecord) intent.getSerializableExtra(getResources().getString(R.string.selected_flight_record));
        initImages(flightRecord);
        initUI(savedInstanceState, flightRecord);
        initDialog(this);
        initTFObjectDetection();
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
                    AnalyzedImage image = (AnalyzedImage)msg.obj;
                    LatLng location = image.getMapMarker().getPosition();
                    image.getMapMarker().remove();
                    MarkerOptions markerOptions = genWarnMarkerOptions(location);
                    Marker marker = visibleAMap.addMarker(markerOptions);
                    image.setMapMarker(marker);
                    break;
                case TFOD_COMPLETED:
                    hideAnalysisProgressDialog();
                    break;
            }
        }
    };

    private void initImages(FlightRecord record) {
        visibleAnalyzedImages.clear();
        infraredAnalyzedImages.clear();

        List<String> visibleImageUrls = new ArrayList<>();
        List<String> infraredImageUrls = new ArrayList<>();

        // 默认可见光、红外光都有
        if (record.isHasVisible() && record.isHasInfrared()) {
            visibleImageUrls = getImageUrls(record, true);
            infraredImageUrls = getImageUrls(record, false);
        }

        // 没有可见光图集，有红外
        if (!record.isHasVisible()) {
            infraredImageUrls = getImageUrls(record, false);
        }

        // 没有红外光，有可见光
        if (!record.isHasInfrared()) {
            visibleImageUrls = getImageUrls(record, true);
        }

        // 两个都没有，应该不可能：
        if (!record.isHasInfrared() && record.isHasVisible()) {
            return;
        }

        if (null != visibleImageUrls && visibleImageUrls.size() != 0) {
            for (String url : visibleImageUrls) {
                AnalyzedImage image = new AnalyzedImage(url, AnalyzedImage.IS_VISIBLE);
                visibleAnalyzedImages.add(image);
            }
        }
        if (null != infraredImageUrls && infraredImageUrls.size() != 0) {
            for (String url : infraredImageUrls) {
                AnalyzedImage image = new AnalyzedImage(url, AnalyzedImage.IS_INFRARED);
                infraredAnalyzedImages.add(image);
            }
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
            imageNow = VISIBLE_IMAGES;

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
            markVisibleImageLocation();
            markInfraredImageLocation();
        }

        // 没有可见光图集，有红外
        if (!record.isHasVisible()) {
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

            UiSettings settings = infraredAMap.getUiSettings();
            settings.setZoomControlsEnabled(false);
            settings.setScaleControlsEnabled(true);

            // 标记照片位置点
            markInfraredImageLocation();
        }

        // 没有红外光，有可见光
        if (!record.isHasInfrared()) {
            imageNow = VISIBLE_IMAGES;

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
            markVisibleImageLocation();
        }

        // 两个都没有，应该不可能：
        if (!record.isHasInfrared() && record.isHasVisible()) {

        }
    }

    private void initDialog(Context context) {

        //Init runAnalyse Dialog
        analysisProgressDialog = new ProgressDialog(context);
        analysisProgressDialog.setTitle(ContextHelper.getApplicationContext().getResources().getString(R.string.run_analysis_title));
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

    private void initTFObjectDetection() {
//        final float textSizePx =
//                TypedValue.applyDimension(
//                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
//        borderedText = new BorderedText(textSizePx);
//        borderedText.setTypeface(Typeface.MONOSPACE);

        // create detector
        try {
            detector = TensorFlowObjectDetectionAPIModel.create(
                    getAssets(), TF_OD_API_MODEL_FILE, TF_OD_API_LABELS_FILE, TF_OD_API_INPUT_SIZE);
        } catch (final IOException e) {
            Log.e(TAG, "Exception initializing classifier!" + e);
            ToastHelper.getInstance().showShortToast("Classifier could not be initialized");
            finish();
        }
    }

    private void runVisibleDataAnalyse() {
        if (null == visibleAnalyzedImages || visibleAnalyzedImages.size() == 0) {
            // 无照片
            return;
        }

        showAnalysisProgressDialog();

        Thread runTFODThread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int index = 0; index < visibleAnalyzedImages.size(); index++) {

                    final AnalyzedImage image = visibleAnalyzedImages.get(index);

                    final int nowNum = index + 1;
                    final int nowProgress = (int) (1.0 * nowNum / visibleAnalyzedImages.size() * 100);

                    runTFObjectDetection(image.getImagePath(), new OneAnalysisListener() {
                        @Override
                        public void onStart() {

                        }

                        @Override
                        public void onSuccess(int imageState) {
                            Log.d(TAG, "now success nums: " + nowNum);
                            image.setImageState(imageState);
                            if (imageState != AnalyzedImage.IS_NORMAL) {
                                Message message = new Message();
                                message.what = TFOD_UPDATE_MARKER;
                                message.obj = image;
                                handler.sendMessage(message);
                            }

                            Message message = new Message();
                            message.what = TFOD_SUCCESS;
                            message.arg1 = nowProgress;
                            handler.sendMessage(message);

                            if (nowNum == visibleAnalyzedImages.size()) {
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

    private void preProcessImageTF() {
//        tracker = new MultiBoxTracker(this);

//
//        trackingOverlay = (OverlayView) findViewById(R.id.tracking_overlay);
//        trackingOverlay.addCallback(
//                new DrawCallback() {
//                    @Override
//                    public void drawCallback(final Canvas canvas) {
//                        tracker.draw(canvas);
//                        if (isDebug()) {
//                            tracker.drawDebug(canvas);
//                        }
//                    }
//                });
    }

    private void runTFObjectDetection(String imagePath, OneAnalysisListener oneAnalysisListener) {
        // read image
        FileInputStream solarImageFS;
        try {
            solarImageFS = new FileInputStream(imagePath);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        Bitmap solarImageBitmap = BitmapFactory.decodeStream(solarImageFS);

        // pre-process image(prepare to do something)
        int previewWidth = solarImageBitmap.getWidth();
        int previewHeight = solarImageBitmap.getHeight();
        Log.d(TAG, "prewW: " + previewWidth + ", prewH: " + previewHeight);
        int cropSize = TF_OD_API_INPUT_SIZE;

//        Bitmap rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        Bitmap rgbFrameBitmap = Bitmap.createBitmap(solarImageBitmap);
        Bitmap croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);

        Matrix frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropSize, cropSize,
                        0, false); // 旋转为0？？？
        Matrix cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

//        ++timestamp;
//        final long currTimestamp = timestamp;
//        byte[] originalLuminance = getLuminance();
//        tracker.onFrame(
//                previewWidth,
//                previewHeight,
//                getLuminanceStride(),
//                sensorOrientation,
//                originalLuminance,
//                timestamp);
//        trackingOverlay.postInvalidate();

        // No mutex needed as this method is not reentrant.
//        if (computingDetection) {
//            readyForNextImage();
//            return;
//        }
//        computingDetection = true;
//        LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");

//        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

//        if (luminanceCopy == null) {
//            luminanceCopy = new byte[originalLuminance.length];
//        }
//        System.arraycopy(originalLuminance, 0, luminanceCopy, 0, originalLuminance.length);
//        readyForNextImage();

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        // For examining the actual TF input.
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap);
        }

//        final long startTime = SystemClock.uptimeMillis();
        // detector recognizeImage
        final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);
//        lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

        Bitmap copyCroppedImageBitmap = Bitmap.createBitmap(croppedBitmap);

        final Canvas canvas1 = new Canvas(copyCroppedImageBitmap);
        final Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.0f);

        // 处理结果
        float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
        final List<Classifier.Recognition> mappedRecognitions = new LinkedList<Classifier.Recognition>();

        boolean hasCovered = false;
        boolean hasBroken = false;

        for (final Classifier.Recognition result : results) {

//            Log.d(TAG, "now Image:" + imagePath + "\n" + result.toString());

            final RectF location = result.getLocation();
            if (location != null && result.getConfidence() >= minimumConfidence) {

                if (result.getTitle().equals(getResources().getString(R.string.covered_panel_title))) {
                    hasCovered = true;
                }
                if (result.getTitle().equals(getResources().getString(R.string.broken_panel_title))) {
                    hasBroken = true;
                }

                canvas1.drawRect(location, paint);

                // for test
                ImageUtils.saveBitmap(copyCroppedImageBitmap);
//                ToastHelper.getInstance().showShortToast("save bitmap");

                cropToFrameTransform.mapRect(location); //location change??
                result.setLocation(location);
                mappedRecognitions.add(result);
            }
        }

//        tracker.trackResults(mappedRecognitions, luminanceCopy, currTimestamp);
//        trackingOverlay.postInvalidate();
//
//        requestRender();
//        computingDetection = false;

        int thisImageState = AnalyzedImage.IS_NORMAL;
        if (hasCovered) {
            thisImageState = AnalyzedImage.HAS_COVERED;
        }
        if (hasBroken) {
            thisImageState = AnalyzedImage.HAS_BROKEN;
        }
        oneAnalysisListener.onSuccess(thisImageState);
    }

    /**
     * 在地图上标记可见光图像
     */
    private void markVisibleImageLocation() {

        if (visibleAnalyzedImages.size() == 0) {
            // 读取照片异常
        }

        for (int i = 0; i < visibleAnalyzedImages.size(); i++) {
            AnalyzedImage image = visibleAnalyzedImages.get(i);
            LatLng location = RecordImageHelper.getImageLatlng(image.getImagePath());
            location = GoogleMapHelper.WGS84ConvertToAmap(location);
            MarkerOptions newMarkerOptions = genNormalMarkerOptions(location);
            Marker newMarker = visibleAMap.addMarker(newMarkerOptions);
            image.setMapMarker(newMarker);
//            visibleMarkerOptions.add(newMarker);

            // 调整地图视角
            CameraUpdate cameraupdate = CameraUpdateFactory.newLatLngZoom(location, 18f);
            visibleAMap.moveCamera(cameraupdate);
        }
    }

    /**
     * 在地图上标记红外光图像
     */
    private void markInfraredImageLocation() {

        if (infraredAnalyzedImages.size() == 0) {
            // 读取照片异常
        }

        for (int i = 0; i < infraredAnalyzedImages.size(); i++) {
            AnalyzedImage image = infraredAnalyzedImages.get(i);
            LatLng location = RecordImageHelper.getImageLatlng(image.getImagePath());
            location = GoogleMapHelper.WGS84ConvertToAmap(location);
            MarkerOptions newMarkerOptions = genNormalMarkerOptions(location);
            Marker newMarker = infraredAMap.addMarker(newMarkerOptions);
            image.setMapMarker(newMarker);
//            infraredMarkerOptions.add(newMarker);

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
//            imagePath = RecordImageHelper.getRecordVisibleImagePath(record);
        } else {
//            imagePath = RecordImageHelper.getRecordTestImagePath();
            imagePath = RecordImageHelper.getRecordVisibleImagePath(record);
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
        File showImage;
        if (isVisible) {
            showImage = new File(visibleAnalyzedImages.get(index).getImagePath());
        } else {
            showImage = new File(infraredAnalyzedImages.get(index).getImagePath());
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
    private MarkerOptions genNormalMarkerOptions(LatLng location) {
        MarkerOptions markerOptions = new MarkerOptions();

        markerOptions.position(location);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));

        return markerOptions;
    }

    private MarkerOptions genWarnMarkerOptions(LatLng location) {
        MarkerOptions markerOptions = new MarkerOptions();

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
