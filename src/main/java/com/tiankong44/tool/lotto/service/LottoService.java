package com.tiankong44.tool.lotto.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tiankong44.tool.lotto.entity.Lotto;

/**

 * @Description :
 * @Author zhanghao_SMEICS
 * @Date 2022/11/2  16:34
 **/
public interface LottoService extends IService<Lotto> {


    Lotto getPredict();
}
