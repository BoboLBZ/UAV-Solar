package com.hitices.autopatrol.missions;

/**
 * Created by Rhys on 2018/1/28.
 * email: bozliu@outlook.com
 * 接口，任务类型，waypiont 和 polygon
 */

public enum MissionType {
    WaypointMission(0),
    PolygonMission(1);

    MissionType(int value) {
    }
}
