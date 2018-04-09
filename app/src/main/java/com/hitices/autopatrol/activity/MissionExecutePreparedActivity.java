package com.hitices.autopatrol.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.hitices.autopatrol.R;
import com.hitices.autopatrol.adapter.MissionPrepareSelectedAdapter;
import com.hitices.autopatrol.entity.dataSupport.PatrolMission;

import org.litepal.crud.DataSupport;

import java.util.List;

public class MissionExecutePreparedActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    private List<PatrolMission> missionList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mission_execute_prepared);
        initUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initAdapter();
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
    }

    private void initUI() {

        recyclerView = findViewById(R.id.recycle_view_mission_prepared);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(manager);
    }

    private void initAdapter() {
        getMissionList();
        MissionPrepareSelectedAdapter adapter = new MissionPrepareSelectedAdapter(missionList, this);
        recyclerView.setAdapter(adapter);
    }

    private void getMissionList() {
        //read from database
        missionList = DataSupport.findAll(PatrolMission.class);
        //read from xml file
    }
}
