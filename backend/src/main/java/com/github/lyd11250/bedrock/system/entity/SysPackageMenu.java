package com.github.lyd11250.bedrock.system.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 套餐-菜单关联（全局表，不带 {@code tenant_id}；无 {@code updated_*} 列，故不继承基类）。
 */
@Data
@TableName("sys_package_menu")
public class SysPackageMenu implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long packageId;

    private Long menuId;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableLogic
    @TableField(select = false)
    private Integer deleted;
}
