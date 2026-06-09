<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, ElTree, type FormInstance, type FormRules } from 'element-plus'
import {
  assignRoleMenus,
  createRole,
  deleteRole,
  getRoleMenus,
  listRoles,
  updateRole,
  type Role,
} from '@/api/role'
import { getAssignableMenus, type MenuNode } from '@/api/menu'

const loading = ref(false)
const list = ref<Role[]>([])

async function load() {
  loading.value = true
  try {
    list.value = await listRoles()
  } finally {
    loading.value = false
  }
}

onMounted(load)

// ---- 新建 / 编辑 ----
const editVisible = ref(false)
const editRef = ref<FormInstance>()
const editing = ref<Role | null>(null)
const form = reactive({ code: '', name: '' })
const rules: FormRules = {
  code: [{ required: true, message: '请输入角色码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
}

function openCreate() {
  editing.value = null
  form.code = ''
  form.name = ''
  editVisible.value = true
}

function openEdit(row: Role) {
  editing.value = row
  form.code = row.code
  form.name = row.name
  editVisible.value = true
}

async function submitEdit() {
  if (!editRef.value) return
  await editRef.value.validate(async (valid) => {
    if (!valid) return
    if (editing.value) {
      await updateRole(editing.value.id, { ...form })
      ElMessage.success('已更新角色')
    } else {
      await createRole({ ...form })
      ElMessage.success('已创建角色')
    }
    editVisible.value = false
    await load()
  })
}

async function handleDelete(row: Role) {
  await ElMessageBox.confirm(`确认删除角色「${row.name}」？`, '提示', { type: 'warning' })
  await deleteRole(row.id)
  ElMessage.success('删除成功')
  await load()
}

// ---- 分配菜单 ----
const menuVisible = ref(false)
const menuTree = ref<MenuNode[]>([])
const treeRef = ref<InstanceType<typeof ElTree>>()
const assigningRole = ref<Role | null>(null)
const treeProps = { label: 'name', children: 'children' }

async function openAssignMenus(row: Role) {
  assigningRole.value = row
  menuTree.value = await getAssignableMenus()
  const checked = await getRoleMenus(row.id)
  menuVisible.value = true
  await nextTick()
  treeRef.value?.setCheckedKeys(checked, false)
}

async function submitAssignMenus() {
  if (!assigningRole.value || !treeRef.value) return
  const menuIds = [
    ...(treeRef.value.getCheckedKeys() as string[]),
    ...(treeRef.value.getHalfCheckedKeys() as string[]),
  ]
  await assignRoleMenus(assigningRole.value.id, menuIds)
  ElMessage.success('已更新菜单权限')
  menuVisible.value = false
}
</script>

<template>
  <el-card>
    <div class="toolbar">
      <el-button v-permission="'system:role:create'" type="success" @click="openCreate">新建角色</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="code" label="角色码" />
      <el-table-column prop="name" label="角色名称" />
      <el-table-column label="操作" width="280">
        <template #default="{ row }">
          <el-button v-permission="'system:role:assignMenu'" link type="primary" @click="openAssignMenus(row)">
            分配菜单
          </el-button>
          <el-button v-permission="'system:role:update'" link type="primary" @click="openEdit(row)">
            编辑
          </el-button>
          <el-button v-permission="'system:role:delete'" link type="danger" @click="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新建 / 编辑 -->
    <el-dialog v-model="editVisible" :title="editing ? '编辑角色' : '新建角色'" width="420px">
      <el-form ref="editRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="角色码" prop="code">
          <el-input v-model="form.code" :disabled="!!editing" placeholder="租户内唯一，如 EDITOR" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" />
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
</style>
