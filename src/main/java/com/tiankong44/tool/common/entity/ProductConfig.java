package com.tiankong44.tool.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 产品key和企业关系
 *
 * @Author zhanghao_SMEICS
 * @Date 2023/10/16  21:01
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductConfig implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    /**
     * 产品key
     */
    private String productKey;

    /**
     * 企业id
     */
    private String orgid;
}
