-- 为用户照片增加动态片段文件编号，片段内容继续保存在共享云端文件表。
ALTER TABLE reef_photo
    ADD COLUMN motion_file_key VARCHAR(64) NULL COMMENT '实况照片动态片段云端编号' AFTER file_key;
