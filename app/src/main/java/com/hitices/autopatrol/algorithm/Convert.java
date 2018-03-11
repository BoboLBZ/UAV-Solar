package com.hitices.autopatrol.algorithm;

import com.amap.api.maps2d.AMapUtils;
import com.amap.api.maps2d.Projection;
import com.amap.api.maps2d.model.LatLng;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Rhys on 2018/3/8.
 * email: bozliu@outlook.com
 */

public class Convert {
    private LatLng standardPoint;
    private double meterPerDegreeLon,meterPerDegreeLat;
    public Convert(LatLng st1,LatLng st2){
        standardPoint=st1;
        meterPerDegreeLon=AMapUtils.calculateLineDistance(
                st1, new LatLng(st1.latitude,st2.longitude))/Math.abs(st1.longitude-st2.longitude);
        meterPerDegreeLat=AMapUtils.calculateLineDistance(
                st2, new LatLng(st1.latitude,st2.longitude))/Math.abs(st1.latitude-st2.latitude);
    }
    public List<Point> LatlngToPoint(List<LatLng> oldlist){
        List<Point> newlist=new ArrayList<>();
        for (int i = 0; i <oldlist.size() ; i++) {
            double x= oldlist.get(i).longitude-standardPoint.longitude;
            double y= oldlist.get(i).latitude-standardPoint.latitude;
            newlist.add(new Point(x*meterPerDegreeLon,y*meterPerDegreeLat));
        }
        return newlist;
    }
    public Point singleLatlngToPoint(LatLng oldPoint){
        double x= oldPoint.longitude-standardPoint.longitude;
        double y= oldPoint.latitude-standardPoint.latitude;
        return new Point(x*meterPerDegreeLon,y*meterPerDegreeLat);
    }
    public List<LatLng> PointToLatlng(List<Point> oldlist){
        List<LatLng> newlist=new ArrayList<>();
        for (int i = 0; i < oldlist.size() ; i++) {
            double lon=oldlist.get(i).getX()/meterPerDegreeLon;
            double lat=oldlist.get(i).getY()/meterPerDegreeLat;
            newlist.add(new LatLng(lat+standardPoint.latitude,lon+standardPoint.longitude));
        }
        return newlist;
    }
}
