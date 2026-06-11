/**
 * 业务模块 party（相关方主数据）：首个上层业务模块，兼作 biz 模块接入样板。
 *
 * <p>Fowler Party 模式：统一主表 {@code party}（相关方）+ 子表 {@code party_person} /
 * {@code party_organization}，按「自然人 vs 组织」二分。组织涵盖企业、政府机构、事业单位、
 * 社会组织等（org_type 区分）。人员/组织与 party 共享主键，其他模块统一以 party_id 引用相关方。
 * 分层与命名仿基座 {@code system} 域；复用多租户隔离、审计、统一响应与 RBAC 鉴权。
 * 接入约定见仓库根《业务模块接入规范.md》。
 */
package com.github.lyd11250.bedrock.biz.party;
