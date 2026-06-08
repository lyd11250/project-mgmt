package com.github.lyd11250.project.common;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体基类：所有业务表共有字段。
 *
 * <p>{@code tenant_id} 由 MyBatis-Plus 多租户插件自动写入/过滤；
 * 审计字段由 {@link com.github.lyd11250.project.config.AuditMetaObjectHandler} 自动填充；
 * {@code deleted} 为逻辑删除标记。
 */
@Data
public class BaseEntity implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户 ID（多租户隔离，由插件维护，业务无需手动设置）。 */
    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 逻辑删除：0 未删除，1 已删除。 */
    @TableLogic
    @TableField(select = false)
    private Integer deleted;
}
