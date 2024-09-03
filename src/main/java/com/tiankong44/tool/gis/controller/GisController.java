package com.tiankong44.tool.gis.controller;

import com.tiankong44.tool.base.entity.BaseRes;
import com.tiankong44.tool.exception.customException.DifferentCoordinateException;
import com.tiankong44.tool.gis.entity.Coordinate;
import com.tiankong44.tool.gis.service.GisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * WGS84坐标系	地球坐标系，国际通用坐标系
 * GCJ02坐标系	火星坐标系，WGS84坐标系加密后的坐标系；Google国内地图、高德、QQ地图 使用
 * BD09坐标系	百度坐标系，GCJ02坐标系加密后的坐标系
 *
 * @author zhanghao_SMEICS
 * @date 2022-10-21 21:45
 */
@RestController("/gis")
@Validated

public class GisController {
    @Autowired
    GisService gisService;



    /**
     * WGS84坐标系转GCJ02坐标系
     *
     * @author zhanghao_SMEICS
     * @date 2022/10/21 22:20
     */
    @PostMapping("/coordinateTransform")
    public BaseRes coordinateTransform(@RequestBody @Valid Coordinate coordinate) {
        return gisService.coordinateTransform(coordinate);
    }

    /**
     * WGS84坐标系转GCJ02坐标系
     *
     * @author zhanghao_SMEICS
     * @date 2022/10/21 22:20
     */
    @PostMapping("/WGS84ToGCJ02")
    public BaseRes WGS84ToGCJ02(@RequestBody @Valid Coordinate coordinate) {
        return gisService.WGS84ToGCJ02(coordinate);
    }

    /**
     * GCJ02坐标系转WGS84坐标系
     *
     * @author zhanghao_SMEICS
     * @date 2022/10/21 22:20
     */

    @PostMapping("/GCJ02ToWGS84")
    public BaseRes GCJ02ToWGS84(@RequestBody @Valid Coordinate coordinate) {
        return gisService.GCJ02ToWGS84(coordinate);
    }

    /**
     * 获取两点或多个点之间的距离
     *
     * @author zhanghao_SMEICS
     * @date 2022/10/21 22:20
     */

    @PostMapping("/getDistance")
    public BaseRes getDistance(@RequestBody List<Coordinate> coordinates) throws DifferentCoordinateException {
        return gisService.getDistance(coordinates);
    }
}
