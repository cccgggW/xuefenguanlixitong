<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">学生管理</div>
        <div class="page-sub">Student Management · 学籍信息维护与学生档案管理</div>
      </div>
    </div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索姓名/学号" clearable style="width:220px;" @keyup.enter="loadData" />
        <el-input v-model="major" placeholder="专业" clearable style="width:160px;" @keyup.enter="loadData" />
        <el-button type="primary" @click="loadData"><el-icon><Search /></el-icon>搜索</el-button>
        <el-button type="success" @click="openAdd"><el-icon><Plus /></el-icon>新增学生</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe max-height="520">
        <el-table-column prop="studentNo" label="学号" width="130" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="gender" label="性别" width="70" align="center" />
        <el-table-column prop="grade" label="年级" width="90" align="center" />
        <el-table-column prop="major" label="专业" width="160" />
        <el-table-column prop="department" label="院系" width="120" />
        <el-table-column prop="className" label="班级" width="120" />
        <el-table-column prop="phone" label="电话" width="130" />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '在读' ? 'success' : 'info'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination style="margin-top:16px;justify-content:flex-end;display:flex;"
        v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total"
        :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next"
        @size-change="loadData" @current-change="loadData" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑学生' : '新增学生'" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="学号"><el-input v-model="form.studentNo" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender"><el-radio value="男">男</el-radio><el-radio value="女">女</el-radio></el-radio-group>
        </el-form-item>
        <el-form-item label="年级"><el-input v-model="form.grade" placeholder="如: 2023级" /></el-form-item>
        <el-form-item label="专业"><el-input v-model="form.major" /></el-form-item>
        <el-form-item label="院系"><el-input v-model="form.department" /></el-form-item>
        <el-form-item label="班级"><el-input v-model="form.className" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
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
import { studentApi } from '../api'

const tableData = ref([])
const loading = ref(false)
const keyword = ref('')
const major = ref('')
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref({})

const loadData = async () => {
  loading.value = true
  try {
    const res = await studentApi.page({ pageNum: pageNum.value, pageSize: pageSize.value, keyword: keyword.value, major: major.value })
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}

const openAdd = () => { isEdit.value = false; form.value = { gender: '男', status: '在读' }; dialogVisible.value = true }
const openEdit = (row) => { isEdit.value = true; form.value = { ...row }; dialogVisible.value = true }

const handleSave = async () => {
  try {
    if (isEdit.value) { await studentApi.update(form.value); ElMessage.success('更新成功') }
    else { await studentApi.add(form.value); ElMessage.success('新增成功') }
    dialogVisible.value = false; loadData()
  } catch (e) { ElMessage.error('操作失败') }
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除学生「${row.name}」?`, '提示', { type: 'warning' })
    .then(async () => { await studentApi.delete(row.id); ElMessage.success('删除成功'); loadData() })
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
