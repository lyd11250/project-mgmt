package com.github.lyd11250.bedrock.biz.party.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

/**
 * 组织/单位创建/编辑入参。名称落在统一主表 party。
 */
@Data
public class OrganizationDTO {

    @NotBlank(message = "单位名称不能为空")
    private String name;

    /** 组织类型（企业/政府机构/事业单位/社会组织/其他，自由文本）。 */
    private String orgType;

    /** 统一社会信用代码（可空；填写时租户内唯一）。 */
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

    /** 状态：1 启用，0 停用（缺省启用）。 */
    private Integer status;

    private String remark;
}
