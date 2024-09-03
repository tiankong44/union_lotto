package com.tiankong44.tool.util;

import com.tiankong44.tool.exception.customException.DifferentCoordinateException;
import com.tiankong44.tool.gis.entity.Coordinate;
import com.tiankong44.tool.gis.entity.CoordinateTypeEnum;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * @author zhanghao_SMEICS
 * @date 2022-10-21 22:03
 */
@Component
public class GisUtil {
    private final static double a = 6378245.0;
    private final static double pi = 3.1415926535897932384626;
    private final static double ee = 0.00669342162296594323;
    public static double xpi = 3.14159265358979324 * 3000.0 / 180.0;

    /**
     * 计算地球上任意两点(经纬度)距离
     *
     * @param lat1 第一点纬度
     * @param lng1 第一点经度
     * @param lat2 第二点纬度
     * @param lng2 第二点经度
     * @return 返回距离 单位：米
     */
    public static double distance(double lng1, double lat1, double lng2, double lat2) {
        double a, b, R;
        R = 6378137; // 地球半径
        lat1 = lat1 * Math.PI / 180.0;
        lat2 = lat2 * Math.PI / 180.0;
        a = lat1 - lat2;
        b = (lng1 - lng2) * Math.PI / 180.0;
        double d;
        double sa2, sb2;
        sa2 = Math.sin(a / 2.0);
        sb2 = Math.sin(b / 2.0);
        d = 2 * R * Math.asin(Math.sqrt(sa2 * sa2 + Math.cos(lat1) * Math.cos(lat2) * sb2 * sb2));
        return d;
    }

    /**
     * 计算地球上任意两点(经纬度)距离 (同类型坐标系)
     *
     * @param startCoordinate 起始点
     * @param endCoordinate   结束点
     * @return 返回距离 单位：米
     */
    public static double distance(Coordinate startCoordinate, Coordinate endCoordinate) throws DifferentCoordinateException {
        if (startCoordinate.getCoordinateType().equals(endCoordinate.getCoordinateType())) {
            throw new DifferentCoordinateException("坐标系不是同一类型(Coordinate systems are not of the same type)",Thread.currentThread().getStackTrace()[1].getClassName());
        }
        return distance(startCoordinate.getLongitude(), startCoordinate.getLatitude(), endCoordinate.getLongitude(), endCoordinate.getLatitude());
    }

    /**
     * 计算地球上任意两点(经纬度)距离  (同类型坐标系)
     *
     * @param coordinates
     * @return 返回距离 单位：米
     */
    public static double distance(List<Coordinate> coordinates) throws DifferentCoordinateException {
        double distance = 0;
        if (coordinates != null && coordinates.size() > 1) {
            for (int i = 0; i < coordinates.size() - 1; i++) {
                Coordinate startCoordinate = coordinates.get(i);
                Coordinate endCoordinate = coordinates.get(i + 1);
                if (!startCoordinate.getCoordinateType().equals(endCoordinate.getCoordinateType())) {
                    throw new DifferentCoordinateException("坐标系不是同一类型(Coordinate systems are not of the same type)",Thread.currentThread().getStackTrace()[1].getClassName());
                }
                distance += distance(startCoordinate.getLongitude(), startCoordinate.getLatitude(), endCoordinate.getLongitude(), endCoordinate.getLatitude());


            }

        }
        return distance;
    }

    /**
     * Description: WGS-84 to GCJ-02 <BR>
     *
     * @param latitude  纬度
     * @param longitude 经度
     * @return [纬度，经度]
     * @author dsn
     * @date 2017年10月24日 下午2:09:27
     * @version 1.0
     */
    public static double[] gps84ToGcj02(double longitude, double latitude) {
        double[] dev = calDev(latitude, longitude);
        double retLat = latitude + dev[0];
        double retLon = longitude + dev[1];
        return new double[]{retLon, retLat};
    }

    /**
     * Description: WGS-84 to GCJ-02 <BR>
     * <p>
     * coordinate
     *
     * @return coordinate
     * @author dsn
     * @date 2017年10月24日 下午2:09:27
     * @version 1.0
     */
    public static Coordinate gps84ToGcj02(Coordinate coordinate) {
        double latitude = coordinate.getLatitude();
        double longitude = coordinate.getLongitude();
        double[] dev = calDev(latitude, longitude);

        return new Coordinate(longitude + dev[1], latitude + dev[0], coordinate.getElevation(), CoordinateTypeEnum.GCJ02.value);
    }

    /**
     * Description: WGS-84 to GCJ-02 <BR>
     *
     * @param latitude  纬度
     * @param longitude 经度
     * @param scale     经纬度保留小数位数
     * @return [纬度，经度]
     * @author dsn
     * @date 2017年10月24日 下午2:09:27
     * @version 1.0
     */
    public static double[] gps84ToGcj02(double longitude, double latitude, int scale) {
        double[] dev = calDev(latitude, longitude);
        double retLat = latitude + dev[0];
        double retLon = longitude + dev[1];
        return new double[]{new BigDecimal(retLon).setScale(scale, RoundingMode.DOWN).doubleValue(),
                new BigDecimal(retLat).setScale(scale, RoundingMode.DOWN).doubleValue()};
    }

    /**
     * Description: WGS-84 to GCJ-02 <BR>
     *
     * @param coordinate
     * @param scale      经纬度保留小数位数
     * @return coordinate
     * @author dsn
     * @date 2017年10月24日 下午2:09:27
     * @version 1.0
     */
    public static Coordinate gps84ToGcj02(Coordinate coordinate, int scale) {
        double longitude = coordinate.getLongitude();
        double latitude = coordinate.getLatitude();
        double[] dev = calDev(latitude, longitude);
        return new Coordinate(
                new BigDecimal(longitude + dev[1]).setScale(scale, RoundingMode.DOWN).doubleValue(),
                new BigDecimal(latitude + dev[0]).setScale(scale, RoundingMode.DOWN).doubleValue(),
                coordinate.getElevation(),
                CoordinateTypeEnum.GCJ02.value);
    }

    /**
     * Description:GCJ-02 to WGS-84 <BR>
     *
     * @param latitude  纬度
     * @param longitude 经度
     * @return [纬度，经度]
     * @author dsn
     * @date 2017年10月24日 下午2:09:54
     * @version 1.0
     */
    public static double[] gcj02ToGps84(double longitude, double latitude) {
        double[] dev = calDev(latitude, longitude);
        double retLat = latitude - dev[0];
        double retLon = longitude - dev[1];
        dev = calDev(retLat, retLon);
        retLat = latitude - dev[0];
        retLon = longitude - dev[1];
        return new double[]{retLon, retLat};
    }


    /**
     * Description:GCJ-02 to WGS-84 <BR>
     *
     * @param coordinate
     * @return coordinate
     * @author dsn
     * @date 2017年10月24日 下午2:09:54
     * @version 1.0
     */
    public static Coordinate gcj02ToGps84(Coordinate coordinate) {
        double longitude = coordinate.getLongitude();
        double latitude = coordinate.getLatitude();
        double[] dev = calDev(latitude, longitude);
        double retLat = latitude - dev[0];
        double retLon = longitude - dev[1];
        dev = calDev(retLat, retLon);
        return new Coordinate(longitude - dev[1], latitude - dev[0], coordinate.getElevation(), CoordinateTypeEnum.GPS84.value);
    }


    /**
     * 火星坐标系 (GCJ-02) 与百度坐标系 (BD-09) 的转换算法 将 GCJ-02 坐标转换成 BD-09 坐标
     *
     * @param lat
     * @param lon
     */
    public static double[] gcj02ToBd09(double lon, double lat) {
        double x = lon, y = lat;
        double z = Math.sqrt(x * x + y * y) + 0.00002 * Math.sin(y * xpi);
        double theta = Math.atan2(y, x) + 0.000003 * Math.cos(x * xpi);
        double tempLon = z * Math.cos(theta) + 0.0065;
        double tempLat = z * Math.sin(theta) + 0.006;
        double[] gps = {tempLon, tempLat};
        return gps;
    }

    /**
     * 火星坐标系 (GCJ-02) 与百度坐标系 (BD-09) 的转换算法 将 GCJ-02 坐标转换成 BD-09 坐标
     * <p>
     * coordinate
     */
    public static Coordinate gcj02ToBd09(Coordinate coordinate) {
        double[] bd09 = gcj02ToBd09(coordinate.getLongitude(), coordinate.getLatitude());
        return new Coordinate(bd09[0], bd09[1], coordinate.getElevation(), CoordinateTypeEnum.BD09.value);
    }


    /**
     * * 火星坐标系 (GCJ-02) 与百度坐标系 (BD-09) 的转换算法 * * 将 BD-09 坐标转换成GCJ-02 坐标 * * @param
     * bdlat * @param bdlon * @return
     */
    public static double[] bd09ToGcj02(double lon, double lat) {
        double x = lon - 0.0065, y = lat - 0.006;
        double z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * xpi);
        double theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * xpi);
        double tempLon = z * Math.cos(theta);
        double tempLat = z * Math.sin(theta);
        double[] gps = {tempLon, tempLat};
        return gps;
    }

    /**
     * * 火星坐标系 (GCJ-02) 与百度坐标系 (BD-09) 的转换算法 * * 将 BD-09 坐标转换成GCJ-02 坐标 * * @param
     * bdlat * @param bdlon * @return
     */
    public static Coordinate bd09ToGcj02(Coordinate coordinate) {
        double[] gcj02 = bd09ToGcj02(coordinate.getLongitude(), coordinate.getLatitude());
        return new Coordinate(gcj02[0], gcj02[1], coordinate.getElevation(), CoordinateTypeEnum.GCJ02.value);
    }


    /**
     * 将gps84转为bd09
     *
     * @param lat
     * @param lon
     * @return
     */
    public static double[] gps84ToBd09(double lon, double lat) {
        double[] gcj02 = gps84ToGcj02(lon, lat);
        double[] bd09 = gcj02ToBd09(gcj02[0], gcj02[1]);
        return bd09;
    }

    /**
     * 将gps84转为bd09
     *
     * @param coordinate
     * @return
     */
    public static Coordinate gps84ToBd09(Coordinate coordinate) {
        double[] gcj02 = gps84ToGcj02(coordinate.getLongitude(), coordinate.getLatitude());
        double[] bd09 = gcj02ToBd09(gcj02[0], gcj02[1]);
        return new Coordinate(bd09[0], bd09[1], coordinate.getElevation(), CoordinateTypeEnum.BD09.value);
    }

    /**
     * bd09转为gps84
     *
     * @author zhanghao_SMEICS
     * @date 2022/10/22 12:26
     */
    public static double[] bd09ToGps84(double lat, double lon) {
        double[] gcj02 = bd09ToGcj02(lat, lon);
        double[] gps84 = gcj02ToGps84(gcj02[0], gcj02[1]);


        return gps84;
    }

    /**
     * bd09转为gps84
     *
     * @author zhanghao_SMEICS
     * @date 2022/10/22 12:26
     */
    public static Coordinate bd09ToGps84(Coordinate coordinate) {
        double[] gcj02 = bd09ToGcj02(coordinate.getLongitude(), coordinate.getLatitude());
        double[] gps84 = gcj02ToGps84(gcj02[0], gcj02[1]);
        return new Coordinate(gps84[0], gps84[1], coordinate.getElevation(), CoordinateTypeEnum.GPS84.value);
    }

    /***
     * 判断是否在中国范围之内
     * @param lat
     * @param lon
     * @return
     */
    private static boolean isOutOfChina(double lon, double lat) {
        if (lon < 72.004 || lon > 137.8347) {
            return true;
        }
        if (lat < 0.8293 || lat > 55.8271) {
            return true;
        }
        return false;
    }

    private static double[] calDev(double wgLat, double wgLon) {
        if (isOutOfChina(wgLon, wgLat)) {
            return new double[]{0, 0};
        }
        double dLat = calLat(wgLon - 105.0, wgLat - 35.0);
        double dLon = calLon(wgLon - 105.0, wgLat - 35.0);
        double radLat = wgLat / 180.0 * pi;
        double magic = Math.sin(radLat);
        magic = 1 - ee * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * pi);
        dLon = (dLon * 180.0) / (a / sqrtMagic * Math.cos(radLat) * pi);
        return new double[]{dLat, dLon};
    }

    private static double calLat(double x, double y) {
        double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * pi) + 20.0 * Math.sin(2.0 * x * pi)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(y * pi) + 40.0 * Math.sin(y / 3.0 * pi)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(y / 12.0 * pi) + 320 * Math.sin(y * pi / 30.0)) * 2.0 / 3.0;
        return ret;
    }

    private static double calLon(double x, double y) {
        double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * pi) + 20.0 * Math.sin(2.0 * x * pi)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(x * pi) + 40.0 * Math.sin(x / 3.0 * pi)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(x / 12.0 * pi) + 300.0 * Math.sin(x / 30.0 * pi)) * 2.0 / 3.0;
        return ret;
    }

}
