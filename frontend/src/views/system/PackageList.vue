<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, ElTree, type FormInstance, type FormRules } from 'element-plus'
import {
  assignPackageMenus,
  assignPackageQuotas,
  createPackage,
  deletePackage,
  getPackageMenus,
  getPackageQuotas,
  listPackages,
  updatePackage,
  type PackageItem,
  type PackageQuota,
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
const form = reactive<{ name: string; code: string; remark: string }>({
  name: '',
  code: '',
  remark: '',
})
const rules: FormRules = {
  name: [{ required: true, message: '请输入套餐名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入套餐编码', trigger: 'blur' }],
}

function openCreate() {
  editing.value = null
  form.name = ''
  form.code = ''
  form.remark = ''
  editVisible.value = true
}

function openEdit(row: PackageItem) {
  editing.value = row
  form.name = row.name
  form.code = row.code
  form.remark = row.remark ?? ''
  editVisible.value = true
}

async function submitEdit() {
  if (!editRef.value) return
  await editRef.value.validate(async (valid) => {
    if (!valid) return
    const payload = { name: form.name, code: form.code, remark: form.remark }
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

// ---- 配置配额 ----
// quotaRows[].quotaValue 为 null 表示不限（提交时落为 -1）
const quotaVisible = ref(false)
const quotaLoading = ref(false)
const quotaSaving = ref(false)
const quotaPackage = ref<PackageItem | null>(null)
const quotaRows = ref<(PackageQuota & { input: number | null })[]>([])

async function openQuotas(row: PackageItem) {
  quotaPackage.value = row
  quotaVisible.value = true
  quotaLoading.value = true
  try {
    const data = await getPackageQuotas(row.id)
    quotaRows.value = data.map((q) => ({ ...q, input: q.quotaValue < 0 ? null : q.quotaValue }))
  } finally {
    quotaLoading.value = false
  }
}

async function submitQuotas() {
  if (!quotaPackage.value) return
  const quotas = quotaRows.value.map((q) => ({
    quotaId: q.quotaId,
    quotaValue: q.input == null ? -1 : q.input,
  }))
  quotaSaving.value = true
  try {
    await assignPackageQuotas(quotaPackage.value.id, quotas)
    ElMessage.success('已更新套餐配额')
    quotaVisible.value = false
  } finally {
    quotaSaving.value = false
  }
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
      <el-table-column prop="remark" label="说明" />
      <el-table-column label="操作" width="320">
        <template #default="{ row }">
          <el-button v-permission="'system:package:assignMenu'" link type="primary" @click="openAssignMenus(row)">
            分配菜单
          </el-button>
          <el-button v-permission="'system:package:quota'" link type="primary" @click="openQuotas(row)">
            配置配额
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

    <!-- 配置配额 -->
    <el-dialog v-model="quotaVisible" :title="`配置配额${quotaPackage ? ' - ' + quotaPackage.name : ''}`" width="520px">
      <el-table v-loading="quotaLoading" :data="quotaRows" border>
        <el-table-column prop="quotaName" label="配额名称" min-width="140" />
        <el-table-column prop="quotaKey" label="标识" min-width="140" />
        <el-table-column label="上限值" width="180">
          <template #default="{ row }">
            <el-input-number v-model="row.input" :min="0" controls-position="right" placeholder="留空=不限" />
          </template>
        </el-table-column>
      </el-table>
      <p v-if="!quotaLoading && !quotaRows.length" class="hint">暂无配额定义</p>
      <p class="hint">上限值留空表示不限。</p>
      <template #footer>
        <el-button @click="quotaVisible = false">取消</el-button>
        <el-button type="primary" :loading="quotaSaving" :disabled="!quotaRows.length" @click="submitQuotas">
          确定
        </el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<style scoped>
.toolbar {
  margin-bottom: 12px;
}
.hint {
  margin-top: 8px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
