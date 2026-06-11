# party 模块设计说明（相关方主数据）

> 本文件是 **party 业务模块的就近设计文档**（随模块维护，不并入基座主文档）。
> 接入基座的通用范式见仓库根 [业务模块接入规范.md](../../../../../../../../../../业务模块接入规范.md)；本文件只记录 party 自身的设计决策与字段语义。
> party 是首个 `biz` 模块，兼作上层业务接入样板。

---

## 1. 定位

管理租户内的**相关方主数据**：相关方 = 与本租户业务有关的主体，可能是**自然人**，也可能是**组织**（企业、政府机构、事业单位、社会组织等）。
其他业务模块（合同、项目等）将来统一以 `party_id` 引用相关方，不关心其具体是人还是组织。

---

## 2. 建模：Fowler Party 模式（统一主表 + 子表）

相关方按「自然人 vs 组织」二分，采用**主表存共同身份、子表存类型专属字段、子表与主表共享主键**的结构：

```
party (相关方主表)                         party_type: PERSON | ORGANIZATION
  id, tenant_id, party_type, name,         name = 统一显示名（姓名 / 单位名称）
  status, remark, 审计, deleted
   │  共享主键（子表 id = party.id）
   ├─ party_person       (PERSON 子表)     gender, id_card, contact
   └─ party_organization (ORGANIZATION 子表)
          org_type, tax_no, registered_capital, established_date,
          legal_person, reg_address, business_scope
```

### 为什么这样选

| 决策 | 理由 |
|------|------|
| 统一主表 + 子表（非单表多态，非独立双表） | 给每个相关方一个**稳定的 `party_id`** 供跨模块引用；公共字段（名称/状态）只存一份；子表只放类型专属字段，不稀疏。 |
| 自然人 vs 组织二分（而非「人员/企业」二分） | 企业只是组织的一种；政府机构/事业单位/社会组织等既非自然人也非企业，统一归「组织」，由 `org_type` 区分，**扩展新机构类型无需加表加页**。 |
| 共享主键（子表 id = 主表 id） | 1:1 关系最简实现；其他模块引用 `party_id` 即可直达主表与子表。 |

### 共享主键的实现要点
子表实体（`PartyPerson`/`PartyOrganization`）**不重声明 `id`**，沿用 `BaseEntity` 的 `@TableId(ASSIGN_ID)`。创建流程：先 `insert` 主表 `Party` 拿到雪花 id → 子表 `setId(party.getId())` 后 `insert`。`ASSIGN_ID` 在 id 已赋值时沿用、不再生成。删除时主表、子表各 `deleteById`（软删除）。

---

## 3. 实体关系（ER）

```
            ┌──────────────────────────┐
            │           party          │  (tenant_id 隔离)
            │  id (PK, 雪花)           │
            │  party_type, name,       │
            │  status, remark          │
            └────────────┬─────────────┘
                         │ 1:1 共享主键
          ┌──────────────┴───────────────┐
          ▼                               ▼
┌───────────────────┐         ┌────────────────────────────┐
│   party_person    │         │     party_organization     │
│  id (PK=party.id) │         │  id (PK=party.id)          │
│  gender, id_card, │         │  org_type, tax_no,         │
│  contact          │         │  registered_capital,       │
└───────────────────┘         │  established_date,         │
                              │  legal_person, reg_address,│
                              │  business_scope            │
                              └────────────────────────────┘
```

三表均带 `tenant_id`，由多租户插件自动隔离（不入 `IGNORE_TABLES`）。

---

## 4. 字段与约束

### party（主表）
| 字段 | 含义 / 约束 |
|------|------|
| `party_type` | `PERSON` 人员 / `ORGANIZATION` 组织 |
| `name` | 统一显示名（人员姓名 / 单位名称），非空 |
| `status` | 1 启用 / 0 停用 |
| `remark` | 备注 |

### party_person（人员）
| 字段 | 含义 / 约束 |
|------|------|
| `gender` | 0 未知 / 1 男 / 2 女 |
| `id_card` | 身份证号；**可空，填了才租户内唯一**（偏过滤唯一索引 `WHERE deleted=0 AND id_card IS NOT NULL`）；DTO 校验 15/18 位 |
| `contact` | 联系方式 |

### party_organization（组织/单位）
| 字段 | 含义 / 约束 |
|------|------|
| `org_type` | 组织类型（企业/政府机构/事业单位/社会组织/其他），自由文本，前端按历史值补全 |
| `tax_no` | 统一社会信用代码；**可空，填了才租户内唯一**（同上偏过滤索引） |
| `registered_capital` | 注册资本，自由文本（如「500 万元人民币」），仅企业适用 |
| `established_date` | 成立日期（`DATE`） |
| `legal_person` | 法定代表人 / 负责人 |
| `reg_address` | 住所（注册地址；避开 `address` 命名，语义更准） |
| `business_scope` | 经营范围（`TEXT`），仅企业适用 |

> 注册资本/经营范围对非企业组织留空即可，不做强校验。

---

## 5. 接口与权限码

URL 前缀 `/api/v1/party/*`；列表按资源路径分接口、**后端固定 `party_type`**（人员接口固定 PERSON，单位接口固定 ORGANIZATION），前端零参数心智。

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| GET | `/party/persons` | `party:person:list` | 人员分页（`page/size/keyword`，按名称模糊） |
| POST | `/party/persons` | `party:person:create` | 新建人员 |
| PUT | `/party/persons/{id}` | `party:person:update` | 编辑人员 |
| DELETE | `/party/persons/{id}` | `party:person:delete` | 删除人员 |
| GET | `/party/organizations` | `party:organization:list` | 单位分页 |
| GET | `/party/organizations/types` | `party:organization:list` | 已入库组织类型（去重），供输入补全 |
| POST | `/party/organizations` | `party:organization:create` | 新建单位 |
| PUT | `/party/organizations/{id}` | `party:organization:update` | 编辑单位 |
| DELETE | `/party/organizations/{id}` | `party:organization:delete` | 删除单位 |

列表查询策略：以主表 `party`（固定类型 + 名称模糊 + 创建时间倒序）分页，再 `selectByIds` 批量补子表详情合并为 VO（2 次查询，无 N+1，无自定义 SQL）。

---

## 6. 菜单与套餐

菜单种子在 `V9__party_module.sql` 注册（id 块 200，避开系统菜单 1xx）：

```
相关方管理 (M /party)                                     id 200
├─ 人员管理 (C /party/persons → party/PersonList)         id 201  party:person:list  (+F 2011/2012/2013)
└─ 单位管理 (C /party/organizations → party/OrganizationList) id 202  party:organization:list (+F 2021/2022/2023)
```

纳入「全功能（FULL，id=2）」套餐；「基础版（BASIC）」不含 party，以体现套餐裁剪。
**无独立「相关方」页面**：相关方是数据层统一概念，UI 上「人员 ∪ 单位」已无遗漏覆盖；将来跨模块「选相关方」用 party 选择器组件解决，而非浏览页。

---

## 7. 前端三件套

| 件 | 位置 |
|----|------|
| api | `frontend/src/api/party.ts` |
| 视图 | `frontend/src/views/party/{PersonList,OrganizationList}.vue` |
| 菜单 | 由后端 `sys_menu` 种子动态下发，无需前端登记（动态路由按 `component` 字符串加载） |

要点：组织类型用 `el-autocomplete` 拉 `/party/organizations/types` 历史值补全（聚焦即展示、可自由输入）；成立日期 `el-date-picker`（`format`/`value-format` = `YYYY-MM-DD`）；按钮级权限 `v-permission`；时间显示 `formatDateTime()`。

---

## 8. 文件清单

| 层 | 文件 |
|----|------|
| 迁移 | `backend/src/main/resources/db/migration/V9__party_module.sql` |
| 实体 | `entity/{Party,PartyPerson,PartyOrganization}.java` |
| Mapper | `mapper/{Party,PartyPerson,PartyOrganization}Mapper.java` |
| DTO | `dto/{Person,Organization}DTO.java` |
| VO | `vo/{Person,Organization}VO.java` |
| Service | `service/{Person,Organization}Service.java` |
| Controller | `controller/{Person,Organization}Controller.java` |
| 前端 api | `frontend/src/api/party.ts` |
| 前端视图 | `frontend/src/views/party/{PersonList,OrganizationList}.vue` |

---

## 9. 待定 / 后续可扩展

- **相关方选择器组件**：将来业务模块需要「引用一个相关方」时，提供跨类型的 party 选择弹窗（按名称同时搜人员+单位），而非独立浏览页。
- **组织类型字典化**：当前 `org_type` 为自由文本 + 历史值补全；若需固定可选项与统计，可升级为字典维护。
- **更多相关方属性**：如人员的邮箱/证件类型、组织的行业分类等，按业务需要在子表增列（Flyway 同序列递增）。
