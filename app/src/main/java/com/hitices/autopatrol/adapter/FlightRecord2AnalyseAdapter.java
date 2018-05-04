package com.hitices.autopatrol.adapter;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.hitices.autopatrol.R;
import com.hitices.autopatrol.activity.DataAnalyseMapActivity;
import com.hitices.autopatrol.entity.dataSupport.FlightRecord;
import com.hitices.autopatrol.helper.RecordInfoHelper;
import com.hitices.autopatrol.helper.ToastHelper;

import java.util.List;

/**
 * Created by dusz7 on 20180330.
 */

public class FlightRecord2AnalyseAdapter extends RecyclerView.Adapter<FlightRecord2AnalyseAdapter.ViewHolder> {

    private static final String TAG = FlightRecord2AnalyseAdapter.class.getName();

    private static final int READY_RECORD_ITEM = 0;
    private static final int NO_READY_RECORD_ITEM = 1;

    private Context mContext;

    //记录当前展开项的索引
    private int expandPosition = -1;

    private List<FlightRecord> flightRecordList;

    static class ViewHolder extends RecyclerView.ViewHolder {
        View flightRecordView;

        TextView missionNameText;
        TextView executeDateText;
        TextView isDownloadText;
        TextView hasVisiblePicView;
        TextView hasInfraredPicView;
        ViewGroup buttons;

        public ViewHolder(View view) {
            super(view);
            flightRecordView = view;

            missionNameText = view.findViewById(R.id.tv_mission_name);
            executeDateText = view.findViewById(R.id.tv_execute_date);
            isDownloadText = view.findViewById(R.id.tv_is_download);
            hasVisiblePicView = view.findViewById(R.id.view_hasVisible);
            hasInfraredPicView = view.findViewById(R.id.view_hasInfrared);
            buttons = view.findViewById(R.id.ll_flight_record_buttons);
        }
    }

    static class ReadyViewHolder extends ViewHolder {

        Button deleteButton;
        Button checkButton;

        public ReadyViewHolder(View view) {
            super(view);
            deleteButton = view.findViewById(R.id.btn_record_delete);
            checkButton = view.findViewById(R.id.btn_record_check);
        }
    }

    static class NoReadyViewHolder extends ViewHolder {
        Button deleteButton;
        public NoReadyViewHolder(View view) {
            super(view);
            deleteButton = view.findViewById(R.id.btn_record_delete);
        }
    }

    public FlightRecord2AnalyseAdapter(List<FlightRecord> missionList) {
        this.flightRecordList = missionList;
    }

    @Override
    public int getItemViewType(int position) {
        if (flightRecordList.get(position).isDownload()) {
            return READY_RECORD_ITEM;
        }
        return NO_READY_RECORD_ITEM;
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
        mContext = parent.getContext();
        final ViewHolder holder;
        if (viewType == READY_RECORD_ITEM) {
            holder = new ReadyViewHolder(LayoutInflater.from(parent.getContext()).
                    inflate(R.layout.item_flight_record_ready, parent, false));
        } else {
            holder = new NoReadyViewHolder(LayoutInflater.from(parent.getContext()).
                    inflate(R.layout.item_flight_record_no_ready, parent, false));
        }
//        holder.flightRecordView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                int position = holder.getAdapterPosition();
//                FlightRecord selectedRecord = flightRecordList.get(position);
//                if (selectedRecord.isDownload()) {
//                    // 跳转页面
//                    Intent intent = new Intent(parent.getContext(), DataAnalyseMapActivity.class);
//                    intent.putExtra(ContextHelper.getApplicationContext().getResources().getString(R.string.selected_flight_record),
//                            selectedRecord);
//                    parent.getContext().startActivity(intent);
//                } else {
//                    // 判断是否连接无人机
//                    // 弹出提示或者开始下载
//                    BaseProduct mProduct = AutoPatrolApplication.getProductInstance();
//                    if (null != mProduct && mProduct.isConnected() &&
//                            null != mProduct.getModel() && null != mProduct.getCamera()) {
//                        Log.d(TAG, "product connected");
//
//                    } else {
//                        Log.d(TAG, "product not connect");
//
//                    }
//                }
//            }
//        });

        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        final FlightRecord record = flightRecordList.get(position);

        // bind mission to view
        holder.missionNameText.setText(record.getMissionName());
        holder.executeDateText.setText(RecordInfoHelper.getRecordStartDateShowName(record));
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

        if (expandPosition == position) {
            holder.buttons.setVisibility(View.VISIBLE);
        } else {
            holder.buttons.setVisibility(View.GONE);
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

        if (record.isDownload()) {
            holder.isDownloadText.setText("（ 已下载 ）");

            ReadyViewHolder readyViewHolder = (ReadyViewHolder) holder;
            readyViewHolder.deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    expandPosition = -1;
                    record.delete();
                    flightRecordList.remove(record);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, getItemCount());
                    ToastHelper.getInstance().showShortToast("已删除");
                }
            });
            readyViewHolder.checkButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(mContext, DataAnalyseMapActivity.class);
                    intent.putExtra(mContext.getResources().getString(R.string.selected_flight_record),
                            record);
                    mContext.startActivity(intent);
                }
            });

        } else {
            holder.isDownloadText.setText("（ 未下载 ）");

            NoReadyViewHolder noReadyViewHolder = (NoReadyViewHolder) holder;
            noReadyViewHolder.deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    expandPosition = -1;
                    record.delete();
                    flightRecordList.remove(record);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, getItemCount());
                    ToastHelper.getInstance().showShortToast("已删除");
                }
            });
        }

    }

    @Override
    public int getItemCount() {
        return flightRecordList.size();
    }
}