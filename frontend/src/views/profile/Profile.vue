<template>
  <div class="profile">
    <el-card>
      <el-tabs v-model="activeTab">
        <!-- 基本资料 -->
        <el-tab-pane label="基本资料" name="basic">
          <el-form
            ref="profileRef"
            :model="profileForm"
            :rules="profileRules"
            label-width="90px"
            class="form"
          >
            <el-form-item label="用户名">
              <el-input :value="profile?.username" disabled />
            </el-form-item>
            <el-form-item label="角色">
              <el-tag v-for="r in profile?.roles ?? []" :key="r.id" class="role-tag">
                {{ r.name }}
              </el-tag>
              <span v-if="!profile?.roles?.length" class="muted">—</span>
            </el-form-item>
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="profileForm.nickname" placeholder="请输入昵称" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="savingProfile" @click="submitProfile">
                保存
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 修改密码 -->
        <el-tab-pane label="修改密码" name="password">
          <el-form ref="pwdRef" :model="pwdForm" :rules="pwdRules" label-width="90px" class="form">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="pwdForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="savingPwd" @click="submitPassword">
                修改密码
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { changePassword, getProfile, updateProfile, type ProfileInfo } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import { resetDynamicRoutes } from '@/router'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

// 标签页以 URL query 为唯一数据源：读取走 query，切换走 router.replace。
// 这样顶栏入口（改 query）与手动点击标签（改 query）走同一条路径，URL 与标签始终一致。
const activeTab = computed({
  get: () => (route.query.tab === 'password' ? 'password' : 'basic'),
  set: (tab) => {
    router.replace({ path: '/profile', query: { tab } })
  },
})

// ---- 基本资料 ----
const profile = ref<ProfileInfo | null>(null)
const profileRef = ref<FormInstance>()
const savingProfile = ref(false)
const profileForm = reactive({ nickname: '', phone: '' })
const profileRules: FormRules = {
  nickname: [{ max: 64, message: '昵称长度不能超过 64', trigger: 'blur' }],
  phone: [{ pattern: /^$|^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
}

async function loadProfile() {
  profile.value = await getProfile()
  profileForm.nickname = profile.value.nickname ?? ''
  profileForm.phone = profile.value.phone ?? ''
}

onMounted(loadProfile)

async function submitProfile() {
  if (!profileRef.value) return
  await profileRef.value.validate(async (valid) => {
    if (!valid) return
    savingProfile.value = true
    try {
      await updateProfile({ ...profileForm })
      ElMessage.success('资料已更新')
      // 同步刷新顶栏展示的昵称
      await auth.fetchMe()
      await loadProfile()
    } finally {
      savingProfile.value = false
    }
  })
}

// ---- 修改密码 ----
const pwdRef = ref<FormInstance>()
const savingPwd = ref(false)
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 64, message: '密码需 8-64 位', trigger: 'blur' },
    {
      pattern: /^(?=.*[A-Za-z])(?=.*\d)\S+$/,
      message: '密码需同时包含字母和数字，且不含空格',
      trigger: 'blur',
    },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== pwdForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

async function submitPassword() {
  if (!pwdRef.value) return
  await pwdRef.value.validate(async (valid) => {
    if (!valid) return
    savingPwd.value = true
    try {
      await changePassword({
        oldPassword: pwdForm.oldPassword,
        newPassword: pwdForm.newPassword,
      })
      ElMessage.success('密码已修改，请重新登录')
      await auth.logout()
      resetDynamicRoutes()
      router.replace('/login')
    } finally {
      savingPwd.value = false
    }
  })
}
</script>

<style scoped>
.form {
  max-width: 480px;
}
.role-tag {
  margin-right: 4px;
}
.muted {
  color: #909399;
}
</style>
