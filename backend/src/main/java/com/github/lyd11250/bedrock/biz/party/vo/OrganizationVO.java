package com.github.lyd11250.bedrock.biz.party.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 组织/单位视图对象（合并主表 party 与子表 party_organization）。
 */
@Data
public class OrganizationVO {

    /** 相关方 id（= party_id）。 */
    private Long id;

    private String name;

    private String orgType;

    private String taxNo;

    private String registeredCapital;

    private LocalDate establishedDate;

    private String legalPerson;

    private String regAddress;

    private String businessScope;

    private Integer status;

    private String remark;

    private LocalDateTime createdAt;
}
