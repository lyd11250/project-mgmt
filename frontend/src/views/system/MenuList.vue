<template>
  <el-card>
    <div class="toolbar">
      <el-button v-permission="'system:menu:create'" type="success" @click="openCreate"
        >新建菜单</el-button
      >
    </div>

    <el-table
      v-loading="loading"
      :data="tree"
      row-key="id"
      border
      :header-cell-style="{ textAlign: 'center' }"
      :cell-style="{ textAlign: 'center' }"
      default-expand-all
      :tree-props="{ children: 'children' }"
    >
      <el-table-column prop="name" label="名称" />
      <el-table-column label="类型">
        <template #default="{ row }">{{ typeLabel(row.type) }}</template>
      </el-table-column>
      <el-table-column prop="perm" label="权限码" />
      <el-table-column prop="path" label="路由" />
      <el-table-column label="缓存" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.type === 'C'" :type="row.keepAlive === 1 ? 'success' : 'info'" size="small">
            {{ row.keepAlive === 1 ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" />
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button
            v-if="row.type !== 'F'"
            v-permission="'system:menu:create'"
            link
            type="primary"
            @click="openCreateChild(row)"
          >
            新增子项
          </el-button>
          <el-button v-permission="'system:menu:update'" link type="primary" @click="openEdit(row)">
            编辑
          </el-button>
          <el-button
            v-permission="'system:menu:delete'"
            link
            type="danger"
            @click="handleDelete(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="editVisible" :title="editingId ? '编辑菜单' : '新建菜单'" width="520px">
      <el-form ref="editRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级菜单">
          <el-select v-model="form.parentId" class="full">
            <el-option
              v-for="o in parentOptions"
              :key="o.value"
              :label="o.label"
              :value="o.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio value="M">目录</el-radio>
            <el-radio value="C">菜单</el-radio>
            <el-radio value="F">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item v-if="form.type !== 'F'" label="路由路径">
          <el-input v-model="form.path" placeholder="如 /system/users" />
        </el-form-item>
        <el-form-item v-if="form.type === 'C'" label="组件">
          <el-input v-model="form.component" placeholder="如 system/UserList" />
        </el-form-item>
        <el-form-item v-if="form.type === 'C'" label="是否缓存">
          <el-switch v-model="form.keepAlive" :active-value="1" :inactive-value="0" />
          <span class="hint">开启后切换页签不丢失已填数据</span>
        </el-form-item>
        <el-form-item v-if="form.type !== 'F'" label="图标">
          <el-input v-model="form.icon" placeholder="Element Plus 图标名，如 Setting" />
        </el-form-item>
        <el-form-item v-if="form.type !== 'M'" label="权限码">
          <el-input v-model="form.perm" placeholder="模块:资源:动作，如 system:user:create" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
        <el-form-item label="显示">
          <el-switch v-model="form.visible" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEdit">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  createMenu,
  deleteMenu,
  getMenuTree,
  updateMenu,
  type MenuNode,
  type MenuType,
} from '@/api/menu'

const loading = ref(false)
const tree = ref<MenuNode[]>([])

async function load() {
  loading.value = true
  try {
    tree.value = await getMenuTree()
  } finally {
    loading.value = false
  }
}

onMounted(load)

// 扁平化为父级下拉选项（含层级缩进）
interface Option {
  value: string
  label: string
}
const parentOptions = computed<Option[]>(() => {
  const opts: Option[] = [{ value: '0', label: '顶级' }]
  const walk = (nodes: MenuNode[], depth: number) => {
    for (const n of nodes) {
      if (n.type !== 'F') {
        opts.push({ value: n.id, label: `${'　'.repeat(depth)}${n.name}` })
        if (n.children?.length) walk(n.children, depth + 1)
      }
    }
  }
  walk(tree.value, 0)
  return opts
})

const typeLabel = (t: MenuType) => ({ M: '目录', C: '菜单', F: '按钮' })[t] ?? t

// ---- 新建 / 编辑 ----
const editVisible = ref(false)
const editRef = ref<FormInstance>()
const editingId = ref<string | null>(null)
const form = reactive({
  parentId: '0',
  type: 'C' as MenuType,
  name: '',
  path: '',
  component: '',
  icon: '',
  perm: '',
  sort: 0,
  visible: 1,
  keepAlive: 0,
})
const rules: FormRules = {
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
}

function reset(parentId = '0') {
  editingId.value = null
  form.parentId = parentId
  form.type = 'C'
  form.name = ''
  form.path = ''
  form.component = ''
  form.icon = ''
  form.perm = ''
  form.sort = 0
  form.visible = 1
  form.keepAlive = 0
}

function openCreate() {
  reset('0')
  editVisible.value = true
}

function openCreateChild(row: MenuNode) {
  reset(row.id)
  editVisible.value = true
}

function openEdit(row: MenuNode) {
  editingId.value = row.id
  form.parentId = row.parentId ?? '0'
  form.type = row.type
  form.name = row.name
  form.path = row.path ?? ''
  form.component = row.component ?? ''
  form.icon = row.icon ?? ''
  form.perm = row.perm ?? ''
  form.sort = row.sort ?? 0
  form.visible = row.visible ?? 1
  form.keepAlive = row.keepAlive ?? 0
  editVisible.value = true
}

async function submitEdit() {
  if (!editRef.value) return
  await editRef.value.validate(async (valid) => {
    if (!valid) return
    if (editingId.value) {
      await updateMenu(editingId.value, { ...form })
      ElMessage.success('已更新菜单')
    } else {
      await createMenu({ ...form })
      ElMessage.success('已创建菜单')
    }
    editVisible.value = false
    await load()
  })
}

async function handleDelete(row: MenuNode) {
  await ElMessageBox.confirm(`确认删除菜单「${row.name}」？`, '提示', { type: 'warning' })
  await deleteMenu(row.id)
  ElMessage.success('删除成功')
  await load()
}
</script>

<style scoped>
.toolbar {
  margin-bottom: 12px;
}
.full {
  width: 100%;
}
.hint {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
}
</style>
