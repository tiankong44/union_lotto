package com.tiankong44.tool.pregnancy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 产检待办实体，保存计划事项和完成状态。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-08-06
 */
@Data
@TableName("antenatal_task")
public class AntenatalTask {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("client_record_id")
    private String clientRecordId;

    @TableField("title")
    private String title;

    @TableField("task_type")
    private String taskType;

    @TableField("planned_at")
    private LocalDateTime plannedAt;

    @TableField("status")
    private String status;

    @TableField("note")
    private String note;

    @TableField("completed_at")
    private LocalDateTime completedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
