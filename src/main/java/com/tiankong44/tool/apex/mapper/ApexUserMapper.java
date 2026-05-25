package com.tiankong44.tool.apex.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tiankong44.tool.apex.entity.ApexUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ApexUserMapper extends BaseMapper<ApexUser> {
}