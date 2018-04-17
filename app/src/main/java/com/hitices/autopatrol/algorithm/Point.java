package com.hitices.autopatrol.algorithm;

/**
 * Created by Rhys on 2018/3/7.
 * email: bozliu@outlook.com
 */

public class Point {
    private double x;        //X坐标
    private double y;        //Y坐标
    private double arCos;    //与P0点的角度

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Point(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.arCos = z;
    }
    public Point() {
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getArCos() {
        return arCos;
    }

    public void setArCos(double arCos) {
        this.arCos = arCos;
    }
}
