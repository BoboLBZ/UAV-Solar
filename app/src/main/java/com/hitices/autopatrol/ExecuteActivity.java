package com.hitices.autopatrol;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

public class ExecuteActivity extends Activity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_execute);
    }

    public void showToast(final String msg) {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
//            case R.id.btn_capture:{
//                captureAction();
//                break;
//            }
//            case R.id.btn_shoot_photo_mode:{
//                switchCameraMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO);
//                break;
//            }
//            case R.id.btn_record_video_mode:{
//                switchCameraMode(SettingsDefinitions.CameraMode.RECORD_VIDEO);
//                break;
//            }
//            default:
//                break;
        }
    }
}
