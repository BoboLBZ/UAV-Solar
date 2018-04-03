package com.hitices.autopatrol.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.hitices.autopatrol.R;
import com.hitices.autopatrol.entity.FlightRecord;

import java.util.List;

/**
 * Created by dusz7 on 20180330.
 */

public class MissionAdapter extends RecyclerView.Adapter<MissionAdapter.ViewHolder> {

    private List<FlightRecord> mMissionList;

    static class ViewHolder extends RecyclerView.ViewHolder {
        View missionView;

        ImageView missionImg;
        TextView nameText;
        TextView dateText;
        TextView downloadText;
        TextView visiblePicView;
        TextView infraredPicView;

        public ViewHolder(View view) {
            super(view);
            missionView = view;

            missionImg = view.findViewById(R.id.image_view_mission);
            nameText = view.findViewById(R.id.text_view_mission_name);
            dateText = view.findViewById(R.id.text_view_mission_date);
            downloadText = view.findViewById(R.id.text_view_mission_is_download);
            visiblePicView = view.findViewById(R.id.view_hasVisible);
            infraredPicView = view.findViewById(R.id.view_hasInfrared);

        }
    }

    public MissionAdapter(List<FlightRecord> missionList) {
        this.mMissionList = missionList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.mission_item, parent, false);
        final ViewHolder holder = new ViewHolder(view);
        holder.missionView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = holder.getAdapterPosition();
                FlightRecord selectedMission = mMissionList.get(position);
                // do something
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
        FlightRecord mission = mMissionList.get(position);

        // bind mission to view
        // holder.view.set(mission.name)
        holder.nameText.setText(mission.getName());
        holder.dateText.setText(mission.getStartTime().toString());
        if (mission.isDownload()) {
            holder.downloadText.setText("（ 已下载 ）");
        } else {
            holder.downloadText.setText("（ 未下载 ）");
        }
        if (mission.isHasVisible()) {
            holder.visiblePicView.setVisibility(View.VISIBLE);
        } else {
            holder.visiblePicView.setVisibility(View.GONE);
        }
        if (mission.isHasInfrared()) {
            holder.infraredPicView.setVisibility(View.VISIBLE);
        } else {
            holder.infraredPicView.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return mMissionList.size();
    }
}