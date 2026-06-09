<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  assignRoles,
  createUser,
  deleteUser,
  pageUsers,
  resetPassword,
  type UserItem,
} from '@/api/user'
import { listRoles, type Role } from '@/api/role'

const loading = ref(false)
const list = ref<UserItem[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, username: '' })
const roles = ref<Role[]>([])

async function loadRoles() {
  roles.value = await listRoles()
}

async function load() {
  loading.value = true
  try {
    const res = await pageUsers(query)
    list.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadRoles()
  await load()
})

// ---- 新建用户 ----
const createVisible = ref(false)
const createRef = ref<FormInstance>()
const createForm = reactive({ username: '', password: '', roleIds: [] as string[] })
const createRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

function openCreate() {
  createForm.username = ''
  createForm.password = ''
  createForm.roleIds = []
  createVisible.value = true
}

async function submitCreate() {
  if (!createRef.value) return
  await createRef.value.validate(async (valid) => {
    if (!valid) return
    await createUser({ ...createForm })
    ElMessage.success('创建成功')
    createVisible.value = false
    await load()
  })
}

// ---- 分配角色 ----
const roleVisible = ref(false)
const roleForm = reactive({ userId: '', roleIds: [] as string[] })

function openAssign(row: UserItem) {
  roleForm.userId = row.id
  roleForm.roleIds = row.roles.map((r) => r.id)
  roleVisible.value = true
}

async function submitAssign() {
  await assignRoles(roleForm.userId, roleForm.roleIds)
  ElMessage.success('已更新角色')
  roleVisible.value = false
  await load()
}

// ---- 重置密码 ----
async function handleReset(row: UserItem) {
  const { value } = await ElMessageBox.prompt(`为用户「${row.username}」设置新密码`, '重置密码', {
    inputType: 'password',
    inputPlaceholder: '新密码',
  })
  await resetPassword(row.id, value)
  ElMessage.success('密码已重置')
}

// ---- 删除 ----
async function handleDelete(row: UserItem) {
  await ElMessageBox.confirm(`确认删除用户「${row.username}」？`, '提示', { type: 'warning' })
  await deleteUser(row.id)
  ElMessage.success('删除成功')
  await load()
}
</script>

<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="query.username" placeholder="用户名" clearable class="search"
        @keyup.enter="load" @clear="load" />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button v-permission="'system:user:create'" type="success" @click="openCreate">新建用户</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="username" label="用户名" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="角色">
        <template #default="{ row }">
          <el-tag v-for="r in row.roles" :key="r.id" class="role-tag">{{ r.name }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column label="操作" width="260">
        <template #default="{ row }">
          <el-button v-permission="'system:user:assignRole'" link type="primary" @click="openAssign(row)">
            分配角色
          </el-button>
          <el-button v-permission="'system:user:resetPwd'" link type="primary" @click="handleReset(row)">
            重置密码
          </el-button>
          <el-button v-permission="'system:user:delete'" link type="danger" @click="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination class="pager" layout="total, prev, pager, next" :total="total"
      :page-size="query.size" :current-page="query.page"
      @current-change="(p: number) => { query.page = p; load() }" />

    <!-- 新建用户 -->
    <el-dialog v-model="createVisible" title="新建用户" width="420px">
      <el-form ref="createRef" :model="createForm" :rules="createRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="createForm.username" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="createForm.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="createForm.roleIds" multiple placeholder="选择角色" class="full">
            <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配角色 -->
    <el-dialog v-model="roleVisible" title="分配角色" width="420px">
      <el-select v-model="roleForm.roleIds" multiple placeholder="选择角色" class="full">
        <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
      </el-select>
      <template #footer>
        <el-button @click="roleVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAssign">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<style scoped>
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.search {
  width: 220px;
}
.role-tag {
  margin-right: 4px;
}
.pager {
  margin-top: 12px;
  justify-content: flex-end;
}
.full {
  width: 100%;
}
</style>
