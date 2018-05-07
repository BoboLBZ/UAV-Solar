package com.hitices.autopatrol.tfObjectDetection;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Trace;
import android.util.Log;

import com.hitices.autopatrol.R;
import com.hitices.autopatrol.activity.DataAnalyseMapActivity;
import com.hitices.autopatrol.entity.imageData.MyRecognition;
import com.hitices.autopatrol.entity.imageData.RecognizingImageBean;

import org.tensorflow.Graph;
import org.tensorflow.Operation;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Vector;

/**
 * Created by dusz7 on 20180424.
 */

public class TensorFlowObjectDetectionAPIModel implements Classifier {
    private static final String TAG = TensorFlowObjectDetectionAPIModel.class.getName();

    // Only return this many results.
    private static final int MAX_RESULTS = 300;

    // Config values.
    private String inputName;
    private int inputSize;

    // Pre-allocated buffers.
    private Vector<String> labels = new Vector<String>();
    private int[] intValues;
    private byte[] byteValues;
    private float[] outputLocations;
    private float[] outputScores;
    private float[] outputClasses;
    private float[] outputNumDetections;
    private String[] outputNames;

    private boolean logStats = false;

    private TensorFlowInferenceInterface inferenceInterface;

    /**
     * Initializes a native TensorFlow session for classifying images.
     *
     * @param assetManager  The asset manager to be used to load assets.
     * @param modelFilename The filepath of the model GraphDef protocol buffer.
     * @param labelFilename The filepath of label file for classes.
     */
    public static Classifier create(
            final AssetManager assetManager,
            final String modelFilename,
            final String labelFilename,
            final int inputSize) throws IOException {

        // d---APIModel
        final TensorFlowObjectDetectionAPIModel d = new TensorFlowObjectDetectionAPIModel();

        // 读取labels
        InputStream labelsInput = null;
        String actualFilename = labelFilename.split("file:///android_asset/")[1];
        labelsInput = assetManager.open(actualFilename);
        BufferedReader br = null;
        br = new BufferedReader(new InputStreamReader(labelsInput));
        String line;
        while ((line = br.readLine()) != null) {
            d.labels.add(line);
        }
        br.close();

        Log.d(TAG, "before read model");
        // 读取模型，构建inferenceInterface
        try {
            d.inferenceInterface = new TensorFlowInferenceInterface(assetManager, modelFilename);
        } catch (Exception e) {
            // 读取失败的话就直接返回
            return null;
        }
        Log.d(TAG, "after read model");

        final Graph g = d.inferenceInterface.graph();

        d.inputName = "image_tensor";
        // The inputName node has a shape of [N, H, W, C], where
        // N is the batch size
        // H = W are the height and width
        // C is the number of channels (3 for our purposes - RGB)
        final Operation inputOp = g.operation(d.inputName);
        if (inputOp == null) {
            throw new RuntimeException("Failed to find input Node '" + d.inputName + "'");
        }
        d.inputSize = inputSize;
        // The outputScoresName node has a shape of [N, NumLocations], where N
        // is the batch size.
        final Operation outputOp1 = g.operation("detection_scores");
        if (outputOp1 == null) {
            throw new RuntimeException("Failed to find output Node 'detection_scores'");
        }
        final Operation outputOp2 = g.operation("detection_boxes");
        if (outputOp2 == null) {
            throw new RuntimeException("Failed to find output Node 'detection_boxes'");
        }
        final Operation outputOp3 = g.operation("detection_classes");
        if (outputOp3 == null) {
            throw new RuntimeException("Failed to find output Node 'detection_classes'");
        }

        // Pre-allocate buffers.
        d.outputNames = new String[]{"detection_boxes", "detection_scores",
                "detection_classes", "num_detections"};
        d.intValues = new int[d.inputSize * d.inputSize];
        d.byteValues = new byte[d.inputSize * d.inputSize * 3];
        d.outputScores = new float[MAX_RESULTS];
        d.outputLocations = new float[MAX_RESULTS * 4];
        d.outputClasses = new float[MAX_RESULTS];
        d.outputNumDetections = new float[1];
        return d;
    }

    private TensorFlowObjectDetectionAPIModel() {
    }

    @Override
    public List<Recognition> recognizeImage(Bitmap bitmap) {
        // Log this method so that it can be analyzed with systrace.
//        Trace.beginSection("recognizeImage");
        Log.d(TAG, "begin recognizeImage");

//        Trace.beginSection("preprocessBitmap");
//        Log.d(TAG, "preprocessBitmap");
        // Preprocess the image data from 0-255 int to normalized float based
        // on the provided parameters.
        // java.lang.ArrayIndexOutOfBoundsException
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        for (int i = 0; i < intValues.length; ++i) {
            byteValues[i * 3 + 2] = (byte) (intValues[i] & 0xFF);
            byteValues[i * 3 + 1] = (byte) ((intValues[i] >> 8) & 0xFF);
            byteValues[i * 3 + 0] = (byte) ((intValues[i] >> 16) & 0xFF);
        }
//        Trace.endSection(); // preprocessBitmap
//        Log.d(TAG, "preprocess done");

        // Copy the input data into TensorFlow.
//        Trace.beginSection("feed");
//        Log.d(TAG, "begin feed");
        inferenceInterface.feed(inputName, byteValues, 1, inputSize, inputSize, 3);
//        Trace.endSection();
//        Log.d(TAG, "end feed");

        // Run the inference call.
//        Trace.beginSection("run");
        Log.d(TAG, "begin run");
        inferenceInterface.run(outputNames, logStats);
//        Trace.endSection();
        Log.d(TAG, "end run");

        // Copy the output Tensor back into the output array.
//        Trace.beginSection("fetch");
//        Log.d(TAG, "begin fetch");
        outputLocations = new float[MAX_RESULTS * 4];
        outputScores = new float[MAX_RESULTS];
        outputClasses = new float[MAX_RESULTS];
        outputNumDetections = new float[1];
        inferenceInterface.fetch(outputNames[0], outputLocations);
        inferenceInterface.fetch(outputNames[1], outputScores);
        inferenceInterface.fetch(outputNames[2], outputClasses);
        inferenceInterface.fetch(outputNames[3], outputNumDetections);
//        Trace.endSection();
//        Log.d(TAG, "end fetch");

        // Find the best detections.
        final PriorityQueue<Recognition> pq =
                new PriorityQueue<Recognition>(
                        1,
                        new Comparator<Recognition>() {
                            @Override
                            public int compare(final Recognition lhs, final Recognition rhs) {
                                // Intentionally reversed to put high confidence at the head of the queue.
                                return Float.compare(rhs.getConfidence(), lhs.getConfidence());
                            }
                        });

        // Scale them back to the input size.
        for (int i = 0; i < outputScores.length; ++i) {
            final RectF detection =
                    new RectF(
                            outputLocations[4 * i + 1] * inputSize,
                            outputLocations[4 * i] * inputSize,
                            outputLocations[4 * i + 3] * inputSize,
                            outputLocations[4 * i + 2] * inputSize);
            pq.add(
                    new Recognition("" + i, labels.get((int) outputClasses[i]), outputScores[i], detection));
        }

        final ArrayList<Recognition> recognitions = new ArrayList<Recognition>();
        for (int i = 0; i < Math.min(pq.size(), MAX_RESULTS); ++i) {
            recognitions.add(pq.poll());
        }
//        Trace.endSection(); // "recognizeImage"
        Log.d(TAG, "end recognizeImage");
        return recognitions;
    }

    @Override
    public void enableStatLogging(final boolean logStats) {
        this.logStats = logStats;
    }

    @Override
    public String getStatString() {
        return inferenceInterface.getStatString();
    }

    @Override
    public void close() {
        inferenceInterface.close();
    }


    // demo example
//    private void runTFObjectDetection(RecognizingImageBean image, DataAnalyseMapActivity.OneAnalysisListener oneAnalysisListener) {
//        // read image
//        FileInputStream solarImageFS;
//        try {
//            solarImageFS = new FileInputStream(image.getImagePath());
//        } catch (IOException e) {
//            e.printStackTrace();
//            return;
//        }
//        Bitmap solarImageBitmap = BitmapFactory.decodeStream(solarImageFS);
//
//        // pre-process image(prepare to do something)
//        int previewWidth = solarImageBitmap.getWidth();
//        int previewHeight = solarImageBitmap.getHeight();
//        int cropSize = TF_OD_API_INPUT_SIZE;
//
//        // 创建压缩图像
//        Bitmap rgbFrameBitmap = Bitmap.createBitmap(solarImageBitmap);
//        Bitmap croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);
//
//        Matrix frameToCropTransform =
//                ImageUtils.getTransformationMatrix(
//                        previewWidth, previewHeight,
//                        cropSize, cropSize,
//                        0, false); // 旋转为0？？？
//        Matrix cropToFrameTransform = new Matrix();
//        frameToCropTransform.invert(cropToFrameTransform);
//
//        final Canvas canvas = new Canvas(croppedBitmap);
//        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
//        // For examining the actual TF input.
////        if (SAVE_PREVIEW_BITMAP) {
////            ImageUtils.saveBitmap(croppedBitmap);
////        }
//
//        // tfodapi 分析
//        // detector recognizeImage
//        Log.d(TAG, image.getImagePath() + " before recognize");
//        final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);
//        Log.d(TAG, "after recognize & have results");
//
//        // recognize small size test
////        Bitmap copyCroppedImageBitmap = Bitmap.createBitmap(croppedBitmap);
////        final Canvas testCanvas = new Canvas(copyCroppedImageBitmap);
////        final Paint paint = new Paint();
////        paint.setColor(Color.RED);
////        paint.setStyle(Paint.Style.STROKE);
////        paint.setStrokeWidth(2.0f);
//
//        // 处理结果
//        boolean hasCovered = false;
//        boolean hasBroken = false;
//        final List<MyRecognition> mappedRecognitions = new LinkedList<MyRecognition>();
//
//        for (final Classifier.Recognition result : results) {
//
//            Log.d(TAG, "result: " + result.toString());
//
//            final RectF location = result.getLocation();
//            if (location != null && result.getConfidence() >= MINIMUM_CONFIDENCE_TF_OD_API) {
//
//                if (result.getTitle().equals(getResources().getString(R.string.covered_panel_title))) {
//                    hasCovered = true;
//                }
//                if (result.getTitle().equals(getResources().getString(R.string.broken_panel_title))) {
//                    hasBroken = true;
//                }
//
//                // for test
////                testCanvas.drawRect(location, paint);
////                ImageUtils.saveBitmap(copyCroppedImageBitmap);
//
//                // location change
//                cropToFrameTransform.mapRect(location);
//                result.setLocation(location);
//                mappedRecognitions.add(new MyRecognition(result));
//            }
//        }
//
//        // 保存结果
//        image.setRecognitions(mappedRecognitions);
//
//        int thisImageState = RecognizingImageBean.IS_NORMAL;
//        if (hasCovered) {
//            thisImageState = RecognizingImageBean.HAS_COVERED;
//        }
//        if (hasBroken) {
//            thisImageState = RecognizingImageBean.HAS_BROKEN;
//        }
//        oneAnalysisListener.onSuccess(thisImageState);
//
//        Log.d(TAG, "save the image result");
//    }

}
