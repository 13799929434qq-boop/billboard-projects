<template>
  <div class="page-card">
    <div class="page-lead report-lead">
      <div>
        <h2>{{ (route.meta.title as string) || '经营报表' }}</h2>
        <p>全部指标从业务台账实时汇总。筛选后的查询条件会同步用于导出，确保页面与文件口径一致。</p>
      </div>
      <div class="report-stamp"><span>数据口径</span><strong>实时业务数据</strong></div>
    </div>

    <!-- 广告牌数量统计 -->
    <div v-if="report === 'billboard'">
      <div class="toolbar">
        <el-input v-model="bbKeyword" placeholder="按承租商家筛选" clearable style="width: 220px" @change="loadBillboard" />
        <el-button :icon="Search" @click="loadBillboard">筛选</el-button>
        <el-button type="success" plain :icon="Download" @click="doExport('billboard', '广告牌数量统计.xlsx')">导出 Excel</el-button>
      </div>
      <div class="stat-grid">
        <div class="stat-card"><div class="label">广告牌总数</div><div class="value">{{ billboardStats?.total ?? '-' }}</div></div>
        <div class="stat-card"><div class="label">在租</div><div class="value green">{{ countOf(billboardStats?.byStatus, '在租') }}</div></div>
        <div class="stat-card"><div class="label">空闲</div><div class="value">{{ countOf(billboardStats?.byStatus, '空闲') }}</div></div>
        <div class="stat-card"><div class="label">维护/停用</div><div class="value orange">{{ countOf(billboardStats?.byStatus, '维护中') + countOf(billboardStats?.byStatus, '停用') }}</div></div>
      </div>
      <el-row :gutter="16">
        <el-col :span="9"><div ref="statusChartRef" class="chart" /></el-col>
        <el-col :span="15">
          <el-table :data="(billboardStats?.bySection as ReportRow[]) || []" size="small" border>
            <el-table-column prop="name" label="路段" />
            <el-table-column prop="count" label="广告牌数量" width="120" align="center" />
          </el-table>
          <div class="group-tables">
            <el-table :data="(billboardStats?.byOwnership as ReportRow[]) || []" size="small" border class="group-table">
              <el-table-column prop="name" label="按产权" />
              <el-table-column prop="count" label="数量" width="90" align="center" />
            </el-table>
            <el-table :data="(billboardStats?.byLandNature as ReportRow[]) || []" size="small" border class="group-table">
              <el-table-column prop="name" label="按用地性质" />
              <el-table-column prop="count" label="数量" width="90" align="center" />
            </el-table>
          </div>
        </el-col>
      </el-row>
      <el-divider />
      <div class="table-title">广告牌明细（含承租商家）</div>
      <el-table :data="(billboardStats?.list as ReportRow[]) || []" size="small" border stripe max-height="420">
        <el-table-column prop="code" label="编号" width="115" />
        <el-table-column prop="sectionName" label="路段" width="150" />
        <el-table-column prop="side" label="侧别" width="70" />
        <el-table-column prop="stake" label="实际桩号" width="110" />
        <el-table-column prop="spec" label="规格" width="100" />
        <el-table-column prop="size" label="尺寸" width="100" />
        <el-table-column prop="ownership" label="产权" width="100" />
        <el-table-column prop="landNature" label="用地性质" width="100" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }"><el-tag size="small" :type="statusTagType(String(row.status))">{{ row.status }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="renterName" label="承租商家" min-width="140" sortable />
      </el-table>
    </div>

    <!-- 合同执行情况 -->
    <div v-else-if="report === 'execution'">
      <div class="toolbar">
        <el-button type="success" plain :icon="Download" @click="doExport('execution', '合同执行情况.xlsx')">导出合同明细</el-button>
        <el-button type="success" plain :icon="Download" @click="doExport('occupancy', '广告牌占用情况表.xlsx')">导出广告牌占用</el-button>
      </div>

      <div v-if="expiringContracts.length" class="warn-banner">
        <el-icon class="warn-icon"><WarningFilled /></el-icon>
        <div class="warn-body">
          <strong>{{ expiringContracts.length }} 份生效合同 90 天内到期，请尽快跟进续签/收款</strong>
          <span class="warn-detail">{{ expiringContracts.map((c) => `${c.merchantName}（${c.code}，剩 ${c.daysRemaining} 天）`).join('；') }}</span>
        </div>
      </div>

      <div class="table-title">广告牌占用情况（每块牌在租/空闲/维护）</div>
      <el-table :data="occupancyList" border stripe v-loading="loading">
        <el-table-column prop="code" label="广告牌编号" width="120" />
        <el-table-column prop="sectionName" label="所属路段" min-width="140" />
        <el-table-column label="当前状态" width="100">
          <template #default="{ row }"><el-tag size="small" :type="statusTagType(String(row.status))">{{ row.status }}</el-tag></template>
        </el-table-column>
        <el-table-column label="生效合同" width="150">
          <template #default="{ row }"><span v-if="row.contractCode">{{ row.contractCode }}</span><span v-else class="empty-text">-</span></template>
        </el-table-column>
        <el-table-column label="承租商家" min-width="140">
          <template #default="{ row }"><span v-if="row.merchantName">{{ row.merchantName }}</span><span v-else class="empty-text">-</span></template>
        </el-table-column>
        <el-table-column label="合同到期" width="120">
          <template #default="{ row }"><span v-if="row.endDate">{{ row.endDate }}</span><span v-else class="empty-text">-</span></template>
        </el-table-column>
        <el-table-column label="剩余天数" width="115">
          <template #default="{ row }">
            <el-tag v-if="row.expiringSoon" size="small" type="danger">{{ row.daysRemaining }} 天 · 临期</el-tag>
            <span v-else-if="row.daysRemaining != null">{{ row.daysRemaining }} 天</span>
            <span v-else class="empty-text">-</span>
          </template>
        </el-table-column>
        <el-table-column label="收款进度" width="170">
          <template #default="{ row }">
            <el-progress v-if="row.progress != null" :percentage="Number(row.progress)" :color="Number(row.progress) >= 100 ? '#67c23a' : '#16488a'" />
            <span v-else class="empty-text">-</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-title">合同执行明细（按合同）</div>
      <el-table :data="executionList" border stripe v-loading="loading">
        <el-table-column prop="code" label="合同编号" width="125" />
        <el-table-column prop="merchantName" label="商家" min-width="160" />
        <el-table-column label="期限" width="200">
          <template #default="{ row }">{{ row.startDate }} ~ {{ row.endDate }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="85">
          <template #default="{ row }"><el-tag size="small" :type="statusTagType(String(row.status))">{{ row.status }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="billboardCount" label="牌数" width="70" align="center" />
        <el-table-column label="合同总额（元）" width="125" align="right">
          <template #default="{ row }"><span class="money">{{ fmtMoney(row.totalAmount as number) }}</span></template>
        </el-table-column>
        <el-table-column label="收款进度" width="180">
          <template #default="{ row }">
            <el-progress :percentage="Number(row.progress)" :color="Number(row.progress) >= 100 ? '#67c23a' : '#16488a'" />
          </template>
        </el-table-column>
        <el-table-column label="剩余天数" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.status === '生效'" size="small" :type="Number(row.daysRemaining) <= 90 ? 'danger' : 'info'">
              {{ row.daysRemaining }} 天
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 收款情况统计 -->
    <div v-else-if="report === 'collection'">
      <div class="toolbar">
        <el-date-picker v-model="collectionRange" type="daterange" value-format="YYYY-MM-DD" range-separator="至"
          start-placeholder="开始日期" end-placeholder="结束日期" @change="loadCollection" />
        <el-button :icon="Search" @click="loadCollection">查询</el-button>
        <el-button type="success" plain :icon="Download" @click="doExport('collection', '收款情况统计.xlsx')">导出 Excel</el-button>
      </div>
      <div class="stat-grid">
        <div class="stat-card"><div class="label">收款笔数</div><div class="value">{{ collection?.count ?? '-' }}</div></div>
        <div class="stat-card"><div class="label">收款总额（元）</div><div class="value green">{{ fmtMoney(collection?.amount as number) }}</div></div>
      </div>
      <el-row :gutter="16">
        <el-col :span="10"><div ref="collectionChartRef" class="chart tall" /></el-col>
        <el-col :span="14">
          <div class="table-title">按商家分布</div>
          <el-table :data="(collection?.byMerchant as ReportRow[]) || []" size="small" border>
            <el-table-column prop="name" label="商家" />
            <el-table-column prop="count" label="笔数" width="80" align="center" />
            <el-table-column label="金额（元）" width="130" align="right">
              <template #default="{ row }"><span class="money">{{ fmtMoney(row.amount as number) }}</span></template>
            </el-table-column>
          </el-table>
          <div class="table-title">按款项类别分布</div>
          <el-table :data="(collection?.byCategory as ReportRow[]) || []" size="small" border>
            <el-table-column prop="name" label="类别" />
            <el-table-column prop="count" label="笔数" width="80" align="center" />
            <el-table-column label="金额（元）" width="130" align="right">
              <template #default="{ row }"><span class="money">{{ fmtMoney(row.amount as number) }}</span></template>
            </el-table-column>
          </el-table>
          <div class="table-title">按合同分布</div>
          <el-table :data="(collection?.byContract as ReportRow[]) || []" size="small" border>
            <el-table-column prop="name" label="合同" />
            <el-table-column prop="count" label="笔数" width="80" align="center" />
            <el-table-column label="金额（元）" width="130" align="right">
              <template #default="{ row }"><span class="money">{{ fmtMoney(row.amount as number) }}</span></template>
            </el-table-column>
          </el-table>
        </el-col>
      </el-row>
    </div>

    <!-- 应收款及欠款统计 -->
    <div v-else-if="report === 'receivable'">
      <div class="toolbar">
        <el-button type="success" plain :icon="Download" @click="doExport('receivable', '应收款催收清单.xlsx')">导出催收清单</el-button>
      </div>
      <div class="stat-grid">
        <div class="stat-card"><div class="label">应收总额（元）</div><div class="value">{{ fmtMoney(receivableStats?.totalAmount as number) }}</div></div>
        <div class="stat-card"><div class="label">已收（元）</div><div class="value green">{{ fmtMoney(receivableStats?.totalReceived as number) }}</div></div>
        <div class="stat-card"><div class="label">欠款（元）</div><div class="value red">{{ fmtMoney(receivableStats?.totalArrears as number) }}</div></div>
      </div>
      <div class="table-title">按年度欠款分布</div>
      <el-table :data="arrearsByYear" border stripe size="small">
        <el-table-column prop="year" label="欠款年度" width="160" />
        <el-table-column label="应收（元）" width="160" align="right">
          <template #default="{ row }"><span class="money">{{ fmtMoney(row.amount) }}</span></template>
        </el-table-column>
        <el-table-column label="欠款（元）" min-width="160" align="right">
          <template #default="{ row }"><span class="money" style="color: #f56c6c">{{ fmtMoney(row.arrears) }}</span></template>
        </el-table-column>
      </el-table>
      <div class="table-title">按商家汇总（欠款从高到低）</div>
      <el-table :data="(receivableStats?.byMerchant as ReportRow[]) || []" border stripe>
        <el-table-column prop="name" label="商家" min-width="170" />
        <el-table-column label="应收（元）" width="140" align="right">
          <template #default="{ row }"><span class="money">{{ fmtMoney(row.totalAmount as number) }}</span></template>
        </el-table-column>
        <el-table-column label="已收（元）" width="140" align="right">
          <template #default="{ row }"><span class="money">{{ fmtMoney(row.receivedAmount as number) }}</span></template>
        </el-table-column>
        <el-table-column label="欠款（元）" width="140" align="right">
          <template #default="{ row }">
            <span class="money" :style="{ color: Number(row.arrears) > 0 ? '#f56c6c' : 'inherit' }">{{ fmtMoney(row.arrears as number) }}</span>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-title">催收清单（全部未收/部分缴纳明细）</div>
      <el-table :data="(receivableStats?.noticeList as ReportRow[]) || []" border stripe max-height="420">
        <el-table-column prop="merchantName" label="商家" min-width="150" />
        <el-table-column label="联系方式" width="150">
          <template #default="{ row }">{{ row.contact }} {{ row.phone }}</template>
        </el-table-column>
        <el-table-column prop="contractCode" label="合同" width="120" />
        <el-table-column prop="billboardCode" label="广告牌" width="115" />
        <el-table-column prop="year" label="年度" width="75" align="center" />
        <el-table-column label="应收（元）" width="115" align="right">
          <template #default="{ row }"><span class="money">{{ fmtMoney(row.amount as number) }}</span></template>
        </el-table-column>
        <el-table-column label="欠款（元）" width="115" align="right">
          <template #default="{ row }"><span class="money" style="color: #f56c6c">{{ fmtMoney(row.arrears as number) }}</span></template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Download, Search, WarningFilled } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { downloadFile, reportApi } from '../api'
import { fmtMoney, statusTagType } from '../utils/format'
import type { ReportRow } from '../types'

const route = useRoute()
const report = computed(() => (route.meta.report as string) || 'billboard')
const loading = ref(false)

const billboardStats = ref<Record<string, unknown> | null>(null)
const executionList = ref<ReportRow[]>([])
const occupancyList = ref<Record<string, unknown>[]>([])
const collection = ref<Record<string, unknown> | null>(null)
const receivableStats = ref<Record<string, unknown> | null>(null)

const bbKeyword = ref('')
const collectionRange = ref<[string, string] | null>(null)

const statusChartRef = ref<HTMLDivElement>()
const collectionChartRef = ref<HTMLDivElement>()

const expiringContracts = computed(() => executionList.value.filter((c) => c.expiringSoon))
const arrearsByYear = computed(() => {
  const rows = (receivableStats.value?.noticeList as Record<string, unknown>[]) || []
  const map = new Map<string, { year: string; amount: number; arrears: number }>()
  for (const r of rows) {
    const y = String(r.year ?? '未知')
    const cur = map.get(y) || { year: y, amount: 0, arrears: 0 }
    cur.amount += Number(r.amount) || 0
    cur.arrears += Number(r.arrears) || 0
    map.set(y, cur)
  }
  return [...map.values()].sort((a, b) => a.year.localeCompare(b.year))
})

function countOf(list: unknown, name: string): number {
  const arr = (list as ReportRow[]) || []
  const found = arr.find((r) => r.name === name)
  return found ? Number(found.count) : 0
}

async function loadBillboard() {
  billboardStats.value = await reportApi.billboardStats(bbKeyword.value || undefined)
  await nextTick()
  renderStatusChart()
}

async function loadExecution() {
  loading.value = true
  try {
    const [rows, occ] = await Promise.all([reportApi.contractExecution(), reportApi.occupancy()])
    executionList.value = rows
    occupancyList.value = occ
  } finally {
    loading.value = false
  }
}

async function loadCollection() {
  collection.value = await reportApi.collection(collectionRange.value?.[0], collectionRange.value?.[1])
  await nextTick()
  renderCollectionChart()
}

async function loadReceivable() {
  receivableStats.value = await reportApi.receivableStats()
}

function renderStatusChart() {
  if (!statusChartRef.value || !billboardStats.value) {
    return
  }
  const chart = echarts.getInstanceByDom(statusChartRef.value) || echarts.init(statusChartRef.value)
  chart.setOption({
    title: { text: '广告牌状态分布', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie',
      radius: ['40%', '68%'],
      data: ((billboardStats.value.byStatus as ReportRow[]) || []).map((r) => ({ name: r.name, value: r.count })),
      label: { formatter: '{b}: {c} 块' }
    }],
    color: ['#67c23a', '#909399', '#e6a23c', '#f56c6c']
  })
}

function renderCollectionChart() {
  if (!collectionChartRef.value || !collection.value) {
    return
  }
  const rows = (collection.value.byMerchant as ReportRow[]) || []
  const chart = echarts.getInstanceByDom(collectionChartRef.value) || echarts.init(collectionChartRef.value)
  chart.setOption({
    title: { text: '各商家收款金额', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: {},
    grid: { left: 10, right: 20, bottom: 10, top: 50, containLabel: true },
    xAxis: { type: 'value', axisLabel: { formatter: (v: number) => `${v / 10000}万` } },
    yAxis: { type: 'category', data: rows.map((r) => r.name).reverse(), axisLabel: { width: 130, overflow: 'truncate' } },
    series: [{ type: 'bar', data: rows.map((r) => r.amount).reverse(), itemStyle: { color: '#16488a' }, barMaxWidth: 22 }]
  })
}

async function doExport(type: string, filename: string) {
  try {
    const params = new URLSearchParams({ type })
    if (type === 'billboard' && bbKeyword.value) params.set('keyword', bbKeyword.value)
    if (type === 'collection' && collectionRange.value?.[0]) params.set('from', collectionRange.value[0])
    if (type === 'collection' && collectionRange.value?.[1]) params.set('to', collectionRange.value[1])
    await downloadFile(`/reports/export?${params.toString()}`, filename)
    ElMessage.success('已导出')
  } catch {
    ElMessage.error('导出失败')
  }
}

onMounted(async () => {
  const r = report.value
  if (r === 'billboard') {
    await loadBillboard()
  } else if (r === 'execution') {
    await loadExecution()
  } else if (r === 'collection') {
    await loadCollection()
  } else if (r === 'receivable') {
    await loadReceivable()
  }
})
</script>

<style scoped>
.warn-banner {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 16px;
  margin-bottom: 14px;
  background: #fdf3e7;
  border: 1px solid #f5d6a4;
  border-left: 4px solid #e6a23c;
  border-radius: 4px;
  color: #8a5a00;
}

.warn-icon {
  font-size: 20px;
  color: #e6a23c;
  margin-top: 2px;
}

.warn-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.warn-detail {
  font-size: 12px;
  color: #a8791e;
  line-height: 1.6;
}

.chart {
  height: 260px;
}

.chart.tall {
  height: 420px;
}

.table-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin: 16px 0 8px;
}

.group-tables {
  display: flex;
  gap: 12px;
  margin-top: 12px;
}

.group-table {
  flex: 1;
}

.report-stamp {
  min-width: 150px;
  padding: 9px 12px;
  border: 1px solid #cbd7e6;
  border-left: 3px solid #d9a514;
  background: #fff;
}

.report-stamp span {
  display: block;
  color: #7b8797;
  font-size: 11px;
}

.report-stamp strong {
  display: block;
  margin-top: 3px;
  color: #16488a;
  font-size: 14px;
}
</style>
