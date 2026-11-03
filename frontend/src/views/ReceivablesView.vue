<template>
  <div class="page-card">
    <div class="page-lead">
      <div>
        <h2>应收管理</h2>
        <p>每份生效合同按“年度 × 广告牌”拆分账单。收款后同步冲减应收并自动形成资金流水。</p>
      </div>
      <div class="page-lead-actions">
        <el-button :icon="Download" @click="exportCurrent">导出当前明细</el-button>
      </div>
    </div>

    <div class="receivable-flow">
      <div><b>1</b><span>合同生效</span></div><i /><div><b>2</b><span>生成年度账单</span></div><i /><div class="current"><b>3</b><span>登记收款</span></div><i /><div><b>4</b><span>同步资金流水</span></div>
    </div>

    <div class="stat-grid">
      <div class="stat-card"><div class="label">应收总额（元）</div><div class="value">{{ fmtMoney(summary.total) }}</div></div>
      <div class="stat-card"><div class="label">已收（元）</div><div class="value green">{{ fmtMoney(summary.received) }}</div></div>
      <div class="stat-card"><div class="label">欠款（元）</div><div class="value red">{{ fmtMoney(summary.arrears) }}</div></div>
      <div class="stat-card"><div class="label">账单数</div><div class="value">{{ displayList.length }}</div></div>
    </div>

    <div class="filter-panel"><div class="toolbar">
      <el-select v-model="query.merchantId" placeholder="商家" clearable style="width: 200px" @change="load">
        <el-option v-for="m in lookup?.merchants || []" :key="m.id" :label="m.name" :value="m.id" />
      </el-select>
      <el-select v-model="query.contractId" placeholder="合同" clearable filterable style="width: 170px">
        <el-option v-for="c in lookup?.contracts || []" :key="c.id" :label="c.code" :value="c.id" />
      </el-select>
      <el-select v-model="query.status" placeholder="收款状态" clearable style="width: 120px" @change="load">
        <el-option v-for="s in ['未收', '部分缴纳', '已收']" :key="s" :label="s" :value="s" />
      </el-select>
      <el-select v-model="query.year" placeholder="年度" clearable style="width: 110px" @change="load">
        <el-option v-for="y in yearOptions" :key="y" :label="`${y} 年`" :value="y" />
      </el-select>
      <el-button :icon="Search" @click="load">查询</el-button>
      <span class="record-count">筛选结果 <strong>{{ displayList.length }}</strong> 条</span>
    </div></div>

    <el-table :data="displayList" v-loading="loading" border stripe>
      <el-table-column prop="merchantName" label="商家" min-width="160" />
      <el-table-column prop="contractCode" label="合同编号" width="125" />
      <el-table-column prop="billboardCode" label="广告牌" width="120" />
      <el-table-column prop="year" label="年度" width="80" align="center" />
      <el-table-column label="应收（元）" width="120" align="right">
        <template #default="{ row }"><span class="money">{{ fmtMoney(row.amount) }}</span></template>
      </el-table-column>
      <el-table-column label="已收（元）" width="120" align="right">
        <template #default="{ row }"><span class="money">{{ fmtMoney(row.received) }}</span></template>
      </el-table-column>
      <el-table-column label="欠款（元）" width="120" align="right">
        <template #default="{ row }">
          <span class="money" :class="{ arrears: row.amount - row.received > 0 }">{{ fmtMoney(row.amount - row.received) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/receivables/${row.id}`)">详情</el-button>
          <el-button v-if="canWrite && row.amount - row.received > 0" link type="success" @click="router.push(`/payments/new?receivableId=${row.id}`)">收款</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="payVisible" title="收款登记" width="440px">
      <el-descriptions :column="1" border size="small" class="pay-desc">
        <el-descriptions-item label="商家">{{ payRow?.merchantName }}</el-descriptions-item>
        <el-descriptions-item label="合同">{{ payRow?.contractCode }}</el-descriptions-item>
        <el-descriptions-item label="广告牌 / 年度">{{ payRow?.billboardCode }} / {{ payRow?.year }} 年</el-descriptions-item>
        <el-descriptions-item label="剩余应收">
          <span class="money arrears">{{ fmtMoney((payRow?.amount || 0) - (payRow?.received || 0)) }} 元</span>
        </el-descriptions-item>
      </el-descriptions>
      <el-form label-width="90px" class="pay-form">
        <el-form-item label="本次收款" required>
          <el-input-number v-model="payAmount" :min="0.01" :max="payRemain" :precision="2" style="width: 100%" :controls="false" />
        </el-form-item>
        <el-form-item>
          <el-checkbox @change="(v: boolean) => v && (payAmount = payRemain)">全额收款</el-checkbox>
          <div class="pay-tip">支持部分缴纳；收款后将同时冲减应收并计入资金流水</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="payVisible = false">取消</el-button>
        <el-button type="primary" :loading="paying" :disabled="!payAmount || payAmount <= 0" @click="onPay">确认收款</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Download, Search } from '@element-plus/icons-vue'
import { lookupApi, receivableApi } from '../api'
import { useUserStore } from '../stores/user'
import { fmtMoney, statusTagType } from '../utils/format'
import type { LookupData, Receivable } from '../types'

const store = useUserStore()
const router = useRouter()
const canWrite = computed(() => store.role === 'finance')

const list = ref<Receivable[]>([])
const lookup = ref<LookupData | null>(null)
const loading = ref(false)
const query = reactive<{ merchantId?: number; contractId?: number; status: string; year?: number }>({ status: '' })

const payVisible = ref(false)
const paying = ref(false)
const payRow = ref<Receivable | null>(null)
const payAmount = ref<number | undefined>(undefined)

const displayList = computed(() => query.contractId
  ? list.value.filter((row) => row.contractId === query.contractId)
  : list.value)

const summary = computed(() => {
  const total = displayList.value.reduce((s, r) => s + r.amount, 0)
  const received = displayList.value.reduce((s, r) => s + r.received, 0)
  return { total, received, arrears: total - received }
})

const payRemain = computed(() => (payRow.value ? payRow.value.amount - payRow.value.received : 0))

const yearOptions = computed(() => {
  const years = new Set<number>()
  list.value.forEach((r) => years.add(r.year))
  for (let y = 2024; y <= 2029; y++) {
    years.add(y)
  }
  return [...years].sort()
})

async function load() {
  loading.value = true
  try {
    list.value = await receivableApi.list({
      merchantId: query.merchantId,
      status: query.status || undefined,
      year: query.year
    })
  } finally {
    loading.value = false
  }
}

function openPay(row: Receivable) {
  payRow.value = row
  payAmount.value = row.amount - row.received
  payVisible.value = true
}

function exportCurrent() {
  const header = ['商家', '合同编号', '广告牌', '年度', '应收', '已收', '欠款', '状态']
  const rows = displayList.value.map((row) => [
    row.merchantName, row.contractCode, row.billboardCode, row.year, row.amount, row.received,
    row.amount - row.received, row.status
  ])
  const csv = '\ufeff' + [header, ...rows].map((line) => line.map((cell) => `"${String(cell ?? '').replace(/"/g, '""')}"`).join(',')).join('\r\n')
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }))
  const link = document.createElement('a')
  link.href = url
  link.download = '应收明细.csv'
  link.click()
  URL.revokeObjectURL(url)
}

async function onPay() {
  if (!payRow.value || !payAmount.value) {
    return
  }
  paying.value = true
  try {
    await receivableApi.pay(payRow.value.id, payAmount.value)
    ElMessage.success('收款成功，已冲减应收并计入流水')
    payVisible.value = false
    load()
  } catch {
    // 拦截器已提示
  } finally {
    paying.value = false
  }
}

onMounted(async () => {
  load()
  lookup.value = await lookupApi.all()
})
</script>

<style scoped>
.arrears {
  color: #f56c6c;
}

.pay-desc {
  margin-bottom: 16px;
}

.pay-form {
  margin-top: 8px;
}

.pay-tip {
  color: #909399;
  font-size: 12px;
  line-height: 1.5;
}

.receivable-flow {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 16px;
  padding: 11px 16px;
  border: 1px solid #dfe6ef;
  background: #f8fafc;
  color: #647185;
}

.receivable-flow > div {
  display: flex;
  align-items: center;
  gap: 7px;
}

.receivable-flow b {
  display: inline-flex;
  width: 22px;
  height: 22px;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #cbd4df;
  color: #fff;
  font-size: 12px;
}

.receivable-flow .current {
  color: #16488a;
  font-weight: 650;
}

.receivable-flow .current b {
  background: #16488a;
}

.receivable-flow i {
  width: 28px;
  height: 1px;
  background: #cbd4df;
}
</style>
