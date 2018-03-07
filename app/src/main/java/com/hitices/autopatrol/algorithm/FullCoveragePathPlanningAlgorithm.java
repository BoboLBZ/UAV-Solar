package com.hitices.autopatrol.algorithm;

import com.amap.api.maps2d.model.LatLng;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Rhys on 2018/3/7.
 * email: bozliu@outlook.com
 */

public class FullCoveragePathPlanningAlgorithm {
    private LatLng[] points;
    private double width;
    private double speed;
    private double waypointTime;  //
    private LatLng startPoint;
    FullCoveragePathPlanningAlgorithm(List<LatLng> waypoints,double width,double speed,double time,LatLng startPoint){
        ConvexHull convexHull=new ConvexHull(waypoints);
        points=convexHull.getTubaoPoint();
        this.width=width;
        this.speed=speed;
        this.waypointTime=time;
        this.startPoint=startPoint;
    }
    public List<LatLng> getSortedWaypoints(){
        int N=points.length-1;
        double[] distance=new double[N];
        List<List<LatLng>> waypoints=new ArrayList<>();
        for(int i=0;i<N;i++)
            distance[i]=0;
        //按边遍历
        for(int i=1;i<=N;i++){
            double A=points[i].latitude-points[i-1].latitude; //y2-y1
            double B=points[i-1].longitude-points[i].longitude; //x1-x2
            double C=points[i].longitude*points[i-1].latitude-points[i-1].longitude*points[i].latitude;//x2*y1-x1*y2
            double fenmu=Math.sqrt(Math.pow(A,2)+Math.pow(B,2));
            //找到边对应的顶点,距离存放在 distance中
            for(int j=0;j<N;j++){
                double dis=Math.abs(A*points[j].longitude + B*points[j].latitude)/fenmu;
                if(dis > distance[i-1]){
                    distance[i-1]=dis;
                }
            }
            double temp=0;
            int nums=0;//nums of waypoints
            List<LatLng> singleLineWaypoints=new ArrayList<>();
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
                        double A2=points[j].latitude-points[j-1].latitude; //y2-y1
                        double B2=points[j-1].longitude-points[j].longitude; //x1-x2
                        double C2=points[j].longitude*points[j-1].latitude-points[j-1].longitude*points[j].latitude;//x2*y1-x1*y2
                        double py=(C2*A-tempC*A2)/(A2*B-A*B2); //lat
                        double px=(C2*B-tempC*B2)/(A*B2-A2*B); //lng
                        if(isOntheLine(points[j-1],points[j],px,py)){
                            nums=nums+1;
                            singleLineWaypoints.add(new LatLng(py,px));
                        }
                    }
                }
            }
            waypoints.add(singleLineWaypoints);
        }
        //主航线太短,小于width，合为一点
        for(int i=0;i<N;i++){
            List<LatLng> tempList=waypoints.get(i);
            for(int j=0;j<tempList.size();j=j+2){
                if(getDistance(tempList.get(j),tempList.get(j)) < width ){
                    double tlng=(tempList.get(j).longitude+tempList.get(j).longitude)/2;
                    double tlat=(tempList.get(j).latitude+tempList.get(j).latitude)/2;
                    tempList.set(j,new LatLng(tlat,tlng));
                    tempList.set(j+1,new LatLng(tlat,tlng));
                }
            }
        }
        //调整航点顺序
        boolean flag=true;
        List<List<LatLng>> reverseWaypoints=waypoints; //maybe have bug
        for(int i=0;i<N;i++)
        {
            List<LatLng> tempList=waypoints.get(i);
            List<LatLng> tempList1=reverseWaypoints.get(i);
            for(int j=0;j<tempList.size();j=j+2){
                flag=!flag;
                if(flag){
                    LatLng l=tempList.get(j);
                    tempList.set(j,tempList.get(j+1));
                    tempList.set(j+1,l);
                }else {
                    LatLng l=tempList1.get(j);
                    tempList1.set(j,tempList1.get(j+1));
                    tempList1.set(j+1,l);
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
            List<LatLng> tempList=waypoints.get(i);
            List<LatLng> tempList1=reverseWaypoints.get(i);
            for(int j=1;j<tempList.size();j++){
                times[i]=times[i]+getDistance(tempList.get(j),tempList.get(j-1))/speed;
            }
            for(int j=1;j<tempList1.size();j++){
                timesReverse[i]=timesReverse[i]+getDistance(tempList1.get(j),tempList1.get(j-1))/speed;
            }
            times[i]=times[i]+waypointTime*tempList.size();
            times[i]=times[i]+(getDistance(tempList.get(0),startPoint)+
                               getDistance(tempList.get(tempList.size()),startPoint))/speed;

            timesReverse[i]=timesReverse[i]+waypointTime*tempList1.size();
            timesReverse[i]=timesReverse[i]+(getDistance(tempList1.get(0),startPoint)+
                                             getDistance(tempList1.get(tempList1.size()),startPoint))/speed;
        }
        int index1=getMin(times);
        int index2=getMin(timesReverse);
        //need to check whether all points can be photoed

        if(times[index1] < timesReverse[index2]){
            return waypoints.get(index1);
        }else{
            return reverseWaypoints.get(index2);
        }
    }
    private boolean isOntheLine(LatLng start,LatLng end,double x,double y){
        if(doubleEqules(start.longitude,end.longitude)){
            //经度（x）相等
            if(doubleEqules(start.latitude,y) ||doubleEqules(end.latitude,y))
                return true;
            if( (y>start.latitude && y< end.latitude)||(y<start.latitude && y> end.latitude))
               return true;
        }else {
            if(doubleEqules(start.longitude,x) ||doubleEqules(end.longitude,x))
                return true;
            if( (x>start.longitude && x< end.longitude)||(x<start.longitude && x> end.longitude))
                return true;
        }
        return false;
    }
    private boolean doubleEqules(double d1,double d2){
        if(Double.doubleToLongBits(d1) == Double.doubleToLongBits(d2))
            return true;
        else
            return false;
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
    private double getDistance(LatLng start,LatLng end){
        return Math.sqrt(Math.pow(start.longitude-end.longitude,2)
                        +Math.pow(start.latitude-end.latitude,2) );
    }
}
