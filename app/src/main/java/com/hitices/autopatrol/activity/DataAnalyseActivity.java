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

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import dji.common.error.DJIError;
import dji.log.DJILog;
import dji.sdk.camera.FetchMediaTask;
import dji.sdk.camera.FetchMediaTaskContent;
import dji.sdk.camera.MediaFile;

// 该Activity用于选择任务，同时下载本次任务采集图片，跳转到结果展示界面
public class DataAnalyseActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = DataAnalyseActivity.class.getName();

    private List<FlightRecord> flightRecordList;

    private RecyclerView flightRecordsRecycleView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_analyse);

        ToastHelper.getInstance().showShortToast("select the mission");
        initMissionList();
        initUI();
        initDialog();
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

        flightRecordList = DataSupport.where("isDownload=?","1")
                .order("startTime desc")
                .find(FlightRecord.class);
        flightRecordList.addAll(DataSupport.where("isDownload=?","0")
                .order("startTime desc")
                .find(FlightRecord.class));
    }

    private void genMissions() {

        FlightRecord record1 = new FlightRecord();
        record1.setMissionName("mission1");
        record1.setStartTime(new Date());
        record1.setEndTime(new Date());
        record1.setHasInfrared(true);
        record1.setDownload(true);
        record1.save();

        FlightRecord record2 = new FlightRecord();
        record2.setMissionName("mission1");
        record2.setStartTime(new Date());
        record2.setEndTime(new Date());
        record2.setDownload(true);
        record2.setHasVisible(true);
        record2.setHasInfrared(true);
        record2.save();

        FlightRecord record3 = new FlightRecord();
        record3.setMissionName("mission2");
        record3.setStartTime(new Date());
        record3.setEndTime(new Date());
        record3.setDownload(false);
        record3.setHasVisible(true);
        record3.save();

        FlightRecord record4 = new FlightRecord();
        record4.setMissionName("mission3");
        record4.setStartTime(new Date());
        record4.setEndTime(new Date());
        record4.setDownload(false);
        record4.save();

        FlightRecord record5 = new FlightRecord();
        record5.setMissionName("mission2");
        record5.setStartTime(new Date());
        record5.setEndTime(new Date());
        record5.setDownload(true);
        record5.save();

        FlightRecord record6 = new FlightRecord();
        record6.setMissionName("downloadMission");
        record6.setStartTime(new Date(2017 - 1900, 1, 1, 1, 1, 1));
        record6.setEndTime(new Date());
        record6.save();
    }

    private void initUI() {

        flightRecordsRecycleView = findViewById(R.id.lv_flight_records);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        flightRecordsRecycleView.setLayoutManager(layoutManager);
        FlightRecord2AnalyseAdapter adapter = new FlightRecord2AnalyseAdapter(flightRecordList);
        flightRecordsRecycleView.setAdapter(adapter);
    }

    /**
     * 初始化"下载提醒"等Dialog
     */
    private void initDialog() {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            default:
                break;
        }
    }

}
