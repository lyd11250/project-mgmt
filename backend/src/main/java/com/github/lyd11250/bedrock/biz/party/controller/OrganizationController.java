package com.github.lyd11250.bedrock.biz.party.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.lyd11250.bedrock.biz.party.dto.OrganizationDTO;
import com.github.lyd11250.bedrock.biz.party.service.OrganizationService;
import com.github.lyd11250.bedrock.biz.party.vo.OrganizationVO;
import com.github.lyd11250.bedrock.common.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 组织/单位管理接口（本租户）。
 */
@Tag(name = "单位管理")
@RestController
@RequestMapping("/api/v1/party/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @GetMapping
    @SaCheckPermission("party:organization:list")
    public Result<IPage<OrganizationVO>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String keyword) {
        return Result.ok(organizationService.page(page, size, keyword));
    }

    /** 已入库的组织类型（去重），供前端输入补全。 */
    @GetMapping("/types")
    @SaCheckPermission("party:organization:list")
    public Result<List<String>> types() {
        return Result.ok(organizationService.distinctTypes());
    }

    @PostMapping
    @SaCheckPermission("party:organization:create")
    public Result<Long> create(@Valid @RequestBody OrganizationDTO dto) {
        return Result.ok(organizationService.create(dto));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("party:organization:update")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody OrganizationDTO dto) {
        organizationService.update(id, dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("party:organization:delete")
    public Result<Void> delete(@PathVariable Long id) {
        organizationService.delete(id);
        return Result.ok();
    }
}
