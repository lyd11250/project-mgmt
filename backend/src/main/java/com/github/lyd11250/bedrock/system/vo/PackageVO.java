package com.github.lyd11250.bedrock.system.vo;

import lombok.Data;

/**
 * 套餐视图对象。
 */
@Data
public class PackageVO {

    private Long id;

    private String name;

    private String code;

    private Integer status;

    private String remark;
}
