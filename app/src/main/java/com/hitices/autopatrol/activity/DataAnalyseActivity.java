package com.hitices.autopatrol.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.hitices.autopatrol.R;
import com.hitices.autopatrol.adapter.FlightRecord2AnalyseAdapter;
import com.hitices.autopatrol.entity.dataSupport.FlightRecord;
import com.hitices.autopatrol.entity.dataSupport.PatrolMission;
import com.hitices.autopatrol.helper.ToastHelper;


import org.litepal.crud.DataSupport;

import java.util.Date;
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

        if (DataSupport.findAll(FlightRecord.class).size() == 0) {
            genMissions();
        }

        flightRecordList = DataSupport.findAll(FlightRecord.class);

    }

    private void genMissions() {

        PatrolMission mission1 = new PatrolMission();
        mission1.setName("mission1");
        mission1.save();
        PatrolMission mission2 = new PatrolMission();
        mission2.setName("mission2");
        mission2.save();
        PatrolMission mission3 = new PatrolMission();
        mission3.setName("mission3");
        mission3.save();

        FlightRecord record1 = new FlightRecord();
        record1.setMission(mission1);
        record1.setStartTime(new Date());
        record1.setEndTime(new Date());
        record1.setHasInfrared(true);
        record1.save();

        FlightRecord record2 = new FlightRecord();
        record2.setMission(mission1);
        record2.setStartTime(new Date());
        record2.setEndTime(new Date());
        record2.setDownload(true);
        record2.setHasVisible(true);
        record2.setHasInfrared(true);
        record2.save();

        FlightRecord record3 = new FlightRecord();
        record3.setMission(mission2);
        record3.setStartTime(new Date());
        record3.setEndTime(new Date());
        record3.save();

        FlightRecord record4 = new FlightRecord();
        record4.setMission(mission3);
        record4.setStartTime(new Date());
        record4.setEndTime(new Date());
        record4.setDownload(true);
        record4.save();

        FlightRecord record5 = new FlightRecord();
        record5.setMission(mission2);
        record5.setStartTime(new Date());
        record5.setEndTime(new Date());
        record5.save();
    }

    private void initUI() {

        flightRecordsRecycleView = findViewById(R.id.lv_flight_records);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        flightRecordsRecycleView.setLayoutManager(layoutManager);
        FlightRecord2AnalyseAdapter adapter = new FlightRecord2AnalyseAdapter(flightRecordList);
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
