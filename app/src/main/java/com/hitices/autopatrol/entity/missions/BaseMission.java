package com.hitices.autopatrol.entity.missions;

/**
 * Created by Rhys on 2018/1/28.
 * email: bozliu@outlook.com
 * BaseMission为基类，定义公用参数和接口
 */

public abstract class BaseMission {
    public String missionName; //任务名称
    public MissionType missionType; //任务类型
    public boolean FLAG_ISSAVED;  //任务是否保存标志变量

    public BaseMission(String name, MissionType type) {
        //初始化，需指明 名称和类型
        this.missionName = name;
        this.missionType = type;
    }

    public BaseMission() {
    }

    public abstract boolean saveMission(); //接口，必须实现
}
