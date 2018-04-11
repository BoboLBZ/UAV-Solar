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
import android.widget.Button;
import android.widget.Toast;

import com.hitices.autopatrol.AutoPatrolApplication;
import com.hitices.autopatrol.R;
import com.hitices.autopatrol.helper.PermissionHelper;
import com.hitices.autopatrol.helper.ToastHelper;

import org.litepal.LitePal;
import org.opencv.android.OpenCVLoader;

import dji.common.error.DJIError;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.useraccount.UserAccountManager;

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
    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(AutoPatrolApplication.FLAG_CONNECTION_CHANGE)) {
                loginAccount();
                refreshSDKRelativeUI();
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
        LitePal.initialize(this);
        LitePal.getDatabase();
        // 初始化界面
        initUI();

        IntentFilter filter = new IntentFilter();
        filter.addAction(AutoPatrolApplication.FLAG_CONNECTION_CHANGE);
        this.registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        this.unregisterReceiver(mReceiver);
        super.onDestroy();
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
                //test
                startActivity(new Intent(this, MissionExecutePreparedActivity.class));
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

        appFunc3Button.setClickable(false);
        appFunc4Button.setClickable(false);
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

    private void refreshSDKRelativeUI() {
        BaseProduct mProduct = AutoPatrolApplication.getProductInstance();

        if (null != mProduct && mProduct.isConnected()) {
            Log.v("connection", "refreshSDK: True");
            if (null != mProduct.getModel()) {
                appFunc3Button.setClickable(true);
                appFunc4Button.setClickable(true);
            } else {
                appFunc3Button.setClickable(false);
                appFunc4Button.setClickable(false);
            }

        } else {
            appFunc3Button.setClickable(false);
            appFunc4Button.setClickable(false);
        }
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
