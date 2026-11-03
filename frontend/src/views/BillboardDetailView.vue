<template>
  <div class="page-card detail-page" v-loading="loading">
    <div class="detail-toolbar">
      <el-button :icon="ArrowLeft" @click="router.push('/billboards')">返回广告牌列表</el-button>
      <div v-if="billboard" class="detail-actions">
        <el-button @click="router.push(`/billboard-changes?billboardId=${billboard.id}`)">登记变动</el-button>
        <el-button type="primary" :icon="Edit" @click="router.push(`/billboards/${billboard.id}/edit`)">编辑档案</el-button>
      </div>
    </div>

    <template v-if="billboard">
      <header class="record-head">
        <div class="record-sign"><span>广告牌档案</span><strong>{{ billboard.code }}</strong></div>
        <div class="record-location">
          <h1>{{ billboard.sectionName || '未关联路段' }}</h1>
          <p>{{ billboard.stakeActual || '未填写实际桩号' }} · {{ billboard.side || '未填写侧别' }}</p>
        </div>
        <el-tag :type="statusTagType(billboard.status)" size="large">{{ billboard.status }}</el-tag>
      </header>

      <el-tabs v-model="activeTab" class="record-tabs">
        <el-tab-pane label="基本档案" name="base">
          <section class="record-section">
            <h2>位置与三个桩号</h2>
            <el-descriptions :column="3" border>
              <el-descriptions-item label="所属路段">{{ billboard.sectionName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="位置侧别">{{ billboard.side || '-' }}</el-descriptions-item>
              <el-descriptions-item label="当前承租方">{{ billboard.renterName || '暂无' }}</el-descriptions-item>
              <el-descriptions-item label="合同桩号">{{ billboard.stakeContract || '-' }}</el-descriptions-item>
              <el-descriptions-item label="实际桩号">{{ billboard.stakeActual || '-' }}</el-descriptions-item>
              <el-descriptions-item label="路政桩号">{{ billboard.stakeApproval || '-' }}</el-descriptions-item>
            </el-descriptions>
          </section>
          <section class="record-section">
            <h2>规格、产权与建设</h2>
            <el-descriptions :column="3" border>
              <el-descriptions-item label="广告牌规格">{{ billboard.spec || '-' }}</el-descriptions-item>
              <el-descriptions-item label="版面尺寸">{{ billboard.width || '-' }} × {{ billboard.height || '-' }} 米</el-descriptions-item>
              <el-descriptions-item label="产权">{{ billboard.ownership || '-' }}</el-descriptions-item>
              <el-descriptions-item label="用地性质">{{ billboard.landNature || '-' }}</el-descriptions-item>
              <el-descriptions-item label="建设单位">{{ billboard.builder || '-' }}</el-descriptions-item>
              <el-descriptions-item label="建成年份">{{ billboard.buildYear || '-' }}</el-descriptions-item>
            </el-descriptions>
            <el-alert v-if="landTip" :title="landTip" type="warning" :closable="false" show-icon class="land-alert" />
          </section>
          <section class="record-section">
            <h2>现场照片</h2>
            <div v-if="photos.length" class="photo-grid">
              <el-image v-for="photo in photos" :key="photo" :src="photo" fit="cover" :preview-src-list="photos" preview-teleported />
            </div>
            <el-empty v-else :image-size="72" description="尚未上传现场照片" />
          </section>
        </el-tab-pane>

        <el-tab-pane label="证照附件" name="compliance">
          <section class="record-section">
            <div class="section-lead"><div><h2>合规证照</h2><p>路政许可、保险与年审是广告牌可经营的合规红线。</p></div></div>
            <el-table :data="complianceRows" border>
              <el-table-column prop="type" label="证照类型" width="120" />
              <el-table-column label="编号" min-width="180"><template #default="{ row }">{{ row.number || '未录入' }}</template></el-table-column>
              <el-table-column label="办理日期" width="120"><template #default="{ row }">{{ row.recordDate || '-' }}</template></el-table-column>
              <el-table-column label="有效期至" width="120"><template #default="{ row }">{{ row.expire || '-' }}</template></el-table-column>
              <el-table-column label="状态" width="110"><template #default="{ row }"><el-tag :type="row.tag">{{ row.status }}</el-tag></template></el-table-column>
            </el-table>
          </section>
        </el-tab-pane>

        <el-tab-pane label="变更记录" name="changes">
          <section class="record-section">
            <div class="section-lead"><div><h2>广告牌变更记录</h2><p>记录新建、拆除、移位和重要信息变更；审批流程待甲方确认。</p></div><el-button type="primary" @click="router.push(`/billboard-changes?billboardId=${billboard.id}`)">登记变动</el-button></div>
            <el-table :data="changes" border stripe>
              <el-table-column prop="changeDate" label="变动日期" width="120" />
              <el-table-column prop="changeType" label="类型" width="100" />
              <el-table-column prop="beforeValue" label="变动前" min-width="160" show-overflow-tooltip />
              <el-table-column prop="afterValue" label="变动后" min-width="160" show-overflow-tooltip />
              <el-table-column prop="reason" label="原因" min-width="180" show-overflow-tooltip />
              <el-table-column prop="operator" label="登记人" width="100" />
            </el-table>
          </section>
        </el-tab-pane>

        <el-tab-pane label="电子档案" name="files">
          <section class="record-section">
            <h2>电子档案</h2>
            <p class="pending-copy">当前版本将现场照片与证照信息统一归档。其他电子文件类型和维护记录范围待甲方确认后补充。</p>
          </section>
        </el-tab-pane>
      </el-tabs>
    </template>
    <el-empty v-else-if="!loading" description="未找到该广告牌档案" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Edit } from '@element-plus/icons-vue'
import { billboardApi, billboardChangeApi } from '../api'
import { isExpired, isExpiring, statusTagType } from '../utils/format'
import type { Billboard, BillboardChange } from '../types'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const activeTab = ref('base')
const billboard = ref<Billboard | null>(null)
const changes = ref<BillboardChange[]>([])
const photos = computed(() => billboard.value?.photos?.split(',').filter(Boolean) || [])
const landTip = computed(() => ({
  '公路用地': '公路用地由路政直接管理，未取得设置许可证不得经营。',
  '集体土地': '集体土地需完成土地流转手续后方可签订合同。',
  '自有土地': '自有产权土地可直接进入经营流程。'
}[billboard.value?.landNature || ''] || ''))
const complianceRows = computed(() => {
  if (!billboard.value) return []
  return [
    { type: '路政许可', number: billboard.value.permitNo, recordDate: '', expire: billboard.value.permitExpire },
    { type: '保险', number: billboard.value.insuranceNo, recordDate: '', expire: billboard.value.insuranceExpire },
    { type: '年审', number: billboard.value.annualCheckNo, recordDate: billboard.value.annualCheckDate, expire: billboard.value.annualCheckExpire }
  ].map((row) => ({ ...row, ...complianceStatus(row.expire) }))
})

function complianceStatus(expire?: string) {
  if (!expire) return { tag: 'info' as const, status: '未录入' }
  if (isExpired(expire)) return { tag: 'danger' as const, status: '已过期' }
  if (isExpiring(expire)) return { tag: 'warning' as const, status: '即将到期' }
  return { tag: 'success' as const, status: '正常' }
}

onMounted(async () => {
  loading.value = true
  try {
    const id = Number(route.params.id)
    const rows = await billboardApi.list({})
    billboard.value = rows.find((row) => row.id === id) || null
    if (billboard.value) changes.value = await billboardChangeApi.list({ billboardId: id })
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.detail-toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.detail-actions { display: flex; gap: 10px; }
.record-head { display: grid; grid-template-columns: 150px minmax(0,1fr) auto; gap: 22px; align-items: center; padding: 22px 26px; border: 1px solid #ccd8e7; background: #f5f8fc; }
.record-sign { padding: 12px 15px; background: #0f2d52; color: #fff; }
.record-sign span, .record-sign strong { display: block; }
.record-sign span { color: #b9cbe0; font-size: 12px; }
.record-sign strong { margin-top: 5px; font-family: Arial, sans-serif; font-size: 18px; }
.record-location h1 { margin: 0; color: #173b64; font-size: 23px; }
.record-location p { margin-top: 7px; color: #718095; }
.record-tabs { margin-top: 16px; }
.record-section { margin-bottom: 22px; }
.record-section h2 { margin: 0 0 12px; color: #243247; font-size: 16px; }
.section-lead { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 12px; }
.section-lead h2 { margin-bottom: 4px; }
.section-lead p, .pending-copy { color: #758296; line-height: 1.7; }
.land-alert { margin-top: 12px; }
.photo-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 12px; }
.photo-grid .el-image { height: 130px; border: 1px solid #dce3eb; }
@media (max-width: 760px) { .record-head { grid-template-columns: 1fr; } .detail-toolbar { align-items: stretch; flex-direction: column; gap: 10px; } }
</style>

