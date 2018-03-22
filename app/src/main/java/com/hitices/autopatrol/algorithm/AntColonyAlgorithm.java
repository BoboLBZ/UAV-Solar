package com.hitices.autopatrol.algorithm;

import com.amap.api.maps2d.AMapUtils;
import com.amap.api.maps2d.model.LatLng;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

import dji.common.mission.waypoint.Waypoint;

/**
 * Created by Rhys on 2018/3/6.
 * email: bozliu@outlook.com
 */

public class AntColonyAlgorithm {
    private List<LatLng> rawWaypoints = new ArrayList<>();
    private List<Integer> sortedWaypointsIndex = new ArrayList<>();
    /*init param*/
    private int antNum = 30; //
    final float alpha = 1; //信息素重要程度
    final float beta = 5;//启发函数重要程度
    final float vol = 0.2f; //信息素挥发因子
    final float Q = 10;  //常系数
    final int iter_max = 100; //最大迭代次数
    private int unchangedTimes = 0;
    private double minLength = 0;
    private Random r = new Random();

    public AntColonyAlgorithm(List<Waypoint> points, LatLng planePoint) {
        rawWaypoints.add(planePoint);
        for (int i = 0; i < points.size(); i++) {
            rawWaypoints.add(new LatLng(points.get(i).coordinate.getLatitude(), points.get(i).coordinate.getLongitude()));
        }
    }

    public List<Integer> getSortedWaypoints() {
        int iter = 1;
        int pointsNum = rawWaypoints.size();
        antNum = pointsNum * 2;
        int[][] table = new int[antNum][pointsNum]; //存放路径，
        double[][] Tau = new double[pointsNum][pointsNum]; //信息素矩阵
        double[][] Heu = new double[pointsNum][pointsNum]; //启发函数矩阵
        double[][] Distance = new double[pointsNum][pointsNum];
        //init Tau and Heu
        for (int i = 0; i < pointsNum; i++) {
            for (int j = 0; j < pointsNum; j++) {
                Tau[i][j] = 1;
                if (i != j) {
                    LatLng one = rawWaypoints.get(i);
                    LatLng two = rawWaypoints.get(j);
                    //Distance[i][j]=Math.sqrt(Math.pow(one.latitude-two.latitude,2)+Math.pow(one.longitude-two.longitude,2));
                    //use aAap function to calculate distance
                    Distance[i][j] = AMapUtils.calculateLineDistance(one, two);
                    Heu[i][j] = 1 / Distance[i][j];
                } else {
                    Distance[i][j] = 0;
                    Heu[i][j] = 0;
                }

            }
        }
        while (iter < iter_max) {
            for (int i = 0; i < antNum; i++) {
//                table[i][0]=r.nextInt(pointsNum);
                // 飞行器起点作为出发点
                table[i][0] = 0;
            }
            for (int i = 0; i < antNum; i++) {
                for (int j = 1; j < pointsNum; j++) {
                    List<Integer> allowPoints = new ArrayList<>();
                    List<Integer> visited = new ArrayList<>();
                    for (int k = 0; k < j; k++)
                        visited.add(table[i][k]);
                    for (int k = 0; k < pointsNum; k++) {
                        if (!visited.contains(k))
                            allowPoints.add(k);
                    }
                    double[] P = new double[allowPoints.size()];
                    int lastIndex = table[i][j - 1];
                    double sum = 0, cumsum = 0;
                    for (int k = 0; k < allowPoints.size(); k++) {
                        P[k] = Math.pow(Tau[lastIndex][allowPoints.get(k)], alpha) * Math.pow(Heu[lastIndex][allowPoints.get(k)], beta);
                        sum += P[k];
                    }
                    for (int k = 0; k < allowPoints.size(); k++) {
                        P[k] = P[k] / sum;
                        cumsum += P[k];
                        P[k] = cumsum;
                    }
                    double limited = r.nextDouble();
                    int nextPointIndex = 0;
                    for (int k = 0; k < allowPoints.size(); k++) {
                        if (P[k] >= limited) {
                            nextPointIndex = k;
                            break;
                        }
                    }
                    table[i][j] = allowPoints.get(nextPointIndex);
                }
            }
            //计算距离
            List<Double> length = new ArrayList<>();
            for (int i = 0; i < antNum; i++) {
                double temp = 0;
                int[] route = table[i];
                for (int j = 1; j < pointsNum; j++) {
                    temp += Distance[route[j - 1]][route[j]];
                }
                temp += Distance[route[pointsNum - 1]][route[0]];
                length.add(temp);
            }
            //选择最短路径记录下来
            int minLengthIndex = findMinIndex(length);
            double min = length.get(minLengthIndex);
            sortedWaypointsIndex.clear();
            for (int i = 1; i < pointsNum; i++) {
                sortedWaypointsIndex.add(i - 1, table[minLengthIndex][i] - 1); //去掉起点，对应索引-1
            }
            //提前结束（超过10次最短路径不发生变化）
            if (iter == 1) {
                unchangedTimes = 1;
                minLength = min;
            } else {
                if (min < minLength) {
                    minLength = min;
                    unchangedTimes = 0;
                } else
                    unchangedTimes++;
                if (unchangedTimes > 10)
                    break;
            }
            //更新信息素
            double[][] delta_tau = new double[pointsNum][pointsNum];
            for (int i = 0; i < pointsNum; i++) {
                for (int j = 0; j < pointsNum; j++) {
                    delta_tau[i][j] = 0;
                }
            }
            for (int i = 0; i < antNum; i++) {
                for (int j = 1; j < pointsNum; j++) {
                    delta_tau[table[i][j - 1]][table[i][j]] = delta_tau[table[i][j - 1]][table[i][j]] + Q / length.get(i);
                }
                delta_tau[table[i][pointsNum - 1]][table[i][0]] = delta_tau[table[i][pointsNum - 1]][table[i][0]] + Q / length.get(i);
            }
            for (int i = 0; i < pointsNum; i++) {
                for (int j = 0; j < pointsNum; j++) {
                    Tau[i][j] = (1 - vol) * Tau[i][j] + delta_tau[i][j];
                }
            }
            //迭代次数+1;清空table
            for (int i = 0; i < antNum; i++) {
                for (int j = 0; j < pointsNum; j++) {
                    table[i][j] = 0;
                }
            }
            iter++;
        }
        return sortedWaypointsIndex;
    }

    public static <T extends Comparable<T>> int findMinIndex(final List<T> xs) {
        int minIndex;
        if (xs.isEmpty()) {
            minIndex = -1;
        } else {
            final ListIterator<T> itr = xs.listIterator();
            T min = itr.next(); // first element as the current minimum
            minIndex = itr.previousIndex();
            while (itr.hasNext()) {
                final T curr = itr.next();
                if (curr.compareTo(min) < 0) {
                    min = curr;
                    minIndex = itr.previousIndex();
                }
            }
        }
        return minIndex;
    }
}
