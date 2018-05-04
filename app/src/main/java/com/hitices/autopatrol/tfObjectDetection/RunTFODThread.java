package com.hitices.autopatrol.tfObjectDetection;

import android.content.Context;
import android.util.Log;

import com.hitices.autopatrol.helper.ToastHelper;

import java.io.IOException;

/**
 * Created by dusz7 on 20180426.
 */

public class RunTFODThread implements Runnable {
    private static final String TAG = RunTFODThread.class.getName();

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


    public RunTFODThread(Context context) {
        initDetector(context);
    }

    private void initDetector(Context context) {
        try {
            detector = TensorFlowObjectDetectionAPIModel.create(
                    context.getAssets(), TF_OD_API_MODEL_FILE, TF_OD_API_LABELS_FILE, TF_OD_API_INPUT_SIZE);
        } catch (final IOException e) {
            Log.e(TAG, "Exception initializing classifier!" + e);
        }
    }

    @Override
    public void run() {

    }
}
