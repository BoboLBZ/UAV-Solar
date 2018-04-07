package com.hitices.autopatrol.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.hitices.autopatrol.R;
import com.hitices.autopatrol.entity.missions.BaseModel;
import com.hitices.autopatrol.entity.missions.ModelType;

import java.util.List;

/**
 * Created by Rhys on 2018/4/5.
 * email: bozliu@outlook.com
 */
public class ChildMissionListAdapter extends RecyclerView.Adapter<ChildMissionListAdapter.ViewHolder> {
    private List<BaseModel> modelList;

    public ChildMissionListAdapter(List<BaseModel> list) {
        this.modelList = list;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.mission_main_list_item, parent, false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        BaseModel model = modelList.get(position);
        ModelType type = model.getModelType();
        holder.nameText.setText(model.getMissionName());
        holder.typeText.setText(type.toString());
        switch (type) {
            case Slope:
                break;
            case Flatland:
                break;
            case MultiPoints:
                break;
            default:
                break;
        }

    }

    @Override
    public int getItemCount() {
        return modelList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        View view;

        ImageView missionImg;
        TextView nameText;
        TextView typeText;

        public ViewHolder(View view) {
            super(view);
            this.view = view;
            missionImg = view.findViewById(R.id.main_list_mission_image);
            nameText = view.findViewById(R.id.main_list_mission_name);
            typeText = view.findViewById(R.id.main_list_mission_type);
        }
    }
}
