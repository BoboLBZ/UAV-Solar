package com.hitices.autopatrol.algorithm;

import com.amap.api.maps2d.model.LatLng;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Rhys on 2018/3/7.
 * email: bozliu@outlook.com
 */

public class FullCoveragePathPlanningAlgorithm {
    private Point[] points;
    private double width;
    private double speed;
    private double waypointTime;  //
    private Point startPoint;
    private Convert convert;
    public FullCoveragePathPlanningAlgorithm(List<LatLng> waypoints,double width,double speed,double time,LatLng startPoint){
        //convert Latlng to Point
        convert=new Convert(waypoints.get(0),waypoints.get(1));
        ConvexHull convexHull=new ConvexHull(convert.LatlngToPoint(waypoints));
        this.startPoint=convert.singleLatlngToPoint(startPoint);

        points=convexHull.getTubaoPoint();
        this.width=width;
        this.speed=speed;
        this.waypointTime=time;
    }
    private List<Point> getSortedPoints(){
        int N=points.length-1;
        double[] distance=new double[N];
        List<List<Point>> waypoints=new ArrayList<>();
        for(int i=0;i<N;i++)
            distance[i]=0;
        //按边遍历
        for(int i=1;i<=N;i++){
            double A=points[i].getY()-points[i-1].getY(); //y2-y1
            double B=points[i-1].getX()-points[i].getX(); //x1-x2
            double C=points[i].getX()*points[i-1].getY()-points[i-1].getX()*points[i].getY();//x2*y1-x1*y2
            double fenmu=Math.sqrt(Math.pow(A,2)+Math.pow(B,2));
            //找到边对应的顶点,距离存放在 distance中
            for(int j=0;j<N;j++){
                double dis=Math.abs(A*points[j].getX() + B*points[j].getY())/fenmu;
                if(dis > distance[i-1]){
                    distance[i-1]=dis;
                }
            }
            double temp=0;
            int nums=0;//nums of waypoints
            List<Point> singleLineWaypoints=new ArrayList<>();
            while (temp < distance[i-1]){
                if(nums < 1 ){
                    temp=temp+width/2;
                }else {
                    temp=temp+width;
                }
                //处理最后剩余部分超过一半宽度的问题
                if(temp > distance[i-1] && temp-distance[i-1]<width/2){
                    temp=temp-width/2;
                    temp=temp+(distance[i-1]-temp)/2;
                }
                double tempC=C+temp*fenmu;
                for(int j=1;j<=N;j++){
                    if(i!=j){
                        double A2=points[j].getY()-points[j-1].getY(); //y2-y1
                        double B2=points[j-1].getX()-points[j].getX(); //x1-x2
                        double C2=points[j].getX()*points[j-1].getY()-points[j-1].getX()*points[j].getY();//x2*y1-x1*y2
                        double py=(C2*A-tempC*A2)/(A2*B-A*B2); //lat
                        double px=(C2*B-tempC*B2)/(A*B2-A2*B); //lng
                        if(isOntheLine(points[j-1],points[j],px,py)){
                            nums=nums+1;
                            singleLineWaypoints.add(new Point(px,py));
                        }
                    }
                }
            }
            waypoints.add(singleLineWaypoints);
        }
        //主航线太短,小于width，合为一点
        for(int i=0;i<N;i++){
            List<Point> tempList=waypoints.get(i);
            for(int j=0;j<tempList.size();j=j+2){
                if(getDistance(tempList.get(j),tempList.get(j+1)) < width ){
                    double tx=(tempList.get(j).getX()+tempList.get(j+1).getX())/2;
                    double ty=(tempList.get(j).getY()+tempList.get(j+1).getY())/2;
                    tempList.set(j,new Point(tx,ty));
                    tempList.set(j+1,new Point(tx,ty));
                }
            }
        }
        //正常顺序
        boolean flag=true;
        for(int i=0;i<N;i++)
        {
            for(int j=0;j<waypoints.get(i).size();j=j+2){
                flag=!flag;
                if(flag){
                    Collections.swap(waypoints.get(i),j,j+1);
                }
            }
        }
        //基于时间选择最好的覆盖方案,把飞行器起点纳入考虑
        double[] times=new double[N];
        double[] timesReverse=new double[N];
        for(int i=0;i<N;i++){
            times[i]=0;
            timesReverse[i]=0;
        }
        for(int i=0;i<N;i++){
            List<Point> tempList=waypoints.get(i);
            for(int j=1;j<tempList.size();j++){
                times[i]=times[i]+getDistance(tempList.get(j),tempList.get(j-1))/speed;
            }
            times[i]=times[i]+waypointTime*tempList.size();
            times[i]=times[i]+(getDistance(tempList.get(0),startPoint)+
                    getDistance(tempList.get(tempList.size()-1),startPoint))/speed;
        }
        int index1=getMin(times);
        double minTime=times[index1];
        List<Point> result=new ArrayList<>();
        for (int i = 0; i < waypoints.get(index1).size(); i++) {
            Point p=waypoints.get(index1).get(i);
            result.add(new Point(p.getX(),p.getY()));
        }
        //reverse
        for(int i=0;i<N;i++)
        {
            for(int j=0;j<waypoints.get(i).size();j=j+2){
                    Collections.swap(waypoints.get(i),j,j+1);
            }
        }
        for(int i=0;i<N;i++){
            List<Point> tempList=waypoints.get(i);
            for(int j=1;j<tempList.size();j++){
                timesReverse[i]=timesReverse[i]+getDistance(tempList.get(j),tempList.get(j-1))/speed;
            }
            timesReverse[i]=timesReverse[i]+waypointTime*tempList.size();
            timesReverse[i]=timesReverse[i]+(getDistance(tempList.get(0),startPoint)+
                    getDistance(tempList.get(tempList.size()-1),startPoint))/speed;
        }
        index1=getMin(timesReverse);
        //need to check whether all points can be photoed
        if(minTime < timesReverse[index1]){
            return result;
        }else{
            return waypoints.get(index1);
        }
    }
    public List<LatLng> getShotWaypoints(){
        List<Point> templist=new ArrayList<>();
        List<Point> old =getSortedPoints();
        for (int i = 0; i <old.size() ; i=i+2) {
             Point start=old.get(i);
            templist.add(start);
             if(i+1 < old.size()){
                 Point end=old.get(i+1);
                 double dis=getDistance(start,end);
                 double cos=(end.getX()-start.getX())/dis;
                 double sin=(end.getY()-start.getY())/dis;
                 double interval=width;
                 while (interval < dis){
                     templist.add(new Point(start.getX()+interval*cos,start.getY()+interval*sin));
                     interval+=width;
                 }
             }
        }
        return convert.PointToLatlng(templist);
    }
    public List<LatLng> getPlanningWaypoints(){
          return convert.PointToLatlng(getSortedPoints());
    }
    private boolean isOntheLine(Point start,Point end,double x,double y){
        if(doubleEqules(start.getX(),end.getX())){
            //经度（x）相等
            if(doubleEqules(start.getY(),y) ||doubleEqules(end.getY(),y))
                return true;
            if( (y>start.getY() && y< end.getY())||(y<start.getY() && y> end.getY()))
               return true;
        }else {
            if(doubleEqules(start.getX(),x) ||doubleEqules(end.getX(),x))
                return true;
            if( (x>start.getX() && x< end.getX())||(x<start.getX() && x> end.getX()))
                return true;
        }
        return false;
    }
    private boolean doubleEqules(double d1,double d2){
        return Double.doubleToLongBits(d1) == Double.doubleToLongBits(d2);
    }
    private int getMin(double[] array){
        int index=0;
        double min=array[0];
        for(int i=1;i<array.length;i++){
            if (array[i]<min){
                min=array[i];
                index=i;
            }
        }
        return index;
    }
    private double getDistance(Point start,Point end){
        return Math.sqrt(Math.pow(start.getX()-end.getX(),2)
                        +Math.pow(start.getY()-end.getY(),2) );
    }
}
