package com.tiankong44.tool.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tiankong44.tool.lotto.entity.Lotto;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Description :
 * @Author zhanghao_SMEICS
 * @Date 2024/9/3  13:13
 **/
@Mapper
public interface LottoMapper extends BaseMapper<Lotto> {

    Lotto getPredict();
}
