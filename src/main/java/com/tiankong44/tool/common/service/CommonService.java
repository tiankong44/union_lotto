package com.tiankong44.tool.common.service;

import com.tiankong44.tool.base.entity.BaseRes;
import org.springframework.web.multipart.MultipartFile;

/**

 * @Description :
 * @Author zhanghao_SMEICS
 * @Date 2022/11/2  16:34
 **/
public interface CommonService {

    BaseRes getBackgroundImage(int clientType);

    BaseRes uploadIcon(MultipartFile file);

    BaseRes getSettingData();
}
