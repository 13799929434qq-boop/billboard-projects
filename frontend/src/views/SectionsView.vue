<template>
  <div class="page-card">
    <div class="page-lead">
      <div>
        <h2>路段与运营组织</h2>
        <p>按“路段—地区—运营单位”维护广告牌所属关系，为档案查询和经营统计提供统一口径。</p>
      </div>
      <div class="page-lead-actions">
        <el-button v-if="canWrite" type="primary" :icon="Plus" @click="openForm()">新增路段</el-button>
      </div>
    </div>
    <div class="org-overview">
      <div><span>路段</span><strong>{{ list.length }}</strong></div>
      <div><span>覆盖地区</span><strong>{{ regionCount }}</strong></div>
      <div><span>运营单位</span><strong>{{ operatorCount }}</strong></div>
      <div class="org-path"><span>组织层级</span><strong>路段 → 地区 → 运营单位</strong></div>
    </div>
    <div class="filter-panel"><div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索路段名称/编号/地区/运营单位" clearable style="width: 300px"
        :prefix-icon="Search" @change="load" />
    </div></div>
    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="code" label="路段编号" width="120" />
      <el-table-column prop="name" label="路段名称" min-width="160" />
      <el-table-column prop="region" label="所属地区" width="120" />
      <el-table-column prop="operator" label="运营单位" min-width="150" />
      <el-table-column prop="mileage" label="里程范围" min-width="160" />
      <el-table-column prop="createdAt" label="创建时间" width="110" />
      <el-table-column v-if="canWrite" label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openForm(row)">编辑</el-button>
          <el-button link type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑路段' : '新增路段'" width="520px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="路段编号" prop="code">
          <el-input v-model="form.code" placeholder="如 SD-JH" />
        </el-form-item>
        <el-form-item label="路段名称" prop="name">
          <el-input v-model="form.name" placeholder="如 京哈高速北京段" />
        </el-form-item>
        <el-form-item label="所属地区" prop="region">
          <el-input v-model="form.region" placeholder="如 北京地区" />
        </el-form-item>
        <el-form-item label="运营单位" prop="operator">
          <el-input v-model="form.operator" placeholder="如 京哈运营分公司" />
        </el-form-item>
        <el-form-item label="里程范围">
          <el-input v-model="form.mileage" placeholder="如 K0+000 ~ K42+500" />
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
import { Plus, Search } from '@element-plus/icons-vue'
import { sectionApi } from '../api'
import { useUserStore } from '../stores/user'
import type { Section } from '../types'

const store = useUserStore()
const canWrite = computed(() => store.role === 'archivist')

const list = ref<Section[]>([])
const loading = ref(false)
const keyword = ref('')
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<Partial<Section>>({})
const regionCount = computed(() => new Set(list.value.map((row) => row.region).filter(Boolean)).size)
const operatorCount = computed(() => new Set(list.value.map((row) => row.operator).filter(Boolean)).size)

const formRules: FormRules = {
  code: [{ required: true, message: '请输入路段编号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入路段名称', trigger: 'blur' }],
  region: [{ required: true, message: '请输入所属地区', trigger: 'blur' }],
  operator: [{ required: true, message: '请输入运营单位', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    list.value = await sectionApi.list(keyword.value || undefined)
  } finally {
    loading.value = false
  }
}

function openForm(row?: Section) {
  Object.keys(form).forEach((k) => delete (form as Record<string, unknown>)[k])
  Object.assign(form, row ? { ...row } : { code: '', name: '', region: '', operator: '', mileage: '' })
  dialogVisible.value = true
}

async function onSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (form.id) {
      await sectionApi.update(form.id, form)
    } else {
      await sectionApi.create(form)
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

async function onDelete(row: Section) {
  await ElMessageBox.confirm(`删除路段「${row.name}」将同时删除其下广告牌档案，确定删除吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '确定删除',
    confirmButtonClass: 'el-button--danger'
  })
  await sectionApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.org-overview {
  display: grid;
  grid-template-columns: 130px 130px 130px 1fr;
  margin-bottom: 14px;
  border: 1px solid #dfe6ef;
  background: #f8fafc;
}

.org-overview > div {
  padding: 12px 16px;
  border-right: 1px solid #dfe6ef;
}

.org-overview > div:last-child {
  border-right: 0;
}

.org-overview span {
  display: block;
  color: #7b8797;
  font-size: 12px;
}

.org-overview strong {
  display: block;
  margin-top: 4px;
  color: #16488a;
  font-size: 21px;
}

.org-overview .org-path strong {
  font-size: 15px;
  font-weight: 600;
}
</style>
