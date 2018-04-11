package com.hitices.autopatrol.helper;

import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CoordinateConverter;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.TileOverlayOptions;
import com.amap.api.maps2d.model.UrlTileProvider;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Rhys on 2018/3/26.
 * email: bozliu@outlook.com
 */

public class GoogleMapHelper {
    //convert
    static double x_pi = 3.14159265358979324 * 3000.0 / 180.0;
    // π
    static double pi = 3.1415926535897932384626;
    // 长半轴
    static double a = 6378245.0;
    // 扁率
    static double ee = 0.00669342162296594323;

    public static void useGoogleMapSatelliteData(AMap aMap){
        final String url = "http://mt3.google.cn/vt/lyrs=s@198&hl=zh-CN&gl=cn&src=app&x=%d&y=%d&z=%d&s=";
        UrlTileProvider tileProvider = new UrlTileProvider(256, 256) {
            public URL getTileUrl(int x, int y, int zoom) {
                try {
                    return new URL(String.format(url, x, y, zoom));
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
        aMap.addTileOverlay(new TileOverlayOptions()
                .tileProvider(tileProvider)
                .diskCacheEnabled(true)
                .diskCacheDir("/storage/emulated/0/amap/cache")
                .diskCacheSize(100000)
                .memoryCacheEnabled(true)
                .memCacheSize(100000));
    }

    /**
     * 将GPS坐标系坐标转换成国内坐标系
     *
     * @return
     */
    public static LatLng WGS84ConvertToAmap(LatLng sourceLatLng) {
        CoordinateConverter converter = new CoordinateConverter();
        converter.from(CoordinateConverter.CoordType.GPS);
        converter.coord(sourceLatLng);
        return converter.convert();
    }

    public static LatLng AmapConvertToWGS84(LatLng sourceLatLng) {
        double dlat = transformlat(sourceLatLng.longitude - 105.0, sourceLatLng.latitude - 35.0);
        double dlng = transformlng(sourceLatLng.longitude - 105.0, sourceLatLng.latitude - 35.0);
        double radlat = sourceLatLng.latitude / 180.0 * pi;
        double magic = Math.sin(radlat);
        magic = 1 - ee * magic * magic;
        double sqrtmagic = Math.sqrt(magic);
        dlat = (dlat * 180.0) / ((a * (1 - ee)) / (magic * sqrtmagic) * pi);
        dlng = (dlng * 180.0) / (a / sqrtmagic * Math.cos(radlat) * pi);
        double mglat = sourceLatLng.latitude + dlat;
        double mglng = sourceLatLng.longitude + dlng;
        return new LatLng(sourceLatLng.latitude * 2 - mglat, sourceLatLng.longitude * 2 - mglng);
    }

    /**
     * 维度转换
     *
     * @param lng
     * @param lat
     * @return
     */
    private static double transformlat(double lng, double lat) {
        double ret = -100.0 + 2.0 * lng + 3.0 * lat + 0.2 * lat * lat + 0.1 * lng * lat + 0.2 * Math.sqrt(Math.abs(lng));
        ret += (20.0 * Math.sin(6.0 * lng * pi) + 20.0 * Math.sin(2.0 * lng * pi)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(lat * pi) + 40.0 * Math.sin(lat / 3.0 * pi)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(lat / 12.0 * pi) + 320 * Math.sin(lat * pi / 30.0)) * 2.0 / 3.0;
        return ret;
    }

    /**
     * 经度转换
     *
     * @param lng
     * @param lat
     * @return
     */
    private static double transformlng(double lng, double lat) {
        double ret = 300.0 + lng + 2.0 * lat + 0.1 * lng * lng + 0.1 * lng * lat + 0.1 * Math.sqrt(Math.abs(lng));
        ret += (20.0 * Math.sin(6.0 * lng * pi) + 20.0 * Math.sin(2.0 * lng * pi)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(lng * pi) + 40.0 * Math.sin(lng / 3.0 * pi)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(lng / 12.0 * pi) + 300.0 * Math.sin(lng / 30.0 * pi)) * 2.0 / 3.0;
        return ret;
    }

}
