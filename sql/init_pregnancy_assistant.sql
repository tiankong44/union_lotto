-- 孕期助手全新环境初始化脚本
-- 说明：本脚本只负责建表，不包含外键和存储过程；历史数据修复脚本待补充。

CREATE TABLE IF NOT EXISTS pregnancy_profile (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    singleton_key TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '单用户固定键',
    lmp_date DATE NULL COMMENT '末次月经日期',
    due_date DATE NULL COMMENT '预产期',
    baby_count TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '胎儿数量',
    nickname VARCHAR(64) NULL COMMENT '孕期称呼',
    note VARCHAR(500) NULL COMMENT '档案备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_pregnancy_profile_singleton (singleton_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='孕期档案';

CREATE TABLE IF NOT EXISTS fetal_movement_session (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    client_record_id VARCHAR(64) NOT NULL COMMENT '客户端幂等记录编号',
    session_mode VARCHAR(20) NOT NULL DEFAULT 'free' COMMENT '会话模式',
    started_at DATETIME(3) NOT NULL COMMENT '开始时间',
    ended_at DATETIME(3) NOT NULL COMMENT '结束时间',
    movement_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '胎动次数',
    target_count INT UNSIGNED NULL COMMENT '目标次数，仅用于记录模式',
    average_strength TINYINT UNSIGNED NULL COMMENT '用户主观平均强度',
    note VARCHAR(500) NULL COMMENT '会话备注',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_fetal_movement_client (client_record_id),
    KEY idx_fetal_movement_started (started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='胎动会话';

CREATE TABLE IF NOT EXISTS fetal_movement_event (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    client_record_id VARCHAR(64) NOT NULL COMMENT '客户端幂等记录编号',
    session_client_record_id VARCHAR(64) NULL COMMENT '所属会话客户端编号',
    occurred_at DATETIME(3) NOT NULL COMMENT '发生时间',
    strength TINYINT UNSIGNED NULL COMMENT '用户主观强度',
    movement_type VARCHAR(32) NULL COMMENT '胎动类型',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_fetal_event_client (client_record_id),
    KEY idx_fetal_event_occurred (occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='单次胎动事件';

CREATE TABLE IF NOT EXISTS contraction_session (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    client_record_id VARCHAR(64) NOT NULL COMMENT '客户端幂等记录编号',
    started_at DATETIME(3) NOT NULL COMMENT '开始时间',
    ended_at DATETIME(3) NOT NULL COMMENT '结束时间',
    duration_seconds INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '持续秒数',
    interval_seconds INT UNSIGNED NULL COMMENT '与上一条间隔秒数',
    intensity TINYINT UNSIGNED NULL COMMENT '用户主观强度',
    note VARCHAR(500) NULL COMMENT '记录备注',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_contraction_client (client_record_id),
    KEY idx_contraction_started (started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='宫缩记录';

CREATE TABLE IF NOT EXISTS pregnancy_health_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    client_record_id VARCHAR(64) NOT NULL COMMENT '客户端幂等记录编号',
    record_type VARCHAR(32) NOT NULL COMMENT '记录类型',
    value_json TEXT NOT NULL COMMENT '记录值 JSON，不保存诊断结论',
    unit VARCHAR(16) NULL COMMENT '单位',
    recorded_at DATETIME(3) NOT NULL COMMENT '记录时间',
    note VARCHAR(500) NULL COMMENT '记录备注',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_health_client (client_record_id),
    KEY idx_health_type_recorded (record_type, recorded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='孕期健康记录';

CREATE TABLE IF NOT EXISTS antenatal_task (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    client_record_id VARCHAR(64) NOT NULL COMMENT '客户端幂等记录编号',
    title VARCHAR(128) NOT NULL COMMENT '待办标题',
    task_type VARCHAR(32) NOT NULL DEFAULT 'custom' COMMENT '待办类型',
    planned_at DATETIME(3) NOT NULL COMMENT '计划时间',
    status VARCHAR(16) NOT NULL DEFAULT 'TODO' COMMENT '待办状态',
    note VARCHAR(500) NULL COMMENT '待办备注',
    completed_at DATETIME(3) NULL COMMENT '完成时间',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_task_client (client_record_id),
    KEY idx_task_status_planned (status, planned_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='产检待办';

CREATE TABLE IF NOT EXISTS reminder_setting (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    task_client_record_id VARCHAR(64) NULL COMMENT '关联待办客户端编号',
    reminder_type VARCHAR(32) NOT NULL COMMENT '提醒类型',
    remind_at DATETIME(3) NULL COMMENT '提醒时间',
    enabled TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '是否启用',
    config_json TEXT NULL COMMENT '提醒配置',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_reminder_enabled (enabled, remind_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提醒设置';

CREATE TABLE IF NOT EXISTS cloud_file (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    file_key VARCHAR(64) NOT NULL COMMENT '随机文件编号',
    original_name VARCHAR(255) NULL COMMENT '原始文件名',
    content_type VARCHAR(128) NOT NULL COMMENT '文件媒体类型',
    file_size BIGINT UNSIGNED NOT NULL COMMENT '文件字节数',
    file_md5 CHAR(32) NOT NULL COMMENT '文件 MD5',
    file_content MEDIUMBLOB NOT NULL COMMENT '文件二进制内容',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_cloud_file_key (file_key),
    KEY idx_cloud_file_md5 (file_md5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='云端文件';
