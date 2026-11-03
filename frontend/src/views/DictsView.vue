<template>
  <div class="page-card">
    <div class="page-lead">
      <div>
        <h2>数据字典</h2>
        <p>由系统管理员统一维护业务下拉选项，档案员和财务人员只在业务表单中使用。</p>
      </div>
    </div>
    <el-row :gutter="16">
      <el-col :span="6">
        <el-menu :default-active="activeType" @select="(k: string) => (activeType = k)">
          <el-menu-item v-for="d in dicts" :key="d.type" :index="d.type">
            <el-icon><Collection /></el-icon>
            <span>{{ d.name }}</span>
          </el-menu-item>
        </el-menu>
      </el-col>
      <el-col :span="18">
        <div class="toolbar">
          <el-input v-model="newLabel" :placeholder="`新增「${activeDict?.name || ''}」选项`" style="width: 260px" @keyup.enter="addItem" />
          <el-button type="primary" :disabled="!newLabel.trim()" @click="addItem">添加选项</el-button>
        </div>
        <el-table :data="activeDict?.items || []" border stripe v-loading="loading">
          <el-table-column type="index" label="#" width="60" />
          <el-table-column prop="label" label="选项名称" />
          <el-table-column prop="sort" label="排序" width="80" />
          <el-table-column label="操作" width="140">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button link type="danger" @click="removeItem(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-col>
    </el-row>

    <el-dialog v-model="editVisible" title="编辑基础选项" width="420px">
      <el-form label-width="80px">
        <el-form-item label="选项名称"><el-input v-model="editForm.label" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="editForm.sort" :min="0" style="width: 100%" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="editVisible=false">取消</el-button><el-button type="primary" :disabled="!editForm.label.trim()" @click="saveEdit">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Collection } from '@element-plus/icons-vue'
import { dictApi } from '../api'
import type { Dict, DictItem } from '../types'

const dicts = ref<Dict[]>([])
const activeType = ref('ownership')
const newLabel = ref('')
const loading = ref(false)
const editVisible = ref(false)
const editForm = reactive({ id: 0, label: '', sort: 0 })

const activeDict = computed(() => dicts.value.find((d) => d.type === activeType.value))

async function load() {
  loading.value = true
  try {
    dicts.value = await dictApi.list()
  } finally {
    loading.value = false
  }
}

async function addItem() {
  const label = newLabel.value.trim()
  if (!label) {
    return
  }
  await dictApi.addItem(activeType.value, label)
  ElMessage.success('已添加')
  newLabel.value = ''
  load()
}

async function removeItem(row: DictItem) {
  await ElMessageBox.confirm(`确定删除选项「${row.label}」吗？已录入的历史数据不受影响。`, '删除确认', {
    type: 'warning'
  })
  await dictApi.removeItem(row.id)
  ElMessage.success('已删除')
  load()
}

function openEdit(row: DictItem) {
  Object.assign(editForm, { id: row.id, label: row.label, sort: row.sort })
  editVisible.value = true
}

async function saveEdit() {
  await dictApi.updateItem(editForm.id, { label: editForm.label.trim(), sort: editForm.sort })
  ElMessage.success('已保存')
  editVisible.value = false
  load()
}

onMounted(load)
</script>
