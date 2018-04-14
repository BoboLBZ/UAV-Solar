package com.hitices.autopatrol.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import com.hitices.autopatrol.helper.MissionHelper;

import java.util.List;

/**
 * Created by Rhys on 2018/4/4.
 * email: bozliu@outlook.com
 */
public class MissionManagementAdapter extends RecyclerView.Adapter<MissionManagementAdapter.MyViewholder> {
    private List<PatrolMission> missionList;
    private Context context;

    public MissionManagementAdapter(List<PatrolMission> missions, Context context) {
        this.missionList = missions;
        this.context = context;
    }
    @Override
    public MissionManagementAdapter.MyViewholder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.mission_management_item, parent, false);
        final MissionManagementAdapter.MyViewholder holder = new MissionManagementAdapter.MyViewholder(view);
        holder.missionView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                int position = holder.getAdapterPosition();
                //deleteMission(position);
                return false;
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
        holder.btn_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //删除当前任务
                new AlertDialog.Builder(context)
                        .setTitle("提醒")
                        .setMessage("确认删除该任务？")
                        .setPositiveButton("是", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                deleteMission(holder.getAdapterPosition());
                            }
                        })
                        .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                            }
                        }).create().show();
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

    private void deleteMission(int position) {
        PatrolMission mission = missionList.get(position);
        MissionHelper.deleteMission(mission, mission.getFilePath());//delete from SD and database
        //mission.deleteAsync();
        missionList.remove(position);
        notifyDataSetChanged();
    }
    static class MyViewholder extends RecyclerView.ViewHolder {
        View missionView;

        TextView missionName;
        TextView date;
        Button btn_adjust;
        Button btn_delete;

        public MyViewholder(View view) {
            super(view);
            missionView = view;
            missionName = view.findViewById(R.id.management_mission_name);
            date = view.findViewById(R.id.management_mission_date);
            btn_adjust = view.findViewById(R.id.management_mission_adjust);
            btn_delete = view.findViewById(R.id.management_mission_delete);
        }
    }
}
