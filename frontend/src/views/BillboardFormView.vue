<template>
  <div class="page-card form-page" v-loading="loading">
    <div class="form-nav">
      <el-button :icon="ArrowLeft" @click="goBack">返回</el-button>
      <span>{{ isEdit ? `编辑 ${form.code || ''}` : '建立新的广告牌档案' }}</span>
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="105px" class="archive-form">
      <section class="form-section">
        <header><span>基本信息</span><small>广告牌编号、所属路段与规格尺寸</small></header>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="广告牌编号" prop="code"><el-input v-model="form.code" placeholder="如 GG-JH-013" /></el-form-item></el-col>
          <el-col :span="10"><el-form-item label="所属路段" prop="sectionId"><el-select v-model="form.sectionId" filterable style="width:100%"><el-option v-for="row in lookup?.sections || []" :key="row.id" :label="row.name" :value="row.id" /></el-select></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="位置侧别"><el-select v-model="form.side" style="width:100%"><el-option v-for="item in ['左侧','右侧','中央分隔带']" :key="item" :label="item" :value="item" /></el-select></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="广告牌规格"><el-select v-model="form.spec" style="width:100%"><el-option v-for="item in dictOptions('spec')" :key="item" :label="item" :value="item" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="宽（米）"><el-input-number v-model="form.width" :min="1" :max="100" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="高（米）"><el-input-number v-model="form.height" :min="1" :max="50" style="width:100%" /></el-form-item></el-col>
        </el-row>
      </section>

      <section class="form-section stake-section">
        <header><span>三个桩号</span><small>三个桩号经常不一致，必须分别保存</small></header>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="合同桩号" prop="stakeContract"><el-input v-model="form.stakeContract" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="实际桩号" prop="stakeActual"><el-input v-model="form.stakeActual" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="路政桩号" prop="stakeApproval"><el-input v-model="form.stakeApproval" /></el-form-item></el-col>
        </el-row>
      </section>

      <section class="form-section">
        <header><span>产权、用地与建设</span><small>用地性质决定经营前需要办理的手续</small></header>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="产权"><el-select v-model="form.ownership" style="width:100%"><el-option v-for="item in dictOptions('ownership')" :key="item" :label="item" :value="item" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="用地性质" prop="landNature"><el-select v-model="form.landNature" style="width:100%"><el-option v-for="item in dictOptions('landNature')" :key="item" :label="item" :value="item" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="档案状态"><el-select v-model="form.status" style="width:100%"><el-option v-for="item in ['空闲','维护中','停用']" :key="item" :label="item" :value="item" /></el-select></el-form-item></el-col>
        </el-row>
        <el-alert v-if="landTip" :title="landTip" type="warning" :closable="false" show-icon class="land-tip" />
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="建设单位"><el-input v-model="form.builder" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="建成年份"><el-input-number v-model="form.buildYear" :min="1990" :max="2100" style="width:100%" /></el-form-item></el-col>
        </el-row>
      </section>

      <section class="form-section compliance-section">
        <header><span>合规手续</span><small>缺少或过期的证照会在档案列表中标记</small></header>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="路政许可编号"><el-input v-model="form.permitNo" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="许可有效期"><el-date-picker v-model="form.permitExpire" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="保险单号"><el-input v-model="form.insuranceNo" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="保险有效期"><el-date-picker v-model="form.insuranceExpire" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="年审编号"><el-input v-model="form.annualCheckNo" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="最近年审"><el-date-picker v-model="form.annualCheckDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="年审有效期"><el-date-picker v-model="form.annualCheckExpire" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
        </el-row>
      </section>

      <section class="form-section">
        <header><span>现场照片与备注</span><small>照片最低数量尚待甲方确认，当前至少保留两张</small></header>
        <el-form-item label="现场照片">
          <div class="upload-area">
            <el-upload :show-file-list="false" :http-request="uploadPhoto" accept="image/*"><el-button :icon="Upload" :loading="uploading">上传照片</el-button></el-upload>
            <span class="upload-hint">建议包含正面、侧面、桩号与证照现场照片</span>
            <div v-if="photos.length" class="photo-list"><div v-for="(photo,index) in photos" :key="photo" class="photo-item"><el-image :src="photo" fit="cover" :preview-src-list="photos" preview-teleported /><button type="button" aria-label="删除照片" @click="photos.splice(index,1)">×</button></div></div>
          </div>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="3" /></el-form-item>
      </section>
    </el-form>

    <footer class="form-footer"><el-button @click="goBack">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存档案</el-button></footer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules, UploadRequestOptions } from 'element-plus'
import { ArrowLeft, Upload } from '@element-plus/icons-vue'
import { billboardApi, dictApi, lookupApi } from '../api'
import type { Billboard, Dict, LookupData } from '../types'

const route=useRoute(); const router=useRouter(); const isEdit=computed(()=>!!route.params.id)
const loading=ref(false); const saving=ref(false); const uploading=ref(false); const formRef=ref<FormInstance>()
const lookup=ref<LookupData|null>(null); const dicts=ref<Dict[]>([]); const photos=ref<string[]>([])
const form=reactive<Partial<Billboard>>({})
const rules:FormRules={ code:[{required:true,message:'请输入广告牌编号',trigger:'blur'}], sectionId:[{required:true,message:'请选择所属路段',trigger:'change'}], stakeContract:[{required:true,message:'请输入合同桩号',trigger:'blur'}], stakeActual:[{required:true,message:'请输入实际桩号',trigger:'blur'}], stakeApproval:[{required:true,message:'请输入路政桩号',trigger:'blur'}], landNature:[{required:true,message:'请选择用地性质',trigger:'change'}] }
const landTip=computed(()=>({公路用地:'公路用地必须取得路政许可，未取得许可证不得经营。',集体土地:'集体土地需先完成土地流转手续，再进入合同流程。',自有土地:'自有产权土地可直接进入经营流程。'}[form.landNature||'']||''))
function dictOptions(type:string){return dicts.value.find((row)=>row.type===type)?.items.map((item)=>item.label)||[]}
function reset(){Object.assign(form,{code:'',sectionId:undefined,side:'右侧',stakeContract:'',stakeActual:'',stakeApproval:'',spec:'',width:18,height:6,ownership:'',landNature:'',builder:'',buildYear:new Date().getFullYear(),status:'空闲',permitNo:'',permitExpire:'',insuranceNo:'',insuranceExpire:'',annualCheckNo:'',annualCheckDate:'',annualCheckExpire:'',remark:''})}
function goBack(){router.push(isEdit.value?`/billboards/${route.params.id}`:'/billboards')}
async function uploadPhoto(options:UploadRequestOptions){uploading.value=true;try{const fd=new FormData();fd.append('file',options.file);const resp=await fetch('/api/files/upload',{method:'POST',headers:{Authorization:`Bearer ${localStorage.getItem('gb_token')||''}`},body:fd});const body=await resp.json();if(body.code!==0)throw new Error(body.msg||'上传失败');photos.value.push(body.data.path);ElMessage.success('照片已上传')}catch(e){ElMessage.error(e instanceof Error?e.message:'上传失败')}finally{uploading.value=false}}
async function save(){await formRef.value?.validate();if(photos.value.length<2){ElMessage.warning('请至少上传两张现场照片');return}saving.value=true;try{form.photos=photos.value.join(',');let row:Billboard;if(isEdit.value){row=await billboardApi.update(Number(route.params.id),form)}else{row=await billboardApi.create(form)}ElMessage.success('广告牌档案已保存');router.push(`/billboards/${row.id}`)}finally{saving.value=false}}
onMounted(async()=>{loading.value=true;try{[lookup.value,dicts.value]=await Promise.all([lookupApi.all(),dictApi.list()]);reset();if(isEdit.value){const rows=await billboardApi.list({});const row=rows.find((item)=>item.id===Number(route.params.id));if(row){Object.assign(form,row);photos.value=row.photos?.split(',').filter(Boolean)||[]}}}finally{loading.value=false}})
</script>

<style scoped>
.form-page{max-width:1180px;margin:0 auto}.form-nav{display:flex;align-items:center;gap:14px;margin:-4px 0 18px;color:#536176}.archive-form{border:1px solid #dbe3ed}.form-section{padding:18px 22px 8px;border-bottom:1px solid #dbe3ed}.form-section:last-child{border-bottom:0}.form-section header{display:flex;align-items:baseline;gap:12px;margin-bottom:17px;padding-left:10px;border-left:3px solid #16488a}.form-section header span{color:#173b64;font-size:16px;font-weight:650}.form-section header small{color:#7a8798}.stake-section{background:#f7f9fc}.compliance-section{background:#fffdf6}.land-tip{margin-bottom:16px}.upload-area{width:100%}.upload-hint{margin-left:10px;color:#7b8797;font-size:12px}.photo-list{display:flex;gap:10px;flex-wrap:wrap;margin-top:12px}.photo-item{position:relative}.photo-item .el-image{width:120px;height:82px;border:1px solid #dce3eb}.photo-item button{position:absolute;top:-7px;right:-7px;width:22px;height:22px;border:2px solid #fff;border-radius:50%;background:#c84040;color:#fff;cursor:pointer}.form-footer{position:sticky;bottom:-16px;display:flex;justify-content:flex-end;gap:10px;margin:18px -20px -16px;padding:14px 22px;border-top:1px solid #dfe6ef;background:#fff;z-index:3}@media(max-width:800px){.form-section{padding:16px 12px}.form-section :deep(.el-col){max-width:100%;flex:0 0 100%}}
</style>
