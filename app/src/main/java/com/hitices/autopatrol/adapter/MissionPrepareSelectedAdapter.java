package com.hitices.autopatrol.adapter;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.hitices.autopatrol.R;
import com.hitices.autopatrol.activity.MissionExecuteActivity;
import com.hitices.autopatrol.activity.MissionParametersAdjustmentActivity;
import com.hitices.autopatrol.entity.dataSupport.PatrolMission;
import com.hitices.autopatrol.helper.ContextHelper;

import java.util.List;

/**
 * Created by Rhys on 2018/4/4.
 * email: bozliu@outlook.com
 */
public class MissionPrepareSelectedAdapter extends RecyclerView.Adapter<MissionPrepareSelectedAdapter.MyViewholder> {
    private final Context context;
    private List<PatrolMission> missionList;

    public MissionPrepareSelectedAdapter(List<PatrolMission> missions, Context context) {
        this.missionList = missions;
        this.context = context;
    }

    @Override
    public MissionPrepareSelectedAdapter.MyViewholder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.mission_prepare_item, parent, false);
        final MissionPrepareSelectedAdapter.MyViewholder holder = new MissionPrepareSelectedAdapter.MyViewholder(view);
        holder.adjust.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final PatrolMission mission = missionList.get(holder.getAdapterPosition());
                Intent intent = new Intent(ContextHelper.getApplicationContext(), MissionParametersAdjustmentActivity.class);
                //intent.putExtra("NAME", mission.getName());
                intent.putExtra("PATH", mission.getFilePath());
                intent.putExtra("ID", mission.getId());
                ContextHelper.getApplicationContext().startActivity(intent);
            }
        });
        holder.execute.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                final PatrolMission mission = missionList.get(holder.getAdapterPosition());
                Intent intent = new Intent(ContextHelper.getApplicationContext(), MissionExecuteActivity.class);
                //intent.putExtra("NAME", mission.getName());
                intent.putExtra("PATH", mission.getFilePath());
                intent.putExtra("ID", mission.getId());
                ContextHelper.getApplicationContext().startActivity(intent);
            }
        });
//        holder.missionView.setOnLongClickListener(new View.OnLongClickListener() {
//            @Override
//            public boolean onLongClick(View view) {
//                final PatrolMission mission = missionList.get(holder.getAdapterPosition());
//
//         AlertDialog dialog= new AlertDialog.Builder(context)
//                        .setTitle("提示")
//                        .setMessage("确认开始执行 " + mission.getName() + " ？")
//                        .setPositiveButton("开始", new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialogInterface, int i) {
//
//                            }
//                        })
//                        .create();
//         dialog.show();
//                return false;
//            }
//        });
        return holder;
    }

    @Override
    public void onBindViewHolder(MissionPrepareSelectedAdapter.MyViewholder holder, int position) {
        PatrolMission mission = missionList.get(position);
        holder.missionName.setText(mission.getName());
        if (mission.getLastModifiedTime() != null) {
            holder.date.setText(mission.getLastModifiedTime().toString());
        }
        holder.child_num.setText(String.valueOf(mission.getChildNums()));
    }

    @Override
    public int getItemCount() {
        return missionList.size();
    }

    static class MyViewholder extends RecyclerView.ViewHolder {
        View missionView;

        TextView missionName;
        TextView date;
        TextView child_num;
        Button adjust;
        Button execute;
        public MyViewholder(View view) {
            super(view);
            missionView = view;
            missionName = view.findViewById(R.id.prepare_mission_name);
            date = view.findViewById(R.id.prepare_mission_date);
            child_num = view.findViewById(R.id.prepare_mission_child_num);
            adjust = view.findViewById(R.id.prepare_mission_adjust);
            execute = view.findViewById(R.id.prepare_mission_execute);

        }
    }
}
