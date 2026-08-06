package com.tiankong44.tool.pregnancy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 孕期健康记录实体，保存体重、血压和症状等用户自述数据。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-08-06
 */
@Data
@TableName("pregnancy_health_record")
public class PregnancyHealthRecord {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("client_record_id")
    private String clientRecordId;

    @TableField("record_type")
    private String recordType;

    @TableField("value_json")
    private String valueJson;

    @TableField("unit")
    private String unit;

    @TableField("recorded_at")
    private LocalDateTime recordedAt;

    @TableField("note")
    private String note;

    @TableField("record_status")
    private String recordStatus;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
