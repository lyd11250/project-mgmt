package com.github.lyd11250.bedrock.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 套餐创建/编辑入参（平台超管）。配额经独立接口配置，见 {@code AssignQuotasDTO}。
 */
@Data
public class PackageDTO {

    @NotBlank(message = "套餐名称不能为空")
    private String name;

    @NotBlank(message = "套餐编码不能为空")
    private String code;

    private Integer status;

    private String remark;
}
