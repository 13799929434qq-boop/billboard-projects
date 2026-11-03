<template>
  <el-container class="main-layout">
    <el-aside width="220px" class="sidebar">
      <div class="logo">
        <el-icon :size="22" color="#ffd04b"><Guide /></el-icon>
        <span>公路广告牌管理系统</span>
      </div>
      <el-menu
        class="sidebar-menu"
        :default-active="activeMenu"
        background-color="#0f2d52"
        text-color="#b8c7d9"
        active-text-color="#ffd04b"
        router
      >
        <template v-for="group in menuGroups" :key="group.title || 'main'">
          <div v-if="group.title" class="menu-group-title">{{ group.title }}</div>
          <el-sub-menu v-for="item in group.items.filter((i) => i.children)" :key="item.path" :index="item.path">
            <template #title>
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.title }}</span>
            </template>
            <el-menu-item v-for="child in item.children" :key="child.path" :index="child.path">
              <el-icon><component :is="child.icon" /></el-icon>
              <span>{{ child.title }}</span>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-for="item in group.items.filter((i) => !i.children)" :key="item.path" :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="topbar" height="56px">
        <div class="crumb">{{ route.meta.title || '公路广告牌管理系统' }}</div>
        <div class="user-area">
          <el-tag type="primary" effect="dark" size="small">{{ roleName }}</el-tag>
          <el-dropdown @command="onCommand">
            <span class="username">
              {{ store.name }}<el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '../stores/user'
import { authApi } from '../api'
import { ROLE_NAMES } from '../types'

interface MenuItem {
  path: string
  title: string
  icon: string
  roles: string[]
  children?: MenuItem[]
}

const ALL_MENUS: MenuItem[] = [
  { path: '/sections', title: '路段档案', icon: 'MapLocation', roles: ['archivist'] },
  { path: '/billboards', title: '广告牌档案', icon: 'Postcard', roles: ['archivist'] },
  { path: '/dicts', title: '数据字典', icon: 'Collection', roles: ['admin'] },
  { path: '/merchants', title: '商家档案', icon: 'OfficeBuilding', roles: ['finance'] },
  { path: '/contracts', title: '合同管理', icon: 'Notebook', roles: ['finance'] },
  { path: '/receivables', title: '应收管理', icon: 'Money', roles: ['finance'] },
  { path: '/payments/new', title: '收款登记', icon: 'Coin', roles: ['finance'] },
  { path: '/transactions', title: '资金流水', icon: 'Wallet', roles: ['finance'] },
  {
    path: '/reports',
    title: '经营报表',
    icon: 'DataAnalysis',
    roles: ['leader'],
    children: [
      { path: '/reports/billboard-stats', title: '广告牌数量统计', icon: 'DataLine', roles: ['leader'] },
      { path: '/reports/contract-execution', title: '合同执行情况', icon: 'Tickets', roles: ['leader'] },
      { path: '/reports/collection-stats', title: '收款情况统计', icon: 'TrendCharts', roles: ['leader'] },
      { path: '/reports/receivable-stats', title: '应收款及欠款统计', icon: 'Money', roles: ['leader'] }
    ]
  },
  { path: '/users', title: '账号管理', icon: 'User', roles: ['admin'] },
  { path: '/roles', title: '角色与权限', icon: 'Key', roles: ['admin'] },
  { path: '/logs', title: '操作日志', icon: 'Document', roles: ['admin'] }
]

const route = useRoute()
const router = useRouter()
const store = useUserStore()

const menus = computed(() => ALL_MENUS.filter((m) => m.roles.includes(store.role)))
const menuGroups = computed(() => {
  if (store.role === 'archivist') {
    return [
      { title: '基础档案', items: menus.value.filter((m) => ['/sections', '/billboards'].includes(m.path)) }
    ]
  }
  if (store.role === 'finance') {
    return [
      { title: '客户与合同', items: menus.value.filter((m) => ['/merchants', '/contracts'].includes(m.path)) },
      { title: '收款与流水', items: menus.value.filter((m) => ['/receivables', '/payments/new', '/transactions'].includes(m.path)) }
    ]
  }
  if (store.role === 'leader') {
    return [{ title: '经营分析', items: menus.value }]
  }
  if (store.role === 'admin') {
    return [{ title: '系统管理', items: menus.value }]
  }
  return []
})
const activeMenu = computed(() => route.path)
const roleName = computed(() => ROLE_NAMES[store.role] || store.role)

async function onCommand(cmd: string) {
  if (cmd === 'logout') {
    await ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' })
    try {
      await authApi.logout()
    } catch {
      // 忽略登出接口异常，本地直接清理
    }
    store.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.main-layout {
  height: 100vh;
}

.sidebar {
  background-color: #0f2d52;
  overflow-y: auto;
}

.logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.sidebar-menu {
  border-right: none;
}

.menu-group-title {
  padding: 18px 20px 7px;
  color: #7187a1;
  font-size: 12px;
  line-height: 1;
}

.topbar {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.06);
}

.crumb {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.user-area {
  display: flex;
  align-items: center;
  gap: 12px;
}

.username {
  cursor: pointer;
  display: flex;
  align-items: center;
  color: #303133;
  font-weight: 500;
}

.content {
  background-color: #f5f7fa;
  padding: 16px;
  overflow-y: auto;
}
</style>
