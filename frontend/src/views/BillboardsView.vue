<template>
  <div class="page-card">
    <div class="page-lead">
      <div>
        <h2>广告牌档案</h2>
        <p>集中维护位置、三桩号、规格和合规证件。签合同前请先确认广告牌状态与证件有效期。</p>
      </div>
      <div class="page-lead-actions">
        <span class="record-count">当前共 <strong>{{ list.length }}</strong> 块</span>
        <el-button :icon="Download" @click="exportCurrent">导出当前档案</el-button>
        <el-button v-if="canWrite" @click="router.push('/billboard-changes')">变动登记</el-button>
        <el-button v-if="canWrite" type="primary" :icon="Plus" @click="router.push('/billboards/new')">新增广告牌</el-button>
      </div>
    </div>
    <div class="filter-panel"><div class="toolbar">
      <el-input v-model="query.keyword" placeholder="搜索编号/桩号" clearable style="width: 200px" :prefix-icon="Search" @change="load" />
      <el-select v-model="query.sectionId" placeholder="所属路段" clearable style="width: 180px" @change="load">
        <el-option v-for="s in lookup?.sections || []" :key="s.id" :label="s.name" :value="s.id" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 110px" @change="load">
        <el-option v-for="s in ['在租', '空闲', '维护中', '停用']" :key="s" :label="s" :value="s" />
      </el-select>
      <el-select v-model="query.ownership" placeholder="产权" clearable style="width: 130px" @change="load">
        <el-option v-for="o in dictOptions('ownership')" :key="o" :label="o" :value="o" />
      </el-select>
      <el-select v-model="query.landNature" placeholder="用地性质" clearable style="width: 130px" @change="load">
        <el-option v-for="o in dictOptions('landNature')" :key="o" :label="o" :value="o" />
      </el-select>
      <el-button :icon="Search" @click="load">查询</el-button>
    </div></div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="code" label="编号" width="115" fixed />
      <el-table-column prop="sectionName" label="所属路段" width="140" />
      <el-table-column label="三个桩号" width="200">
        <template #default="{ row }">
          <div class="stake">合同：{{ row.stakeContract }}</div>
          <div class="stake">实际：{{ row.stakeActual }}</div>
          <div class="stake">路政：{{ row.stakeApproval }}</div>
        </template>
      </el-table-column>
      <el-table-column label="规格/尺寸" width="130">
        <template #default="{ row }">{{ row.spec }} {{ row.width }}×{{ row.height }}米</template>
      </el-table-column>
      <el-table-column prop="ownership" label="产权" width="100" />
      <el-table-column prop="landNature" label="用地性质" width="100" />
      <el-table-column label="合规证件" width="170">
        <template #default="{ row }">
          <div v-for="c in complianceOf(row)" :key="c.type" class="compliance">
            <el-tag :type="c.tag" size="small" effect="plain">{{ c.type }} {{ c.expire || '未录入' }}</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="承租方" min-width="130">
        <template #default="{ row }">
          <span v-if="row.renterName">{{ row.renterName }}</span>
          <span v-else class="empty-text">-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="View" @click="router.push(`/billboards/${row.id}`)">详情</el-button>
          <el-button v-if="canWrite" link type="primary" @click="router.push(`/billboards/${row.id}/edit`)">编辑</el-button>
          <el-button v-if="canWrite" link type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑广告牌' : '新增广告牌'" width="720px" top="4vh">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <div class="form-section-title"><span>基本信息</span><small>广告牌编号与所属位置</small></div>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="编号" prop="code"><el-input v-model="form.code" placeholder="如 GG-JH-013" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属路段" prop="sectionId">
              <el-select v-model="form.sectionId" style="width: 100%">
                <el-option v-for="s in lookup?.sections || []" :key="s.id" :label="s.name" :value="s.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <div class="form-section-title"><span>三个桩号</span><small>合同、现场与路政批复桩号分别记录</small></div>
        <el-row :gutter="12">
          <el-col :span="8"><el-form-item label="合同桩号" prop="stakeContract"><el-input v-model="form.stakeContract" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="实际桩号" prop="stakeActual"><el-input v-model="form.stakeActual" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="路政桩号" prop="stakeApproval"><el-input v-model="form.stakeApproval" /></el-form-item></el-col>
        </el-row>
        <div class="form-section-title"><span>产权与建设</span><small>用地性质会影响合同签订前置手续</small></div>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="侧别">
              <el-select v-model="form.side" style="width: 100%">
                <el-option v-for="s in ['左侧', '右侧', '中央分隔带']" :key="s" :label="s" :value="s" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="规格">
              <el-select v-model="form.spec" style="width: 100%">
                <el-option v-for="o in dictOptions('spec')" :key="o" :label="o" :value="o" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="4"><el-form-item label="宽(米)" label-width="60px"><el-input-number v-model="form.width" :min="1" :max="100" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="4"><el-form-item label="高(米)" label-width="60px"><el-input-number v-model="form.height" :min="1" :max="50" style="width: 100%" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="产权">
              <el-select v-model="form.ownership" style="width: 100%">
                <el-option v-for="o in dictOptions('ownership')" :key="o" :label="o" :value="o" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="用地性质" prop="landNature">
              <el-select v-model="form.landNature" style="width: 100%" @change="onLandNatureChange">
                <el-option v-for="o in dictOptions('landNature')" :key="o" :label="o" :value="o" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option v-for="s in ['在租', '空闲', '维护中', '停用']" :key="s" :label="s" :value="s" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-alert v-if="landTip" :title="landTip" type="warning" :closable="false" show-icon class="land-tip" />
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="建设单位"><el-input v-model="form.builder" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="建成年份"><el-input-number v-model="form.buildYear" :min="1990" :max="2100" style="width: 100%" /></el-form-item></el-col>
        </el-row>
        <div class="form-section-title"><span>合规手续</span><small>路政许可、保险和年审是经营红线</small></div>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="路政审批号"><el-input v-model="form.permitNo" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="审批有效期">
              <el-date-picker v-model="form.permitExpire" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="保险单号"><el-input v-model="form.insuranceNo" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="保险有效期">
              <el-date-picker v-model="form.insuranceExpire" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="8"><el-form-item label="年审编号"><el-input v-model="form.annualCheckNo" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="最近年审"><el-date-picker v-model="form.annualCheckDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="年审有效期"><el-date-picker v-model="form.annualCheckExpire" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item></el-col>
        </el-row>
        <div class="form-section-title"><span>现场资料</span><small>建议保留多角度照片，便于档案核验</small></div>
        <el-form-item label="照片">
          <div>
            <el-upload :show-file-list="false" :http-request="doUpload" accept="image/*">
              <el-button :icon="Upload" :loading="uploading">上传照片</el-button>
            </el-upload>
            <div v-if="photoList.length" class="photo-list">
              <div v-for="(p, index) in photoList" :key="p" class="photo-wrap">
                <el-image :src="p" fit="cover" class="photo-item" :preview-src-list="photoList" preview-teleported />
                <button type="button" class="photo-remove" aria-label="删除照片" @click="photoList.splice(index, 1)">×</button>
              </div>
            </div>
            <div class="photo-tip">至少上传 2 张，建议包含正面、侧面、桩号和证件现场照片。</div>
          </div>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="广告牌档案详情" size="620px">
      <template v-if="detailRow">
        <div class="detail-summary">
          <div class="summary-item"><div class="summary-label">广告牌编号</div><div class="summary-value code-value">{{ detailRow.code }}</div></div>
          <div class="summary-item"><div class="summary-label">当前状态</div><div class="summary-value"><el-tag :type="statusTagType(detailRow.status)">{{ detailRow.status }}</el-tag></div></div>
          <div class="summary-item"><div class="summary-label">当前承租方</div><div class="summary-value renter-value">{{ detailRow.renterName || '暂无' }}</div></div>
        </div>
        <div class="drawer-section-title">位置与规格</div>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="所属路段">{{ detailRow.sectionName }}</el-descriptions-item>
          <el-descriptions-item label="侧别">{{ detailRow.side || '-' }}</el-descriptions-item>
          <el-descriptions-item label="合同桩号">{{ detailRow.stakeContract || '-' }}</el-descriptions-item>
          <el-descriptions-item label="实际桩号">{{ detailRow.stakeActual || '-' }}</el-descriptions-item>
          <el-descriptions-item label="路政桩号">{{ detailRow.stakeApproval || '-' }}</el-descriptions-item>
          <el-descriptions-item label="规格尺寸">{{ detailRow.spec }} {{ detailRow.width }}×{{ detailRow.height }} 米</el-descriptions-item>
          <el-descriptions-item label="产权">{{ detailRow.ownership || '-' }}</el-descriptions-item>
          <el-descriptions-item label="用地性质">{{ detailRow.landNature || '-' }}</el-descriptions-item>
        </el-descriptions>
        <div class="drawer-section-title">合规证件</div>
        <el-table :data="complianceOf(detailRow)" border size="small">
          <el-table-column prop="type" label="证件" width="85" />
          <el-table-column label="编号" min-width="125"><template #default="{ row }">{{ row.number || '-' }}</template></el-table-column>
          <el-table-column label="办理日期" width="105"><template #default="{ row }">{{ row.recordDate || '-' }}</template></el-table-column>
          <el-table-column label="有效期至" width="105"><template #default="{ row }">{{ row.expire || '-' }}</template></el-table-column>
          <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="row.tag" size="small">{{ row.statusText }}</el-tag></template></el-table-column>
        </el-table>
        <div v-if="detailPhotos.length" class="drawer-section-title">现场照片</div>
        <div v-if="detailPhotos.length" class="detail-photos">
          <el-image v-for="p in detailPhotos" :key="p" :src="p" fit="cover" :preview-src-list="detailPhotos" preview-teleported />
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules, UploadRequestOptions } from 'element-plus'
import { Download, Plus, Search, Upload, View } from '@element-plus/icons-vue'
import { billboardApi, dictApi, lookupApi } from '../api'
import type { BillboardQuery } from '../api'
import { useUserStore } from '../stores/user'
import { exportCsv, isExpired, isExpiring, statusTagType } from '../utils/format'
import type { Billboard, Dict, LookupData } from '../types'

const store = useUserStore()
const router = useRouter()
const canWrite = computed(() => store.role === 'archivist')

const list = ref<Billboard[]>([])
const dicts = ref<Dict[]>([])
const lookup = ref<LookupData | null>(null)
const loading = ref(false)
const query = reactive<BillboardQuery>({})

const dialogVisible = ref(false)
const saving = ref(false)
const uploading = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<Partial<Billboard>>({})
const photoList = ref<string[]>([])
const detailVisible = ref(false)
const detailRow = ref<Billboard | null>(null)
const detailPhotos = computed(() => detailRow.value?.photos ? detailRow.value.photos.split(',').filter(Boolean) : [])

const formRules: FormRules = {
  code: [{ required: true, message: '请输入编号', trigger: 'blur' }],
  sectionId: [{ required: true, message: '请选择路段', trigger: 'change' }],
  stakeContract: [{ required: true, message: '必填', trigger: 'blur' }],
  stakeActual: [{ required: true, message: '必填', trigger: 'blur' }],
  stakeApproval: [{ required: true, message: '必填', trigger: 'blur' }],
  landNature: [{ required: true, message: '请选择用地性质', trigger: 'change' }]
}

const landTip = computed(() => {
  switch (form.landNature) {
    case '公路用地':
      return '公路用地由路政直接管理，未取得设置许可证属于违建，请先办理路政审批。'
    case '集体土地':
      return '集体土地需先完成土地流转手续，否则不能签订合同。'
    case '自有土地':
      return '自有产权土地可直接经营。'
    default:
      return ''
  }
})

function dictOptions(type: string): string[] {
  return dicts.value.find((d) => d.type === type)?.items.map((i) => i.label) || []
}

function complianceOf(row: Billboard) {
  return [
    { type: '路政', number: row.permitNo, recordDate: '', expire: row.permitExpire },
    { type: '保险', number: row.insuranceNo, recordDate: '', expire: row.insuranceExpire },
    { type: '年审', number: row.annualCheckNo, recordDate: row.annualCheckDate, expire: row.annualCheckExpire || annualCheckExpire(row.annualCheckDate) }
  ].map((c) => ({
    ...c,
    tag: !c.expire ? 'info' : isExpired(c.expire) ? 'danger' : isExpiring(c.expire) ? 'warning' : 'success',
    statusText: !c.expire ? '未录入' : isExpired(c.expire) ? '已过期' : isExpiring(c.expire) ? '即将到期' : '正常'
  }))
}

function annualCheckExpire(date?: string): string {
  if (!date) return ''
  const value = new Date(`${date}T00:00:00`)
  if (Number.isNaN(value.getTime())) return date
  value.setFullYear(value.getFullYear() + 1)
  const year = value.getFullYear()
  const month = String(value.getMonth() + 1).padStart(2, '0')
  const day = String(value.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function onLandNatureChange() {
  // 触发提示联动即可
}

async function doUpload(options: UploadRequestOptions) {
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', options.file)
    const resp = await fetch('/api/files/upload', {
      method: 'POST',
      headers: { Authorization: `Bearer ${localStorage.getItem('gb_token') || ''}` },
      body: fd
    })
    const body = await resp.json()
    if (body.code !== 0) {
      throw new Error(body.msg || '上传失败')
    }
    photoList.value.push(body.data.path)
    ElMessage.success('上传成功')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '上传失败')
  } finally {
    uploading.value = false
  }
}

async function load() {
  loading.value = true
  try {
    const params: BillboardQuery = {}
    Object.entries(query).forEach(([k, v]) => {
      if (v !== undefined && v !== '' && v !== null) {
        ;(params as Record<string, unknown>)[k] = v
      }
    })
    list.value = await billboardApi.list(params)
  } finally {
    loading.value = false
  }
}

function openForm(row?: Billboard) {
  Object.keys(form).forEach((k) => delete (form as Record<string, unknown>)[k])
  Object.assign(
    form,
    row
      ? { ...row }
      : {
          code: '', sectionId: null, side: '右侧', stakeContract: '', stakeActual: '', stakeApproval: '',
          spec: '', width: 18, height: 6, ownership: '', landNature: '', builder: '', buildYear: new Date().getFullYear(),
          status: '空闲', permitNo: '', permitExpire: '', insuranceNo: '', insuranceExpire: '',
          annualCheckNo: '', annualCheckDate: '', annualCheckExpire: '', remark: ''
        }
  )
  photoList.value = row?.photos ? row.photos.split(',').filter(Boolean) : []
  dialogVisible.value = true
}

function openDetail(row: Billboard) {
  detailRow.value = row
  detailVisible.value = true
}

function exportCurrent() {
  exportCsv('广告牌档案',
    ['编号', '所属路段', '侧别', '合同桩号', '实际桩号', '路政桩号', '规格', '宽(米)', '高(米)', '产权', '用地性质', '状态', '承租方', '路政许可有效期', '保险有效期', '年审有效期'],
    list.value.map((row) => [row.code, row.sectionName, row.side, row.stakeContract, row.stakeActual, row.stakeApproval,
      row.spec, row.width, row.height, row.ownership, row.landNature, row.status, row.renterName,
      row.permitExpire, row.insuranceExpire, row.annualCheckExpire || annualCheckExpire(row.annualCheckDate)]))
}

async function onSave() {
  await formRef.value?.validate()
  if (photoList.value.length < 2) {
    ElMessage.warning('请至少上传 2 张现场照片，建议包含正面和侧面照片')
    return
  }
  saving.value = true
  try {
    form.photos = photoList.value.join(',')
    if (form.id) {
      await billboardApi.update(form.id, form)
    } else {
      await billboardApi.create(form)
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

async function onDelete(row: Billboard) {
  await ElMessageBox.confirm(
    `确定删除广告牌「${row.code}」吗？删除后不可恢复，请确认该牌没有生效合同。`,
    '删除确认',
    { type: 'warning', confirmButtonText: '确定删除', confirmButtonClass: 'el-button--danger' }
  )
  await billboardApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(async () => {
  load()
  dicts.value = await dictApi.list()
  lookup.value = await lookupApi.all()
})
</script>

<style scoped>
.stake {
  font-size: 12px;
  line-height: 1.6;
}

.compliance {
  margin-bottom: 2px;
}

.empty-text {
  color: #c0c4cc;
}

.land-tip {
  margin-bottom: 14px;
}

.form-section-title {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin: 4px 0 16px;
  padding: 8px 10px;
  border-left: 3px solid #16488a;
  background: #f3f7fb;
}

.form-section-title:not(:first-child) {
  margin-top: 8px;
}

.form-section-title span {
  color: #163b66;
  font-weight: 650;
}

.form-section-title small {
  color: #7d8999;
  font-size: 12px;
}

.photo-list {
  display: flex;
  gap: 8px;
  margin-top: 8px;
  flex-wrap: wrap;
}

.photo-item {
  width: 88px;
  height: 60px;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
}

.photo-wrap {
  position: relative;
}

.photo-remove {
  position: absolute;
  top: -6px;
  right: -6px;
  width: 20px;
  height: 20px;
  border: 2px solid #fff;
  border-radius: 50%;
  background: #f56c6c;
  color: #fff;
  cursor: pointer;
  line-height: 15px;
}

.photo-tip {
  margin-top: 7px;
  color: #909399;
  font-size: 12px;
}

.drawer-section-title {
  margin: 20px 0 10px;
  padding-left: 9px;
  border-left: 3px solid #16488a;
  color: #243247;
  font-weight: 650;
}

.detail-photos {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.detail-photos .el-image {
  height: 110px;
  border: 1px solid #dfe6ef;
  border-radius: 4px;
}

.code-value,
.renter-value {
  font-size: 16px !important;
}
</style>
