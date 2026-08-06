-- 云端文件表结构变更脚本
-- 说明：本脚本只新增云端文件表，不迁移或删除历史磁盘文件。

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
