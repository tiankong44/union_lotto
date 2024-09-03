package com.tiankong44.tool.mapper.master;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tiankong44.tool.tab.entity.Tab;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * (entity)表数据库访问层
 *
 * @author zhanghao
 * @since 2022-11-01 13:21:27
 */
@Mapper
public interface TabMapper extends BaseMapper<Tab> {


    int insertTab(@Param("entity") Tab entity);
}

