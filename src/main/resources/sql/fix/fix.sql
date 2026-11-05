-- 为鱼缸资料增加用户填写的备注字段。
ALTER TABLE reef_tank ADD COLUMN note VARCHAR(1000) NULL COMMENT '鱼缸备注';

-- 为影像记录增加用户填写的备注字段。
ALTER TABLE reef_photo ADD COLUMN note VARCHAR(2000) NULL COMMENT '照片备注';

-- 为设备资料增加功率字段，单位为瓦。
ALTER TABLE reef_equipment ADD COLUMN power_watt DECIMAL(10,2) NULL COMMENT '设备功率瓦数';

-- 更新用户表的产品名称说明。
ALTER TABLE reef_user COMMENT='水境日记用户';
