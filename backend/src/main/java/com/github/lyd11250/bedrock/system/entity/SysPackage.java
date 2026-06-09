package com.github.lyd11250.bedrock.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.lyd11250.bedrock.common.GlobalBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 套餐（平台定义，= 菜单子集）。全局表，不带 {@code tenant_id}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_package")
public class SysPackage extends GlobalBaseEntity {

    private String name;

    /** 套餐码（全局唯一），如 BASIC / FULL。 */
    private String code;

    /** 状态：1 启用，0 停用。 */
    private Integer status;

    private String remark;
}
