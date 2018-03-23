package com.hitices.autopatrol.activity;

import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.hitices.autopatrol.R;
import com.hitices.autopatrol.fragment.AircraftFragment;
import com.hitices.autopatrol.fragment.MediaFragment;
import com.hitices.autopatrol.fragment.MediaLocalFragment;
import com.hitices.autopatrol.fragment.MediaSDFragment;
import com.hitices.autopatrol.fragment.MissionFragment;

import org.opencv.android.OpenCVLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.log.DJILog;
import dji.sdk.base.BaseProduct;
import dji.sdk.sdkmanager.DJISDKManager;

/**
 * Created by Rhys
 * email: bozliu@outlook.com
 * 主activity，控制三个fragment的显示逻辑，应用权限申请
 */
public class PatrolMainActivity extends AppCompatActivity
        implements RadioGroup.OnCheckedChangeListener,
        AircraftFragment.OnFragmentInteractionListener,
        MissionFragment.OnFragmentInteractionListener,
        MediaFragment.OnFragmentInteractionListener,
        MediaLocalFragment.OnFragmentInteractionListener,
        MediaSDFragment.OnFragmentInteractionListener {

    //权限列表
    private static final String[] REQUIRED_PERMISSION_LIST = new String[]{
            android.Manifest.permission.VIBRATE,
            android.Manifest.permission.INTERNET,
            android.Manifest.permission.ACCESS_WIFI_STATE,
            android.Manifest.permission.WAKE_LOCK,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_NETWORK_STATE,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.CHANGE_WIFI_STATE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.BLUETOOTH,
            android.Manifest.permission.BLUETOOTH_ADMIN,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_PHONE_STATE,
    };
    private static final int REQUEST_PERMISSION_CODE = 12345;
    //radio
    private RadioGroup tabBar; //通过radio button的方式控制fragment的显示
    private RadioButton rbMission, rbAircraft, rbMedia;
    private FragmentManager fragmentManager;
    private AircraftFragment fAircraft;
    private MediaFragment fMedia;
    private MissionFragment fMission;
    //back
    private int position;
    private List<String> missingPermission = new ArrayList<>();
    private AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkAndRequestPermissions();
        setContentView(R.layout.activity_patrol_main);
        //fragment
        fragmentManager = getSupportFragmentManager();
        initUI();
    }

    public void initUI() {
        tabBar = findViewById(R.id.tab_bar);
        tabBar.setOnCheckedChangeListener(this);

        rbAircraft = findViewById(R.id.aircraft);
        rbMedia = findViewById(R.id.media);
        rbMission = findViewById(R.id.mission);
        setFirstView(R.id.mission);
    }

    private void checkAndRequestPermissions() {
        // Check for permissions
        for (String eachPermission : REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(this, eachPermission) != PackageManager.PERMISSION_GRANTED) {
                missingPermission.add(eachPermission);
            }
        }
        // Request for missing permissions
        if (!missingPermission.isEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    missingPermission.toArray(new String[missingPermission.size()]),
                    REQUEST_PERMISSION_CODE);
        }

    }

    /**
     * Result of runtime permission request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Check for granted permission and remove from missing list
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = grantResults.length - 1; i >= 0; i--) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    missingPermission.remove(permissions[i]);
                }
            }
        }
        // If there is enough permission, we will start the registration
        if (missingPermission.isEmpty()) {
            startSDKRegistration();
        } else {
            setResultToToast("Missing permissions!!!");
        }
    }

    private void startSDKRegistration() {
        if (isRegistrationInProgress.compareAndSet(false, true)) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    setResultToToast("registering, pls wait...");
                    DJISDKManager.getInstance().registerApp(getApplicationContext(), new DJISDKManager.SDKManagerCallback() {
                        @Override
                        public void onRegister(DJIError djiError) {
                            if (djiError == DJISDKError.REGISTRATION_SUCCESS) {
                                DJILog.e("App registration", DJISDKError.REGISTRATION_SUCCESS.getDescription());
                                DJISDKManager.getInstance().startConnectionToProduct();
                                setResultToToast("Register Success");
                            } else {
                                setResultToToast("Register sdk fails, check network is available");
                            }
                            Log.v("my", djiError.getDescription());
                        }

                        @Override
                        public void onProductChange(BaseProduct baseProduct, BaseProduct baseProduct1) {
                            Log.d("my", String.format("onProductChanged oldProduct:%s, newProduct:%s", baseProduct, baseProduct1));
                        }
                    });
                }
            });
        }
    }

    private void setFirstView(int id) {
        //设置默认显示的fragment
        switch (id) {
            case R.id.aircraft:
                rbAircraft.setSelected(true);
                rbAircraft.setTextColor(getResources().getColor(R.color.selected));
                position = R.id.aircraft;
                fragmentSelected(position);
                break;
            case R.id.mission:
                rbMission.setSelected(true);
                rbMission.setTextColor(getResources().getColor(R.color.selected));
                position = R.id.mission;
                fragmentSelected(position);
                break;
            case R.id.media:
                rbMedia.setSelected(true);
                rbMedia.setTextColor(getResources().getColor(R.color.selected));
                position = R.id.media;
                fragmentSelected(position);
                break;
            default:
                break;
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
        colorUnselected();
        switch (checkedId) {
            case R.id.aircraft:
                rbAircraft.setTextColor(getResources().getColor(R.color.selected));
                fragmentSelected(checkedId);
                break;
            case R.id.media:
                rbMedia.setTextColor(getResources().getColor(R.color.selected));
                fragmentSelected(checkedId);
                break;
            case R.id.mission:
                fragmentSelected(checkedId);
                break;
        }
    }

    private void colorUnselected() {
        //重置button颜色
        rbAircraft.setTextColor(getResources().getColor(R.color.unselected));
        rbMission.setTextColor(getResources().getColor(R.color.unselected));
        rbMedia.setTextColor(getResources().getColor(R.color.unselected));
    }

    private void fragmentSelected(int checkId) {
        //设置显示fragment
        position = checkId;
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        hideAllFragments(fragmentTransaction);
        fragmentTransaction = fragmentManager.beginTransaction();
        switch (checkId) {
            case R.id.aircraft:
                rbAircraft.setTextColor(getResources().getColor(R.color.selected));
                if (fAircraft == null) {
                    fAircraft = new AircraftFragment();
                    fragmentTransaction.add(R.id.layout_content, fAircraft, "aircraft");
                } else {
                    fragmentTransaction.show(fAircraft);
                }

                break;
            case R.id.media:
                rbMedia.setTextColor(getResources().getColor(R.color.selected));
                if (fMedia == null) {
                    fMedia = new MediaFragment();
                    fragmentTransaction.add(R.id.layout_content, fMedia, "media");
                } else {
                    fragmentTransaction.show(fMedia);
                }
                break;
            case R.id.mission:
                rbMission.setTextColor(getResources().getColor(R.color.selected));
                if (fMission == null) {
                    fMission = new MissionFragment();
                    fragmentTransaction.add(R.id.layout_content, fMission, "mission");
                } else {
                    fragmentTransaction.show(fMission);
                }
                break;
        }
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void hideAllFragments(FragmentTransaction transaction) {
        fAircraft = (AircraftFragment) fragmentManager.findFragmentByTag("aircraft");
        fMission = (MissionFragment) fragmentManager.findFragmentByTag("mission");
        fMedia = (MediaFragment) fragmentManager.findFragmentByTag("media");
        if (fMission != null) transaction.hide(fMission);
        if (fMedia != null) transaction.hide(fMedia);
        if (fAircraft != null) transaction.hide(fAircraft);
        transaction.commit();

    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        //you can leave it empty
    }

    private void setResultToToast(final String msg) {
        PatrolMainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(PatrolMainActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

}
