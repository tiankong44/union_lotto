package com.tiankong44.tool.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tiankong44.tool.app.entity.App;
import com.tiankong44.tool.app.service.AppService;
import com.tiankong44.tool.base.entity.BaseRes;
import com.tiankong44.tool.mapper.slave.AppMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * (App)表服务实现类
 *
 * @author makejava
 * @since 2022-11-01 15:15:33
 */
@Service("appService")
public class AppServiceImpl implements AppService {


    @Resource
    AppMapper appMapper;

    @Override
    public BaseRes getAppList() {
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.orderBy(true, true, "name");
        List<App> list = appMapper.selectList(queryWrapper);
        return BaseRes.success(list);
    }
}

