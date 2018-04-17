package com.hitices.autopatrol.algorithm;

import android.support.annotation.NonNull;

import com.amap.api.maps2d.model.LatLng;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Created by Rhys on 2018/4/16.
 * email: bozliu@outlook.com
 */
public class SlopePathPlanningAlgorithm {
    private Point[] points;
    private double width;
    private Point lowPoint;
    private Point highPoint;
    private Convert convert;
    private double maxWidth;

    public SlopePathPlanningAlgorithm(
            @NonNull List<LatLng> vertexs,
            @NonNull double width,
            @NonNull LatLng lineA,
            @NonNull LatLng lineB) {
        convert = new Convert(vertexs.get(0), vertexs.get(1));
        ConvexHull convexHull = new ConvexHull(convert.LatlngToPoint(vertexs));

        points = convexHull.getTubaoPoint();
        this.width = width;
        this.lowPoint = convert.singleLatlngToPoint(lineA);
        this.highPoint = convert.singleLatlngToPoint(lineB);
    }

    public List<Point> generateWaypoints(float low, double high) {
        List<Point> cutPoints = getCuttingPoints();
        //List<Waypoint> result=new ArrayList<>();
        List<Point> result = new ArrayList<>();
        double sinTheta = Math.abs(high - low) / Math.sqrt(Math.pow((high - low), 2) + Math.pow(getDistance(highPoint, lowPoint), 2));
        double h = width * sinTheta;
        for (int i = 0; i < cutPoints.size(); i = i + 2) {
            Point start = cutPoints.get(i);
            double alt = h * start.getArCos() + h / 2;
            start.setArCos(alt);   //这里arc存储的是相对于最低点的相对高度
            result.add(start);
            if (i + 1 < cutPoints.size()) {
                //remain to modify,here are some bugs
                Point end = cutPoints.get(i + 1);
                double dis = getDistance(start, end);
                double cos = (end.getX() - start.getX()) / dis;
                double sin = (end.getY() - start.getY()) / dis;
                double interval = width;
                while (interval < dis) {
                    result.add(new Point(start.getX() + interval * cos, start.getY() + interval * sin, alt));
                    interval += width;
                }
                if (interval > dis && interval - dis < width / 2) {
                    end.setArCos(alt);
                    result.add(end);
                }
            }
        }
        List<LatLng> temp = convert.PointToLatlng(result);
        for (int i = 0; i < result.size(); i++) {
            Point point = result.get(i);
            LatLng latLng = temp.get(i);
            point.setX(latLng.latitude);
            point.setY(latLng.longitude);
        }
        return result;
    }

    private void reorganizePoints() {
        double[] baseVector = {highPoint.getX() - lowPoint.getX(), highPoint.getY() - lowPoint.getY()};
        double[] projectionOnBaseline = new double[points.length - 1];
        //重新组织顺序，把边界最低点放在首位；-1 是因为points中的点构成闭环，最后一个点和最开始的点是同一个点
        for (int i = 0; i < points.length - 1; i++) {
            double[] vertor = {points[i].getX() - lowPoint.getX(), points[i].getY() - lowPoint.getY()};
            //用内积取代在baseVector方向上投影长度(带方向)，因为baseline是固定的
            projectionOnBaseline[i] = baseVector[0] * vertor[0] + baseVector[1] * vertor[1];
        }
        double min = projectionOnBaseline[0];
        int index = 0;
        //找到最低点
        for (int i = 1; i < projectionOnBaseline.length; i++) {
            if (projectionOnBaseline[i] < min) {
                min = projectionOnBaseline[i];
                index = i;
            }
        }
        Point[] p = new Point[points.length];
        int nums = points.length - 1;
        for (int i = 0; i < nums; i++) {
            p[i] = points[index % nums];
            index = index + 1;
        }
        for (int i = 0; i < nums; i++) {
            points[i] = p[i];
        }
        points[nums] = p[0]; //形成闭环
        maxWidth = 0;
        double lenBaseline = getDistance(highPoint, lowPoint);
        for (int i = 1; i < nums; i++) {
            double[] vertor = {points[i].getX() - points[0].getX(), points[i].getY() - points[0].getY()};
            double temp = Math.abs(baseVector[0] * vertor[0] + baseVector[1] * vertor[1]) / lenBaseline;
//            double temp=(baseVector[0]*vertor[0]+baseVector[1]*vertor[1])/lenBaseline;

            if (temp > maxWidth) {
                maxWidth = temp;
            }
        }
    }

    private List<Point> getCuttingPoints() {
        List<Point> cutPoints = new ArrayList<>();
        reorganizePoints();
        //扫面线斜率,先不考虑斜率为0的情况
//        double k= (lowPoint.getX() -highPoint.getY())/(highPoint.getY() - lowPoint.getY());//x1-x2 / y2-y1
//        double b=points[0].getY()- k * points[0].getX();
        double A = (lowPoint.getX() - highPoint.getY()) / (highPoint.getY() - lowPoint.getY());
        double B = -1;
        double C = points[0].getY() - A * points[0].getX();
        double fenmu = Math.sqrt(Math.pow(A, 2) + Math.pow(B, 2));

        int swapLineNums = 0; //用于计算高度差
        double dis = 0;
        while (dis < maxWidth) {
            if (swapLineNums < 1) {
                dis = dis + width / 2;
            } else {
                dis = dis + width;
            }
            //处理最后剩余部分超过一半宽度的问题
            if (dis > maxWidth && dis - maxWidth < width / 2) {
                dis = dis - width / 2;
                dis = dis + (maxWidth - dis) / 2;
            }
            //计算交点
            double tempC = C + dis * fenmu;
            boolean flag = false;
            for (int j = 1; j < points.length; j++) {
                double A2 = points[j].getY() - points[j - 1].getY(); //y2-y1
                double B2 = points[j - 1].getX() - points[j].getX(); //x1-x2
                double C2 = points[j].getX() * points[j - 1].getY() - points[j - 1].getX() * points[j].getY();//x2*y1-x1*y2
                double py = (C2 * A - tempC * A2) / (A2 * B - A * B2);
                double px = (C2 * B - tempC * B2) / (A * B2 - A2 * B);
                if (isOntheLine(points[j - 1], points[j], px, py)) {
                    flag = true;
                    cutPoints.add(new Point(px, py, swapLineNums));  //用point的arc参数存储高度值
                }
            }
            if (flag) {
                swapLineNums = swapLineNums + 1;
            }
        }
        boolean flag = true;
        for (int j = 0; j < cutPoints.size(); j = j + 2) {
            flag = !flag;
            if (flag) {
                Collections.swap(cutPoints, j, j + 1);
            }
        }

        return cutPoints;
    }

    private double getDistance(Point p1, Point p2) {
        return Math.sqrt(Math.pow(p1.getX() - p2.getX(), 2) + Math.pow(p1.getY() - p2.getY(), 2));
    }

    private boolean isOntheLine(Point start, Point end, double x, double y) {
        if (doubleEqules(start.getX(), end.getX())) {
            //经度（x）相等
            if (doubleEqules(start.getY(), y) || doubleEqules(end.getY(), y))
                return true;
            if ((y > start.getY() && y < end.getY()) || (y < start.getY() && y > end.getY()))
                return true;
        } else {
            if (doubleEqules(start.getX(), x) || doubleEqules(end.getX(), x))
                return true;
            if ((x > start.getX() && x < end.getX()) || (x < start.getX() && x > end.getX()))
                return true;
        }
        return false;
    }

    private boolean doubleEqules(double d1, double d2) {
        return Double.doubleToLongBits(d1) == Double.doubleToLongBits(d2);
    }
}
