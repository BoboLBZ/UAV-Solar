package com.hitices.autopatrol.helper;

import com.hitices.autopatrol.entity.dataSupport.FlightRecord;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by dusz7 on 20180412.
 */

public class RecordInfoHelper {

    public static String getRecordStartDateShowName(FlightRecord record) {
        if (null != record.getStartTime()) {
            Date startDate = record.getStartTime();
            SimpleDateFormat format = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
            String dateStr = format.format(startDate);
            return dateStr;
        }else {
            return "";
        }
    }

    public static String getRecordStartDateDir(FlightRecord record) {
        if (null != record.getStartTime()) {
            Date startDate = record.getStartTime();
            SimpleDateFormat format = new SimpleDateFormat("YYYYMMddHHmmss");
            String dir = format.format(startDate);
            return dir;
        } else {
            return "noDateName";
        }
    }
}
