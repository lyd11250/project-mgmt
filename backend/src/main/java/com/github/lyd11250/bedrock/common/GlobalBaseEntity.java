package com.github.lyd11250.bedrock.common;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 全局表实体基类：不带 {@code tenant_id}（区别于 {@link BaseEntity}）。
 *
 * <p>用于平台维护、跨租户共享的全局表（如 {@code sys_menu} / {@code sys_package}），
 * 这些表已纳入多租户插件 {@code IGNORE_TABLES}，不参与租户隔离。
 * 审计字段由 {@link com.github.lyd11250.bedrock.config.AuditMetaObjectHandler} 自动填充。
 */
@Data
public class GlobalBaseEntity implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    @TableField(select = false)
    private Integer deleted;
}
