package com.hitices.autopatrol.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.hitices.autopatrol.R;
import com.hitices.autopatrol.adapter.MissionAdapter;
import com.hitices.autopatrol.entity.dataSupport.FlightRecord;
import com.hitices.autopatrol.helper.ToastHelper;


import org.litepal.crud.DataSupport;

import java.util.List;

import dji.common.error.DJIError;
import dji.log.DJILog;
import dji.sdk.camera.FetchMediaTask;
import dji.sdk.camera.FetchMediaTaskContent;
import dji.sdk.camera.MediaFile;

// 该Activity用于选择任务，同时下载本次任务采集图片，跳转到结果展示界面
public class DataAnalyseActivity extends AppCompatActivity implements View.OnClickListener {

    private final String TAG = DataAnalyseActivity.class.getName();

    private List<FlightRecord> flightRecordList;

    private RecyclerView flightRecordsRecycleView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_analyse);

        ToastHelper.getInstance().showShortToast("select the mission");
//        missionMediaHelper = new DJIMissionMediaHelper(this, this, taskCallback);
        initMissionList();
        initUI();
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
    }

    private void initMissionList() {

//        genMissions();
        flightRecordList = DataSupport.findAll(FlightRecord.class);

    }

//    private void genMissions() {
//
//        FlightRecordHelper.SaveRecord("屋顶1", new Date(118, 2, 16, 13, 11),
//                new Date(), false, true, true, false);
//        FlightRecordHelper.SaveRecord("乳山巡航1", new Date(118, 2, 15, 14, 21),
//                new Date(), false, false, true, false);
//        FlightRecordHelper.SaveRecord("乳山巡航", new Date(118, 2, 14, 11, 11),
//                new Date(), true, false, true, true);
//        FlightRecordHelper.SaveRecord("屋顶", new Date(118, 2, 10, 10, 5),
//                new Date(), false, true, true, false);
//    }

    private void initUI() {

        flightRecordsRecycleView = findViewById(R.id.lv_flight_records);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        flightRecordsRecycleView.setLayoutManager(layoutManager);
        MissionAdapter adapter = new MissionAdapter(flightRecordList);
        flightRecordsRecycleView.setAdapter(adapter);
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
//            case R.id.btn_select_mission:
//
//                ToastHelper.showShortToast("start download");
//
//                selectedMission = DataSupport.findLast(FlightRecord.class);
//
//                missionMediaHelper.analyseMission(selectedMission);
//                missionMediaHelper.setCompleteListener(new DJIMissionMediaHelper.FilesDownloadCompleteListener() {
//                    @Override
//                    public void onStart() {
//
//                    }
//
//                    @Override
//                    public void onComplete() {
//                        Intent intent = new Intent(DataAnalyseActivity.this, MissionReportActivity.class);
//                        intent.putExtra("path", missionMediaHelper.getMissionDir().getPath());
//                        startActivity(intent);
//                    }
//                });
//
//                missionMediaHelper.downloadFilesByMission();
//
//                break;
            default:
                break;
        }
    }

}
