<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { createTenant, pageTenants, renewTenant, type TenantItem } from '@/api/tenant'
import { listPackages, type PackageItem } from '@/api/package'
import { formatDateTime } from '@/utils/time'

const loading = ref(false)
const list = ref<TenantItem[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10 })
const packages = ref<PackageItem[]>([])

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

onMounted(async () => {
  packages.value = await listPackages()
  await load()
})

const createVisible = ref(false)
const createRef = ref<FormInstance>()
const form = reactive({
  name: '', code: '', packageId: undefined as string | undefined,
  contact: '', adminUsername: '', adminPassword: '',
  expireAt: '' as string,
})
const rules: FormRules = {
  name: [{ required: true, message: '请输入租户名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入租户编码', trigger: 'blur' }],
  packageId: [{ required: true, message: '请选择套餐', trigger: 'change' }],
  adminUsername: [{ required: true, message: '请输入管理员用户名', trigger: 'blur' }],
  adminPassword: [{ required: true, message: '请输入管理员密码', trigger: 'blur' }],
}

function openCreate() {
  form.name = ''
  form.code = ''
  form.packageId = undefined
  form.contact = ''
  form.adminUsername = ''
  form.adminPassword = ''
  form.expireAt = ''
  createVisible.value = true
}

async function submitCreate() {
  if (!createRef.value) return
  await createRef.value.validate(async (valid) => {
    if (!valid) return
    await createTenant({
      ...form,
      packageId: form.packageId!,
      expireAt: form.expireAt || undefined,
    })
    ElMessage.success('租户创建成功，已生成其管理员账号')
    createVisible.value = false
    await load()
  })
}

const renewVisible = ref(false)
const renewTarget = ref<TenantItem | null>(null)
const renewExpireAt = ref('')
const renewPackageId = ref<string | undefined>(undefined)

function openRenew(row: TenantItem) {
  renewTarget.value = row
  renewExpireAt.value = row.expireAt ?? ''
  renewPackageId.value = row.packageId
  renewVisible.value = true
}

async function submitRenew() {
  if (!renewTarget.value) return
  // 套餐与当前一致则不传 changePackageId
  const changePackageId =
    renewPackageId.value && renewPackageId.value !== renewTarget.value.packageId
      ? renewPackageId.value
      : undefined
  await renewTenant(renewTarget.value.id, {
    expireAt: renewExpireAt.value || undefined,
    changePackageId,
  })
  ElMessage.success('续费成功')
  renewVisible.value = false
  await load()
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
      <el-table-column prop="packageName" label="套餐" width="120" />
      <el-table-column prop="contact" label="联系人" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="到期时间" width="200">
        <template #default="{ row }">
          <span>{{ row.expireAt ? formatDateTime(row.expireAt) : '永久' }}</span>
          <el-tag v-if="row.expired" type="danger" size="small" class="expired-tag">已过期</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="180">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openRenew(row)">续费</el-button>
        </template>
      </el-table-column>
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
        <el-form-item label="套餐" prop="packageId">
          <el-select v-model="form.packageId" placeholder="选择套餐" class="full">
            <el-option v-for="p in packages" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="form.contact" />
        </el-form-item>
        <el-form-item label="到期时间">
          <el-date-picker v-model="form.expireAt" type="datetime" class="full"
            placeholder="留空表示永久" format="YYYY-MM-DD HH:mm:ss" value-format="YYYY-MM-DDTHH:mm:ss" />
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

    <el-dialog v-model="renewVisible" title="续费 / 调整到期时间" width="420px">
      <el-form label-width="100px">
        <el-form-item label="租户">
          <span>{{ renewTarget?.name }}（{{ renewTarget?.code }}）</span>
        </el-form-item>
        <el-form-item label="当前到期">
          <span>{{ renewTarget?.expireAt ? formatDateTime(renewTarget.expireAt) : '永久' }}</span>
          <el-tag v-if="renewTarget?.expired" type="danger" size="small" class="expired-tag">已过期</el-tag>
        </el-form-item>
        <el-form-item label="新到期时间">
          <el-date-picker v-model="renewExpireAt" type="datetime" class="full"
            placeholder="留空表示永久" format="YYYY-MM-DD HH:mm:ss" value-format="YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>
        <el-form-item label="套餐">
          <el-select v-model="renewPackageId" placeholder="选择套餐" class="full">
            <el-option v-for="p in packages" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="renewVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRenew">确定</el-button>
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
.full {
  width: 100%;
}
.expired-tag {
  margin-left: 8px;
}
</style>
