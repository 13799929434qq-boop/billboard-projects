<template>
  <div class="page-card">
    <div class="page-lead">
      <div>
        <h2>合同管理</h2>
        <p>合同按多块广告牌签订，生效时自动拆分年度应收。草稿确认无误后再执行生效。</p>
      </div>
      <div class="page-lead-actions">
        <span class="record-count">当前共 <strong>{{ list.length }}</strong> 份</span>
        <el-button :icon="Download" @click="exportCurrent">导出当前合同</el-button>
        <el-button v-if="canWrite" type="primary" :icon="Plus" @click="router.push('/contracts/new')">签订合同</el-button>
      </div>
    </div>
    <div class="contract-flow">
      <span>录入合同</span><i /> <span>确认广告牌与年租金</span><i /> <span>合同生效</span><i /> <span>生成年度应收</span>
    </div>
    <div class="filter-panel"><div class="toolbar">
      <el-input v-model="query.keyword" placeholder="搜索合同编号/备注" clearable style="width: 220px" :prefix-icon="Search" @change="load" />
      <el-select v-model="query.merchantId" placeholder="商家" clearable style="width: 200px" @change="load">
        <el-option v-for="m in lookup?.merchants || []" :key="m.id" :label="m.name" :value="m.id" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 110px" @change="load">
        <el-option v-for="s in ['草稿', '生效', '到期', '解除']" :key="s" :label="s" :value="s" />
      </el-select>
      <el-button :icon="Search" @click="load">查询</el-button>
    </div></div>

    <el-table :data="list" v-loading="loading" border stripe row-key="id">
      <el-table-column type="expand">
        <template #default="{ row }">
          <div class="items-panel">
            <div class="items-title">租赁广告牌明细（{{ row.items?.length || 0 }} 块牌，每牌每年单独定价）</div>
            <el-table :data="row.items || []" size="small" border>
              <el-table-column prop="billboardCode" label="广告牌编号" width="140" />
              <el-table-column prop="sectionName" label="所属路段" />
              <el-table-column label="每年租金（元）" width="150" align="right">
                <template #default="{ row: item }"><span class="money">{{ fmtMoney(item.yearlyPrice) }}</span></template>
              </el-table-column>
              <el-table-column label="合同期总额（元）" width="150" align="right">
                <template #default="{ row: item }">
                  <span class="money">{{ fmtMoney((item.yearlyPrice || 0) * row.years) }}</span>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="code" label="合同编号" width="125" />
      <el-table-column prop="merchantName" label="商家" min-width="160" />
      <el-table-column label="合同期限" width="200">
        <template #default="{ row }">{{ row.startDate }} ~ {{ row.endDate }}</template>
      </el-table-column>
      <el-table-column prop="years" label="年限" width="70" align="center">
        <template #default="{ row }">{{ row.years }} 年</template>
      </el-table-column>
      <el-table-column label="合同总额（元）" width="130" align="right">
        <template #default="{ row }"><span class="money">{{ fmtMoney(row.totalAmount) }}</span></template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusTagType(displayStatus(row))" size="small">{{ displayStatus(row) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="275" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/contracts/${row.id}`)">详情</el-button>
          <el-button v-if="canWrite && row.status === '草稿'" link type="success" @click="activate(row)">生效</el-button>
          <el-button v-if="canWrite && row.status === '草稿'" link type="primary" @click="router.push(`/contracts/${row.id}/edit`)">编辑</el-button>
          <el-button v-if="canWrite && row.status === '生效'" link type="warning" @click="terminate(row)">解除</el-button>
          <el-button v-if="canWrite && row.status === '草稿'" link type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? `编辑合同 ${form.code}` : '签订合同'" width="780px" top="4vh">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="商家" prop="merchantId">
              <el-select v-model="form.merchantId" style="width: 100%" :disabled="!!form.id">
                <el-option v-for="m in lookup?.merchants || []" :key="m.id" :value="m.id"
                  :label="m.name + (m.blacklisted ? '（黑名单）' : '')" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="签订日期">
              <el-date-picker v-model="form.signDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="起始日期" prop="startDate">
              <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" :disabled="!!form.id" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="到期日期" prop="endDate">
              <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" :disabled="!!form.id" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="租赁广告牌" prop="items">
          <div class="items-editor">
            <div v-for="(item, idx) in form.items" :key="idx" class="item-row">
              <el-select v-model="item.billboardId" placeholder="选择广告牌" style="width: 300px" :disabled="!!form.id">
                <el-option v-for="b in lookup?.billboards || []" :key="b.id" :value="b.id"
                  :label="`${b.code}（${b.sectionName || ''} ${b.stakeActual || ''}）`"
                  :disabled="!!form.items?.some((it, i) => i !== idx && it.billboardId === b.id)" />
              </el-select>
              <el-input-number v-model="item.yearlyPrice" :min="0" :step="1000" placeholder="每年租金"
                style="width: 180px" :controls="false" />
              <span class="unit">元/年</span>
              <el-button v-if="!form.id" link type="danger" :icon="Delete" @click="form.items?.splice(idx, 1)" />
            </div>
            <el-button v-if="!form.id" link type="primary" :icon="Plus"
              @click="form.items?.push({ billboardId: null, yearlyPrice: null })">
              添加广告牌
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="合同总额">
          <span class="total-amount">{{ fmtMoney(computedTotal) }} 元（{{ computedYears }} 年）</span>
          <span class="total-tip">按"每年每牌价格 × 年数"自动汇总，应收将按年拆分</span>
        </el-form-item>
        <el-form-item v-if="!form.id" label="签订后状态">
          <el-radio-group v-model="form.status">
            <el-radio value="草稿">存为草稿</el-radio>
            <el-radio value="生效">直接生效（立即生成应收账单）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="合同执行详情" size="760px">
      <template v-if="detailRow">
        <div class="contract-detail-head">
          <div><div class="contract-detail-code">{{ detailRow.code }}</div><div class="contract-detail-merchant">{{ detailRow.merchantName }}</div></div>
          <el-tag :type="statusTagType(displayStatus(detailRow))" size="large">{{ displayStatus(detailRow) }}</el-tag>
        </div>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="签订日期">{{ detailRow.signDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="起租日期">{{ detailRow.startDate }}</el-descriptions-item>
          <el-descriptions-item label="到期日期">{{ detailRow.endDate }}</el-descriptions-item>
          <el-descriptions-item label="合同年限">{{ detailRow.years }} 年</el-descriptions-item>
          <el-descriptions-item label="广告牌数">{{ detailRow.items?.length || 0 }} 块</el-descriptions-item>
          <el-descriptions-item label="合同总额"><span class="money">{{ fmtMoney(detailRow.totalAmount) }} 元</span></el-descriptions-item>
        </el-descriptions>
        <div class="drawer-title">广告牌与年度租金</div>
        <el-table :data="detailRow.items || []" border size="small">
          <el-table-column prop="billboardCode" label="广告牌编号" width="140" />
          <el-table-column prop="sectionName" label="所属路段" />
          <el-table-column label="年租金" width="135" align="right"><template #default="{ row }"><span class="money">{{ fmtMoney(row.yearlyPrice) }}</span></template></el-table-column>
          <el-table-column label="合同期金额" width="145" align="right"><template #default="{ row }"><span class="money">{{ fmtMoney((row.yearlyPrice || 0) * detailRow!.years) }}</span></template></el-table-column>
        </el-table>
        <div class="drawer-title">年度应收计划</div>
        <el-table :data="detailSchedule" border size="small">
          <el-table-column prop="year" label="年度" width="100" />
          <el-table-column prop="billboardCount" label="广告牌数" width="110" />
          <el-table-column label="年度应收" width="150" align="right"><template #default="{ row }"><span class="money">{{ fmtMoney(row.amount) }}</span></template></el-table-column>
          <el-table-column label="说明"><template #default>合同生效后按“年度 × 广告牌”生成账单</template></el-table-column>
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
import { Delete, Download, Plus, Search } from '@element-plus/icons-vue'
import { contractApi, lookupApi } from '../api'
import { useUserStore } from '../stores/user'
import { exportCsv, fmtMoney, statusTagType } from '../utils/format'
import type { Contract, LookupData } from '../types'

const store = useUserStore()
const router = useRouter()
const canWrite = computed(() => store.role === 'finance')

const list = ref<Contract[]>([])
const lookup = ref<LookupData | null>(null)
const loading = ref(false)
const query = reactive<{ keyword: string; status: string; merchantId: number | undefined }>({
  keyword: '',
  status: '',
  merchantId: undefined
})

const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<Partial<Contract>>({ items: [] })
const detailVisible = ref(false)
const detailRow = ref<Contract | null>(null)
const detailSchedule = computed(() => {
  if (!detailRow.value?.startDate) return []
  const start = new Date(detailRow.value.startDate).getFullYear()
  const annualAmount = (detailRow.value.items || []).reduce((sum, item) => sum + (item.yearlyPrice || 0), 0)
  return Array.from({ length: detailRow.value.years }, (_, index) => ({
    year: start + index,
    billboardCount: detailRow.value?.items?.length || 0,
    amount: annualAmount
  }))
})

const formRules: FormRules = {
  merchantId: [{ required: true, message: '请选择商家', trigger: 'change' }],
  startDate: [{ required: true, message: '请选择起始日期', trigger: 'change' }],
  endDate: [{ required: true, message: '请选择到期日期', trigger: 'change' }],
  items: [
    {
      validator: (_rule, value: Contract['items'], cb) => {
        if (!value || value.length === 0) {
          cb(new Error('至少绑定一块广告牌'))
        } else if (value.some((it) => !it.billboardId)) {
          cb(new Error('存在未选择广告牌的明细行'))
        } else if (value.some((it) => !it.yearlyPrice || it.yearlyPrice <= 0)) {
          cb(new Error('每块牌的每年租金必须大于 0'))
        } else {
          cb()
        }
      },
      trigger: 'change'
    }
  ]
}

const computedYears = computed(() => {
  if (!form.startDate || !form.endDate) {
    return 0
  }
  return new Date(form.endDate).getFullYear() - new Date(form.startDate).getFullYear() + 1
})

const computedTotal = computed(
  () => (form.items || []).reduce((sum, it) => sum + (it.yearlyPrice || 0), 0) * computedYears.value
)

function displayStatus(row: Contract): string {
  if (row.status === '生效' && row.endDate && new Date(row.endDate).getTime() < Date.now()) {
    return '到期'
  }
  return row.status
}

async function load() {
  loading.value = true
  try {
    list.value = await contractApi.list({
      keyword: query.keyword || undefined,
      status: query.status || undefined,
      merchantId: query.merchantId
    })
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.keys(form).forEach((k) => delete (form as Record<string, unknown>)[k])
  Object.assign(form, {
    merchantId: null, signDate: new Date().toISOString().slice(0, 10),
    startDate: '', endDate: '', status: '草稿', remark: '',
    items: [{ billboardId: null, yearlyPrice: null }]
  })
  dialogVisible.value = true
}

function openEdit(row: Contract) {
  Object.keys(form).forEach((k) => delete (form as Record<string, unknown>)[k])
  Object.assign(form, JSON.parse(JSON.stringify(row)))
  dialogVisible.value = true
}

function openDetail(row: Contract) {
  detailRow.value = row
  detailVisible.value = true
}

function exportCurrent() {
  exportCsv('合同档案',
    ['合同编号', '商家', '签订日期', '起始日期', '到期日期', '年限', '广告牌数量', '合同总额', '状态', '广告牌及年租金'],
    list.value.map((row) => [row.code, row.merchantName, row.signDate, row.startDate, row.endDate, row.years,
      row.items?.length || 0, row.totalAmount, displayStatus(row),
      (row.items || []).map((item) => `${item.billboardCode || item.billboardId}：${fmtMoney(item.yearlyPrice)}元/年`).join('；')]))
}

async function onSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (form.id) {
      await contractApi.update(form.id, form)
    } else {
      await contractApi.create(form)
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

async function activate(row: Contract) {
  await ElMessageBox.confirm(
    `合同「${row.code}」生效后将按"年 × 牌"生成应收账单，且校验广告牌时间重叠，确定生效吗？`,
    '合同生效',
    { type: 'warning' }
  )
  await contractApi.update(row.id, { status: '生效' })
  ElMessage.success('合同已生效，应收账单已生成')
  load()
}

async function terminate(row: Contract) {
  await ElMessageBox.confirm(
    `解除合同「${row.code}」后，其广告牌将置为空闲，已产生的应收与收款记录保留，确定解除吗？`,
    '解除合同',
    { type: 'warning', confirmButtonText: '确定解除', confirmButtonClass: 'el-button--danger' }
  )
  await contractApi.update(row.id, { status: '解除' })
  ElMessage.success('合同已解除')
  load()
}

async function onDelete(row: Contract) {
  await ElMessageBox.confirm(`确定删除草稿合同「${row.code}」吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '确定删除',
    confirmButtonClass: 'el-button--danger'
  })
  await contractApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(async () => {
  load()
  lookup.value = await lookupApi.all()
})
</script>

<style scoped>
.items-panel {
  padding: 8px 24px;
  background: #fafbfc;
}

.items-title {
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
}

.items-editor {
  width: 100%;
}

.item-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.unit {
  color: #909399;
  font-size: 13px;
}

.total-amount {
  font-size: 16px;
  font-weight: 700;
  color: #16488a;
}

.total-tip {
  margin-left: 12px;
  color: #909399;
  font-size: 12px;
}

.contract-flow {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
  padding: 10px 14px;
  border-left: 3px solid #d9a514;
  background: #f7f9fc;
  color: #445269;
  font-size: 13px;
}

.contract-flow i {
  width: 22px;
  height: 1px;
  background: #aab5c4;
}

.contract-detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.contract-detail-code {
  color: #0f2d52;
  font-size: 22px;
  font-weight: 700;
}

.contract-detail-merchant {
  margin-top: 4px;
  color: #697586;
}

.drawer-title {
  margin: 20px 0 10px;
  color: #243247;
  font-weight: 650;
}
</style>
