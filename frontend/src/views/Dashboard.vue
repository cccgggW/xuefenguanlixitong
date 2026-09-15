<template>
  <div class="dashboard">
    <!-- 欢迎横幅 -->
    <div class="welcome-banner">
      <div class="banner-left">
        <div class="banner-title">学业规划数据概览</div>
        <div class="banner-sub">Academic Planning Dashboard · 实时掌握学业进度与资源分布</div>
      </div>
      <div class="banner-right">
        <el-icon size="40" class="banner-icon"><School /></el-icon>
      </div>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon-wrap">
            <el-icon size="24"><Reading /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-num">{{ stats.courseCount }}</div>
            <div class="stat-label">课程总数</div>
            <div class="stat-en">Courses</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon-wrap green">
            <el-icon size="24"><User /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-num">{{ stats.studentCount }}</div>
            <div class="stat-label">学生总数</div>
            <div class="stat-en">Students</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon-wrap gold">
            <el-icon size="24"><Tickets /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-num">{{ stats.enrollmentCount }}</div>
            <div class="stat-label">选课记录</div>
            <div class="stat-en">Enrollments</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon-wrap purple">
            <el-icon size="24"><MagicStick /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-num">160</div>
            <div class="stat-label">毕业学分要求</div>
            <div class="stat-en">Required Credits</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="16" style="margin-top: 16px;">
      <el-col :span="12">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">课程类型分布</span>
              <span class="card-en">Course Distribution</span>
            </div>
          </template>
          <div ref="typeChart" style="height: 280px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">学分完成情况</span>
              <span class="card-en">Credit Progress</span>
            </div>
          </template>
          <div ref="creditChart" style="height: 280px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px;">
      <el-col :span="12">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">各学期学分获取趋势</span>
              <span class="card-en">Semester Trend</span>
            </div>
          </template>
          <div ref="semesterChart" style="height: 280px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">AI Agent 能力说明</span>
              <span class="card-en">Agent Capabilities</span>
            </div>
          </template>
          <div class="agent-info">
            <div class="agent-item" v-for="tool in agentTools" :key="tool.name">
              <div class="agent-dot"></div>
              <div>
                <div class="agent-name">{{ tool.name }}</div>
                <div class="agent-desc">{{ tool.desc }}</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import * as echarts from 'echarts'
import { courseApi, studentApi } from '../api'

const stats = ref({ courseCount: 0, studentCount: 0, enrollmentCount: 0 })
const typeChart = ref(null)
const creditChart = ref(null)
const semesterChart = ref(null)

const agentTools = [
  { name: 'student_info_query', desc: '查询学生基本信息、专业、年级' },
  { name: 'course_query', desc: '课程库查询、类型筛选、先修依赖检查' },
  { name: 'credit_analysis', desc: '多表联合+数学计算：学分进度、毕业要求、未修必修课' },
  { name: 'course_recommendation', desc: '学分约束+先修依赖+侧重点推理，生成选课方案' },
  { name: 'credit_recovery', desc: '挂科记录分析+选修课匹配+弥补方案生成' }
]

const academicColors = ['#1a365d', '#b08d57', '#2d6a4f', '#6b46c1', '#c53030']

onMounted(async () => {
  try {
    const [courses, students] = await Promise.all([
      courseApi.list(), studentApi.list()
    ])
    stats.value.courseCount = courses.data?.length || 0
    stats.value.studentCount = students.data?.length || 0
  } catch (e) { console.error(e) }

  // 课程类型分布图 - 学术风配色
  const typeChartInst = echarts.init(typeChart.value)
  typeChartInst.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c}门 ({d}%)' },
    legend: { bottom: 0, textStyle: { color: '#4a5568', fontSize: 12 } },
    color: ['#1a365d', '#b08d57'],
    series: [{
      type: 'pie', radius: ['45%', '70%'],
      avoidLabelOverlap: true,
      itemStyle: { borderColor: '#fff', borderWidth: 2 },
      label: { formatter: '{b}\n{c}门', fontSize: 12, color: '#4a5568' },
      data: [
        { value: 37, name: '必修/公共基础' },
        { value: 407, name: '选修(个性化课程)' }
      ]
    }]
  })

  // 学分完成情况图
  const creditChartInst = echarts.init(creditChart.value)
  creditChartInst.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['已获得', '毕业要求'], bottom: 0, textStyle: { color: '#4a5568' } },
    grid: { left: 50, right: 20, top: 20, bottom: 40 },
    xAxis: { type: 'category', data: ['总学分', '必修', '选修', '公共基础', '专业核心'], axisLabel: { color: '#718096' } },
    yAxis: { type: 'value', name: '学分', axisLabel: { color: '#718096' }, splitLine: { lineStyle: { color: '#edf2f7' } } },
    series: [
      { name: '已获得', type: 'bar', data: [72.5, 55, 0, 35, 0], itemStyle: { color: '#2d6a4f', borderRadius: [4,4,0,0] }, barWidth: 18 },
      { name: '毕业要求', type: 'bar', data: [160, 120, 20, 50, 30], itemStyle: { color: '#cbd5e0', borderRadius: [4,4,0,0] }, barWidth: 18 }
    ]
  })

  // 学期趋势图
  const semesterChartInst = echarts.init(semesterChart.value)
  semesterChartInst.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, top: 20, bottom: 40 },
    xAxis: { type: 'category', data: ['第1学期', '第2学期', '第3学期', '第4学期'], axisLabel: { color: '#718096' } },
    yAxis: { type: 'value', name: '学分', axisLabel: { color: '#718096' }, splitLine: { lineStyle: { color: '#edf2f7' } } },
    series: [{
      type: 'line', smooth: true, symbol: 'circle', symbolSize: 8,
      data: [22, 24.5, 14.5, 11.5],
      lineStyle: { color: '#1a365d', width: 2.5 },
      itemStyle: { color: '#b08d57', borderColor: '#1a365d', borderWidth: 2 },
      areaStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'rgba(26,54,93,0.15)'},{offset:1,color:'rgba(26,54,93,0.02)'}]) }
    }]
  })
})
</script>

<style scoped>
.welcome-banner {
  background: linear-gradient(135deg, #1a365d 0%, #2c5282 100%);
  border-radius: 10px;
  padding: 24px 28px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: relative;
  overflow: hidden;
}
.welcome-banner::before {
  content: '';
  position: absolute;
  top: -50%; right: -10%;
  width: 300px; height: 300px;
  background: radial-gradient(circle, rgba(176,141,87,0.2), transparent 70%);
  border-radius: 50%;
}
.banner-title {
  font-family: var(--ac-font-serif);
  font-size: 22px; font-weight: 700; color: #fff;
  letter-spacing: 2px;
}
.banner-sub {
  font-size: 12px; color: rgba(255,255,255,0.6);
  margin-top: 6px;
  font-family: 'Georgia', serif;
  letter-spacing: 0.5px;
}
.banner-icon { color: rgba(176,141,87,0.6); }

.stat-row { margin-bottom: 0; }
.stat-card {
  background: #fff;
  border: 1px solid var(--ac-border-warm);
  border-radius: 10px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all 0.3s;
  position: relative;
  overflow: hidden;
}
.stat-card::before {
  content: '';
  position: absolute;
  top: 0; left: 0;
  width: 4px; height: 100%;
  background: var(--ac-primary);
}
.stat-card:hover {
  box-shadow: 0 4px 16px rgba(26,54,93,0.1);
  transform: translateY(-2px);
}
.stat-icon-wrap {
  width: 52px; height: 52px;
  background: rgba(26,54,93,0.08);
  border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  color: var(--ac-primary);
  flex-shrink: 0;
}
.stat-icon-wrap.green { background: rgba(45,106,79,0.08); color: var(--ac-success); }
.stat-icon-wrap.gold { background: rgba(176,141,87,0.1); color: var(--ac-gold); }
.stat-icon-wrap.purple { background: rgba(107,70,193,0.08); color: #6b46c1; }
.stat-num {
  font-family: var(--ac-font-num);
  font-size: 30px; font-weight: 700;
  color: var(--ac-primary);
  line-height: 1.2;
}
.stat-label { font-size: 13px; color: var(--ac-ink-light); margin-top: 2px; }
.stat-en { font-size: 10px; color: #a0aec0; font-family: 'Georgia', serif; letter-spacing: 1px; }

.chart-card { border-radius: 10px; }
.card-header { display: flex; align-items: baseline; gap: 10px; }
.card-title { font-family: var(--ac-font-serif); font-weight: 600; color: var(--ac-primary); font-size: 15px; }
.card-en { font-size: 11px; color: #a0aec0; font-family: 'Georgia', serif; }

.agent-info { display: flex; flex-direction: column; gap: 10px; }
.agent-item {
  display: flex; align-items: flex-start; gap: 10px;
  padding: 10px 12px;
  background: #faf8f5;
  border-radius: 6px;
  border-left: 3px solid var(--ac-gold);
}
.agent-dot {
  width: 8px; height: 8px;
  background: var(--ac-primary);
  border-radius: 50%;
  margin-top: 6px;
  flex-shrink: 0;
}
.agent-name { font-weight: 600; font-size: 13px; color: var(--ac-primary); font-family: 'Consolas', monospace; }
.agent-desc { font-size: 12px; color: var(--ac-gray); margin-top: 2px; line-height: 1.5; }
</style>
