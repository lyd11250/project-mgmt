package com.github.lyd11250.bedrock.biz.party.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.lyd11250.bedrock.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 组织/单位（party 的 ORGANIZATION 子表，共享主键）。
 *
 * <p>涵盖企业、政府机构、事业单位、社会组织等，由 {@code orgType} 区分；
 * 主键与所属 {@link Party} 共享。名称/状态/备注落在 {@link Party}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("party_organization")
public class PartyOrganization extends BaseEntity {

    /** 组织类型：企业/政府机构/事业单位/社会组织/其他。 */
    private String orgType;

    /** 统一社会信用代码。 */
    private String taxNo;

    /** 注册资本（自由文本，仅企业适用）。 */
    private String registeredCapital;

    /** 成立日期。 */
    private LocalDate establishedDate;

    /** 法定代表人 / 负责人。 */
    private String legalPerson;

    /** 住所（注册地址）。 */
    private String regAddress;

    /** 经营范围（仅企业适用）。 */
    private String businessScope;
}
