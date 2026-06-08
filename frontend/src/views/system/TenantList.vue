<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { createTenant, pageTenants, type TenantItem } from '@/api/tenant'

const loading = ref(false)
const list = ref<TenantItem[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10 })

async function load() {
  loading.value = true
  try {
    const res = await pageTenants(query)
    list.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

onMounted(load)

const createVisible = ref(false)
const createRef = ref<FormInstance>()
const form = reactive({ name: '', code: '', contact: '', adminUsername: '', adminPassword: '' })
const rules: FormRules = {
  name: [{ required: true, message: '请输入租户名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入租户编码', trigger: 'blur' }],
  adminUsername: [{ required: true, message: '请输入管理员用户名', trigger: 'blur' }],
  adminPassword: [{ required: true, message: '请输入管理员密码', trigger: 'blur' }],
}

function openCreate() {
  form.name = ''
  form.code = ''
  form.contact = ''
  form.adminUsername = ''
  form.adminPassword = ''
  createVisible.value = true
}

async function submitCreate() {
  if (!createRef.value) return
  await createRef.value.validate(async (valid) => {
    if (!valid) return
    await createTenant({ ...form })
    ElMessage.success('租户创建成功，已生成其管理员账号')
    createVisible.value = false
    await load()
  })
}
</script>

<template>
  <el-card>
    <div class="toolbar">
      <el-button type="success" @click="openCreate">新建租户</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="name" label="租户名称" />
      <el-table-column prop="code" label="租户编码" />
      <el-table-column prop="contact" label="联系人" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180" />
    </el-table>

    <el-pagination class="pager" layout="total, prev, pager, next" :total="total"
      :page-size="query.size" :current-page="query.page"
      @current-change="(p: number) => { query.page = p; load() }" />

    <el-dialog v-model="createVisible" title="新建租户" width="460px">
      <el-form ref="createRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="租户名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="租户编码" prop="code">
          <el-input v-model="form.code" placeholder="登录时使用，唯一" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="form.contact" />
        </el-form-item>
        <el-divider>租户管理员</el-divider>
        <el-form-item label="管理员账号" prop="adminUsername">
          <el-input v-model="form.adminUsername" />
        </el-form-item>
        <el-form-item label="管理员密码" prop="adminPassword">
          <el-input v-model="form.adminPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<style scoped>
.toolbar {
  margin-bottom: 12px;
}
.pager {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
