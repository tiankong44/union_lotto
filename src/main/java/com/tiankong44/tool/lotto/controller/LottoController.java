package com.tiankong44.tool.lotto.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.tiankong44.tool.base.entity.BaseRes;
import com.tiankong44.tool.lotto.entity.Draw;
import com.tiankong44.tool.lotto.entity.Lotto;
import com.tiankong44.tool.lotto.service.LottoService;
import com.tiankong44.tool.util.JsonUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
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
        List<Lotto> list = lottoService.list(new LambdaQueryWrapper<Lotto>().orderBy(true, false, Lotto::getPeriod));
        List<Draw> draws = new ArrayList<>();
        if (!list.isEmpty()) {
            for (Lotto lotto : list) {
                Draw draw = new Draw();
                draw.setPeriod(lotto.getPeriod());
                draw.setDayOfWeek(lotto.getDayOfWeek());
                draw.setDatetime(lotto.getDatetime());
                draw.setBlueBall(lotto.getBlueOne());
                List<Long> redBalls = new ArrayList<>();
                redBalls.add(lotto.getRedOne());
                redBalls.add(lotto.getRedTwo());
                redBalls.add(lotto.getRedTree());
                redBalls.add(lotto.getRedFour());
                redBalls.add(lotto.getRedFive());
                redBalls.add(lotto.getRedSix());
                draw.setRedBalls(redBalls);
                draws.add(draw);
            }
        }

        PageInfo<Draw> pageInfo = new PageInfo<>(draws);
        return BaseRes.success(pageInfo);
    }

    @PostMapping("/predict")
    public BaseRes predict() {
        Lotto lotto = lottoService.getPredict();
        Draw draw = new Draw();
        draw.setPeriod(lotto.getPeriod());
        draw.setDayOfWeek(lotto.getDayOfWeek());
        draw.setDatetime(lotto.getDatetime());
        draw.setBlueBall(lotto.getBlueOne());
        List<Long> redBalls = new ArrayList<>();
        redBalls.add(lotto.getRedOne());
        redBalls.add(lotto.getRedTwo());
        redBalls.add(lotto.getRedTree());
        redBalls.add(lotto.getRedFour());
        redBalls.add(lotto.getRedFive());
        redBalls.add(lotto.getRedSix());
        draw.setRedBalls(redBalls);

        return BaseRes.success(draw);
    }
}
