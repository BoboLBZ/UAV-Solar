package com.hitices.autopatrol.entity.missions;

import com.amap.api.maps2d.model.LatLng;

/**
 * Created by Rhys on 2018/4/4.
 * email: bozliu@outlook.com
 */
public class SlopeModel extends BaseModel {
    public SlopeModel(String name) {
        this.missionName = name;
        this.modelType = ModelType.Slope;
    }

    @Override
    public void generateExecutablePoints(LatLng formerPoint) {

    }
}
