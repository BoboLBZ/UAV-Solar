package com.hitices.autopatrol.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdate;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;
import com.hitices.autopatrol.R;
import com.hitices.autopatrol.entity.dataSupport.FlightRecord;
import com.hitices.autopatrol.entity.imageData.RecognizingImageBean;
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

    private List<RecognizingImageBean> visibleRecognizingImageBeans = new ArrayList<>();
    private List<RecognizingImageBean> infraredRecognizingImageBeans = new ArrayList<>();

    private String imageNow = VISIBLE_IMAGES;
    private FlightRecord mFlightRecord;
    private int imagesType = BOTH_IMAGES;

    // object_detection

    // 模型及Label位置
    private static final String FASTERRCNN_RESNET101_MODEL_FILE = "file:///android_asset/fasterrcnn_resnet101.pb";
    private static final String RFCN_RESNET101_MODEL_FILE = "file:///android_asset/rfcn_resnet101.pb";
    private static final String SSD_MOBILENET1_MODEL_FILE = "file:///android_asset/ssd_mobilenet1.pb";
    private static final String SSD_MOBILENET2_MODEL_FILE = "file:///android_asset/ssd_mobilenet2.pb";
    private static final String SSD_INCEPTION2_MODEL_FILE = "file:///android_asset/ssd_inception2.pb";
    private static final String TF_OD_API_MODEL_FILE = SSD_INCEPTION2_MODEL_FILE;
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/solar_labels_list.txt";
    // API输入图片的尺寸（要压缩处理）
    private static final int TF_OD_API_INPUT_SIZE = 750;
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
//            initTFObjectDetection();
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
                    if (null == visibleRecognizingImageBeans || visibleRecognizingImageBeans.size() == 0) {
                        // 无照片
                        return;
                    }
                    new RunVisibleAnalysisTask(this).execute();
                } else if (imageNow.equals(INFRARED_IMAGES)) {
                    runInfraredDataAnalyse();
                }
                break;
        }
    }

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
        genReportBtn.setVisibility(View.GONE);
        runAnalysisBtn.setOnClickListener(this);

        initMapView(savedInstanceState);
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
        visibleRecognizingImageBeans.clear();
        infraredRecognizingImageBeans.clear();

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
                RecognizingImageBean image = new RecognizingImageBean(url, RecognizingImageBean.IS_VISIBLE);
                visibleRecognizingImageBeans.add(image);
            }
        }
        if (null != infraredImageUrls && infraredImageUrls.size() != 0) {
            for (String url : infraredImageUrls) {
                RecognizingImageBean image = new RecognizingImageBean(url, RecognizingImageBean.IS_INFRARED);
                infraredRecognizingImageBeans.add(image);
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

//                UiSettings settings1 = visibleAMap.getUiSettings();
//                settings1.setZoomControlsEnabled(true);
//                settings1.setScaleControlsEnabled(true);
//                UiSettings settings2 = infraredAMap.getUiSettings();
//                settings2.setZoomControlsEnabled(false);
//                settings2.setScaleControlsEnabled(true);

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
            Log.d(TAG, "init detector");
            if (null == detector) {
                ToastHelper.getInstance().showShortToast("模型初始化失败");
            }
        } catch (final IOException e) {
            Log.e(TAG, "Exception initializing classifier!" + e);
            finish();
        }
    }

    private void runInfraredDataAnalyse() {

    }

    /**
     * 在地图上标记可见光图像
     */
    private void markVisibleImageLocation() {

        if (visibleRecognizingImageBeans.size() == 0) {
            // 读取照片异常
        }

        for (int i = 0; i < visibleRecognizingImageBeans.size(); i++) {
            RecognizingImageBean image = visibleRecognizingImageBeans.get(i);
            LatLng location = RecordImageHelper.getImageLatlng(image.getImagePath());
            location = GoogleMapHelper.WGS84ConvertToAmap(location);
            MarkerOptions newMarkerOptions = genNormalMarkerOptions(i, location);
            Marker newMarker = visibleAMap.addMarker(newMarkerOptions);
            image.setMapMarker(newMarker);

            // 调整地图视角
            CameraUpdate cameraupdate = CameraUpdateFactory.newLatLngZoom(location, 16);
            visibleAMap.moveCamera(cameraupdate);
        }
    }

    /**
     * 在地图上标记红外光图像
     */
    private void markInfraredImageLocation() {

        if (infraredRecognizingImageBeans.size() == 0) {
            // 读取照片异常
        }

        for (int i = 0; i < infraredRecognizingImageBeans.size(); i++) {
            RecognizingImageBean image = infraredRecognizingImageBeans.get(i);
            LatLng location = RecordImageHelper.getImageLatlng(image.getImagePath());
            location = GoogleMapHelper.WGS84ConvertToAmap(location);
            MarkerOptions newMarkerOptions = genNormalMarkerOptions(i, location);
            Marker newMarker = infraredAMap.addMarker(newMarkerOptions);
            image.setMapMarker(newMarker);

            // 调整地图视角
            CameraUpdate cameraupdate = CameraUpdateFactory.newLatLngZoom(location, 16);
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
            imagePath = RecordImageHelper.getRecordTestSSizeImagePath();
//            imagePath = RecordImageHelper.getRecordVisibleImagePath(mFlightRecord);
        } else {
            imagePath = RecordImageHelper.getRecordTestSSizeImagePath();
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
//        ToastHelper.getInstance().showShortToast("image_index: " + index);
        RecognizingImageBean showImage;
        if (isVisible) {
            showImage = visibleRecognizingImageBeans.get(index);
            Intent intent = new Intent(this, AnalyzedImageActivity.class);
            intent.putExtra(AnalyzedImageActivity.EXTRA_IMAGE, showImage);
            startActivity(intent);
        } else {
            showImage = infraredRecognizingImageBeans.get(index);
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

    /**
     * 生成一个警告的位置标记（红色）
     *
     * @param index
     * @param location
     * @return
     */
    private MarkerOptions genWarnMarkerOptions(int index, LatLng location) {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.title(String.valueOf(index));
        markerOptions.position(location);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

        return markerOptions;
    }

    class RunVisibleAnalysisTask extends AsyncTask<Void, RecognizingImageBean, Boolean> {
        private Context context;
        private ProgressDialog analysisProgressDialog;

        public RunVisibleAnalysisTask(Context context) {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            analysisProgressDialog = new ProgressDialog(context);
            analysisProgressDialog.setTitle(ContextHelper.getApplicationContext()
                    .getResources().getString(R.string.run_analysis_title));
            analysisProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            analysisProgressDialog.setCanceledOnTouchOutside(false);
            analysisProgressDialog.setCancelable(false);
            analysisProgressDialog.setButton("停止分析", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    // 中止分析
                    cancel(true);
                    ToastHelper.getInstance().showShortToast("分析已中止！");
                }
            });
            analysisProgressDialog.setProgressNumberFormat("已分析：%1d/%2d");
            analysisProgressDialog.setMax(visibleRecognizingImageBeans.size());
            analysisProgressDialog.show();
            analysisProgressDialog.setProgress(0);
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            initTFObjectDetection();

            for (int index = 0; index < visibleRecognizingImageBeans.size(); index++) {

                if (isCancelled()) {
                    break;
                }

                final RecognizingImageBean image = visibleRecognizingImageBeans.get(index);

                // read image
                FileInputStream solarImageFS;
                try {
                    solarImageFS = new FileInputStream(image.getImagePath());
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
                Bitmap solarImageBitmap = BitmapFactory.decodeStream(solarImageFS);

                // pre-process image(prepare to do something)
                int previewWidth = solarImageBitmap.getWidth();
                int previewHeight = solarImageBitmap.getHeight();
                int cropSize = TF_OD_API_INPUT_SIZE;
                // create resize bitmap
                Bitmap rgbFrameBitmap = Bitmap.createBitmap(solarImageBitmap);
                Bitmap croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);
                // 计算图像变换矩阵
                Matrix frameToCropTransform =
                        ImageUtils.getTransformationMatrix(
                                previewWidth, previewHeight,
                                cropSize, cropSize,
                                0, false); // 旋转为0？？？
                Matrix cropToFrameTransform = new Matrix();
                frameToCropTransform.invert(cropToFrameTransform);
                // get resized bitmap
                final Canvas canvas = new Canvas(croppedBitmap);
                canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);

                // detector recognizeImage
                Log.d(TAG, image.getImagePath() + " before recognize");
                final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);
                Log.d(TAG, "after recognize & have results");

                // 处理结果
                boolean hasCovered = false;
                boolean hasBroken = false;
                final List<MyRecognition> mappedRecognitions = new LinkedList<MyRecognition>();

                // analyse every result
                for (final Classifier.Recognition result : results) {

                    // to test results & confidence
//                    Log.d(TAG, "result: " + result.toString());

                    final RectF location = result.getLocation();
                    if (location != null && result.getConfidence() >= MINIMUM_CONFIDENCE_TF_OD_API) {

                        if (result.getTitle().equals(getResources().getString(R.string.covered_panel_title))) {
                            hasCovered = true;
                        }
                        if (result.getTitle().equals(getResources().getString(R.string.broken_panel_title))) {
                            hasBroken = true;
                        }

                        // location transform
                        cropToFrameTransform.mapRect(location);
                        result.setLocation(location);
                        mappedRecognitions.add(new MyRecognition(result));
                    }
                }

                // 保存结果
                image.setRecognitions(mappedRecognitions);

                int thisState = RecognizingImageBean.IS_NORMAL;
                if (hasCovered) {
                    thisState = RecognizingImageBean.HAS_COVERED;
                }
                if (hasBroken) {
                    thisState = RecognizingImageBean.HAS_BROKEN;
                }
                image.setSolarState(thisState);

                publishProgress(image);
            }
            return true;
        }

        @Override
        protected void onProgressUpdate(RecognizingImageBean... values) {
            RecognizingImageBean image = values[0];

            // set dialog progress
            analysisProgressDialog.incrementProgressBy(1);

            // update this image's marker on MapView
            if (image.getSolarState() != RecognizingImageBean.IS_NORMAL) {
                LatLng location = image.getMapMarker().getPosition();
                image.getMapMarker().remove();
                MarkerOptions markerOptions = genWarnMarkerOptions(visibleRecognizingImageBeans.indexOf(image), location);
                Marker marker = visibleAMap.addMarker(markerOptions);
                image.setMapMarker(marker);
            }
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            if (aBoolean) {
                analysisProgressDialog.dismiss();
            }
        }

        @Override
        protected void onCancelled() {
            analysisProgressDialog.dismiss();
        }
    }
}
