<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">成绩管理</div>
        <div class="page-sub">Grade Management · 成绩录入、查询与学分统计</div>
      </div>
    </div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-input
          v-model="searchKeyword"
          placeholder="输入学号或姓名查询学生"
          style="width:260px;"
          clearable
          @keyup.enter="handleSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon>查询</el-button>
        <el-button type="success" @click="openAdd" :disabled="!currentStudent"><el-icon><Plus /></el-icon>录入成绩</el-button>
      </div>

      <!-- 当前学生信息 -->
      <div v-if="currentStudent" class="student-bar">
        <el-icon color="#409eff"><User /></el-icon>
        <span class="stu-name">{{ currentStudent.name }}</span>
        <span class="stu-no">{{ currentStudent.studentNo }}</span>
        <el-tag size="small" type="info">{{ currentStudent.grade }}{{ currentStudent.major }}</el-tag>
        <span class="stu-dept">{{ currentStudent.department }}</span>
      </div>

      <!-- 学分概览 -->
      <el-row :gutter="12" v-if="currentStudent" style="margin-bottom:16px;">
        <el-col :span="6">
          <el-card class="mini-card" shadow="hover">
            <div class="mini-label">已获学分</div>
            <div class="mini-num green">{{ earnedCredit }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="mini-card" shadow="hover">
            <div class="mini-label">GPA</div>
            <div class="mini-num blue">{{ gpa }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="mini-card" shadow="hover">
            <div class="mini-label">挂科数</div>
            <div class="mini-num red">{{ failedCount }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="mini-card" shadow="hover">
            <div class="mini-label">课程数</div>
            <div class="mini-num">{{ tableData.length }}</div>
          </el-card>
        </el-col>
      </el-row>

      <el-table :data="tableData" v-loading="loading" stripe max-height="420" :empty-text="currentStudent ? '暂无成绩记录' : '请先查询学生'">
        <el-table-column prop="semester" label="学期" width="140" />
        <el-table-column prop="course_code" label="课程编号" width="130" />
        <el-table-column prop="course_name" label="课程名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="credit" label="学分" width="70" align="center" />
        <el-table-column prop="score" label="成绩" width="80" align="center">
          <template #default="{ row }">
            <span :style="{ color: row.is_pass ? '#67c23a' : '#f56c6c', fontWeight: 600 }">{{ row.score }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="grade_level" label="等级" width="80" align="center" />
        <el-table-column prop="credit_earned" label="获得学分" width="90" align="center" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑成绩' : '录入成绩'" width="620px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="学生">
          <el-select v-model="form.studentId" style="width:100%;" filterable @change="onStudentChange">
            <el-option v-for="s in students" :key="s.id" :label="`${s.name} (${s.studentNo})`" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="课程">
          <CoursePicker
            v-model="form.courseId"
            :courses="courses"
            :include-ids="isEdit ? null : enrolledCourseIds"
            :exclude-ids="isEdit ? [] : gradedCourseIds"
          />
          <div v-if="!isEdit && enrolledCourseIds.length === 0" style="font-size:12px;color:#e6a23c;margin-top:4px;">
            该学生暂无选课记录，请先在「选课管理」中选课
          </div>
          <div v-else-if="!isEdit && gradedCourseIds.length >= enrolledCourseIds.length && enrolledCourseIds.length > 0" style="font-size:12px;color:#67c23a;margin-top:4px;">
            该学生所有已选课程均已录入成绩，如需修改请在列表中点击「编辑」
          </div>
        </el-form-item>
        <el-form-item label="成绩"><el-input-number v-model="form.score" :min="0" :max="100" :step="0.5" /></el-form-item>
        <el-form-item label="学期"><el-input v-model="form.semester" placeholder="如: 2024-2025-1" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :disabled="!form.courseId">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, User } from '@element-plus/icons-vue'
import { gradeApi, studentApi, courseApi, enrollmentApi } from '../api'
import CoursePicker from '../components/CoursePicker.vue'

const students = ref([])
const courses = ref([])
const searchKeyword = ref('')
const currentStudent = ref(null)
const tableData = ref([])
const loading = ref(false)
const earnedCredit = ref(0)
const gpa = ref(0)
const failedCount = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref({})
const enrolledCourseIds = ref([])
const gradedCourseIds = ref([])

/** 根据学号或姓名查询学生 */
const handleSearch = () => {
  const kw = searchKeyword.value.trim()
  if (!kw) { ElMessage.warning('请输入学号或姓名'); return }
  const found = students.value.find(s =>
    (s.studentNo && s.studentNo.includes(kw)) ||
    (s.name && s.name.includes(kw))
  )
  if (!found) {
    ElMessage.warning('未找到匹配的学生，请检查学号或姓名')
    currentStudent.value = null
    tableData.value = []
    earnedCredit.value = 0
    gpa.value = 0
    failedCount.value = 0
    return
  }
  currentStudent.value = found
  loadAll(found.id)
}

const loadAll = async (studentId) => {
  if (!studentId) return
  loading.value = true
  try {
    const [grades, credit, gpaRes, failed] = await Promise.all([
      gradeApi.getByStudent(studentId),
      gradeApi.earnedCredit(studentId),
      gradeApi.gpa(studentId),
      gradeApi.failed(studentId)
    ])
    tableData.value = grades.data || []
    earnedCredit.value = credit.data || 0
    gpa.value = gpaRes.data || 0
    failedCount.value = (failed.data || []).length
  } finally { loading.value = false }
}

/** 加载该学生已选课程 ID（录入成绩时只显示已选课程） */
const loadEnrolledIds = async (studentId) => {
  if (!studentId) { enrolledCourseIds.value = []; return }
  try {
    const res = await enrollmentApi.getByStudent(studentId)
    enrolledCourseIds.value = (res.data || []).map(e => e.course_id)
  } catch { enrolledCourseIds.value = [] }
}

/** 加载该学生已录入成绩的课程 ID（录入成绩时排除，防止重复） */
const loadGradedIds = async (studentId) => {
  if (!studentId) { gradedCourseIds.value = []; return }
  try {
    const res = await gradeApi.getByStudent(studentId)
    gradedCourseIds.value = (res.data || []).map(g => g.course_id)
  } catch { gradedCourseIds.value = [] }
}

const onStudentChange = async (studentId) => {
  if (!isEdit.value) {
    form.value.courseId = null
    await Promise.all([loadEnrolledIds(studentId), loadGradedIds(studentId)])
  }
}

const openAdd = async () => {
  if (!currentStudent.value) return
  isEdit.value = false
  form.value = { studentId: currentStudent.value.id, score: 80, semester: '2024-2025-2' }
  await Promise.all([loadEnrolledIds(currentStudent.value.id), loadGradedIds(currentStudent.value.id)])
  dialogVisible.value = true
}

const openEdit = (row) => {
  isEdit.value = true
  form.value = { ...row }
  enrolledCourseIds.value = [] // 编辑模式显示全部课程
  gradedCourseIds.value = []
  dialogVisible.value = true
}

const handleSave = async () => {
  try {
    if (isEdit.value) { await gradeApi.update(form.value); ElMessage.success('更新成功') }
    else { await gradeApi.add(form.value); ElMessage.success('录入成功') }
    dialogVisible.value = false
    if (currentStudent.value) loadAll(currentStudent.value.id)
  } catch (e) { ElMessage.error(e.response?.data?.message || '操作失败') }
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确定删除该成绩记录?', '提示', { type: 'warning' })
    .then(async () => {
      await gradeApi.delete(row.id)
      ElMessage.success('删除成功')
      if (currentStudent.value) loadAll(currentStudent.value.id)
    })
    .catch(() => {})
}

onMounted(async () => {
  const [sRes, cRes] = await Promise.all([studentApi.list(), courseApi.list()])
  students.value = sRes.data || []
  courses.value = cRes.data || []
})
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; margin-bottom: 12px; align-items: center; }
.student-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 18px;
  margin-bottom: 14px;
  background: linear-gradient(135deg, rgba(26,54,93,0.04), rgba(176,141,87,0.06));
  border-radius: 8px;
  border: 1px solid rgba(26,54,93,0.1);
  border-left: 4px solid var(--ac-gold);
}
.stu-name { font-family: var(--ac-font-serif); font-size: 16px; font-weight: 700; color: var(--ac-primary); }
.stu-no { font-size: 13px; color: var(--ac-ink-light); font-family: monospace; }
.stu-dept { font-size: 12px; color: var(--ac-gray); margin-left: auto; }
.page-header { margin-bottom: 16px; }
.page-title { font-family: var(--ac-font-serif); font-size: 20px; font-weight: 700; color: var(--ac-primary); letter-spacing: 1px; }
.page-sub { font-size: 12px; color: var(--ac-gray); margin-top: 4px; font-family: 'Georgia', serif; }
.mini-card { border-radius: 8px; border: 1px solid var(--ac-border-warm); }
.mini-card :deep(.el-card__body) { padding: 18px; text-align: center; }
.mini-label { font-size: 13px; color: var(--ac-gray); font-family: var(--ac-font-serif); }
.mini-num { font-family: var(--ac-font-num); font-size: 28px; font-weight: 700; margin-top: 6px; }
.mini-num.green { color: var(--ac-success); }
.mini-num.blue { color: var(--ac-primary); }
.mini-num.red { color: var(--ac-danger); }
</style>
