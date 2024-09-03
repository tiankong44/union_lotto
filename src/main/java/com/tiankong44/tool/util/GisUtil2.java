package com.tiankong44.tool.util;

import com.tiankong44.tool.gis.entity.Coordinate;

import java.util.Arrays;

/**
 * @author zhanghaoSMEICS
 * @date 2022-10-21 22:03
 */
@Deprecated
public class GisUtil2 {
    public static double pi = 3.1415926535897932384626;
    public static double xpi = 3.14159265358979324 * 3000.0 / 180.0;
    public static double a = 6378245.0;
    public static double ee = 0.00669342162296594323;

    public static double transformLat(double x, double y) {
        double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y
                + 0.2 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * pi) + 20.0 * Math.sin(2.0 * x * pi)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(y * pi) + 40.0 * Math.sin(y / 3.0 * pi)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(y / 12.0 * pi) + 320 * Math.sin(y * pi / 30.0)) * 2.0 / 3.0;
        return ret;
    }

    public static double transformLon(double x, double y) {
        double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1
                * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * pi) + 20.0 * Math.sin(2.0 * x * pi)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(x * pi) + 40.0 * Math.sin(x / 3.0 * pi)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(x / 12.0 * pi) + 300.0 * Math.sin(x / 30.0
                * pi)) * 2.0 / 3.0;
        return ret;
    }

    public static double[] transform(double lat, double lon) {
        if (outOfChina(lon, lat)) {
            return new double[]{lat, lon};
        }
        double dLat = transformLat(lon - 105.0, lat - 35.0);
        double dLon = transformLon(lon - 105.0, lat - 35.0);
        double radLat = lat / 180.0 * pi;
        double magic = Math.sin(radLat);
        magic = 1 - ee * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * pi);
        dLon = (dLon * 180.0) / (a / sqrtMagic * Math.cos(radLat) * pi);
        double mgLat = lat + dLat;
        double mgLon = lon + dLon;
        return new double[]{mgLat, mgLon};
    }

    public static boolean outOfChina(double lon, double lat) {
        if (lon < 72.004 || lon > 137.8347) {
            return true;
        }
        if (lat < 0.8293 || lat > 55.8271) {
            return true;
        }
        return false;
    }

    /**
     * 84 to 火星坐标系 (GCJ-02) World Geodetic System ==> Mars Geodetic System
     *
     * @param lat
     * @param lon
     * @return
     */
    public static double[] gps84ToGcj02(double lon, double lat) {
        if (outOfChina(lon, lat)) {
            return new double[]{lon, lat};
        }
        double dLat = transformLat(lon - 105.0, lat - 35.0);
        double dLon = transformLon(lon - 105.0, lat - 35.0);
        double radLat = lat / 180.0 * pi;
        double magic = Math.sin(radLat);
        magic = 1 - ee * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * pi);
        dLon = (dLon * 180.0) / (a / sqrtMagic * Math.cos(radLat) * pi);
        double mgLat = lat + dLat;
        double mgLon = lon + dLon;
        return new double[]{mgLon, mgLat};
    }

    /**
     * 84 to 火星坐标系 (GCJ-02) World Geodetic System ==> Mars Geodetic System
     *
     * @param coordinate
     * @return
     */
    public static Coordinate gps84ToGcj02(Coordinate coordinate) {
        double[] gcj02 = gps84ToGcj02(coordinate.getLongitude(), coordinate.getLatitude());
        coordinate.setLongitude(gcj02[0]);
        coordinate.setLatitude(gcj02[1]);
        return coordinate;
    }

    /**
     * * 火星坐标系 (GCJ-02) to 84 * * @param lon * @param lat * @return
     */
    public static double[] gcj02ToGps84(double lon, double lat) {
        double[] gps = transform(lat, lon);
        double lontitude = lon * 2 - gps[1];
        double latitude = lat * 2 - gps[0];
        return new double[]{lontitude, latitude};
    }

    /**
     * * 火星坐标系 (GCJ-02) to 84 * * @param lon * @param lat * @return
     */
    public static Coordinate gcj02ToGps84(Coordinate coordinate) {
        double lat = coordinate.getLatitude();
        double lon = coordinate.getLongitude();
        double[] gps = transform(lat, lon);
        coordinate.setLongitude(lon * 2 - gps[1]);
        coordinate.setLatitude(lat * 2 - gps[0]);
        return coordinate;
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
        coordinate.setLongitude(bd09[0]);
        coordinate.setLatitude(bd09[1]);
        return coordinate;
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
        coordinate.setLongitude(gcj02[0]);
        coordinate.setLatitude(gcj02[1]);
        return coordinate;
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
        coordinate.setLongitude(bd09[0]);
        coordinate.setLatitude(bd09[1]);
        return coordinate;
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
        coordinate.setLongitude(gps84[0]);
        coordinate.setLatitude(gps84[1]);
        return coordinate;
    }

    public static void main(String[] args) {
        double[] doubles = gps84ToGcj02(29.566170442769, 106.5619526988);
        double[] doubles1 = gcj02ToGps84(29.563304388714954, 106.56565476266101);
        System.out.println(Arrays.toString(doubles));
        System.out.println(Arrays.toString(doubles1));
    }


}
