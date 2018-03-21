package com.hitices.autopatrol;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.media.audiofx.Equalizer;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import android.os.Handler;

import com.amap.api.maps2d.CoordinateConverter;
import com.amap.api.maps2d.model.LatLng;

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
    public static final String missionDir = Environment.getExternalStorageDirectory().getPath() + "/AutoPatrol/TwoMissions";  //默认任务保存位置
    public static final String photoDir=Environment.getExternalStorageDirectory().getPath() + "/AutoPatrol/RawData";  //默认照片保存位置
    private static BaseProduct mProduct; //产品
    private Handler mHandler;
    private Application instance;
    //处理DJI SDK相关
    private DJISDKManager.SDKManagerCallback mDJISDKManagerCallback;
    private BaseProduct.BaseProductListener mDJIBaseProductListener;
    private BaseComponent.ComponentListener mDJIComponentListener;
    //convert
    static double x_pi = 3.14159265358979324 * 3000.0 / 180.0;
    // π
    static double pi = 3.1415926535897932384626;
    // 长半轴
    static double a = 6378245.0;
    // 扁率
    static double ee = 0.00669342162296594323;

    public void setContext(Application application) {
        instance = application;
    }

    @Override
    public Context getApplicationContext() {
        return instance;
    }

    public AutoPatrolApplication() {

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

                if(newComponent != null) {
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

                if(error == DJISDKError.REGISTRATION_SUCCESS) {

                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.e("autopatrol","Register Success");
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
                if(mProduct != null) {
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
        camera =getProductInstance().getCamera();
        if (getProductInstance() instanceof Aircraft){
            //DJILog.e("rhys","get camera instance");
            camera = ((Aircraft) getProductInstance()).getCamera();

        } else if (getProductInstance() instanceof HandHeld) {
            camera = ((HandHeld) getProductInstance()).getCamera();
        }

        return camera;
    }
    public static synchronized Gimbal getGimbalInstance(){
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
    public static List<String> getMissionList(){
        //获取默认文件夹内的所有任务名称
        File dir=new File(missionDir);
        List<String> missionList =new ArrayList<>();
        if(dir.exists()){
            if(dir.listFiles() == null)
                return missionList;
            for(int i=0;i<dir.listFiles().length;i++){
                File f=dir.listFiles()[i];
                if(f.isFile()){
                    if(f.getName().endsWith("xml")) {//xml file
                        missionList.add(f.getName().substring(0,f.getName().lastIndexOf(".")));
                    }
                }
            }
        }
        return missionList;
    }
    private void notifyStatusChange() {
        mHandler.removeCallbacks(updateRunnable);
        mHandler.postDelayed(updateRunnable, 500);
    }

    /**
     * 将GPS坐标系坐标转换成国内坐标系
     * @return
     */
    public static LatLng WGS84ConvertToAmap(LatLng sourceLatLng){
        CoordinateConverter converter  = new CoordinateConverter();
        converter.from(CoordinateConverter.CoordType.GPS);
        converter.coord(sourceLatLng);
        return converter.convert();
    }
    public static LatLng AmapConvertToWGS84(LatLng sourceLatLng){
        double dlat = transformlat(sourceLatLng.longitude - 105.0, sourceLatLng.latitude - 35.0);
        double dlng = transformlng(sourceLatLng.longitude - 105.0, sourceLatLng.latitude - 35.0);
        double radlat = sourceLatLng.latitude / 180.0 * pi;
        double magic = Math.sin(radlat);
        magic = 1 - ee * magic * magic;
        double sqrtmagic = Math.sqrt(magic);
        dlat = (dlat * 180.0) / ((a * (1 - ee)) / (magic * sqrtmagic) * pi);
        dlng = (dlng * 180.0) / (a / sqrtmagic * Math.cos(radlat) * pi);
        double mglat = sourceLatLng.latitude + dlat;
        double mglng = sourceLatLng.longitude + dlng;
        return new LatLng(sourceLatLng.latitude * 2 - mglat,sourceLatLng.longitude * 2 - mglng );
    }

    /**
     * 维度转换
     * @param lng
     * @param lat
     * @return
     */
    private static double transformlat(double lng, double lat) {
        double ret = -100.0 + 2.0 * lng + 3.0 * lat + 0.2 * lat * lat + 0.1 * lng * lat + 0.2 * Math.sqrt(Math.abs(lng));
        ret += (20.0 * Math.sin(6.0 * lng * pi) + 20.0 * Math.sin(2.0 * lng * pi)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(lat * pi) + 40.0 * Math.sin(lat / 3.0 * pi)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(lat / 12.0 * pi) + 320 * Math.sin(lat * pi / 30.0)) * 2.0 / 3.0;
        return ret;
    }

    /**
     * 经度转换
     *
     * @param lng
     * @param lat
     * @return
     */
    private static double transformlng(double lng, double lat) {
        double ret = 300.0 + lng + 2.0 * lat + 0.1 * lng * lng + 0.1 * lng * lat + 0.1 * Math.sqrt(Math.abs(lng));
        ret += (20.0 * Math.sin(6.0 * lng * pi) + 20.0 * Math.sin(2.0 * lng * pi)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(lng * pi) + 40.0 * Math.sin(lng / 3.0 * pi)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(lng / 12.0 * pi) + 300.0 * Math.sin(lng / 30.0 * pi)) * 2.0 / 3.0;
        return ret;
    }
    private Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent(FLAG_CONNECTION_CHANGE);
            getApplicationContext().sendBroadcast(intent);
        }
    };
}
