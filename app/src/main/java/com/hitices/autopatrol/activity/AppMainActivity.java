package com.hitices.autopatrol.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;

import com.hitices.autopatrol.R;
import com.hitices.autopatrol.entity.dataSupport.PatrolMission;
import com.hitices.autopatrol.helper.PermissionHelper;
import com.hitices.autopatrol.helper.ToastHelper;

import org.litepal.LitePal;
import org.opencv.android.OpenCVLoader;

public class AppMainActivity extends AppCompatActivity implements View.OnClickListener {

    // opencv
    static {
        if (!OpenCVLoader.initDebug()) {
            Log.d("PatrolMainActivity", "OpenCV not loaded");
        } else {
            Log.d("PatrolMainActivity", "OpenCV loaded！");
        }

        // ndk support test
        System.loadLibrary("native-lib");
    }

    private long mExitTime = 0;
    private Button appFunc1Button, appFunc2Button, appFunc3Button, appFunc4Button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ndk测试
        Log.d("PatrolMainActivity", stringFromJNI());

        setContentView(R.layout.activity_app_main);

        // 初始化权限
        PermissionHelper.checkAndRequestPermissions(this);
        LitePal.initialize(this);
        LitePal.getDatabase();
//        PatrolMission mission=new PatrolMission();
//        mission.setName("test");
//        mission.setFilePath("path");
//        mission.save();
        // 初始化界面
        initUI();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.app_func1_button:
                startActivity(new Intent(this, MissionManagemantActivity.class));
                break;
            case R.id.app_func2_button:
                startActivity(new Intent(this, MissionSelectActivity.class));
                break;
            case R.id.app_func3_button:
                break;
            case R.id.app_func4_button:
                break;
        }
    }

    private void initUI() {
        appFunc1Button = findViewById(R.id.app_func1_button);
        appFunc2Button = findViewById(R.id.app_func2_button);
        appFunc3Button = findViewById(R.id.app_func3_button);
        appFunc4Button = findViewById(R.id.app_func4_button);
        appFunc1Button.setOnClickListener(this);
        appFunc2Button.setOnClickListener(this);
        appFunc3Button.setOnClickListener(this);
        appFunc4Button.setOnClickListener(this);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if ((System.currentTimeMillis() - mExitTime) > 2000) {
                ToastHelper.showShortToast(this, "再按一次退出程序");
                mExitTime = System.currentTimeMillis();
            } else {
                System.exit(0);
            }
            return true;
        }
        return super.onKeyDown(keyCode, keyEvent);
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}
