package com.github.lyd11250.bedrock.biz.party.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.lyd11250.bedrock.biz.party.dto.PersonDTO;
import com.github.lyd11250.bedrock.biz.party.service.PersonService;
import com.github.lyd11250.bedrock.biz.party.vo.PersonVO;
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

/**
 * 人员管理接口（本租户）。
 */
@Tag(name = "人员管理")
@RestController
@RequestMapping("/api/v1/party/persons")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService personService;

    @GetMapping
    @SaCheckPermission("party:person:list")
    public Result<IPage<PersonVO>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String keyword) {
        return Result.ok(personService.page(page, size, keyword));
    }

    @PostMapping
    @SaCheckPermission("party:person:create")
    public Result<Long> create(@Valid @RequestBody PersonDTO dto) {
        return Result.ok(personService.create(dto));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("party:person:update")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody PersonDTO dto) {
        personService.update(id, dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("party:person:delete")
    public Result<Void> delete(@PathVariable Long id) {
        personService.delete(id);
        return Result.ok();
    }
}
