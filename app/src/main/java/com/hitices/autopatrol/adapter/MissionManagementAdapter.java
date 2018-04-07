package com.hitices.autopatrol.adapter;

import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.hitices.autopatrol.R;
import com.hitices.autopatrol.activity.MissionMainActivity;
import com.hitices.autopatrol.entity.dataSupport.PatrolMission;
import com.hitices.autopatrol.helper.ContextHelper;

import java.util.List;

/**
 * Created by Rhys on 2018/4/4.
 * email: bozliu@outlook.com
 */
public class MissionManagementAdapter extends RecyclerView.Adapter<MissionManagementAdapter.MyViewholder> {
    private List<PatrolMission> missionList;

    public MissionManagementAdapter(List<PatrolMission> missions) {
        this.missionList = missions;
    }

    @Override
    public MissionManagementAdapter.MyViewholder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.mission_management_item, parent, false);
        final MissionManagementAdapter.MyViewholder holder = new MissionManagementAdapter.MyViewholder(view);
        holder.missionView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = holder.getAdapterPosition();
                PatrolMission mission = missionList.get(position);
                // do something
            }
        });

        // choose adjust function
        holder.btn_adjust.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //修改当前任务
                int position = holder.getAdapterPosition();
                PatrolMission mission = missionList.get(position);

                Intent intent = new Intent(ContextHelper.getApplicationContext(), MissionMainActivity.class);
                intent.putExtra("TYPE", "modify");
                intent.putExtra("PTAH", mission.getFilePath());
                intent.putExtra("ID", mission.getId());
                ContextHelper.getApplicationContext().startActivity(intent);
            }
        });

        return holder;
    }

    @Override
    public void onBindViewHolder(MissionManagementAdapter.MyViewholder holder, int position) {
        PatrolMission mission = missionList.get(position);
        holder.missionName.setText(mission.getName());
        if (mission.getLastModifiedTime() != null) {
            holder.date.setText(mission.getLastModifiedTime().toString());
        }
    }

    @Override
    public int getItemCount() {
        return missionList.size();
    }

    static class MyViewholder extends RecyclerView.ViewHolder {
        View missionView;

        TextView missionName;
        TextView date;
        Button btn_adjust;

        public MyViewholder(View view) {
            super(view);
            missionView = view;
            missionName = view.findViewById(R.id.management_mission_name);
            date = view.findViewById(R.id.management_mission_date);
            btn_adjust = view.findViewById(R.id.management_mission_adjust);
        }
    }
}
