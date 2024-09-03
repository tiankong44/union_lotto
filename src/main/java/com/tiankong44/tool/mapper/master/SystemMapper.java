package com.tiankong44.tool.mapper.master;


import com.baomidou.dynamic.datasource.annotation.DS;
import com.tiankong44.tool.common.entity.Appearance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * (entity)表数据库访问层
 *
 * @author zhanghao
 * @since 2022-11-01 13:21:27
 */
@Mapper
public interface SystemMapper {

    @Select("select password from  tk_tabs.system limit 1")
    String getPassword();

    // @DS("slave")
    @Select("select background_mode,background_url from  tk_tabs.appearance limit 1")
    Appearance getAppearance();
}

