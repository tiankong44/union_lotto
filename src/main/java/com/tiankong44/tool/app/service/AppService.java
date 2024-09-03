package com.tiankong44.tool.app.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tiankong44.tool.app.entity.App;
import com.tiankong44.tool.base.entity.BaseRes;

/**
 * (App)表服务接口
 *
 * @author makejava
 * @since 2022-11-01 15:15:33
 */
public interface AppService {

    BaseRes getAppList();
}

