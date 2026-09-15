<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">选课管理</div>
        <div class="page-sub">Enrollment Management · 学生选课与退课管理</div>
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
        <el-button type="success" @click="openEnroll" :disabled="!currentStudent"><el-icon><Plus /></el-icon>选课</el-button>
      </div>

      <!-- 当前学生信息 -->
      <div v-if="currentStudent" class="student-bar">
        <el-icon color="#409eff"><User /></el-icon>
        <span class="stu-name">{{ currentStudent.name }}</span>
        <span class="stu-no">{{ currentStudent.studentNo }}</span>
        <el-tag size="small" type="info">{{ currentStudent.grade }}{{ currentStudent.major }}</el-tag>
        <span class="stu-dept">{{ currentStudent.department }}</span>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe max-height="480" :empty-text="currentStudent ? '暂无选课记录' : '请先查询学生'">
        <el-table-column prop="semester" label="学期" width="140" />
        <el-table-column prop="course_code" label="课程编号" width="130" />
        <el-table-column prop="course_name" label="课程名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="credit" label="学分" width="70" align="center" />
        <el-table-column prop="course_type" label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.course_type === '选修' ? 'success' : 'danger'">{{ row.course_type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '已修完' ? 'success' : row.status === '已退' ? 'info' : 'warning'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="danger" :disabled="row.status === '已退'" @click="handleDrop(row)">退课</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 选课弹窗 -->
    <el-dialog v-model="enrollVisible" title="选课" width="620px">
      <el-form label-width="90px">
        <el-form-item label="学生">
          <el-select v-model="enrollForm.studentId" style="width:100%;" filterable @change="loadEnrolledIds">
            <el-option v-for="s in students" :key="s.id" :label="`${s.name} (${s.studentNo})`" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="课程">
          <CoursePicker
            v-model="enrollForm.courseId"
            :courses="courses"
            :exclude-ids="enrolledCourseIds"
          />
        </el-form-item>
        <el-form-item label="学期"><el-input v-model="enrollForm.semester" placeholder="如: 2024-2025-2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="enrollVisible = false">取消</el-button>
        <el-button type="primary" @click="handleEnroll" :disabled="!enrollForm.courseId">确认选课</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, User } from '@element-plus/icons-vue'
import { enrollmentApi, studentApi, courseApi } from '../api'
import CoursePicker from '../components/CoursePicker.vue'

const students = ref([])
const courses = ref([])
const searchKeyword = ref('')
const currentStudent = ref(null)
const tableData = ref([])
const loading = ref(false)
const enrollVisible = ref(false)
const enrollForm = ref({ studentId: null, courseId: null, semester: '2024-2025-2' })
const enrolledCourseIds = ref([])

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
    return
  }
  currentStudent.value = found
  loadData(found.id)
}

const loadData = async (studentId) => {
  if (!studentId) return
  loading.value = true
  try {
    const res = await enrollmentApi.getByStudent(studentId)
    tableData.value = res.data || []
  } finally { loading.value = false }
}

/** 加载该学生已选课程 ID，用于在选课器中排除 */
const loadEnrolledIds = async (studentId) => {
  if (!studentId) { enrolledCourseIds.value = []; return }
  try {
    const res = await enrollmentApi.getByStudent(studentId)
    enrolledCourseIds.value = (res.data || []).map(e => e.course_id)
  } catch { enrolledCourseIds.value = [] }
}

const openEnroll = async () => {
  if (!currentStudent.value) return
  enrollForm.value.studentId = currentStudent.value.id
  enrollForm.value.courseId = null
  await loadEnrolledIds(currentStudent.value.id)
  enrollVisible.value = true
}

const handleEnroll = async () => {
  try {
    await enrollmentApi.enroll(enrollForm.value)
    ElMessage.success('选课成功')
    enrollVisible.value = false
    loadData(currentStudent.value.id)
  } catch (e) { ElMessage.error(e.response?.data?.message || '选课失败') }
}

const handleDrop = (row) => {
  ElMessageBox.confirm(`确定退选「${row.course_name}」?`, '提示', { type: 'warning' })
    .then(async () => { await enrollmentApi.drop(row.id); ElMessage.success('退课成功'); loadData(currentStudent.value.id) })
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
</style>
