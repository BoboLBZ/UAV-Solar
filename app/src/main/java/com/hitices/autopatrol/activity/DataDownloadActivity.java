package com.hitices.autopatrol.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import com.hitices.autopatrol.R;
import com.hitices.autopatrol.adapter.FlightRecord2DownloadAdapter;
import com.hitices.autopatrol.entity.dataSupport.FlightRecord;
import com.hitices.autopatrol.helper.ContextHelper;
import com.hitices.autopatrol.helper.RecordImageDownloadHelper;
import com.hitices.autopatrol.helper.ToastHelper;

import org.litepal.crud.DataSupport;

import java.util.List;

public class DataDownloadActivity extends AppCompatActivity {

    private static final String TAG = DataDownloadActivity.class.getName();

    private List<FlightRecord> flightRecordList;

    private Toolbar toolbar;
    private RecyclerView flightRecordsRecycleView;
    FlightRecord2DownloadAdapter adapter;

    private RecordImageDownloadHelper downloadHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_download);

        downloadHelper = new RecordImageDownloadHelper(this, this);
        updateMissionList();
        initUI();
    }

    private void initUI() {
        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getResources().getString(R.string.record_download_title));
        setSupportActionBar(toolbar);

        flightRecordsRecycleView = findViewById(R.id.lv_flight_records);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        flightRecordsRecycleView.setLayoutManager(layoutManager);
        adapter = new FlightRecord2DownloadAdapter(this, flightRecordList);
        flightRecordsRecycleView.setAdapter(adapter);
    }

    private void updateMissionList() {
        String notDownload = "0";
        flightRecordList = DataSupport.where("isDownload=?", notDownload)
                .order("startTime desc").find(FlightRecord.class);

    }

    public void downloadSelectedRecordImg(final FlightRecord record, final int position) {
        downloadHelper.downloadFilesByMissionRecord(record, new RecordImageDownloadHelper.FilesDownloadCompleteListener() {
            @Override
            public void onComplete() {
                ToastHelper.getInstance().showShortToast("下载完成");
                record.setDownload(true);
                record.save();
                flightRecordList.remove(record);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyItemRemoved(position);
                        adapter.notifyItemRangeChanged(position, flightRecordList.size());
                    }
                });
            }
        });
    }
}
