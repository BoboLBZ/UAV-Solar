package com.hitices.autopatrol.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.hitices.autopatrol.AutoPatrolApplication;
import com.hitices.autopatrol.R;
import com.hitices.autopatrol.entity.dataSupport.PatrolMission;
import com.hitices.autopatrol.helper.MissionConstraintHelper;
import com.hitices.autopatrol.helper.MissionHelper;
import com.hitices.autopatrol.helper.ContextHelper;
import com.hitices.autopatrol.helper.PermissionHelper;
import com.hitices.autopatrol.helper.ToastHelper;

import org.litepal.LitePal;
import org.opencv.android.OpenCVLoader;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import dji.common.error.DJIError;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.useraccount.UserAccountManager;

public class AppMainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = AppMainActivity.class.getName();

    // opencv
    static {
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV not loaded");
        } else {
            Log.d(TAG, "OpenCV loaded！");
        }

        // ndk support test
        System.loadLibrary("native-lib");
    }

    private Button missionManageButton, missionRunButton, dataAnalysisButton, dataDownloadButton;
    private TextView droneStateLightText, droneStateText, droneInfoName, droneInfoCamera;
    private ViewGroup droneInfoGroup, droneFunc2Group;
    private ImageView droneLogoImage;

    private boolean isDroneConnected;

    private long mExitTime = 0;
    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(AutoPatrolApplication.FLAG_CONNECTION_CHANGE)) {
                loginAccount();
                refreshDroneState();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ndk测试
        Log.d("PatrolMainActivity", stringFromJNI());

        setContentView(R.layout.activity_app_main);
        // 初始化权限
        PermissionHelper.checkAndRequestPermissions(this);

        // 初始化ToastHelper
        ToastHelper.getInstance();

        LitePal.initialize(this);
        LitePal.getDatabase();
        // 初始化界面
        initUI();
        scanLocalMission();
        IntentFilter filter = new IntentFilter();
        filter.addAction(AutoPatrolApplication.FLAG_CONNECTION_CHANGE);
        this.registerReceiver(mReceiver, filter);

        TensorFlowInferenceInterface inferenceInterface =
                new TensorFlowInferenceInterface(ContextHelper.getApplicationContext().getAssets(), "frozen_har.pb");
    }

    @Override
    protected void onDestroy() {
        this.unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_app_func_mission_manage:
                startActivity(new Intent(this, MissionManagemantActivity.class));
                break;
            case R.id.btn_app_func_data_analysis:
                startActivity(new Intent(this, DataAnalyseActivity.class));
                break;
            case R.id.btn_app_func_mission_run:
                //test
                startActivity(new Intent(this, MissionExecutePreparedActivity.class));
                break;
            case R.id.btn_app_func_data_download:
                startActivity(new Intent(this, DataDownloadActivity.class));
                break;
        }
    }

    private void initUI() {
        droneLogoImage = findViewById(R.id.iv_drone_logo);
        droneStateLightText = findViewById(R.id.tv_drone_state_light);
        droneStateText = findViewById(R.id.tv_drone_state);
        droneInfoGroup = findViewById(R.id.ll_drone_info);
        droneInfoName = findViewById(R.id.tv_drone_info_name);
        droneInfoCamera = findViewById(R.id.tv_drone_info_camera);

        missionManageButton = findViewById(R.id.btn_app_func_mission_manage);
        missionRunButton = findViewById(R.id.btn_app_func_mission_run);
        dataAnalysisButton = findViewById(R.id.btn_app_func_data_analysis);
        dataDownloadButton = findViewById(R.id.btn_app_func_data_download);
        missionManageButton.setOnClickListener(this);
        missionRunButton.setOnClickListener(this);
        dataAnalysisButton.setOnClickListener(this);
        dataDownloadButton.setOnClickListener(this);
        droneFunc2Group = findViewById(R.id.ll_func_2);

        refreshDroneState();
    }

    private boolean judgeDroneConnected() {

        boolean isConnected = false;

        BaseProduct mProduct = AutoPatrolApplication.getProductInstance();

        if (null != mProduct && mProduct.isConnected()) {
            Log.v(TAG, "refreshSDK: True");
            isConnected = true;
            if (null != mProduct.getModel()) {

            } else {

            }

        } else {
            isConnected = false;
        }

        return isConnected;
    }

    private void refreshDroneState() {
        isDroneConnected = judgeDroneConnected();
        if (isDroneConnected) {
            droneLogoImage.setAlpha(255);
            droneStateLightText.setActivated(true);
            droneStateText.setText(getResources().getString(R.string.drone_state_on));
            droneInfoGroup.setVisibility(View.VISIBLE);
            droneFunc2Group.setVisibility(View.VISIBLE);
        } else {
            droneLogoImage.setAlpha(100);
            droneStateLightText.setActivated(false);
            droneStateText.setText(getResources().getString(R.string.drone_state_off));
            droneInfoGroup.setVisibility(View.INVISIBLE);
            droneFunc2Group.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if ((System.currentTimeMillis() - mExitTime) > 2000) {
                ToastHelper.getInstance().showShortToast("再按一次退出程序");
                mExitTime = System.currentTimeMillis();
            } else {
                System.exit(0);
            }
            return true;
        }
        return super.onKeyDown(keyCode, keyEvent);
    }

    private void loginAccount() {
        UserAccountManager.getInstance().logIntoDJIUserAccount(this,
                new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
                    @Override
                    public void onSuccess(final UserAccountState userAccountState) {
                        setResultToToast("Login Success");
                    }

                    @Override
                    public void onFailure(DJIError error) {
                        setResultToToast("Login Error:"
                                + error.getDescription());
                    }
                });
    }

    private void scanLocalMission() {
        List<PatrolMission> patrolMissions = MissionHelper.readMissionsFromDataBase();
        List<String> names = new ArrayList<>();
        for (int i = 0; i < patrolMissions.size(); i++) {
            names.add(patrolMissions.get(i).getName());
//            System.out.println("testNameA:"+names.get(i));
        }
        File file = new File(MissionConstraintHelper.MISSION_DIR);
        if (file.exists()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    File f = files[i];
                    if (f.getName().endsWith("xml")) {
                        MissionHelper helper = new MissionHelper(f.getAbsolutePath(), new PatrolMission());
                        PatrolMission patrolMission = helper.getPatrolMission();
//                        System.out.println("testNameB:"+patrolMission.getName());
                        if (!names.contains(patrolMission.getName())) {
                            patrolMission.save();
                        }
                    }
                }
            }
        }

    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    private void setResultToToast(final String msg) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
