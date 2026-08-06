package com.tiankong44.tool.pregnancy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 本地优先同步记录实体，使用客户端记录编号实现幂等上传。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-08-06
 */
@Data
@TableName("pregnancy_sync_record")
public class PregnancySyncRecord {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("entity_type")
    private String entityType;

    @TableField("client_record_id")
    private String clientRecordId;

    @TableField("payload")
    private String payload;

    @TableField("payload_hash")
    private String payloadHash;

    @TableField("sync_status")
    private String syncStatus;

    @TableField("last_error")
    private String lastError;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
