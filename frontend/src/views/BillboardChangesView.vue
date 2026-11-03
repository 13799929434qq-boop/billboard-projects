<template>
  <div class="page-card">
    <div class="page-lead">
      <div><h2>广告牌变动登记</h2><p>记录广告牌新建、拆除、移位和重要信息变更。当前仅登记事实，不包含审批流程。</p></div>
      <div class="page-lead-actions"><el-button type="primary" :icon="Plus" @click="openForm">登记变动</el-button></div>
    </div>
    <el-alert type="info" :closable="false" show-icon title="是否需要审批、由谁审批，需甲方确认后再增加流程。" class="scope-note" />
    <div class="filter-panel"><div class="toolbar">
      <el-select v-model="query.billboardId" clearable filterable placeholder="广告牌" style="width: 220px" @change="load">
        <el-option v-for="row in billboards" :key="row.id" :label="`${row.code} · ${row.sectionName || ''}`" :value="row.id" />
      </el-select>
      <el-select v-model="query.changeType" clearable placeholder="变动类型" style="width: 140px" @change="load">
        <el-option v-for="item in changeTypes" :key="item" :label="item" :value="item" />
      </el-select>
    </div></div>
    <el-table :data="list" border stripe v-loading="loading">
      <el-table-column prop="changeDate" label="变动日期" width="120" />
      <el-table-column prop="billboardCode" label="广告牌" width="130" />
      <el-table-column prop="sectionName" label="所属路段" min-width="150" />
      <el-table-column prop="changeType" label="类型" width="100"><template #default="{ row }"><el-tag effect="plain">{{ row.changeType }}</el-tag></template></el-table-column>
      <el-table-column prop="beforeValue" label="变动前" min-width="160" show-overflow-tooltip />
      <el-table-column prop="afterValue" label="变动后" min-width="160" show-overflow-tooltip />
      <el-table-column prop="reason" label="变动原因" min-width="180" show-overflow-tooltip />
      <el-table-column prop="operator" label="登记人" width="100" />
      <el-table-column label="档案" width="90" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="router.push(`/billboards/${row.billboardId}`)">查看</el-button></template></el-table-column>
    </el-table>

    <el-dialog v-model="visible" title="登记广告牌变动" width="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="14"><el-form-item label="广告牌" prop="billboardId"><el-select v-model="form.billboardId" filterable style="width:100%"><el-option v-for="row in billboards" :key="row.id" :label="`${row.code} · ${row.sectionName || ''}`" :value="row.id" /></el-select></el-form-item></el-col>
          <el-col :span="10"><el-form-item label="变动类型" prop="changeType"><el-select v-model="form.changeType" style="width:100%"><el-option v-for="item in changeTypes" :key="item" :label="item" :value="item" /></el-select></el-form-item></el-col>
        </el-row>
        <el-form-item label="变动日期" prop="changeDate"><el-date-picker v-model="form.changeDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="变动前"><el-input v-model="form.beforeValue" type="textarea" :rows="2" placeholder="如：K12+300 右侧" /></el-form-item>
        <el-form-item label="变动后"><el-input v-model="form.afterValue" type="textarea" :rows="2" placeholder="如：K12+650 右侧" /></el-form-item>
        <el-form-item label="变动原因" prop="reason"><el-input v-model="form.reason" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存记录</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { billboardApi, billboardChangeApi } from '../api'
import type { Billboard, BillboardChange } from '../types'

const route = useRoute(); const router = useRouter()
const changeTypes = ['新建', '拆除', '移位', '信息变更']
const list = ref<BillboardChange[]>([]); const billboards = ref<Billboard[]>([]); const loading = ref(false)
const visible = ref(false); const saving = ref(false); const formRef = ref<FormInstance>()
const query = reactive<{billboardId?: number; changeType?: string}>({})
const form = reactive<Partial<BillboardChange>>({})
const rules: FormRules = { billboardId: [{ required: true, message: '请选择广告牌', trigger: 'change' }], changeType: [{ required: true, message: '请选择类型', trigger: 'change' }], changeDate: [{ required: true, message: '请选择日期', trigger: 'change' }], reason: [{ required: true, message: '请填写变动原因', trigger: 'blur' }] }

async function load() { loading.value=true; try { list.value=await billboardChangeApi.list(query) } finally { loading.value=false } }
function openForm() { Object.assign(form, { billboardId: query.billboardId, changeType: '信息变更', changeDate: new Date().toISOString().slice(0,10), beforeValue: '', afterValue: '', reason: '' }); visible.value=true }
async function save() { await formRef.value?.validate(); saving.value=true; try { await billboardChangeApi.create(form); ElMessage.success('变动记录已保存'); visible.value=false; load() } finally { saving.value=false } }
onMounted(async () => { billboards.value=await billboardApi.list({}); const id=Number(route.query.billboardId); if (id) query.billboardId=id; load() })
</script>

<style scoped>.scope-note{margin-bottom:14px}</style>

