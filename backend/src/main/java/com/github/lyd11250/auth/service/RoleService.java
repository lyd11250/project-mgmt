package com.github.lyd11250.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.lyd11250.auth.entity.SysRole;
import com.github.lyd11250.auth.mapper.SysRoleMapper;
import com.github.lyd11250.auth.vo.RoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 角色查询（本租户，供分配角色使用）。
 */
@Service
@RequiredArgsConstructor
public class RoleService {

    private final SysRoleMapper roleMapper;

    public List<RoleVO> list() {
        return roleMapper.selectList(Wrappers.<SysRole>lambdaQuery().orderByAsc(SysRole::getId))
                .stream().map(r -> {
                    RoleVO v = new RoleVO();
                    v.setId(r.getId());
                    v.setCode(r.getCode());
                    v.setName(r.getName());
                    return v;
                }).toList();
    }
}
