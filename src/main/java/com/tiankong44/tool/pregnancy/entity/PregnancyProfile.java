package com.tiankong44.tool.pregnancy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 孕期档案实体，保存单用户孕期基础信息。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-08-06
 */
@Data
@TableName("pregnancy_profile")
public class PregnancyProfile {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("lmp_date")
    private LocalDate lmpDate;

    @TableField("due_date")
    private LocalDate dueDate;

    @TableField("baby_count")
    private Integer babyCount;

    @TableField("nickname")
    private String nickname;

    @TableField("note")
    private String note;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
