package com.hitices.autopatrol.entity.missions;

/**
 * Created by Rhys on 2018/1/28.
 * email: bozliu@outlook.com
 * 接口，任务类型，waypiont 和 polygon
 */

public enum ModelType {
    MultiPoints(0),
    Flatland(1),
    Slope(2);

    ModelType(int value) {
    }
}
