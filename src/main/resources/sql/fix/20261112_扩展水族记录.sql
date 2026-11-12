-- 为鱼缸增加用户自选水体、饲养主题、指标和归档状态；不推断旧鱼缸类型。
ALTER TABLE reef_tank ADD COLUMN water_type VARCHAR(24) NULL COMMENT '用户选择的水体类型';
ALTER TABLE reef_tank ADD COLUMN themes VARCHAR(255) NULL COMMENT '用户选择的饲养主题编码';
ALTER TABLE reef_tank ADD COLUMN enabled_metrics VARCHAR(1000) NULL COMMENT '用户选择的水质指标编码';
ALTER TABLE reef_tank ADD COLUMN archived TINYINT NOT NULL DEFAULT 0 COMMENT '是否归档，归档后保留历史记录';

-- 旧生物按一条个体记录兼容展示，不修改原有事件和状态。
ALTER TABLE reef_biology ADD COLUMN tracking_mode VARCHAR(16) NOT NULL DEFAULT 'SINGLE' COMMENT '个体或批次记录方式';
ALTER TABLE reef_biology ADD COLUMN current_tank_since DATE NULL COMMENT '进入当前鱼缸的日期';
ALTER TABLE reef_biology ADD COLUMN quantity INT NOT NULL DEFAULT 1 COMMENT '当前数量';
ALTER TABLE reef_biology ADD COLUMN position_note VARCHAR(255) NULL COMMENT '缸内位置';
ALTER TABLE reef_biology ADD COLUMN note VARCHAR(1000) NULL COMMENT '生物或植物补充说明';
ALTER TABLE reef_biology_event ADD COLUMN event_type VARCHAR(32) NULL COMMENT '用户选择的事件类型';
ALTER TABLE reef_biology_event ADD COLUMN from_tank_id BIGINT NULL COMMENT '事件发生前的鱼缸编号';
ALTER TABLE reef_biology_event ADD COLUMN to_tank_id BIGINT NULL COMMENT '转缸目标鱼缸编号';
ALTER TABLE reef_biology_event ADD COLUMN related_biology_id BIGINT NULL COMMENT '拆分转缸形成的关联生物编号';
ALTER TABLE reef_biology_event ADD COLUMN quantity INT NULL COMMENT '事件记录的数量';

-- 为日常维护记录补充可选的项目、用量、水源和光照信息。
ALTER TABLE reef_log ADD COLUMN activity_item VARCHAR(128) NULL COMMENT '饲料、肥料、补剂或维护项目';
ALTER TABLE reef_log ADD COLUMN activity_amount DECIMAL(16,4) NULL COMMENT '本次用量或处理量';
ALTER TABLE reef_log ADD COLUMN activity_unit VARCHAR(32) NULL COMMENT '本次用量单位';
ALTER TABLE reef_log ADD COLUMN water_source VARCHAR(128) NULL COMMENT '补水或换水来源';
ALTER TABLE reef_log ADD COLUMN water_volume_liter DECIMAL(12,3) NULL COMMENT '补水或换水升数';
ALTER TABLE reef_log ADD COLUMN light_hours DECIMAL(6,2) NULL COMMENT '记录的光照时长小时';

-- 保存新检测的逐项测量信息，原有六项历史值继续从日志表读取。
CREATE TABLE reef_water_measurement (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '水质测量主键',
    user_id BIGINT NOT NULL COMMENT '所属用户编号',
    tank_id BIGINT NOT NULL COMMENT '检测时所属鱼缸编号',
    log_id BIGINT NOT NULL COMMENT '所属检测日志编号',
    recorded_at DATETIME NOT NULL COMMENT '测量时间',
    metric VARCHAR(48) NOT NULL COMMENT '水质指标编码',
    measured_value DECIMAL(16,6) NOT NULL COMMENT '用户填写的测量值',
    unit VARCHAR(32) NOT NULL COMMENT '测量值单位',
    method VARCHAR(128) NULL COMMENT '测量工具或方法',
    PRIMARY KEY (id),
    UNIQUE KEY uk_reef_measurement_log_metric (user_id, log_id, metric),
    KEY idx_reef_measurement_trend (user_id, tank_id, metric, recorded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鱼缸水质测量明细';

-- 恢复批次和编号映射限定当前用户，并为重复恢复提供幂等校验。
CREATE TABLE reef_backup_restore (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '恢复批次主键',
    user_id BIGINT NOT NULL COMMENT '执行恢复的用户编号',
    backup_key VARCHAR(64) NOT NULL COMMENT '备份随机编号',
    created_at DATETIME NOT NULL COMMENT '恢复时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_reef_backup_restore (user_id, backup_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鱼缸备份恢复批次';

CREATE TABLE reef_backup_mapping (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '映射主键',
    user_id BIGINT NOT NULL COMMENT '所属用户编号',
    backup_key VARCHAR(64) NOT NULL COMMENT '备份随机编号',
    entity_type VARCHAR(24) NOT NULL COMMENT '记录类别',
    original_id VARCHAR(32) NOT NULL COMMENT '备份内原编号',
    restored_id BIGINT NOT NULL COMMENT '恢复后的记录编号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_reef_backup_mapping (user_id, backup_key, entity_type, original_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鱼缸备份恢复编号映射';
