package com.hitices.autopatrol.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.amap.api.maps2d.model.LatLng;
import com.hitices.autopatrol.R;

import java.util.List;

/**
 * Created by Rhys on 2018/4/8.
 * email: bozliu@outlook.com
 */
public class FlatlandSettingGridviewAdapter extends BaseAdapter {
    private LayoutInflater inflater;
    private List<LatLng> vertexs;

    public FlatlandSettingGridviewAdapter(Context context, List<LatLng> vertexs) {
        inflater = LayoutInflater.from(context);
        this.vertexs = vertexs;
    }

    @Override
    public int getCount() {
        return vertexs.size();
    }

    @Override
    public Object getItem(int position) {
        return vertexs.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = inflater.inflate(R.layout.w_p_gv_item, null);
            holder.id = convertView.findViewById(R.id.preview_gvitem_id);
            holder.lat = convertView.findViewById(R.id.preview_gvitem_lat);
            holder.lng = convertView.findViewById(R.id.preview_gvitem_lng);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.id.setText(String.valueOf(position));
        holder.lat.setText(String.valueOf(vertexs.get(position).latitude));
        holder.lng.setText(String.valueOf(vertexs.get(position).longitude));
        return convertView;
    }

    class ViewHolder {
        TextView id, lat, lng;
    }

}
