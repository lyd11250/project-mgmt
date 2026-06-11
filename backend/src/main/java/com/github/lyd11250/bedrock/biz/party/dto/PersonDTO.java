package com.github.lyd11250.bedrock.biz.party.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 人员创建/编辑入参。姓名落在统一主表 party。
 */
@Data
public class PersonDTO {

    @NotBlank(message = "姓名不能为空")
    private String name;

    /** 性别：0 未知，1 男，2 女（缺省按未知）。 */
    private Integer gender;

    /** 身份证号（可空；填写时校验 15/18 位）。 */
    @Pattern(regexp = "^$|^\\d{15}$|^\\d{17}[\\dXx]$", message = "身份证号格式不正确")
    private String idCard;

    /** 联系方式。 */
    private String contact;

    /** 状态：1 启用，0 停用（缺省启用）。 */
    private Integer status;

    private String remark;
}
