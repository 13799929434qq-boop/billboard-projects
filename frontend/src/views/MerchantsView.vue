<template>
  <div class="page-card">
    <div class="page-lead">
      <div>
        <h2>商家档案</h2>
        <p>维护客户联系方式、开票资料和合作状态；点击“往来账”可查看该商家的合同与欠款汇总。</p>
      </div>
      <div class="page-lead-actions">
        <span class="record-count">当前共 <strong>{{ list.length }}</strong> 家</span>
        <el-button v-if="canWrite" type="primary" :icon="Plus" @click="router.push('/merchants/new')">新增商家</el-button>
      </div>
    </div>
    <div class="filter-panel"><div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索商家名称/联系人/电话" clearable style="width: 260px" :prefix-icon="Search" @change="load" />
      <el-checkbox v-model="onlyBlacklist" @change="load">只看黑名单</el-checkbox>
    </div></div>
    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="name" label="商家名称" min-width="180" />
      <el-table-column prop="contact" label="联系人" width="100" />
      <el-table-column prop="phone" label="联系电话" width="130" />
      <el-table-column prop="taxNo" label="税号" min-width="170" show-overflow-tooltip />
      <el-table-column label="开户行/账号" min-width="200">
        <template #default="{ row }">
          <div>{{ row.bankName }}</div>
          <div class="bank-no">{{ row.bankAccount }}</div>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.blacklisted" type="danger" size="small">黑名单</el-tag>
          <el-tag v-else type="success" size="small">正常</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="255" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/merchants/${row.id}`)">详情</el-button>
          <el-button v-if="canWrite" link type="primary" @click="router.push(`/merchants/${row.id}/edit`)">编辑</el-button>
          <el-button v-if="canWrite" link :type="row.blacklisted ? 'success' : 'warning'" @click="toggleBlacklist(row)">
            {{ row.blacklisted ? '移出黑名单' : '加入黑名单' }}
          </el-button>
          <el-button v-if="canWrite" link type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑商家' : '新增商家'" width="560px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="商家名称" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="联系人" prop="contact"><el-input v-model="form.contact" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="联系电话" prop="phone"><el-input v-model="form.phone" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="地址"><el-input v-model="form.address" /></el-form-item>
        <el-form-item label="税号"><el-input v-model="form.taxNo" /></el-form-item>
        <el-form-item label="开户行"><el-input v-model="form.bankName" /></el-form-item>
        <el-form-item label="银行账号"><el-input v-model="form.bankAccount" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="accountVisible" title="商家往来账" size="720px">
      <template v-if="accountMerchant">
        <div class="merchant-account-head">
          <div><div class="merchant-name">{{ accountMerchant.name }}</div><div class="merchant-contact">{{ accountMerchant.contact }} · {{ accountMerchant.phone }}</div></div>
          <el-tag :type="accountMerchant.blacklisted ? 'danger' : 'success'">{{ accountMerchant.blacklisted ? '黑名单' : '正常合作' }}</el-tag>
        </div>
        <div class="detail-summary" v-loading="accountLoading">
          <div class="summary-item"><div class="summary-label">累计应收</div><div class="summary-value">{{ fmtMoney(accountSummary.total) }}</div></div>
          <div class="summary-item"><div class="summary-label">累计已收</div><div class="summary-value received">{{ fmtMoney(accountSummary.received) }}</div></div>
          <div class="summary-item arrears-item"><div class="summary-label">当前欠款</div><div class="summary-value arrears-value">{{ fmtMoney(accountSummary.arrears) }}</div></div>
        </div>
        <div class="account-section-title">关联合同</div>
        <el-table :data="accountContracts" border size="small" empty-text="暂无关联合同">
          <el-table-column prop="code" label="合同编号" width="135" />
          <el-table-column label="合同期限" min-width="190"><template #default="{ row }">{{ row.startDate }} 至 {{ row.endDate }}</template></el-table-column>
          <el-table-column prop="status" label="状态" width="85"><template #default="{ row }"><el-tag size="small" :type="statusTagType(row.status)">{{ row.status }}</el-tag></template></el-table-column>
          <el-table-column label="合同金额" width="125" align="right"><template #default="{ row }">{{ fmtMoney(row.totalAmount) }}</template></el-table-column>
        </el-table>
        <div class="account-section-title">应收明细</div>
        <el-table :data="accountReceivables" border size="small" max-height="330" empty-text="暂无应收记录">
          <el-table-column prop="contractCode" label="合同" width="125" />
          <el-table-column prop="billboardCode" label="广告牌" width="115" />
          <el-table-column prop="year" label="年度" width="72" />
          <el-table-column label="应收" width="105" align="right"><template #default="{ row }">{{ fmtMoney(row.amount) }}</template></el-table-column>
          <el-table-column label="欠款" width="105" align="right"><template #default="{ row }"><span :class="{ 'account-arrears': row.amount > row.received }">{{ fmtMoney(row.amount - row.received) }}</span></template></el-table-column>
          <el-table-column prop="status" label="状态" width="90" />
        </el-table>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { contractApi, merchantApi, receivableApi } from '../api'
import { useUserStore } from '../stores/user'
import { fmtMoney, statusTagType } from '../utils/format'
import type { Contract, Merchant, Receivable } from '../types'

const store = useUserStore()
const router = useRouter()
const canWrite = computed(() => store.role === 'finance')

const list = ref<Merchant[]>([])
const loading = ref(false)
const keyword = ref('')
const onlyBlacklist = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<Partial<Merchant>>({})
const accountVisible = ref(false)
const accountLoading = ref(false)
const accountMerchant = ref<Merchant | null>(null)
const accountReceivables = ref<Receivable[]>([])
const accountContracts = ref<Contract[]>([])
const accountSummary = computed(() => {
  const total = accountReceivables.value.reduce((sum, row) => sum + row.amount, 0)
  const received = accountReceivables.value.reduce((sum, row) => sum + row.received, 0)
  return { total, received, arrears: total - received }
})

const formRules: FormRules = {
  name: [{ required: true, message: '请输入商家名称', trigger: 'blur' }],
  contact: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    list.value = await merchantApi.list(keyword.value || undefined, onlyBlacklist.value || undefined)
  } finally {
    loading.value = false
  }
}

function openForm(row?: Merchant) {
  Object.keys(form).forEach((k) => delete (form as Record<string, unknown>)[k])
  Object.assign(
    form,
    row ? { ...row } : { name: '', contact: '', phone: '', address: '', taxNo: '', bankName: '', bankAccount: '', remark: '' }
  )
  dialogVisible.value = true
}

async function openAccount(row: Merchant) {
  accountMerchant.value = row
  accountVisible.value = true
  accountLoading.value = true
  try {
    ;[accountReceivables.value, accountContracts.value] = await Promise.all([
      receivableApi.list({ merchantId: row.id }),
      contractApi.list({ merchantId: row.id })
    ])
  } finally {
    accountLoading.value = false
  }
}

async function onSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (form.id) {
      await merchantApi.update(form.id, form)
    } else {
      await merchantApi.create(form)
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

async function toggleBlacklist(row: Merchant) {
  const action = row.blacklisted ? '移出黑名单' : '加入黑名单'
  await ElMessageBox.confirm(
    row.blacklisted
      ? `确定将「${row.name}」移出黑名单吗？`
      : `加入黑名单后将无法再与「${row.name}」签订生效合同，已签合同不受影响（自然到期），确定继续吗？`,
    action,
    { type: 'warning' }
  )
  await merchantApi.update(row.id, { blacklisted: !row.blacklisted })
  ElMessage.success(`已${action}`)
  load()
}

async function onDelete(row: Merchant) {
  await ElMessageBox.confirm(`确定删除商家「${row.name}」吗？存在合同的商家无法删除。`, '删除确认', {
    type: 'warning',
    confirmButtonText: '确定删除',
    confirmButtonClass: 'el-button--danger'
  })
  await merchantApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.bank-no {
  color: #909399;
  font-size: 12px;
}

.merchant-account-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid #dfe6ef;
}

.merchant-name {
  color: #0f2d52;
  font-size: 20px;
  font-weight: 650;
}

.merchant-contact {
  margin-top: 5px;
  color: #7b8797;
}

.received {
  color: #409f69 !important;
}

.arrears-item {
  border-left-color: #d95555 !important;
}

.arrears-value,
.account-arrears {
  color: #d95555 !important;
}

.account-section-title {
  margin: 20px 0 9px;
  color: #243247;
  font-weight: 650;
}
</style>
