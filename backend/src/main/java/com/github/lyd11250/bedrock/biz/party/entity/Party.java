package com.github.lyd11250.bedrock.biz.party.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.lyd11250.bedrock.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 相关方（统一主表）：人员与组织的共同身份（Fowler Party 模式）。
 *
 * <p>{@code partyType} 区分类型（PERSON/ORGANIZATION）；{@code id} 与子表
 * （{@link PartyPerson} / {@link PartyOrganization}）共享，其他业务模块统一以该 id（party_id）引用相关方。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("party")
public class Party extends BaseEntity {

    /** 相关方类型：PERSON 人员 / ORGANIZATION 组织。 */
    private String partyType;

    /** 统一显示名（人员姓名 / 单位名称）。 */
    private String name;

    /** 状态：1 启用，0 停用。 */
    private Integer status;

    private String remark;
}
