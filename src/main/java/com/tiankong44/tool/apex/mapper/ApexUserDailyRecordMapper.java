package com.tiankong44.tool.apex.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tiankong44.tool.apex.entity.ApexUserDailyRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ApexUserDailyRecordMapper extends BaseMapper<ApexUserDailyRecord> {
    @Select("SELECT * FROM apex_user_daily_records WHERE user_id = #{userId} ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<ApexUserDailyRecord> selectByUserIdWithLimit(@Param("userId") String userId, @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT * FROM apex_user_daily_records WHERE user_id = #{userId} AND record_date = #{date} ORDER BY id DESC")
    List<ApexUserDailyRecord> selectByUserIdAndDate(@Param("userId") String userId, @Param("date") LocalDate date);
}