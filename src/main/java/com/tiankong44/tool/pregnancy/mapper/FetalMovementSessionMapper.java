package com.tiankong44.tool.pregnancy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tiankong44.tool.pregnancy.entity.FetalMovementSession;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 胎动会话数据访问接口。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-08-06
 */
@Mapper
public interface FetalMovementSessionMapper extends BaseMapper<FetalMovementSession> {
    /**
     * 删除指定胎动会话下的单次胎动事件，避免逻辑从属数据残留。
     *
     * @param clientRecordId 会话客户端编号
     * @return 删除的事件数量
     */
    @Delete("DELETE FROM fetal_movement_event WHERE session_client_record_id = #{clientRecordId}")
    int deleteEventsBySessionClientRecordId(@Param("clientRecordId") String clientRecordId);
}
