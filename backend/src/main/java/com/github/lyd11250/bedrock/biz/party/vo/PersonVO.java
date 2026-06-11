package com.github.lyd11250.bedrock.biz.party.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 人员视图对象（合并主表 party 与子表 party_person）。
 */
@Data
public class PersonVO {

    /** 相关方 id（= party_id）。 */
    private Long id;

    private String name;

    /** 性别：0 未知，1 男，2 女。 */
    private Integer gender;

    private String idCard;

    private String contact;

    private Integer status;

    private String remark;

    private LocalDateTime createdAt;
}
