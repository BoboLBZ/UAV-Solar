package com.hitices.autopatrol;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.gimbal.Gimbal;
import dji.sdk.products.Aircraft;
import dji.sdk.products.HandHeld;
import dji.sdk.sdkmanager.DJISDKManager;

/**
 * Created by Rhys on 2017/12/26.
 * Email: bozliu@outllok.com
 * 主程序类，主要是DJI SDK的初始化和一些公用函数
 */

public class AutoPatrolApplication extends Application {
    public static final String FLAG_CONNECTION_CHANGE = "connection_change"; //用于通知飞行器连接状态的变化
    public static final String MISSION_DIR = Environment.getExternalStorageDirectory().getPath() + "/AutoPatrol/TwoMissions";  //默认任务保存位置
    public static final String PHOTO_DIR = Environment.getExternalStorageDirectory().getPath() + "/AutoPatrol/RawData";  //默认照片保存位置
    public static final String MISSION_PHOTO_DIR = Environment.getExternalStorageDirectory().getPath() + "/AutoPatrol/MissionPhoto";  //任务采集照片保存位置

    private static BaseProduct mProduct; //产品
    private Handler mHandler;
    private Application instance;
    //处理DJI SDK相关
    private DJISDKManager.SDKManagerCallback mDJISDKManagerCallback;
    private BaseProduct.BaseProductListener mDJIBaseProductListener;
    private BaseComponent.ComponentListener mDJIComponentListener;
    private Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent(FLAG_CONNECTION_CHANGE);
            getApplicationContext().sendBroadcast(intent);
        }
    };

    public AutoPatrolApplication() {

    }

    public static synchronized BaseProduct getProductInstance() {
        if (null == mProduct) {
            mProduct = DJISDKManager.getInstance().getProduct();
        }
        return mProduct;
    }

    public static synchronized Aircraft getAircraftInstance() {
        if (!isAircraftConnected()) return null;
        return (Aircraft) getProductInstance();
    }

    public static synchronized Camera getCameraInstance() {

        if (getProductInstance() == null) return null;

        Camera camera = null;
        //rhys
        //DJILog.e("rhys","get product instance");
        camera = getProductInstance().getCamera();
        if (getProductInstance() instanceof Aircraft) {
            //DJILog.e("rhys","get camera instance");
            camera = ((Aircraft) getProductInstance()).getCamera();

        } else if (getProductInstance() instanceof HandHeld) {
            camera = ((HandHeld) getProductInstance()).getCamera();
        }

        return camera;
    }

    public static synchronized Gimbal getGimbalInstance() {
        if (getProductInstance() == null) return null;

        return getProductInstance().getGimbal();
    }

    public static boolean isAircraftConnected() {
        return getProductInstance() != null && getProductInstance() instanceof Aircraft;
    }

    public static boolean isHandHeldConnected() {
        return getProductInstance() != null && getProductInstance() instanceof HandHeld;
    }

    public static boolean isProductModuleAvailable() {
        return (null != AutoPatrolApplication.getProductInstance());
    }

    public static boolean isCameraModuleAvailable() {
        return isProductModuleAvailable() &&
                (null != AutoPatrolApplication.getProductInstance().getCamera());
    }

    public static boolean isPlaybackAvailable() {
        return isCameraModuleAvailable() &&
                (null != AutoPatrolApplication.getProductInstance().getCamera().getPlaybackManager());
    }

    public static List<String> getMissionList() {
        //获取默认文件夹内的所有任务名称
        File dir = new File(MISSION_DIR);
        List<String> missionList = new ArrayList<>();
        if (dir.exists()) {
            if (dir.listFiles() == null)
                return missionList;
            for (int i = 0; i < dir.listFiles().length; i++) {
                File f = dir.listFiles()[i];
                if (f.isFile()) {
                    if (f.getName().endsWith("xml")) {//xml file
                        missionList.add(f.getName().substring(0, f.getName().lastIndexOf(".")));
                    }
                }
            }
        }
        return missionList;
    }


    public void setContext(Application application) {
        instance = application;
    }

    @Override
    public Context getApplicationContext() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler(Looper.getMainLooper());
        //check alert window
        mDJIComponentListener = new BaseComponent.ComponentListener() {

            @Override
            public void onConnectivityChange(boolean isConnected) {
                notifyStatusChange();
            }

        };
        mDJIBaseProductListener = new BaseProduct.BaseProductListener() {

            @Override
            public void onComponentChange(BaseProduct.ComponentKey key, BaseComponent oldComponent, BaseComponent newComponent) {

                if (newComponent != null) {
                    newComponent.setComponentListener(mDJIComponentListener);
                }
                notifyStatusChange();
            }

            @Override
            public void onConnectivityChange(boolean isConnected) {

                notifyStatusChange();
            }

        };
        mDJISDKManagerCallback = new DJISDKManager.SDKManagerCallback() {

            //Listens to the SDK registration result
            @Override
            public void onRegister(DJIError error) {

                if (error == DJISDKError.REGISTRATION_SUCCESS) {

                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.e("autopatrol", "Register Success");
                            //Toast.makeText(getApplicationContext(), "Register Success", Toast.LENGTH_LONG).show();
                        }
                    });

                    DJISDKManager.getInstance().startConnectionToProduct();

                } else {

                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {

                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Register sdk fails, check network is available", Toast.LENGTH_LONG).show();
                        }
                    });

                }
                Log.e("TAG", error.toString());
            }

            //Listens to the connected product changing, including two parts, component changing or product connection changing.
            @Override
            public void onProductChange(BaseProduct oldProduct, BaseProduct newProduct) {

                mProduct = newProduct;
                if (mProduct != null) {
                    mProduct.setBaseProductListener(mDJIBaseProductListener);
                }

                notifyStatusChange();
            }
        };
        //Check the permissions before registering the application for android system 6.0 above.
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permissionCheck2 = ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.READ_PHONE_STATE);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || (permissionCheck == 0 && permissionCheck2 == 0)) {
            //This is used to start SDK services and initiate SDK.
            DJISDKManager.getInstance().registerApp(getApplicationContext(), mDJISDKManagerCallback);
        } else {
            Toast.makeText(getApplicationContext(), "Please check if the permission is granted.", Toast.LENGTH_LONG).show();
        }
    }

    private void notifyStatusChange() {
        mHandler.removeCallbacks(updateRunnable);
        mHandler.postDelayed(updateRunnable, 500);
    }
}
