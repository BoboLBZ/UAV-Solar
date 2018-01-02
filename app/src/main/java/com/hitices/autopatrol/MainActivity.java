package com.hitices.autopatrol;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity
                          implements RadioGroup.OnCheckedChangeListener ,
                                        AircraftFragment.OnFragmentInteractionListener,
                                        MissionFragment.OnFragmentInteractionListener,
                                        MediaFragment.OnFragmentInteractionListener,
                                        MediaListFragment.OnFragmentInteractionListener{
    //radio
    private RadioGroup tabBar;
    private RadioButton rbMission,rbAircraft,rbMedia;
    private FragmentManager fragmentManager;
    private AircraftFragment fAircraft;
    private MediaFragment fMedia;
    private MissionFragment fMission;
    //back
    private long mExitTime=0;
    private int position;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.VIBRATE,
                            android.Manifest.permission.INTERNET, android.Manifest.permission.ACCESS_WIFI_STATE,
                            android.Manifest.permission.WAKE_LOCK, android.Manifest.permission.ACCESS_COARSE_LOCATION,
                            android.Manifest.permission.ACCESS_NETWORK_STATE, android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.CHANGE_WIFI_STATE, android.Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                            android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.SYSTEM_ALERT_WINDOW,
                            android.Manifest.permission.READ_PHONE_STATE,
                    }
                    , 1);
        }
        setContentView(R.layout.activity_main);
        //fragment
        fragmentManager=getSupportFragmentManager();
        initUI();
    }
    public void initUI(){

        tabBar=findViewById(R.id.tab_bar);
        tabBar.setOnCheckedChangeListener(this);
        rbAircraft=findViewById(R.id.aircraft);
        rbMedia=findViewById(R.id.media);
        rbMission=findViewById(R.id.mission);
        rbMedia.setSelected(true);
        rbMedia.setTextColor(getResources().getColor(R.color.selected));

        position=R.id.media;
        fragmentSelected(position);
    }
    @Override
    public void onCheckedChanged(RadioGroup radioGroup,int checkedId){
        colorUnselected();
        switch (checkedId){
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
    private void colorUnselected(){
        rbAircraft.setTextColor(getResources().getColor(R.color.unselected));
        rbMission.setTextColor(getResources().getColor(R.color.unselected));
        rbMedia.setTextColor(getResources().getColor(R.color.unselected));
    }
    private void fragmentSelected(int checkId){
        position = checkId;
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        hideAllFragments(fragmentTransaction);
        fragmentTransaction = fragmentManager.beginTransaction();
        switch (checkId){
            case R.id.aircraft:
                rbAircraft.setTextColor(getResources().getColor(R.color.selected));
                if(fAircraft == null){
                    fAircraft=new AircraftFragment();
                    fragmentTransaction.add(R.id.layout_content,fAircraft,"aircraft");
                }else {
                    fragmentTransaction.show(fAircraft);
                }

                break;
            case R.id.media:
                rbMedia.setTextColor(getResources().getColor(R.color.selected));
                if(fMedia == null){
                    fMedia=new MediaFragment();
                    fragmentTransaction.add(R.id.layout_content,fMedia,"media");
                }else {
                    fragmentTransaction.show(fMedia);
                }
                break;
            case R.id.mission:
                rbMission.setTextColor(getResources().getColor(R.color.selected));
                if(fMission == null){
                    fMission=new MissionFragment();
                    fragmentTransaction.add(R.id.layout_content,fMission,"mission");
                }else {
                    fragmentTransaction.show(fMission);
                }
                break;
        }
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }
    private void hideAllFragments(FragmentTransaction transaction){
        fAircraft=(AircraftFragment)fragmentManager.findFragmentByTag("aircraft");
        fMission=(MissionFragment)fragmentManager.findFragmentByTag("mission");
        fMedia=(MediaFragment)fragmentManager.findFragmentByTag("media");
        if(fMission != null) transaction.hide(fMission);
        if(fMedia != null) transaction.hide(fMedia);
        if(fAircraft != null) transaction.hide(fAircraft);
        transaction.commit();

    }
    @Override
    public void onFragmentInteraction(Uri uri){
        //you can leave it empty
    }
    private void setResultToToast(final String msg){
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this,msg,Toast.LENGTH_LONG).show();
            }
        });
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent){
        if(keyCode == KeyEvent.KEYCODE_BACK){
            if((System.currentTimeMillis()-mExitTime) > 2000){
                setResultToToast("再按一次退出程序");
                mExitTime=System.currentTimeMillis();
            } else {
            System.exit(0);
        }
        return true;
        }
        return super.onKeyDown(keyCode,keyEvent);
    }

}
