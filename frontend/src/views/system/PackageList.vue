<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, ElTree, type FormInstance, type FormRules } from 'element-plus'
import {
  assignPackageMenus,
  createPackage,
  deletePackage,
  getPackageMenus,
  listPackages,
  updatePackage,
  type PackageItem,
} from '@/api/package'
import { getMenuTree, type MenuNode } from '@/api/menu'

const loading = ref(false)
const list = ref<PackageItem[]>([])

async function load() {
  loading.value = true
  try {
    list.value = await listPackages()
  } finally {
    loading.value = false
  }
}

onMounted(load)

// ---- 新建 / 编辑 ----
const editVisible = ref(false)
const editRef = ref<FormInstance>()
const editing = ref<PackageItem | null>(null)
// maxUsers 为 null 表示不限（提交时落为 -1）
const form = reactive<{ name: string; code: string; remark: string; maxUsers: number | null }>({
  name: '',
  code: '',
  remark: '',
  maxUsers: null,
})
const rules: FormRules = {
  name: [{ required: true, message: '请输入套餐名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入套餐编码', trigger: 'blur' }],
}

/** -1 / 未配置 视为不限，表单用 null 表示。 */
function quotaToForm(value?: number): number | null {
  return value == null || value < 0 ? null : value
}

function buildQuotas(): Record<string, number> {
  return { max_users: form.maxUsers == null ? -1 : form.maxUsers }
}

function openCreate() {
  editing.value = null
  form.name = ''
  form.code = ''
  form.remark = ''
  form.maxUsers = null
  editVisible.value = true
}

function openEdit(row: PackageItem) {
  editing.value = row
  form.name = row.name
  form.code = row.code
  form.remark = row.remark ?? ''
  form.maxUsers = quotaToForm(row.quotas?.max_users)
  editVisible.value = true
}

async function submitEdit() {
  if (!editRef.value) return
  await editRef.value.validate(async (valid) => {
    if (!valid) return
    const payload = {
      name: form.name,
      code: form.code,
      remark: form.remark,
      quotas: buildQuotas(),
    }
    if (editing.value) {
      await updatePackage(editing.value.id, payload)
      ElMessage.success('已更新套餐')
    } else {
      await createPackage(payload)
      ElMessage.success('已创建套餐')
    }
    editVisible.value = false
    await load()
  })
}

/** 列表展示用：-1/未配置 显示“不限”。 */
function displayMaxUsers(row: PackageItem): string {
  const v = row.quotas?.max_users
  return v == null || v < 0 ? '不限' : String(v)
}

async function handleDelete(row: PackageItem) {
  await ElMessageBox.confirm(`确认删除套餐「${row.name}」？`, '提示', { type: 'warning' })
  await deletePackage(row.id)
  ElMessage.success('删除成功')
  await load()
}

// ---- 分配菜单 ----
const menuVisible = ref(false)
const menuTree = ref<MenuNode[]>([])
const treeRef = ref<InstanceType<typeof ElTree>>()
const assigning = ref<PackageItem | null>(null)
const treeProps = { label: 'name', children: 'children' }

async function openAssignMenus(row: PackageItem) {
  assigning.value = row
  menuTree.value = await getMenuTree()
  const checked = await getPackageMenus(row.id)
  menuVisible.value = true
  await nextTick()
  treeRef.value?.setCheckedKeys(checked, false)
}

async function submitAssignMenus() {
  if (!assigning.value || !treeRef.value) return
  const menuIds = [
    ...(treeRef.value.getCheckedKeys() as string[]),
    ...(treeRef.value.getHalfCheckedKeys() as string[]),
  ]
  await assignPackageMenus(assigning.value.id, menuIds)
  ElMessage.success('已更新套餐菜单')
  menuVisible.value = false
}
</script>

<template>
  <el-card>
    <div class="toolbar">
      <el-button v-permission="'system:package:create'" type="success" @click="openCreate">新建套餐</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="name" label="套餐名称" />
      <el-table-column prop="code" label="套餐编码" />
      <el-table-column label="最大用户数" width="110">
        <template #default="{ row }">{{ displayMaxUsers(row) }}</template>
      </el-table-column>
      <el-table-column prop="remark" label="说明" />
      <el-table-column label="操作" width="240">
        <template #default="{ row }">
          <el-button v-permission="'system:package:assignMenu'" link type="primary" @click="openAssignMenus(row)">
            分配菜单
          </el-button>
          <el-button v-permission="'system:package:update'" link type="primary" @click="openEdit(row)">
            编辑
          </el-button>
          <el-button v-permission="'system:package:delete'" link type="danger" @click="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新建 / 编辑 -->
    <el-dialog v-model="editVisible" :title="editing ? '编辑套餐' : '新建套餐'" width="420px">
      <el-form ref="editRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="编码" prop="code">
          <el-input v-model="form.code" :disabled="!!editing" placeholder="全局唯一，如 PRO" />
        </el-form-item>
        <el-form-item label="最大用户数">
          <el-input-number v-model="form.maxUsers" :min="0" controls-position="right" placeholder="留空=不限" />
          <span class="hint">留空表示不限</span>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEdit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配菜单 -->
    <el-dialog v-model="menuVisible" title="分配菜单" width="420px">
      <el-tree ref="treeRef" :data="menuTree" :props="treeProps" node-key="id"
        show-checkbox default-expand-all />
      <template #footer>
        <el-button @click="menuVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAssignMenus">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<style scoped>
.toolbar {
  margin-bottom: 12px;
}
.hint {
  margin-left: 8px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
