package com.hitices.autopatrol.helper;

import com.hitices.autopatrol.entity.dataSupport.FlightRecord;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by dusz7 on 20180412.
 */

public class RecordInfoHelper {

    public static String getRecordStartDateShowName(FlightRecord record) {
        Date startDate = record.getStartTime();
        SimpleDateFormat format = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
        String dir = format.format(startDate);
        return dir;
    }

    public static String getRecordStartDateDir(FlightRecord record) {
        Date startDate = record.getStartTime();
        SimpleDateFormat format = new SimpleDateFormat("YYYYMMddHHmmss");
        String dir = format.format(startDate);
        return dir;
    }
}
