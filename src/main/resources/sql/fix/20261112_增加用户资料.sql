-- 为现有水境日记用户增加可选的昵称和共享云端头像编号。
ALTER TABLE reef_user ADD COLUMN nickname VARCHAR(64) NULL COMMENT '用户主动填写的昵称';
ALTER TABLE reef_user ADD COLUMN avatar_file_key VARCHAR(64) NULL COMMENT '用户头像的云端文件编号';
