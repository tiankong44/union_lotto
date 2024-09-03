package com.tiankong44.tool.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Description :
 * @Author zhanghao_SMEICS
 * @Date 2023/11/15  14:47
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseEntity {
    private String appId;
    private String appSecret;
}
