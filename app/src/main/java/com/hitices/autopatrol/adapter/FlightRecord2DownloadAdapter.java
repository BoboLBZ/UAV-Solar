package com.hitices.autopatrol.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.hitices.autopatrol.R;
import com.hitices.autopatrol.activity.DataDownloadActivity;
import com.hitices.autopatrol.entity.dataSupport.FlightRecord;
import com.hitices.autopatrol.helper.RecordInfoHelper;

import java.util.List;

/**
 * Created by dusz7 on 20180412.
 */

public class FlightRecord2DownloadAdapter extends RecyclerView.Adapter<FlightRecord2DownloadAdapter.ViewHolder> {

    private static final String TAG = FlightRecord2DownloadAdapter.class.getName();

    //记录当前展开项的索引
    private int expandPosition = -1;

    private List<FlightRecord> flightRecordList;
    private DataDownloadActivity activity;

    static class ViewHolder extends RecyclerView.ViewHolder {
        View flightRecordView;

        TextView missionNameText;
        TextView executeDateText;

        ViewGroup recordFuncButtons;
        Button recordDownloadButton;

        public ViewHolder(View view) {
            super(view);
            flightRecordView = view;

            missionNameText = view.findViewById(R.id.tv_mission_name);
            executeDateText = view.findViewById(R.id.tv_execute_date);

            recordFuncButtons = view.findViewById(R.id.ll_flight_record_buttons);
            recordDownloadButton = view.findViewById(R.id.btn_record_download);

        }
    }

    public FlightRecord2DownloadAdapter(DataDownloadActivity activity, List<FlightRecord> missionList) {
        this.activity = activity;
        this.flightRecordList = missionList;
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_flight_record_to_download, parent, false);
        final FlightRecord2DownloadAdapter.ViewHolder holder = new FlightRecord2DownloadAdapter.ViewHolder(view);

        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        FlightRecord record = flightRecordList.get(position);

        // bind mission to view
        holder.missionNameText.setText(record.getMissionName());
        holder.executeDateText.setText(RecordInfoHelper.getRecordStartDateShowName(record));

        if (expandPosition == position) {
            holder.recordFuncButtons.setVisibility(View.VISIBLE);
        } else {
            holder.recordFuncButtons.setVisibility(View.GONE);
        }

        holder.flightRecordView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int lastExpandPosition = expandPosition;
                if (lastExpandPosition == position) {
                    expandPosition = -1;
                } else {
                    expandPosition = position;
                }
                notifyItemChanged(position);
                if (lastExpandPosition != -1) {
                    notifyItemChanged(lastExpandPosition);
                }
            }
        });
        holder.recordDownloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final FlightRecord selectedRecord = flightRecordList.get(position);

                if (null != activity) {
                    activity.downloadSelectedRecordImg(selectedRecord);
                }
            }
        });
    }


    @Override
    public int getItemCount() {
        return flightRecordList.size();
    }
}
