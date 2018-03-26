package com.hitices.autopatrol.helper;

import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.model.TileOverlay;
import com.amap.api.maps2d.model.TileOverlayOptions;
import com.amap.api.maps2d.model.TileProvider;
import com.amap.api.maps2d.model.UrlTileProvider;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Rhys on 2018/3/26.
 * email: bozliu@outlook.com
 */

public class GoogleMap {
    public static void useGoogleMapSatelliteData(AMap aMap){
        final String url = "http://mt0.google.cn/vt/lyrs=s@198&hl=zh-CN&gl=cn&src=app&x=%d&y=%d&z=%d&s=";
        TileProvider tileProvider = new UrlTileProvider(256, 256) {
            public URL getTileUrl(int x, int y, int zoom) {
                try {
                    return new URL(String.format(url, x, y, zoom));
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
        if (tileProvider != null) {
            aMap.addTileOverlay(new TileOverlayOptions()
                    .tileProvider(tileProvider)
                    .diskCacheEnabled(true)
                    .diskCacheDir("/storage/emulated/0/amap/cache")
                    .diskCacheSize(100000)
                    .memoryCacheEnabled(true)
                    .memCacheSize(100000));
        }
    }
}
