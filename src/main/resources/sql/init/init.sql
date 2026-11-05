-- 水境日记全新环境初始化脚本
-- 本脚本仅创建业务表，不添加外键或存储过程。

-- 保存微信用户的服务端身份映射。
CREATE TABLE reef_user (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户主键',
    openid VARCHAR(128) NOT NULL COMMENT '微信小程序用户唯一标识',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_reef_user_openid (openid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='水境日记用户';

-- 保存用户维护的鱼缸基本资料。
CREATE TABLE reef_tank (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '鱼缸主键',
    user_id BIGINT NOT NULL COMMENT '所属用户编号',
    name VARCHAR(128) NOT NULL COMMENT '鱼缸名称',
    started_at DATE NOT NULL COMMENT '开缸日期',
    capacity_liter DECIMAL(10,2) NULL COMMENT '鱼缸容量升数',
    system_type VARCHAR(64) NULL COMMENT '系统类型',
    note VARCHAR(1000) NULL COMMENT '鱼缸备注',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_reef_tank_user (user_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='海缸鱼缸';

-- 按鱼缸和参数保存用户设定的水质上下限。
CREATE TABLE reef_water_range (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '范围主键',
    user_id BIGINT NOT NULL COMMENT '所属用户编号',
    tank_id BIGINT NOT NULL COMMENT '鱼缸编号',
    metric VARCHAR(32) NOT NULL COMMENT '水质参数编码',
    minimum_value DECIMAL(12,4) NULL COMMENT '用户设置的下限',
    maximum_value DECIMAL(12,4) NULL COMMENT '用户设置的上限',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_reef_water_range (user_id, tank_id, metric),
    KEY idx_reef_water_range_tank (user_id, tank_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='海缸水质范围';

-- 保存鱼、珊瑚和其他缸内生物资料。
CREATE TABLE reef_biology (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '生物主键',
    user_id BIGINT NOT NULL COMMENT '所属用户编号',
    tank_id BIGINT NOT NULL COMMENT '所属鱼缸编号',
    name VARCHAR(128) NOT NULL COMMENT '生物名称',
    scientific_name VARCHAR(128) NULL COMMENT '学名',
    category VARCHAR(32) NOT NULL COMMENT '生物分类',
    added_at DATE NOT NULL COMMENT '入缸日期',
    body_length_cm DECIMAL(8,2) NULL COMMENT '体长厘米',
    source VARCHAR(128) NULL COMMENT '生物来源',
    status VARCHAR(24) NOT NULL COMMENT '用户维护的生物状态',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_reef_biology_user_tank (user_id, tank_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='海缸生物';

-- 保存生物入缸、检疫、隔离和治疗等事件。
CREATE TABLE reef_biology_event (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '事件主键',
    user_id BIGINT NOT NULL COMMENT '所属用户编号',
    biology_id BIGINT NOT NULL COMMENT '生物编号',
    event_at DATETIME NOT NULL COMMENT '事件发生时间',
    title VARCHAR(128) NOT NULL COMMENT '事件标题',
    description VARCHAR(1000) NULL COMMENT '事件描述',
    status VARCHAR(24) NULL COMMENT '事件对应状态',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_reef_biology_event (user_id, biology_id, event_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='海缸生物事件';

-- 保存水质、换水、喂食、用药和观察日志。
CREATE TABLE reef_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志主键',
    user_id BIGINT NOT NULL COMMENT '所属用户编号',
    tank_id BIGINT NOT NULL COMMENT '所属鱼缸编号',
    type VARCHAR(32) NOT NULL COMMENT '日志类型',
    title VARCHAR(128) NOT NULL COMMENT '日志标题',
    description VARCHAR(1000) NULL COMMENT '日志摘要',
    recorded_at DATETIME NOT NULL COMMENT '记录时间',
    note VARCHAR(2000) NULL COMMENT '日志备注',
    change_percent DECIMAL(6,2) NULL COMMENT '换水比例百分数',
    salinity DECIMAL(12,4) NULL COMMENT '盐度比重',
    temperature DECIMAL(8,3) NULL COMMENT '水温摄氏度',
    ph DECIMAL(8,4) NULL COMMENT '酸碱度',
    no2 DECIMAL(12,4) NULL COMMENT '亚硝酸盐毫克每升',
    no3 DECIMAL(12,4) NULL COMMENT '硝酸盐毫克每升',
    po4 DECIMAL(12,4) NULL COMMENT '磷酸盐毫克每升',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_reef_log_user_time (user_id, recorded_at),
    KEY idx_reef_log_tank_type_time (user_id, tank_id, type, recorded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='海缸日志';

-- 保存日志与生物的多对多关联。
CREATE TABLE reef_log_biology (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '关联主键',
    user_id BIGINT NOT NULL COMMENT '所属用户编号',
    log_id BIGINT NOT NULL COMMENT '日志编号',
    biology_id BIGINT NOT NULL COMMENT '生物编号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_reef_log_biology (user_id, log_id, biology_id),
    KEY idx_reef_log_biology_log (user_id, log_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='海缸日志生物关联';

-- 保存设备资料及设备维护周期。
CREATE TABLE reef_equipment (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '设备主键',
    user_id BIGINT NOT NULL COMMENT '所属用户编号',
    tank_id BIGINT NULL COMMENT '所属鱼缸编号',
    category VARCHAR(32) NOT NULL COMMENT '设备分类',
    name VARCHAR(128) NOT NULL COMMENT '设备名称',
    model VARCHAR(128) NULL COMMENT '设备型号',
    status VARCHAR(24) NOT NULL COMMENT '设备运行状态',
    power_watt DECIMAL(10,2) NULL COMMENT '设备功率瓦数',
    daily_hours DECIMAL(6,2) NULL COMMENT '每日运行小时数',
    started_at DATE NULL COMMENT '开始运行日期',
    maintenance_interval_days INT NULL COMMENT '维护间隔天数',
    last_maintained_at DATE NULL COMMENT '上次维护日期',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_reef_equipment_user (user_id, category, id),
    KEY idx_reef_equipment_maintenance (user_id, last_maintained_at, maintenance_interval_days)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='海缸设备';

-- 保存用户照片与云端文件编号的归属关系。
CREATE TABLE reef_photo (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '照片主键',
    user_id BIGINT NOT NULL COMMENT '所属用户编号',
    file_key VARCHAR(64) NOT NULL COMMENT '云端文件随机编号',
    category VARCHAR(32) NOT NULL COMMENT '照片分类',
    title VARCHAR(255) NULL COMMENT '照片名称',
    reference_type VARCHAR(32) NULL COMMENT '关联对象类型',
    reference_id VARCHAR(32) NULL COMMENT '关联对象编号',
    recorded_at DATETIME NOT NULL COMMENT '照片记录时间',
    note VARCHAR(2000) NULL COMMENT '照片备注',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_reef_photo_user_time (user_id, recorded_at),
    KEY idx_reef_photo_reference (user_id, reference_type, reference_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='海缸影像记录';

-- 保存用户统一设置的提醒发送时刻。
CREATE TABLE reef_reminder_preference (
    user_id BIGINT NOT NULL COMMENT '用户编号',
    send_time TIME NULL COMMENT '用户设置的上海时间提醒时刻',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='海缸提醒偏好';

-- 保存各类提醒的开启状态。
CREATE TABLE reef_reminder_setting (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '提醒设置主键',
    user_id BIGINT NOT NULL COMMENT '所属用户编号',
    type VARCHAR(32) NOT NULL COMMENT '提醒类型',
    enabled TINYINT NOT NULL COMMENT '是否启用',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_reef_reminder_setting (user_id, type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='海缸提醒开关';

-- 保存用户主动授权的订阅消息额度。
CREATE TABLE reef_subscription_quota (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '授权额度主键',
    user_id BIGINT NOT NULL COMMENT '所属用户编号',
    type VARCHAR(32) NOT NULL COMMENT '提醒类型',
    available_count INT NOT NULL COMMENT '可发送通知额度',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_reef_subscription_quota (user_id, type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='海缸订阅授权额度';

-- 保存提醒发送任务，唯一任务编号用于防止多实例重复发送。
CREATE TABLE reef_reminder_delivery (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '提醒任务主键',
    task_key VARCHAR(160) NOT NULL COMMENT '提醒周期唯一编号',
    user_id BIGINT NOT NULL COMMENT '所属用户编号',
    type VARCHAR(32) NOT NULL COMMENT '提醒类型',
    entity_id BIGINT NULL COMMENT '关联设备编号',
    due_at DATETIME NOT NULL COMMENT '提醒到期时间',
    status VARCHAR(24) NOT NULL COMMENT '发送任务状态',
    last_error VARCHAR(500) NULL COMMENT '最近失败原因',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_reef_reminder_task (task_key),
    KEY idx_reef_reminder_due (status, due_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='海缸提醒发送任务';
