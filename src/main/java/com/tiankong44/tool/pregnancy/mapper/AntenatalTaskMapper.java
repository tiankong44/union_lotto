package com.tiankong44.tool.pregnancy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tiankong44.tool.pregnancy.entity.AntenatalTask;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 产检待办数据访问接口。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-08-06
 */
@Mapper
public interface AntenatalTaskMapper extends BaseMapper<AntenatalTask> {
    /**
     * 删除指定待办关联的提醒设置，避免待办删除后继续保留无效提醒。
     *
     * @param clientRecordId 待办客户端编号
     * @return 删除的提醒设置数量
     */
    @Delete("DELETE FROM reminder_setting WHERE task_client_record_id = #{clientRecordId}")
    int deleteReminderSettingsByTaskClientRecordId(@Param("clientRecordId") String clientRecordId);
}
