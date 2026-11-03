<template>
  <div class="page-card" v-loading="loading">
    <div class="detail-toolbar"><el-button :icon="ArrowLeft" @click="router.push('/merchants')">返回客户列表</el-button></div>
    <template v-if="merchant">
      <header class="customer-head">
        <div class="customer-mark">{{ merchant.name.slice(0, 1) }}</div>
        <div><h1>{{ merchant.name }}</h1><p>{{ merchant.contact || '未填写联系人' }} · {{ merchant.phone || '未填写电话' }}</p></div>
        <el-tag :type="merchant.blacklisted ? 'danger' : 'success'" size="large">{{ merchant.blacklisted ? '黑名单' : '正常合作' }}</el-tag>
      </header>
      <div class="account-strip">
        <div><span>合同数量</span><strong>{{ contracts.length }}</strong></div>
        <div><span>累计应收</span><strong>¥ {{ fmtMoney(summary.total) }}</strong></div>
        <div><span>累计已收</span><strong class="green">¥ {{ fmtMoney(summary.received) }}</strong></div>
        <div><span>当前欠款</span><strong class="red">¥ {{ fmtMoney(summary.arrears) }}</strong></div>
      </div>
      <el-tabs v-model="tab" class="detail-tabs">
        <el-tab-pane label="基本信息" name="base"><el-descriptions :column="2" border><el-descriptions-item label="客户名称">{{ merchant.name }}</el-descriptions-item><el-descriptions-item label="联系人">{{ merchant.contact || '-' }}</el-descriptions-item><el-descriptions-item label="联系电话">{{ merchant.phone || '-' }}</el-descriptions-item><el-descriptions-item label="联系地址">{{ merchant.address || '-' }}</el-descriptions-item><el-descriptions-item label="备注" :span="2">{{ merchant.remark || '-' }}</el-descriptions-item></el-descriptions></el-tab-pane>
        <el-tab-pane label="开票与账户" name="invoice"><el-descriptions :column="2" border><el-descriptions-item label="纳税人识别号">{{ merchant.taxNo || '-' }}</el-descriptions-item><el-descriptions-item label="开户银行">{{ merchant.bankName || '-' }}</el-descriptions-item><el-descriptions-item label="银行账号" :span="2">{{ merchant.bankAccount || '-' }}</el-descriptions-item></el-descriptions></el-tab-pane>
        <el-tab-pane :label="`关联合同 ${contracts.length}`" name="contracts"><el-table :data="contracts" border stripe><el-table-column prop="code" label="合同编号" width="140" /><el-table-column label="期限" min-width="210"><template #default="{row}">{{ row.startDate }} ~ {{ row.endDate }}</template></el-table-column><el-table-column label="合同总额" width="140" align="right"><template #default="{row}">{{ fmtMoney(row.totalAmount) }}</template></el-table-column><el-table-column prop="status" label="状态" width="90" /><el-table-column label="操作" width="90"><template #default="{row}"><el-button link type="primary" @click="router.push(`/contracts/${row.id}`)">详情</el-button></template></el-table-column></el-table></el-tab-pane>
        <el-tab-pane :label="`应收与欠款 ${arrearsCount}`" name="receivables"><el-table :data="receivables" border stripe><el-table-column prop="year" label="年度" width="80" /><el-table-column prop="contractCode" label="合同" width="130" /><el-table-column prop="billboardCode" label="广告牌" width="120" /><el-table-column label="应收" width="120" align="right"><template #default="{row}">{{ fmtMoney(row.amount) }}</template></el-table-column><el-table-column label="欠款" width="120" align="right"><template #default="{row}"><span class="red">{{ fmtMoney(row.amount-row.received) }}</span></template></el-table-column><el-table-column prop="status" label="状态" /></el-table></el-tab-pane>
      </el-tabs>
    </template>
    <el-empty v-else-if="!loading" description="未找到客户档案" />
  </div>
</template>
<script setup lang="ts">
import { computed,onMounted,ref } from 'vue';import{useRoute,useRouter}from'vue-router';import{ArrowLeft}from'@element-plus/icons-vue';import{contractApi,merchantApi,receivableApi}from'../api';import{fmtMoney}from'../utils/format';import type{Contract,Merchant,Receivable}from'../types';
const route=useRoute(),router=useRouter();const loading=ref(false),tab=ref('base'),merchant=ref<Merchant|null>(null),contracts=ref<Contract[]>([]),receivables=ref<Receivable[]>([]);const summary=computed(()=>receivables.value.reduce((s,r)=>({total:s.total+r.amount,received:s.received+r.received,arrears:s.arrears+r.amount-r.received}),{total:0,received:0,arrears:0}));const arrearsCount=computed(()=>receivables.value.filter(r=>r.amount>r.received).length);onMounted(async()=>{loading.value=true;try{const id=Number(route.params.id);merchant.value=(await merchantApi.list()).find(r=>r.id===id)||null;if(merchant.value)[contracts.value,receivables.value]=await Promise.all([contractApi.list({merchantId:id}),receivableApi.list({merchantId:id})])}finally{loading.value=false}})
</script>
<style scoped>.detail-toolbar{margin-bottom:14px}.customer-head{display:grid;grid-template-columns:56px 1fr auto;gap:15px;align-items:center;padding:20px 22px;border:1px solid #d9e2ec;background:#f6f9fc}.customer-mark{display:flex;width:56px;height:56px;align-items:center;justify-content:center;background:#123d70;color:#fff;font-size:24px;font-weight:700}.customer-head h1{margin:0;color:#173b64;font-size:22px}.customer-head p{margin-top:6px;color:#768396}.account-strip{display:grid;grid-template-columns:repeat(4,1fr);border:1px solid #d9e2ec;border-top:0}.account-strip div{padding:16px 20px;border-right:1px solid #e1e7ef}.account-strip div:last-child{border-right:0}.account-strip span,.account-strip strong{display:block}.account-strip span{color:#7a8798;font-size:12px}.account-strip strong{margin-top:6px;color:#16488a;font-size:20px}.green{color:#3c8c5a!important}.red{color:#c84040!important}.detail-tabs{margin-top:16px}@media(max-width:800px){.account-strip{grid-template-columns:repeat(2,1fr)}.customer-head{grid-template-columns:56px 1fr}.customer-head>.el-tag{grid-column:1/-1}}</style>

