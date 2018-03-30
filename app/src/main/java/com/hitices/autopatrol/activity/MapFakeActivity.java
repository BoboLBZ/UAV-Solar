package com.hitices.autopatrol.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.LinearLayout;

import com.hitices.autopatrol.R;

public class MapFakeActivity extends AppCompatActivity {

    LinearLayout buttonsLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_fake);
        buttonsLayout = findViewById(R.id.layout_buttons);
        buttonsLayout.bringToFront();
    }
}
