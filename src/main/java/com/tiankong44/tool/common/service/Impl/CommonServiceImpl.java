package com.tiankong44.tool.common.service.Impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tiankong44.tool.base.entity.BaseRes;
import com.tiankong44.tool.common.entity.Appearance;
import com.tiankong44.tool.common.entity.ClientEnum;
import com.tiankong44.tool.common.entity.Image;
import com.tiankong44.tool.common.service.CommonService;
import com.tiankong44.tool.mapper.slave.AppearanceMapper;
import com.tiankong44.tool.util.RedisUtil;
import com.tiankong44.tool.util.getImageUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @Description :
 * @Author zhanghao_SMEICS
 * @Date 2022/11/2  16:35
 **/
@Service

public class CommonServiceImpl implements CommonService {

    @Resource
    RedisUtil redisUtil;

    @Resource
    AppearanceMapper appearanceMapper;

    @Value("${images-url-prefix}")
    String imagesUrlPrefix;
    @Value("${spring.servlet.multipart.location}")
    String uploadPath;
    private static String defaultImage = "https://cn.bing.com/th?id=OHR.BridgeofSighs_ZH-CN5414607871_1920x1080.jpg&rf=LaDigue_1920x1080.jpg&pid=hp";  // 图片地址前缀

    private static List<Image> images = new ArrayList<>();

    @Override
    public BaseRes getBackgroundImage(int clientType) {
        Appearance appearance = appearanceMapper.selectById(1);
        if (appearance.getBackgroundMode() == 0) {
            List<Image> images = new ArrayList<>();
            String redisKey = "";
            if (clientType == ClientEnum.PC.getValue()) {
                redisKey = ClientEnum.PC.getLabel();
            } else if (clientType == ClientEnum.MOBILE.getValue()) {
                redisKey = ClientEnum.MOBILE.getLabel();
            }
            String s = redisUtil.lLeftPop(redisKey);
            JSONArray array = JSONArray.parseArray(s);
            if (array == null || array.size() == 0) {
                images = getImageUtil.getImages(clientType);
                if (images.size() > 0) {
                    redisUtil.lLeftPush(redisKey, JSONArray.toJSON(images).toString());
                    redisUtil.expire(redisKey, 1, TimeUnit.DAYS);
                } else {
                    return BaseRes.success(defaultImage);
                }

            } else {
                images = array.toJavaList(Image.class);
            }

            return BaseRes.success(images.get(RandomUtil.randomInt(0, images.size())).getUrl());
        } else {
            return BaseRes.success(appearance.getBackgroundUrl());
        }
    }

    @Override
    public BaseRes uploadIcon(MultipartFile uploadFile) {

        if (uploadFile != null) {
            //整个文件名，名称+后缀
            String fileAllName = uploadFile.getOriginalFilename();
            if (!StrUtil.isEmpty(fileAllName)) {
                String suffix = fileAllName.substring(fileAllName.lastIndexOf("."));
                String fileName = StrUtil.uuid().replaceAll("-", "") + suffix;
                long fileSize = uploadFile.getSize();
                if (fileSize > 1024 * 1024 * 10) {
                    return BaseRes.failure("图片大小在50M以内!");
                }
                DateTime date = new DateTime();
                int year = date.year();
                int month = date.month()+1;

                try {
                    String yearPath = uploadPath + "/" + year;
                    File yearFile = new File(yearPath);
                    if (!yearFile.exists()) {
                        yearFile.mkdir();
                    }
                    String yearMonth = yearPath + "/" + month;
                    File yearMonthPath = new File(yearMonth);
                    if (!yearMonthPath.exists()) {
                        yearMonthPath.mkdir();
                    }
                    String fileAll = yearMonth + "/" + fileName;
                    System.out.println("地址============" + fileAll);
                    // 创建输出流
                    FileOutputStream fos = new FileOutputStream(fileAll);
                    fos.write(uploadFile.getBytes());
                    fos.flush();
                    fos.close();
                    String relativePath = imagesUrlPrefix + year + "/" + month + "/" + fileName;
                    return BaseRes.success(relativePath);
                } catch (Exception e) {
                    e.printStackTrace();
                    return BaseRes.failure("上传文件异常");

                }
            } else {
                return BaseRes.failure("上传文件失败");
            }
        }

        return BaseRes.success();
    }

    @Override
    public BaseRes getSettingData() {
        JSONObject resObject = new JSONObject();
        /**
         *  基础设置
         */

        Appearance appearance = appearanceMapper.selectById(1);
        resObject.put("base", appearance);

        return BaseRes.success(resObject);
    }
}
