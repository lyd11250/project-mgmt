package com.github.lyd11250.bedrock.system.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.lyd11250.bedrock.common.BusinessException;
import com.github.lyd11250.bedrock.config.TenantLineHandlerImpl;
import com.github.lyd11250.bedrock.system.RbacConstants;
import com.github.lyd11250.bedrock.system.entity.SysMenu;
import com.github.lyd11250.bedrock.system.entity.SysPackageMenu;
import com.github.lyd11250.bedrock.system.entity.Tenant;
import com.github.lyd11250.bedrock.system.dto.MenuDTO;
import com.github.lyd11250.bedrock.system.mapper.SysMenuMapper;
import com.github.lyd11250.bedrock.system.mapper.SysPackageMenuMapper;
import com.github.lyd11250.bedrock.system.mapper.TenantMapper;
import com.github.lyd11250.bedrock.system.vo.MenuVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 菜单管理（全局目录，平台超管维护）与导航下发（按当前用户裁剪）。
 */
@Service
@RequiredArgsConstructor
public class MenuService {

    private final SysMenuMapper menuMapper;
    private final SysPackageMenuMapper packageMenuMapper;
    private final TenantMapper tenantMapper;
    private final PermissionCacheService permissionCache;

    /** 全量菜单树（菜单管理用）。 */
    public List<MenuVO> tree() {
        return buildTree(menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()));
    }

    /**
     * 当前用户导航树：仅 M 目录 / C 页面，按权限码裁剪（超管见全部），并带上其祖先目录。
     */
    public List<MenuVO> nav() {
        List<String> perms = StpUtil.getPermissionList();
        boolean all = perms.contains(RbacConstants.PERMISSION_ALL);
        List<SysMenu> menus = menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery());
        Map<Long, SysMenu> byId = menus.stream().collect(Collectors.toMap(SysMenu::getId, m -> m));

        // 命中的 C 页面
        Set<Long> keep = menus.stream()
                .filter(m -> "C".equals(m.getType()))
                .filter(m -> isVisible(m))
                .filter(m -> all || (StringUtils.hasText(m.getPerm()) && perms.contains(m.getPerm())))
                .map(SysMenu::getId)
                .collect(Collectors.toCollection(java.util.HashSet::new));
        // 补齐祖先目录
        for (Long id : new ArrayList<>(keep)) {
            Long pid = byId.get(id).getParentId();
            while (pid != null && pid != 0 && byId.containsKey(pid) && keep.add(pid)) {
                pid = byId.get(pid).getParentId();
            }
        }
        return buildTree(menus.stream().filter(m -> keep.contains(m.getId())).toList());
    }

    /** 可分配菜单树（角色分配菜单用）：超管见全部，租户管理员限本租户套餐边界。 */
    public List<MenuVO> assignableTree() {
        if (StpUtil.getPermissionList().contains(RbacConstants.PERMISSION_ALL)) {
            return tree();
        }
        List<Long> boundary = currentTenantPackageMenuIds();
        if (boundary.isEmpty()) {
            return List.of();
        }
        return buildTree(menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery().in(SysMenu::getId, boundary)));
    }

    public Long create(MenuDTO dto) {
        SysMenu menu = new SysMenu();
        apply(menu, dto);
        menuMapper.insert(menu);
        return menu.getId();
    }

    public void update(Long id, MenuDTO dto) {
        SysMenu menu = requireMenu(id);
        apply(menu, dto);
        menuMapper.updateById(menu);
        permissionCache.evictAll();
    }

    public void delete(Long id) {
        requireMenu(id);
        if (menuMapper.selectCount(Wrappers.<SysMenu>lambdaQuery().eq(SysMenu::getParentId, id)) > 0) {
            throw new BusinessException("存在子菜单，无法删除");
        }
        menuMapper.deleteById(id);
        permissionCache.evictAll();
    }

    // ---- 内部 ----

    /** 当前租户套餐圈定的菜单 id（package_id 为空则空）。 */
    private List<Long> currentTenantPackageMenuIds() {
        Object tid = StpUtil.getSession().get(TenantLineHandlerImpl.SESSION_TENANT_ID);
        if (tid == null) {
            return List.of();
        }
        Tenant tenant = tenantMapper.selectById(Long.valueOf(tid.toString()));
        if (tenant == null || tenant.getPackageId() == null) {
            return List.of();
        }
        return packageMenuMapper.selectList(
                        Wrappers.<SysPackageMenu>lambdaQuery().eq(SysPackageMenu::getPackageId, tenant.getPackageId()))
                .stream().map(SysPackageMenu::getMenuId).distinct().toList();
    }

    private boolean isVisible(SysMenu m) {
        return (m.getVisible() == null || m.getVisible() == 1)
                && (m.getStatus() == null || m.getStatus() == 1);
    }

    private void apply(SysMenu menu, MenuDTO dto) {
        menu.setParentId(dto.getParentId() == null ? 0L : dto.getParentId());
        menu.setType(dto.getType());
        menu.setName(dto.getName());
        menu.setPath(dto.getPath());
        menu.setComponent(dto.getComponent());
        menu.setIcon(dto.getIcon());
        menu.setPerm(dto.getPerm());
        menu.setSort(dto.getSort() == null ? 0 : dto.getSort());
        menu.setVisible(dto.getVisible() == null ? 1 : dto.getVisible());
        menu.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
    }

    private SysMenu requireMenu(Long id) {
        SysMenu menu = menuMapper.selectById(id);
        if (menu == null) {
            throw new BusinessException("菜单不存在");
        }
        return menu;
    }

    private List<MenuVO> buildTree(List<SysMenu> menus) {
        Map<Long, MenuVO> voById = new LinkedHashMap<>();
        menus.stream()
                .sorted(Comparator.comparingInt((SysMenu m) -> m.getSort() == null ? 0 : m.getSort())
                        .thenComparing(SysMenu::getId))
                .forEach(m -> voById.put(m.getId(), toVO(m)));
        List<MenuVO> roots = new ArrayList<>();
        for (MenuVO vo : voById.values()) {
            Long pid = vo.getParentId();
            if (pid != null && pid != 0 && voById.containsKey(pid)) {
                voById.get(pid).getChildren().add(vo);
            } else {
                roots.add(vo);
            }
        }
        return roots;
    }

    private MenuVO toVO(SysMenu m) {
        MenuVO vo = new MenuVO();
        vo.setId(m.getId());
        vo.setParentId(m.getParentId());
        vo.setType(m.getType());
        vo.setName(m.getName());
        vo.setPath(m.getPath());
        vo.setComponent(m.getComponent());
        vo.setIcon(m.getIcon());
        vo.setPerm(m.getPerm());
        vo.setSort(m.getSort());
        vo.setVisible(m.getVisible());
        vo.setStatus(m.getStatus());
        return vo;
    }
}
