package com.hitices.autopatrol.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.hitices.autopatrol.R;
import com.hitices.autopatrol.activity.DataDownloadActivity;
import com.hitices.autopatrol.entity.dataSupport.FlightRecord;
import com.hitices.autopatrol.helper.RecordImageDownloadHelper;
import com.hitices.autopatrol.helper.ToastHelper;

import java.util.List;

/**
 * Created by dusz7 on 20180412.
 */

public class FlightRecord2DownloadAdapter extends RecyclerView.Adapter<FlightRecord2DownloadAdapter.ViewHolder> {

    private static final String TAG = FlightRecord2DownloadAdapter.class.getName();

    private List<FlightRecord> flightRecordList;
    private DataDownloadActivity activity;

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

    public FlightRecord2DownloadAdapter(DataDownloadActivity activity, List<FlightRecord> missionList) {
        this.activity = activity;
        this.flightRecordList = missionList;
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_flight_record, parent, false);
        final FlightRecord2DownloadAdapter.ViewHolder holder = new FlightRecord2DownloadAdapter.ViewHolder(view);
        holder.flightRecordView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = holder.getAdapterPosition();
                final FlightRecord selectedRecord = flightRecordList.get(position);
                if (null != activity) {
                    activity.downloadSelectedRecordImg(selectedRecord);
                }
            }
        });

        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        FlightRecord record = flightRecordList.get(position);

        // bind mission to view
        holder.missionNameText.setText(record.getExecuteMission().getName());
        holder.executeDateText.setText(record.getStartTime().toString());
        holder.isDownloadText.setVisibility(View.GONE);
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
