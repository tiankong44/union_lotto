package com.tiankong44.tool.lotto.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tiankong44.tool.lotto.entity.Lotto;
import com.tiankong44.tool.lotto.service.LottoService;
import com.tiankong44.tool.mapper.LottoMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @Description :
 * @Author zhanghao_SMEICS
 * @Date 2022/11/2  16:35
 **/
@Service
public class LottoServiceImpl extends ServiceImpl<LottoMapper, Lotto> implements LottoService {
    @Resource
    LottoMapper lottoMapper;

    @Override
    public Lotto getPredict() {
        return lottoMapper.getPredict();
    }
}
