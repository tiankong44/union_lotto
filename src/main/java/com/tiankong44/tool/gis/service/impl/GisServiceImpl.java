package com.tiankong44.tool.gis.service.impl;

import com.tiankong44.tool.base.entity.BaseRes;
import com.tiankong44.tool.exception.customException.DifferentCoordinateException;
import com.tiankong44.tool.gis.entity.Coordinate;
import com.tiankong44.tool.gis.entity.CoordinateTypeEnum;
import com.tiankong44.tool.gis.service.GisService;
import com.tiankong44.tool.util.GisUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zhanghao_SMEICS
 * @date 2022-10-21 21:46
 */
@Service
public class GisServiceImpl implements GisService {


    /**
     * WGS84坐标系转GCJ02坐标系
     *
     * @param coordinate
     * @author zhanghao_SMEICS
     * @date 2022/10/21 22:20
     */
    @Override
    public BaseRes WGS84ToGCJ02(Coordinate coordinate) {
        return BaseRes.success(GisUtil.gps84ToGcj02(coordinate));
    }


    /**
     * GCJ02坐标系转WGS84坐标系
     *
     * @param coordinate
     * @author zhanghao_SMEICS
     * @date 2022/10/21 22:20
     */
    @Override
    public BaseRes GCJ02ToWGS84(Coordinate coordinate) {
        return BaseRes.success(GisUtil.gcj02ToGps84(coordinate));
    }

    /**
     * 获取两点或多个点之间的距离
     *
     * @param coordinates
     * @author zhanghao_SMEICS
     * @date 2022/10/21 22:20
     */
    @Override
    public BaseRes getDistance(List<Coordinate> coordinates) throws DifferentCoordinateException {
        if (coordinates != null && coordinates.size() > 1) {
            return BaseRes.success(GisUtil.distance(coordinates));
        } else {
            return BaseRes.failure("请至少添加两个坐标");
        }

    }

    @Override
    public BaseRes coordinateTransform(Coordinate coordinate) {
        CoordinateTypeEnum coordinateTypeEnum = CoordinateTypeEnum.getByValue(coordinate.getCoordinateType());
        List<Coordinate> coordinates = new ArrayList<>();
        coordinates.add(coordinate);
        switch (coordinateTypeEnum) {
            case GPS84:
                coordinates.add(GisUtil.gps84ToGcj02(coordinate));
                coordinates.add(GisUtil.gps84ToBd09(coordinate));
                return BaseRes.success(coordinates);
            case GCJ02:
                coordinates.add(GisUtil.gcj02ToGps84(coordinate));
                coordinates.add(GisUtil.gcj02ToBd09(coordinate));
                return BaseRes.success(coordinates);
            case BD09:
                coordinates.add(GisUtil.bd09ToGcj02(coordinate));
                coordinates.add(GisUtil.bd09ToGps84(coordinate));
                return BaseRes.success(coordinates);
            default:
                return BaseRes.failure("坐标系类型有误");

        }

    }
}
