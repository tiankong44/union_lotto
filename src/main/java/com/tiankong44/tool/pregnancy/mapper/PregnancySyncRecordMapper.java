package com.tiankong44.tool.pregnancy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tiankong44.tool.pregnancy.entity.PregnancySyncRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 孕期同步记录数据访问接口。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-08-06
 */
@Mapper
public interface PregnancySyncRecordMapper extends BaseMapper<PregnancySyncRecord> {
}
