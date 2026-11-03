<template>
  <div class="page-card roles-page">
    <div class="page-lead">
      <div>
        <h2>角色与权限</h2>
        <p>系统采用四类固定岗位角色。菜单与操作权限由角色统一控制，账号在“账号管理”中分配角色。</p>
      </div>
      <div class="page-lead-actions">
        <el-button type="primary" @click="router.push('/users')">分配账号角色</el-button>
      </div>
    </div>

    <el-alert type="info" :closable="false" show-icon class="role-note"
      title="四类角色来自甲方需求。为保证业务边界一致，当前版本不允许新增自定义角色。" />

    <div class="role-layout">
      <nav class="role-list" aria-label="角色列表">
        <button v-for="role in roles" :key="role.key" type="button" :class="{ active: activeRole === role.key }"
          @click="activeRole = role.key">
          <el-icon><component :is="role.icon" /></el-icon>
          <span><b>{{ role.name }}</b><small>{{ role.summary }}</small></span>
        </button>
      </nav>

      <section class="permission-sheet">
        <div class="permission-head">
          <div><span class="role-code">{{ currentRole.code }}</span><h3>{{ currentRole.name }}</h3></div>
          <el-tag type="success" effect="plain">已启用</el-tag>
        </div>
        <div class="permission-block">
          <h4>菜单权限</h4>
          <div class="permission-tags">
            <el-tag v-for="item in currentRole.menus" :key="item" effect="plain">{{ item }}</el-tag>
          </div>
        </div>
        <div class="permission-block">
          <h4>操作权限</h4>
          <el-table :data="currentRole.actions" border size="small">
            <el-table-column prop="module" label="业务模块" min-width="150" />
            <el-table-column prop="scope" label="允许操作" min-width="240" />
            <el-table-column label="权限性质" width="110">
              <template #default="{ row }"><el-tag :type="row.readonly ? 'info' : 'success'" size="small">{{ row.readonly ? '只读' : '可维护' }}</el-tag></template>
            </el-table-column>
          </el-table>
        </div>
        <div class="permission-block">
          <h4>角色说明</h4>
          <p>{{ currentRole.description }}</p>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

interface RoleDefinition {
  key: string
  code: string
  name: string
  summary: string
  icon: string
  menus: string[]
  actions: Array<{ module: string; scope: string; readonly?: boolean }>
  description: string
}

const router = useRouter()
const activeRole = ref('archivist')
const roles: RoleDefinition[] = [
  {
    key: 'archivist', code: 'ARCHIVIST', name: '运营 / 档案人员', summary: '维护路段与广告牌档案', icon: 'FolderOpened',
    menus: ['路段管理', '广告牌管理'],
    actions: [
      { module: '路段档案', scope: '查询、新增、编辑、删除' },
      { module: '广告牌档案', scope: '查询、新增、编辑、变动登记、照片与证照维护' }
    ],
    description: '负责基础档案真实性与合规资料完整性，不处理客户、合同和财务数据。'
  },
  {
    key: 'finance', code: 'FINANCE', name: '合同 / 财务人员', summary: '处理客户、合同和收付款', icon: 'Wallet',
    menus: ['客户档案', '合同列表', '应收账款', '收款登记', '资金流水'],
    actions: [
      { module: '客户与合同', scope: '查询、新增、编辑、合同生效与解除' },
      { module: '应收与收款', scope: '查询、收款登记、部分缴纳、导出' }
    ],
    description: '负责合同签订、年度应收、实际收付款和资金流水，不修改广告牌基础档案。'
  },
  {
    key: 'admin', code: 'ADMIN', name: '系统管理人员', summary: '维护账号、权限和系统配置', icon: 'Setting',
    menus: ['用户管理', '角色与权限', '数据字典', '操作日志'],
    actions: [
      { module: '用户账号', scope: '创建、编辑、启停、重置密码、分配角色' },
      { module: '数据字典', scope: '新增、编辑、排序、删除选项' },
      { module: '操作日志', scope: '查询与追溯', readonly: true }
    ],
    description: '负责系统配置与账号安全，不直接办理档案、合同和财务业务。'
  },
  {
    key: 'leader', code: 'LEADER', name: '领导', summary: '查看经营分析与风险', icon: 'DataAnalysis',
    menus: ['广告牌数量统计', '合同执行情况', '收款情况统计', '应收款统计'],
    actions: [{ module: '经营报表', scope: '查询、筛选、排序、导出', readonly: true }],
    description: '只读取实时经营报表，不新增或修改任何业务数据。'
  }
]

const currentRole = computed(() => roles.find((role) => role.key === activeRole.value) || roles[0])
</script>

<style scoped>
.role-note { margin-bottom: 16px; }
.role-layout { display: grid; grid-template-columns: 260px minmax(0, 1fr); min-height: 520px; border: 1px solid #dfe6ef; }
.role-list { padding: 10px; border-right: 1px solid #dfe6ef; background: #f7f9fc; }
.role-list button { display: grid; grid-template-columns: 34px minmax(0, 1fr); gap: 10px; width: 100%; align-items: center; padding: 14px 12px; border: 0; border-left: 3px solid transparent; background: transparent; text-align: left; cursor: pointer; }
.role-list button:hover { background: #edf3fa; }
.role-list button.active { border-left-color: #d9a514; background: #eaf1f9; color: #123d70; }
.role-list b, .role-list small { display: block; }
.role-list b { font-size: 14px; }
.role-list small { margin-top: 4px; color: #7c8797; }
.permission-sheet { padding: 22px 26px; }
.permission-head { display: flex; align-items: flex-start; justify-content: space-between; padding-bottom: 17px; border-bottom: 2px solid #183f6e; }
.permission-head h3 { margin: 4px 0 0; color: #173b64; font-size: 21px; }
.role-code { color: #7e8a9b; font-family: Arial, sans-serif; font-size: 12px; }
.permission-block { margin-top: 22px; }
.permission-block h4 { margin: 0 0 11px; color: #26384e; font-size: 14px; }
.permission-block p { color: #657286; line-height: 1.8; }
.permission-tags { display: flex; gap: 8px; flex-wrap: wrap; }
@media (max-width: 850px) { .role-layout { grid-template-columns: 1fr; } .role-list { display: grid; grid-template-columns: repeat(2, 1fr); border-right: 0; border-bottom: 1px solid #dfe6ef; } }
</style>
