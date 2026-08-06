package com.tiankong44.tool.pregnancy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 胎动会话实体，记录一次连续胎动观察。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-08-06
 */
@Data
@TableName("fetal_movement_session")
public class FetalMovementSession {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("client_record_id")
    private String clientRecordId;

    @TableField("session_mode")
    private String sessionMode;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("ended_at")
    private LocalDateTime endedAt;

    @TableField("movement_count")
    private Integer movementCount;

    @TableField("target_count")
    private Integer targetCount;

    @TableField("average_strength")
    private Integer averageStrength;

    @TableField("note")
    private String note;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
