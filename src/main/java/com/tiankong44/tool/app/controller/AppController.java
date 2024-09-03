package com.tiankong44.tool.app.controller;


import com.tiankong44.tool.app.service.AppService;
import com.tiankong44.tool.base.entity.BaseRes;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * (App)表控制层
 *
 * @author makejava
 * @since 2022-11-01 15:15:32
 */
@RestController
@RequestMapping("app")
public class AppController {
    @Resource
    AppService appService;

    /**
     * 获取bing壁纸地址
     *
     * @return 新增结果
     */
    @PostMapping("/getAppList")
    public BaseRes getAppList() {
        return appService.getAppList();
    }


}

