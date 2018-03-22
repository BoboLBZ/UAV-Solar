package com.hitices.autopatrol.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.hitices.autopatrol.R;

public class MissionReportActivity extends AppCompatActivity {
    private TextView name, type, photo_nums;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mission_report);
        initUI();
        setContent();
    }

    private void initUI() {
        name = findViewById(R.id.report_mission_name);
        type = findViewById(R.id.report_mission_type);
        photo_nums = findViewById(R.id.report_mission_photo_nums);
    }

    private void setContent() {
        Intent intent = getIntent();
        name.setText(intent.getStringExtra("name"));
        type.setText(intent.getStringExtra("type"));
        photo_nums.setText(intent.getStringExtra("photoNums"));
    }
}
