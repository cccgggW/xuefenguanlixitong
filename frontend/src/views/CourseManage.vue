<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">课程管理</div>
        <div class="page-sub">Course Management · 维护课程库信息、学分与先修依赖</div>
      </div>
    </div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索课程名称/编号" clearable style="width:240px;" @keyup.enter="loadData" />
        <el-select v-model="courseType" placeholder="课程类型" clearable style="width:160px;">
          <el-option label="必修" value="必修" />
          <el-option label="选修" value="选修" />
          <el-option label="公共基础" value="公共基础" />
          <el-option label="专业基础" value="专业基础" />
          <el-option label="专业核心" value="专业核心" />
          <el-option label="实践" value="实践" />
        </el-select>
        <el-button type="primary" @click="loadData"><el-icon><Search /></el-icon>搜索</el-button>
        <el-button type="success" @click="openAdd"><el-icon><Plus /></el-icon>新增课程</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe style="width:100%;" max-height="520">
        <el-table-column prop="courseCode" label="课程编号" width="130" />
        <el-table-column prop="courseName" label="课程名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="credit" label="学分" width="70" align="center" />
        <el-table-column prop="courseType" label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.courseType)" size="small">{{ row.courseType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="courseCategory" label="课程类别" width="160" show-overflow-tooltip />
        <el-table-column prop="department" label="开课学院" width="120" />
        <el-table-column prop="semester" label="建议学期" width="100" align="center" />
        <el-table-column prop="campus" label="校区" width="80" align="center" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        style="margin-top:16px;justify-content:flex-end;display:flex;"
        v-model:current-page="pageNum" v-model:page-size="pageSize"
        :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next"
        @size-change="loadData" @current-change="loadData" />
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑课程' : '新增课程'" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="课程编号"><el-input v-model="form.courseCode" /></el-form-item>
        <el-form-item label="课程名称"><el-input v-model="form.courseName" /></el-form-item>
        <el-form-item label="学分"><el-input-number v-model="form.credit" :min="0" :step="0.5" /></el-form-item>
        <el-form-item label="课程类型">
          <el-select v-model="form.courseType" style="width:100%;">
            <el-option v-for="t in ['必修','选修','公共基础','专业基础','专业核心','实践']" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="课程类别"><el-input v-model="form.courseCategory" placeholder="如: 新文科/新工科" /></el-form-item>
        <el-form-item label="开课学院"><el-input v-model="form.department" /></el-form-item>
        <el-form-item label="建议学期"><el-input v-model="form.semester" placeholder="如: 第1学期" /></el-form-item>
        <el-form-item label="先修课程"><el-input v-model="form.prerequisites" placeholder="课程编号，逗号分隔" /></el-form-item>
        <el-form-item label="课程描述"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { courseApi } from '../api'

const tableData = ref([])
const loading = ref(false)
const keyword = ref('')
const courseType = ref('')
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref({})

const typeTag = (t) => ({ '必修': 'danger', '选修': 'success', '公共基础': '', '专业基础': 'warning', '专业核心': 'danger', '实践': 'info' }[t] || '')

const loadData = async () => {
  loading.value = true
  try {
    const res = await courseApi.page({ pageNum: pageNum.value, pageSize: pageSize.value, keyword: keyword.value, courseType: courseType.value })
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}

const openAdd = () => { isEdit.value = false; form.value = { credit: 2, courseType: '选修' }; dialogVisible.value = true }
const openEdit = (row) => { isEdit.value = true; form.value = { ...row }; dialogVisible.value = true }

const handleSave = async () => {
  try {
    if (isEdit.value) { await courseApi.update(form.value); ElMessage.success('更新成功') }
    else { await courseApi.add(form.value); ElMessage.success('新增成功') }
    dialogVisible.value = false; loadData()
  } catch (e) { ElMessage.error('操作失败') }
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除课程「${row.courseName}」?`, '提示', { type: 'warning' })
    .then(async () => { await courseApi.delete(row.id); ElMessage.success('删除成功'); loadData() })
    .catch(() => {})
}

onMounted(loadData)
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; margin-bottom: 16px; align-items: center; }
.page-header { margin-bottom: 16px; }
.page-title { font-family: var(--ac-font-serif); font-size: 20px; font-weight: 700; color: var(--ac-primary); letter-spacing: 1px; }
.page-sub { font-size: 12px; color: var(--ac-gray); margin-top: 4px; font-family: 'Georgia', serif; }
</style>
