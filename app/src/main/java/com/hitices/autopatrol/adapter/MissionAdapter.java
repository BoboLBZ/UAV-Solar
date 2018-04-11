package com.hitices.autopatrol.adapter;

import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.hitices.autopatrol.R;
import com.hitices.autopatrol.activity.DataAnalyseMapActivity;
import com.hitices.autopatrol.entity.dataSupport.FlightRecord;
import com.hitices.autopatrol.entity.dataSupport.PatrolMission;
import com.hitices.autopatrol.helper.ContextHelper;

import java.util.List;

/**
 * Created by dusz7 on 20180330.
 */

public class MissionAdapter extends RecyclerView.Adapter<MissionAdapter.ViewHolder> {

    private List<FlightRecord> flightRecordList;

    static class ViewHolder extends RecyclerView.ViewHolder {
        View flightRecordView;

        ImageView powerStationImg;
        TextView missionNameText;
        TextView executeDateText;
        TextView isDownloadText;
        TextView hasVisiblePicView;
        TextView hasInfraredPicView;

        public ViewHolder(View view) {
            super(view);
            flightRecordView = view;

            powerStationImg = view.findViewById(R.id.img_station_thumb);
            missionNameText = view.findViewById(R.id.tv_mission_name);
            executeDateText = view.findViewById(R.id.tv_execute_date);
            isDownloadText = view.findViewById(R.id.tv_is_download);
            hasVisiblePicView = view.findViewById(R.id.view_hasVisible);
            hasInfraredPicView = view.findViewById(R.id.view_hasInfrared);

        }
    }

    public MissionAdapter(List<FlightRecord> missionList) {
        this.flightRecordList = missionList;
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.mission_item, parent, false);
        final ViewHolder holder = new ViewHolder(view);
        holder.flightRecordView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = holder.getAdapterPosition();
                FlightRecord selectedRecord = flightRecordList.get(position);
                if (selectedRecord.isDownload()) {
                    // 跳转页面
                    Intent intent = new Intent(parent.getContext(), DataAnalyseMapActivity.class);
                    intent.putExtra(ContextHelper.getApplicationContext().getResources().getString(R.string.selected_flight_record),
                            selectedRecord);
                    parent.getContext().startActivity(intent);
                } else {
                    // 判断是否连接无人机，
                    // 弹出提示或者开始下载
                }
            }
        });

        // 局部点击
//        holder.imageView.setOnClickListener(new View.OnClickListener() {
//
//        })

        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        FlightRecord record = flightRecordList.get(position);

        // bind mission to view
        try {
            holder.missionNameText.setText(record.getMission().getName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        holder.executeDateText.setText(record.getStartTime().toString());
        if (record.isDownload()) {
            holder.isDownloadText.setText("（ 已下载 ）");
        } else {
            holder.isDownloadText.setText("（ 未下载 ）");
        }
        if (record.isHasVisible()) {
            holder.hasVisiblePicView.setVisibility(View.VISIBLE);
        } else {
            holder.hasVisiblePicView.setVisibility(View.GONE);
        }
        if (record.isHasInfrared()) {
            holder.hasInfraredPicView.setVisibility(View.VISIBLE);
        } else {
            holder.hasInfraredPicView.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return flightRecordList.size();
    }
}