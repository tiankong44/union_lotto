package com.tiankong44.tool.app.entity;

import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * (App)表实体类
 *
 * @author makejava
 * @since 2022-11-01 15:15:33
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class App extends Model<App> {
    //主键
    private Long id;
    //应用名称
    private String name;
    //图标地址
    private String icon;


}

