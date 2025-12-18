package com.tiankong44.tool.apex.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tiankong44.tool.apex.entity.ApexUserStats;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ApexUserStatsMapper extends BaseMapper<ApexUserStats> {
    @Update("UPDATE apex_user_stats SET total_opened = total_opened + #{count} WHERE user_id = #{userId}")
    int incrementTotalOpened(@Param("userId") String userId, @Param("count") int count);
}