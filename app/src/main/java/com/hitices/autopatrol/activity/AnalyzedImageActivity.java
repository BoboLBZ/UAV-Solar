package com.hitices.autopatrol.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.github.chrisbanes.photoview.PhotoView;
import com.hitices.autopatrol.R;
import com.hitices.autopatrol.entity.imageData.AnalyzedImage;
import com.hitices.autopatrol.entity.imageData.MyRecognition;
import com.hitices.autopatrol.tfObjectDetection.Classifier;
import com.hitices.autopatrol.tfObjectDetection.ImageUtils;

import java.io.FileInputStream;
import java.io.IOException;

public class AnalyzedImageActivity extends AppCompatActivity {
    private static final String TAG = AnalyzedImageActivity.class.getName();

    public static final String EXTRA_IMAGE = "extra_image";

    private PhotoView photoView;
    private AnalyzedImage showImage;
    private Bitmap showImageBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analyzed_image);

        Intent intent = getIntent();
        showImage = intent.getParcelableExtra(EXTRA_IMAGE);

        photoView = findViewById(R.id.pv_show_image);

        if (null != showImage) {
            showTheImage();
        } else {
            finish();
        }
    }

    private void showTheImage() {
        FileInputStream showImageFS;

        try {
            showImageFS = new FileInputStream(showImage.getImagePath());
            showImageBitmap = BitmapFactory.decodeStream(showImageFS);
            photoView.setImageDrawable(new BitmapDrawable(showImageBitmap));

            if (null != showImage.getRecognitions()) {
                drawRecognitions();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

    private void drawRecognitions() {
        Bitmap drawBitmap = showImageBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(drawBitmap);
        Paint paintNormal = new Paint();
        paintNormal.setColor(Color.GREEN);
        paintNormal.setStyle(Paint.Style.STROKE);
        paintNormal.setStrokeWidth(5.0f);
        Paint paintWarn = new Paint();
        paintWarn.setColor(Color.RED);
        paintWarn.setStyle(Paint.Style.STROKE);
        paintWarn.setStrokeWidth(5.0f);

        Log.d(TAG, "drawRecognitions");
        Log.d(TAG, "recognitions size: " + showImage.getRecognitions().size());

        for (MyRecognition recognition: showImage.getRecognitions()) {
            final RectF location = recognition.getLocation();

            if (location != null) {

                if (recognition.getTitle().equals(getResources().getString(R.string.covered_panel_title))) {
                    canvas.drawRect(location, paintWarn);
                } else if (recognition.getTitle().equals(getResources().getString(R.string.broken_panel_title))) {
                    canvas.drawRect(location, paintWarn);
                }else {
                    canvas.drawRect(location, paintNormal);
                }

            } else {
                Log.d(TAG, "recognition has null location");
            }
        }

        photoView.setImageDrawable(new BitmapDrawable(drawBitmap));
    }
}
