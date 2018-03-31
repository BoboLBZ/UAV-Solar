package com.hitices.autopatrol.helper;


import org.litepal.crud.DataSupport;

import java.util.Date;
import java.util.List;

/**
 * Created by Rhys on 2018/3/26.
 * email: bozliu@outlook.com
 */

public class FlightRecordHelper {
    public static void SaveRecord(String name, Date start, Date end) {
        FlightRecords record = new FlightRecords();
        //record.setId(id);
        record.setName(name);
        record.setStartTime(start);
        record.setEndTime(end);
        record.save();
    }

    public static void SaveRecord(String name, Date start, Date end, boolean isDownload, boolean isDistributed,
                                  boolean hasVisible, boolean hasInfrared) {
        FlightRecords record = new FlightRecords();
        //record.setId(id);
        record.setName(name);
        record.setStartTime(start);
        record.setEndTime(end);
        record.setDownload(isDownload);
        record.setDistributed(isDistributed);
        record.setHasInfrared(hasInfrared);
        record.setHasVisible(hasVisible);
        record.save();
    }

    public static void readRecord(String name) {
        List<FlightRecords> records = DataSupport.findAll(FlightRecords.class);
        if (records != null) {
            for (int i = 0; i < records.size(); i++) {
                System.out.println(records.get(i).getName());
                System.out.println(records.get(i).getStartTime().toString());
            }
        }

    }
}
