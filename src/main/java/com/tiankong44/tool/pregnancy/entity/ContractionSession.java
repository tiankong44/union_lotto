package com.tiankong44.tool.pregnancy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 宫缩记录实体，保存用户手动计时结果。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-08-06
 */
@Data
@TableName("contraction_session")
public class ContractionSession {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("client_record_id")
    private String clientRecordId;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("ended_at")
    private LocalDateTime endedAt;

    @TableField("duration_seconds")
    private Integer durationSeconds;

    @TableField("interval_seconds")
    private Integer intervalSeconds;

    @TableField("intensity")
    private Integer intensity;

    @TableField("note")
    private String note;

    @TableField("record_status")
    private String recordStatus;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
