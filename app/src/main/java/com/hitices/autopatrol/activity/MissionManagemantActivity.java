package com.hitices.autopatrol.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.hitices.autopatrol.R;
import com.hitices.autopatrol.adapter.MissionManagementAdapter;
import com.hitices.autopatrol.entity.dataSupport.PatrolMission;

import org.litepal.crud.DataSupport;

import java.util.List;

public class MissionManagemantActivity extends AppCompatActivity implements View.OnClickListener {
    private RecyclerView recyclerView;
    private FloatingActionButton btn_create;
    private List<PatrolMission> missionList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mission_managemant);
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
        btn_create = findViewById(R.id.btn_create_mission_management);
        btn_create.setOnClickListener(this);

        recyclerView = findViewById(R.id.recycle_view_mission_management);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(manager);
    }

    private void initAdapter() {
        getMissionList();
        MissionManagementAdapter adapter = new MissionManagementAdapter(missionList);
        recyclerView.setAdapter(adapter);
    }

    private void createMission() {
        Intent intent = new Intent(this, MissionMainActivity.class);
        intent.putExtra("TYPE", "create");
        startActivity(intent);
    }

    private void getMissionList() {
        //read from database
        missionList = DataSupport.findAll(PatrolMission.class);
        //read from xml file
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_create_mission_management:
                createMission();
                break;
        }
    }
}
