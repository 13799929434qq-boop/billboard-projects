<template>
  <div class="login-page">
    <div class="login-box">
      <div class="brand">
        <el-icon :size="36" color="#ffd04b"><Guide /></el-icon>
        <h1>公路广告牌管理系统</h1>
        <p>路段 · 广告牌 · 合同 · 收款 · 报表 一体化管理</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="onLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入账号" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" :prefix-icon="Lock" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="login-btn" :loading="loading" @click="onLogin">登 录</el-button>
        </el-form-item>
      </el-form>
      <el-divider content-position="center"><span class="demo-tip">演示账号（点击填充）</span></el-divider>
      <div class="demo-accounts">
        <div v-for="acc in demoAccounts" :key="acc.username" class="acc-card" @click="fill(acc)">
          <div class="acc-role">{{ acc.roleName }}</div>
          <div class="acc-user">{{ acc.username }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { authApi } from '../api'
import { useUserStore } from '../stores/user'
import { ROLE_NAMES } from '../types'
import { landingPath } from '../router'

const router = useRouter()
const route = useRoute()
const store = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

const rules: FormRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const demoAccounts = [
  { username: 'dangan', password: 'dangan123', roleName: ROLE_NAMES.archivist },
  { username: 'caiwu', password: 'caiwu123', roleName: ROLE_NAMES.finance },
  { username: 'admin', password: 'admin123', roleName: ROLE_NAMES.admin },
  { username: 'lingdao', password: 'lingdao123', roleName: ROLE_NAMES.leader }
]

function fill(acc: { username: string; password: string }) {
  form.username = acc.username
  form.password = acc.password
}

async function onLogin() {
  await formRef.value?.validate()
  loading.value = true
  try {
    const data = await authApi.login(form.username, form.password)
    store.setAuth(data.token, data.user)
    ElMessage.success(`欢迎，${data.user.name}`)
    const redirect = (route.query.redirect as string) || landingPath(store.role)
    router.push(redirect)
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(180deg, #0f2d52 0%, #16488a 60%, #2b6cb0 100%);
}

.login-box {
  width: 420px;
  background: #fff;
  border-radius: 8px;
  padding: 36px 40px 28px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.25);
}

.brand {
  text-align: center;
  margin-bottom: 26px;
}

.brand h1 {
  font-size: 22px;
  color: #16488a;
  margin: 10px 0 6px;
}

.brand p {
  color: #909399;
  font-size: 13px;
}

.login-btn {
  width: 100%;
  letter-spacing: 8px;
}

.demo-tip {
  font-size: 12px;
  color: #909399;
}

.demo-accounts {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.acc-card {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 8px 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.acc-card:hover {
  border-color: #16488a;
  background: #f0f5fb;
}

.acc-role {
  font-weight: 600;
  color: #16488a;
  font-size: 13px;
}

.acc-user {
  color: #909399;
  font-size: 12px;
  margin-top: 2px;
}
</style>
