import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', component: () => import('../views/Dashboard.vue'), meta: { title: '数据概览' } },
  { path: '/courses', component: () => import('../views/CourseManage.vue'), meta: { title: '课程管理' } },
  { path: '/students', component: () => import('../views/StudentManage.vue'), meta: { title: '学生管理' } },
  { path: '/enrollments', component: () => import('../views/EnrollmentManage.vue'), meta: { title: '选课管理' } },
  { path: '/grades', component: () => import('../views/GradeManage.vue'), meta: { title: '成绩管理' } },
  { path: '/agent', component: () => import('../views/AgentChat.vue'), meta: { title: 'AI 智能问答' } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
