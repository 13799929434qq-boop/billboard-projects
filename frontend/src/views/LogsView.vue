<template>
  <div class="page-card">
    <div class="page-header"><span class="title">操作日志</span></div>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索操作人/动作/内容" clearable style="width: 280px" :prefix-icon="Search"
        @change="load" />
      <el-button :icon="Search" @click="load">查询</el-button>
    </div>
    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="time" label="时间" width="160" />
      <el-table-column prop="name" label="操作人" width="110">
        <template #default="{ row }">{{ row.name }}（{{ row.username }}）</template>
      </el-table-column>
      <el-table-column prop="action" label="动作" width="110">
        <template #default="{ row }">
          <el-tag size="small" :type="actionTag(row.action)">{{ row.action }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="detail" label="操作内容" min-width="320" show-overflow-tooltip />
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { logApi } from '../api'
import type { OperationLog } from '../types'

const list = ref<OperationLog[]>([])
const loading = ref(false)
const keyword = ref('')

function actionTag(action: string): 'success' | 'danger' | 'warning' | 'info' | 'primary' {
  if (action.includes('删除') || action.includes('解除')) {
    return 'danger'
  }
  if (action.includes('收款') || action.includes('生效') || action.includes('创建')) {
    return 'success'
  }
  if (action.includes('修改') || action.includes('黑名单')) {
    return 'warning'
  }
  if (action.includes('登录')) {
    return 'info'
  }
  return 'primary'
}

async function load() {
  loading.value = true
  try {
    list.value = await logApi.list(keyword.value || undefined)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
