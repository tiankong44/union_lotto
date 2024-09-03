package com.tiankong44.tool.lotto.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.tiankong44.tool.base.entity.BaseRes;
import com.tiankong44.tool.lotto.entity.Lotto;
import com.tiankong44.tool.lotto.service.LottoService;
import com.tiankong44.tool.util.JsonUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * @Description :
 * @Author zhanghao_SMEICS
 * @Date 2024/9/3  13:04
 **/
@RestController
@RequestMapping("lotto")
public class LottoController {
    @Resource
    LottoService lottoService;

    @PostMapping("/history")
    public BaseRes history(@RequestBody String requestJson) {
        JSONObject requestJSON = JSON.parseObject(requestJson);
        Map<?, ?> checkMap = JsonUtils.noNulls(requestJSON, "pageSize", "pageNum");
        if (checkMap != null) {
            return BaseRes.failure(checkMap.get("desc").toString());
        }
        PageHelper.startPage(requestJSON.getInteger("pageNum"), requestJSON.getInteger("pageSize"), true);
        List<Lotto> list = lottoService.list();
        PageInfo<Lotto> pageInfo = new PageInfo<>(list);
        return BaseRes.success(pageInfo);
    }

    @PostMapping("/predict")
    public BaseRes predict() {
        Lotto predict = lottoService.getPredict();
        return BaseRes.success(predict);
    }
}
