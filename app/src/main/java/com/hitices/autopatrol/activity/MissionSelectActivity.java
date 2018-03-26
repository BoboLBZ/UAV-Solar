package com.hitices.autopatrol.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.hitices.autopatrol.R;
import com.hitices.autopatrol.helper.DJIMediaHelper;
import com.hitices.autopatrol.helper.DJIMissionMediaHelper;
import com.hitices.autopatrol.helper.FilesDownloadCompleteListener;
import com.hitices.autopatrol.helper.ToastHelper;


import dji.common.error.DJIError;
import dji.log.DJILog;
import dji.sdk.camera.FetchMediaTask;
import dji.sdk.camera.FetchMediaTaskContent;
import dji.sdk.camera.MediaFile;

// 该Activity用于选择任务，同时下载本次任务采集图片，跳转到结果展示界面
public class MissionSelectActivity extends AppCompatActivity implements View.OnClickListener {

    private final String TAG = MissionSelectActivity.class.getName();

    private DJIMissionMediaHelper missionMediaHelper;

    private MyMission selectedMission;

    private Button selectMissionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mission_select);

        ToastHelper.showShortToast("select the mission");
        initUI();
        missionMediaHelper = new DJIMissionMediaHelper(this, this, taskCallback);
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
    }

    private void initUI() {

        selectMissionButton = findViewById(R.id.btn_select_mission);
        selectMissionButton.setOnClickListener(this);
    }

    // FetchMediaTask.Callback示例
    private FetchMediaTask.Callback taskCallback = new FetchMediaTask.Callback() {
        @Override
        public void onUpdate(MediaFile file, FetchMediaTaskContent option, DJIError error) {
            if (null == error) {
//                if (option == FetchMediaTaskContent.PREVIEW) {
//                    runOnUiThread(new Runnable() {
//                        public void run() {
//                            mListAdapter.notifyDataSetChanged();
//                        }
//                    });
//                }
//                if (option == FetchMediaTaskContent.THUMBNAIL) {
//                    runOnUiThread(new Runnable() {
//                        public void run() {
//                            mListAdapter.notifyDataSetChanged();
//                        }
//                    });
//                }
            } else {
                DJILog.e(TAG, "Fetch Media Task Failed" + error.getDescription());
            }
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_select_mission:

                ToastHelper.showShortToast("start download");
                missionMediaHelper.analyseMission(selectedMission);
                missionMediaHelper.setCompleteListener(new FilesDownloadCompleteListener() {
                    @Override
                    public void onStart() {

                    }

                    @Override
                    public void onComplete() {
                        startActivity(new Intent(MissionSelectActivity.this, MissionReportActivity.class));
                    }
                });

                break;
            default:
                break;
        }
    }

}
