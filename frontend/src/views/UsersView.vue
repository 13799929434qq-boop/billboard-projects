<template>
  <div class="page-card">
    <div class="page-header">
      <span class="title">账号管理</span>
      <el-button type="primary" :icon="Plus" @click="openForm()">新增账号</el-button>
    </div>
    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="username" label="登录账号" width="140" />
      <el-table-column prop="name" label="姓名" width="120" />
      <el-table-column label="角色" width="140">
        <template #default="{ row }">
          <el-tag :type="roleTagType(row.role)" size="small">{{ ROLE_NAMES[row.role] || row.role }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === '启用' ? 'success' : 'info'" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="110" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openForm(row)">编辑</el-button>
          <el-button link :type="row.status === '启用' ? 'warning' : 'success'" :disabled="row.username === store.user?.username"
            @click="toggleStatus(row)">
            {{ row.status === '启用' ? '停用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑账号' : '新增账号'" width="480px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="登录账号" prop="username">
          <el-input v-model="form.username" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="姓名" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" style="width: 100%">
            <el-option v-for="(label, value) in ROLE_NAMES" :key="value" :label="label" :value="value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="form.id ? '重置密码' : '初始密码'" :prop="form.id ? '' : 'password'">
          <el-input v-model="form.password" type="password" show-password
            :placeholder="form.id ? '留空则不修改密码' : '请输入初始密码'" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { userApi } from '../api'
import { useUserStore } from '../stores/user'
import { ROLE_NAMES } from '../types'
import type { SysUser } from '../types'

const store = useUserStore()

const list = ref<SysUser[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<{ id?: number; username: string; name: string; role: string; password: string }>({
  username: '',
  name: '',
  role: 'archivist',
  password: ''
})

const formRules: FormRules = {
  username: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  password: [{ required: true, message: '请输入初始密码', trigger: 'blur' }]
}

function roleTagType(role: string): 'danger' | 'primary' | 'warning' | 'success' {
  switch (role) {
    case 'admin':
      return 'danger'
    case 'finance':
      return 'warning'
    case 'leader':
      return 'success'
    default:
      return 'primary'
  }
}

async function load() {
  loading.value = true
  try {
    list.value = await userApi.list()
  } finally {
    loading.value = false
  }
}

function openForm(row?: SysUser) {
  Object.assign(form, row
    ? { id: row.id, username: row.username, name: row.name, role: row.role, password: '' }
    : { id: undefined, username: '', name: '', role: 'archivist', password: '' })
  dialogVisible.value = true
}

async function onSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (form.id) {
      await userApi.update(form.id, { name: form.name, role: form.role, password: form.password || undefined })
    } else {
      await userApi.create({ username: form.username, name: form.name, role: form.role, password: form.password })
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } catch {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row: SysUser) {
  const next = row.status === '启用' ? '停用' : '启用'
  await ElMessageBox.confirm(`确定${next}账号「${row.username}」吗？`, '提示', { type: 'warning' })
  await userApi.update(row.id, { status: next })
  ElMessage.success(`已${next}`)
  load()
}

onMounted(load)
</script>
