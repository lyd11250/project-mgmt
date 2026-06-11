package com.github.lyd11250.bedrock.biz.party.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.lyd11250.bedrock.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 人员（party 的 PERSON 子表，共享主键）。
 *
 * <p>主键与所属 {@link Party} 共享：插入前显式 {@code setId(party.getId())}，
 * {@code ASSIGN_ID} 在 id 已赋值时沿用、不再生成。姓名/状态/备注落在 {@link Party}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("party_person")
public class PartyPerson extends BaseEntity {

    /** 性别：0 未知，1 男，2 女。 */
    private Integer gender;

    /** 身份证号。 */
    private String idCard;

    /** 联系方式。 */
    private String contact;
}
