package com.hitices.autopatrol.helper;

import android.media.ExifInterface;

import com.amap.api.maps2d.model.LatLng;
import com.hitices.autopatrol.entity.dataSupport.FlightRecord;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by dusz7 on 20180412.
 */

public class RecordImageHelper {

    public static File getRecordVisibleImagePath(FlightRecord record) {
        return new File(MissionConstraintHelper.MISSION_PHOTO_DIR + "/" +
                record.getMissionName() + "/Visible/" + RecordInfoHelper.getRecordStartDateDir(record));
    }

    public static File getRecordInfraredImagePath(FlightRecord record) {
        return new File(MissionConstraintHelper.MISSION_PHOTO_DIR + "/" +
                record.getMissionName() + "/Infrared/" + RecordInfoHelper.getRecordStartDateDir(record));
    }

    public static File getRecordTestImagePath() {
        return new File(MissionConstraintHelper.MISSION_PHOTO_DIR + "/Test");
    }

    /**
     * get gps info
     */
    public static LatLng getImageLatlng(String path) {
        double lat = 0;
        double lng = 0;

        try {
            ExifInterface exifInterface = new ExifInterface(path);
            lat = getLatlng(exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE),
                    exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF));
            lng = getLatlng(exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE),
                    exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new LatLng(lat, lng);
    }

    private static double getLatlng(String rawData, String ref) {
        String[] parts = rawData.split(",");
        String[] pair;
        pair = parts[0].split("/");
        double degrees = Double.parseDouble(pair[0].trim()) / Double.parseDouble(pair[1].trim());
        pair = parts[1].split("/");
        double minites = Double.parseDouble(pair[0].trim()) / Double.parseDouble(pair[1].trim());
        pair = parts[2].split("/");
        double secondes = Double.parseDouble(pair[0].trim()) / Double.parseDouble(pair[1].trim());
        double result = degrees + (minites / 60.0) + (secondes / 3600.0);
        if (ref.equals("S") || ref.equals("W")) {
            return -result;
        }
        return result;
    }
}
