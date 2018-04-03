package com.hitices.autopatrol.helper;

import android.media.ExifInterface;

import com.amap.api.maps2d.model.LatLng;

import java.io.IOException;

/**
 * Created by Rhys on 2018/3/26.
 * email: bozliu@outlook.com
 * 读取无人机拍摄照片的地理位置信息
 */

public class ImageInfoReadHelper {
    private ExifInterface exifInterface;

    public ImageInfoReadHelper(String path) {
        try {
            exifInterface = new ExifInterface(path);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * get gps info
     */
    public LatLng getLatlng() {
        double lat = getLatlng(exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE),
                exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF));
        double lng = getLatlng(exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE),
                exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF));
        return new LatLng(lat, lng);
    }

    private double getLatlng(String rawData, String ref) {
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
