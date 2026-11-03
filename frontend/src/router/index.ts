import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '../stores/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('../layout/MainLayout.vue'),
    redirect: () => landingPath(useUserStore().role),
    children: [
      { path: 'sections', component: () => import('../views/SectionsView.vue'), meta: { title: '路段档案', roles: ['archivist'] } },
      { path: 'billboards', component: () => import('../views/BillboardsView.vue'), meta: { title: '广告牌档案', roles: ['archivist'] } },
      { path: 'billboards/new', component: () => import('../views/BillboardFormView.vue'), meta: { title: '新增广告牌', roles: ['archivist'] } },
      { path: 'billboards/:id/edit', component: () => import('../views/BillboardFormView.vue'), meta: { title: '编辑广告牌', roles: ['archivist'] } },
      { path: 'billboards/:id', component: () => import('../views/BillboardDetailView.vue'), meta: { title: '广告牌详情', roles: ['archivist'] } },
      { path: 'billboard-changes', component: () => import('../views/BillboardChangesView.vue'), meta: { title: '广告牌变动登记', roles: ['archivist'] } },
      { path: 'dicts', component: () => import('../views/DictsView.vue'), meta: { title: '数据字典', roles: ['admin'] } },
      { path: 'merchants', component: () => import('../views/MerchantsView.vue'), meta: { title: '商家档案', roles: ['finance'] } },
      { path: 'merchants/new', component: () => import('../views/MerchantFormView.vue'), meta: { title: '新增客户', roles: ['finance'] } },
      { path: 'merchants/:id/edit', component: () => import('../views/MerchantFormView.vue'), meta: { title: '编辑客户', roles: ['finance'] } },
      { path: 'merchants/:id', component: () => import('../views/MerchantDetailView.vue'), meta: { title: '客户详情', roles: ['finance'] } },
      { path: 'contracts', component: () => import('../views/ContractsView.vue'), meta: { title: '合同管理', roles: ['finance'] } },
      { path: 'contracts/new', component: () => import('../views/ContractFormView.vue'), meta: { title: '新增合同', roles: ['finance'] } },
      { path: 'contracts/:id/edit', component: () => import('../views/ContractFormView.vue'), meta: { title: '编辑合同', roles: ['finance'] } },
      { path: 'contracts/:id', component: () => import('../views/ContractDetailView.vue'), meta: { title: '合同详情', roles: ['finance'] } },
      { path: 'receivables', component: () => import('../views/ReceivablesView.vue'), meta: { title: '应收管理', roles: ['finance'] } },
      { path: 'receivables/:id', component: () => import('../views/ReceivableDetailView.vue'), meta: { title: '应收账单详情', roles: ['finance'] } },
      { path: 'payments/new', component: () => import('../views/PaymentRegistrationView.vue'), meta: { title: '收款登记', roles: ['finance'] } },
      { path: 'transactions', component: () => import('../views/TransactionsView.vue'), meta: { title: '资金流水', roles: ['finance'] } },
      { path: 'reports/billboard-stats', component: () => import('../views/ReportsView.vue'), meta: { title: '广告牌数量统计', report: 'billboard', roles: ['leader'] } },
      { path: 'reports/contract-execution', component: () => import('../views/ReportsView.vue'), meta: { title: '合同执行情况', report: 'execution', roles: ['leader'] } },
      { path: 'reports/collection-stats', component: () => import('../views/ReportsView.vue'), meta: { title: '收款情况统计', report: 'collection', roles: ['leader'] } },
      { path: 'reports/receivable-stats', component: () => import('../views/ReportsView.vue'), meta: { title: '应收款及欠款统计', report: 'receivable', roles: ['leader'] } },
      { path: 'users', component: () => import('../views/UsersView.vue'), meta: { title: '账号管理', roles: ['admin'] } },
      { path: 'roles', component: () => import('../views/RolesView.vue'), meta: { title: '角色与权限', roles: ['admin'] } },
      { path: 'logs', component: () => import('../views/LogsView.vue'), meta: { title: '操作日志', roles: ['admin'] } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: () => landingPath(useUserStore().role) }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export function landingPath(role?: string): string {
  if (role === 'archivist') return '/sections'
  if (role === 'leader') return '/reports/billboard-stats'
  if (role === 'finance') return '/merchants'
  if (role === 'admin') return '/users'
  return '/login'
}

router.beforeEach((to) => {
  const store = useUserStore()
  document.title = `${(to.meta.title as string) || ''} - 公路广告牌管理系统`
  if (to.path === '/login') {
    return store.isLoggedIn ? landingPath(store.role) : true
  }
  if (!store.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.path === '/') {
    return landingPath(store.role)
  }
  const roles = to.meta.roles as string[] | undefined
  if (roles && !roles.includes(store.role)) {
    return landingPath(store.role)
  }
  return true
})

export default router
