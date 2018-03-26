package com.hitices.autopatrol.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.hitices.autopatrol.R;

public class DJIDefaultLayoutActivity extends AppCompatActivity implements View.OnClickListener {

    private Button missionTestButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dji_default_layout);

        missionTestButton = findViewById(R.id.btn_media_manage_test);
        missionTestButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_media_manage_test:
                startActivity(new Intent(this, DJIMediaManageActivity.class));
                break;
            default:
                break;
        }
    }
}
