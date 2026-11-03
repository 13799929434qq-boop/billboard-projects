<template>
  <div class="page-card">
    <div class="page-lead">
      <div>
        <h2>资金流水</h2>
        <p>记录全部收入和支出。客户未指明合同的款项先进入待核对，确认后再到应收账单登记收款。</p>
      </div>
      <div class="page-lead-actions">
        <el-button :icon="Download" @click="exportCurrent">导出当前流水</el-button>
        <el-button v-if="canWrite" type="primary" :icon="Plus" @click="openForm()">登记流水</el-button>
      </div>
    </div>
    <div class="ledger-tabs">
      <button :class="{ active: ledgerView === 'all' }" @click="ledgerView = 'all'">全部流水 <span>{{ list.length }}</span></button>
      <button :class="{ active: ledgerView === 'pending' }" @click="ledgerView = 'pending'">待核对挂账 <span>{{ pendingCount }}</span></button>
    </div>
    <el-alert v-if="ledgerView === 'pending'" type="warning" :closable="false" show-icon class="pending-alert"
      title="这些收入尚未关联具体合同。核对付款用途后，请到应收管理登记对应账单的收款。" />
    <div class="filter-panel"><div class="toolbar">
      <el-radio-group v-model="query.direction" @change="load">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="收入">收入</el-radio-button>
        <el-radio-button value="支出">支出</el-radio-button>
      </el-radio-group>
      <el-select v-model="query.category" placeholder="类别" clearable style="width: 150px" @change="load">
        <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
      </el-select>
      <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD" range-separator="至"
        start-placeholder="开始日期" end-placeholder="结束日期" style="width: 260px" @change="load" />
      <el-input v-model="query.keyword" placeholder="搜索摘要/经办人" clearable style="width: 180px" @change="load" />
      <span class="record-count">筛选结果 <strong>{{ displayList.length }}</strong> 条</span>
    </div></div>

    <el-table :data="displayList" v-loading="loading" border stripe>
      <el-table-column prop="txDate" label="日期" width="105" />
      <el-table-column prop="direction" label="方向" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.direction === '收入' ? 'success' : 'danger'" size="small">{{ row.direction }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="category" label="类别" width="110" />
      <el-table-column label="金额（元）" width="130" align="right">
        <template #default="{ row }">
          <span class="money" :class="row.direction === '收入' ? 'income' : 'expense'">
            {{ row.direction === '收入' ? '+' : '-' }}{{ fmtMoney(row.amount) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="关联商家 / 合同" min-width="200">
        <template #default="{ row }">
          <span>{{ row.merchantName || '（未关联，挂账）' }}</span>
          <span v-if="row.contractCode" class="contract-code"> / {{ row.contractCode }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="summary" label="摘要" min-width="200" show-overflow-tooltip />
      <el-table-column prop="operator" label="经办人" width="90" />
      <el-table-column label="核对状态" width="105">
        <template #default="{ row }">
          <el-tag v-if="row.direction === '收入' && !row.contractId" type="warning" size="small">待核对</el-tag>
          <el-tag v-else type="success" size="small" effect="plain">已关联</el-tag>
        </template>
      </el-table-column>
      <el-table-column v-if="canWrite" label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <template v-if="!isAutoReceipt(row)">
            <el-button link type="primary" @click="openForm(row)">编辑</el-button>
            <el-button link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
          <span v-else class="locked-text">应收生成</span>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑手工流水' : '登记流水'" width="520px">
      <el-alert type="info" :closable="false" show-icon class="form-tip"
        title="客户付款未指明合同时，可先选择商家登记为挂账收入，后续人工核对冲销" />
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="方向" prop="direction">
          <el-radio-group v-model="form.direction">
            <el-radio value="收入">收入</el-radio>
            <el-radio value="支出">支出</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="类别" prop="category">
          <el-select v-model="form.category" style="width: 100%">
            <el-option v-for="c in form.direction === '收入' ? incomeCategories : expenseCategories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="金额（元）" prop="amount">
          <el-input-number v-model="form.amount" :min="0.01" :precision="2" :controls="false" style="width: 100%" />
        </el-form-item>
        <el-form-item label="日期" prop="txDate">
          <el-date-picker v-model="form.txDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="关联商家">
          <el-select v-model="form.merchantId" clearable placeholder="未指明可先挂账" style="width: 100%">
            <el-option v-for="m in lookup?.merchants || []" :key="m.id" :label="m.name" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联合同">
          <el-select v-model="form.contractId" clearable placeholder="可选" style="width: 100%">
            <el-option v-for="c in lookup?.contracts || []" :key="c.id" :label="c.code" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="摘要" prop="summary">
          <el-input v-model="form.summary" type="textarea" :rows="2" placeholder="款项说明" />
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Download, Plus } from '@element-plus/icons-vue'
import { lookupApi, transactionApi } from '../api'
import { useUserStore } from '../stores/user'
import { fmtMoney } from '../utils/format'
import type { FundTransaction, LookupData } from '../types'

const store = useUserStore()
const canWrite = computed(() => store.role === 'finance')

const incomeCategories = ['合同款', '履约保证金', '维护保证金', '其他收入']
const expenseCategories = ['路政费', '保险费', '维护费', '其他支出']
const categories = [...incomeCategories, ...expenseCategories]

const list = ref<FundTransaction[]>([])
const lookup = ref<LookupData | null>(null)
const loading = ref(false)
const query = reactive({ direction: '', category: '', keyword: '' })
const dateRange = ref<[string, string] | null>(null)
const ledgerView = ref<'all' | 'pending'>('all')
const displayList = computed(() => ledgerView.value === 'pending'
  ? list.value.filter((row) => row.direction === '收入' && !row.contractId)
  : list.value)
const pendingCount = computed(() => list.value.filter((row) => row.direction === '收入' && !row.contractId).length)

const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<Partial<FundTransaction>>({})

const formRules: FormRules = {
  direction: [{ required: true, message: '请选择方向', trigger: 'change' }],
  category: [{ required: true, message: '请选择类别', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  txDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  summary: [{ required: true, message: '请填写摘要', trigger: 'blur' }]
}

const canShowMerchant = computed(() => form.direction === '收入')

async function load() {
  loading.value = true
  try {
    list.value = await transactionApi.list({
      direction: query.direction || undefined,
      category: query.category || undefined,
      keyword: query.keyword || undefined,
      from: dateRange.value?.[0],
      to: dateRange.value?.[1]
    })
  } finally {
    loading.value = false
  }
}

function openForm(row?: FundTransaction) {
  Object.keys(form).forEach((k) => delete (form as Record<string, unknown>)[k])
  Object.assign(form, row ? { ...row } : {
    direction: '收入',
    category: '合同款',
    amount: undefined,
    txDate: new Date().toISOString().slice(0, 10),
    merchantId: null,
    contractId: null,
    summary: ''
  })
  dialogVisible.value = true
}

async function onSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (form.id) {
      await transactionApi.update(form.id, form)
      ElMessage.success('修改成功')
    } else {
      await transactionApi.create(form)
      ElMessage.success('已登记')
    }
    dialogVisible.value = false
    load()
  } catch {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}

function isAutoReceipt(row: FundTransaction): boolean {
  return row.direction === '收入' && row.category === '合同款' && !!row.contractId
}

function exportCurrent() {
  const header = ['日期', '方向', '类别', '金额', '商家', '合同', '摘要', '经办人', '核对状态']
  const rows = displayList.value.map((row) => [
    row.txDate, row.direction, row.category, row.amount, row.merchantName || '', row.contractCode || '', row.summary,
    row.operator, row.direction === '收入' && !row.contractId ? '待核对' : '已关联'
  ])
  const csv = '\ufeff' + [header, ...rows].map((line) => line.map((cell) => `"${String(cell ?? '').replace(/"/g, '""')}"`).join(',')).join('\r\n')
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }))
  const link = document.createElement('a')
  link.href = url
  link.download = '资金流水.csv'
  link.click()
  URL.revokeObjectURL(url)
}

async function onDelete(row: FundTransaction) {
  await ElMessageBox.confirm(`确定删除该笔${row.direction}（${fmtMoney(row.amount)} 元）吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '确定删除',
    confirmButtonClass: 'el-button--danger'
  })
  await transactionApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(async () => {
  load()
  lookup.value = await lookupApi.all()
})
</script>

<style scoped>
.form-tip {
  margin-bottom: 14px;
}

.income {
  color: #67c23a;
}

.locked-text {
  color: #8b96a5;
  font-size: 12px;
}

.expense {
  color: #f56c6c;
}

.contract-code {
  color: #909399;
  font-size: 12px;
}

.ledger-tabs {
  display: flex;
  gap: 2px;
  margin-bottom: 14px;
  border-bottom: 1px solid #dfe6ef;
}

.ledger-tabs button {
  padding: 10px 18px;
  border: 0;
  border-bottom: 3px solid transparent;
  background: transparent;
  color: #697586;
  cursor: pointer;
  font: inherit;
}

.ledger-tabs button.active {
  border-bottom-color: #16488a;
  color: #16488a;
  font-weight: 650;
}

.ledger-tabs span {
  display: inline-block;
  min-width: 20px;
  margin-left: 5px;
  padding: 1px 6px;
  border-radius: 10px;
  background: #edf1f6;
  font-size: 12px;
}

.pending-alert {
  margin-bottom: 12px;
}
</style>
