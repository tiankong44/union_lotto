package com.tiankong44.tool.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tiankong44.tool.common.entity.CloudFile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 云端文件数据访问 Mapper。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-08-06
 */
@Mapper
public interface CloudFileMapper extends BaseMapper<CloudFile> {
}
