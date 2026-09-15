import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 60000
})

api.interceptors.response.use(
  res => res.data,
  err => Promise.reject(err)
)

// 课程 API
export const courseApi = {
  page: (params) => api.get('/courses/page', { params }),
  list: () => api.get('/courses/list'),
  get: (id) => api.get(`/courses/${id}`),
  add: (data) => api.post('/courses', data),
  update: (data) => api.put('/courses', data),
  delete: (id) => api.delete(`/courses/${id}`),
  required: () => api.get('/courses/required'),
  elective: () => api.get('/courses/elective')
}

// 学生 API
export const studentApi = {
  page: (params) => api.get('/students/page', { params }),
  list: () => api.get('/students/list'),
  get: (id) => api.get(`/students/${id}`),
  getByNo: (no) => api.get(`/students/no/${no}`),
  add: (data) => api.post('/students', data),
  update: (data) => api.put('/students', data),
  delete: (id) => api.delete(`/students/${id}`)
}

// 选课 API
export const enrollmentApi = {
  getByStudent: (studentId) => api.get(`/enrollments/student/${studentId}`),
  getSemesters: (studentId) => api.get(`/enrollments/student/${studentId}/semesters`),
  enroll: (data) => api.post('/enrollments/enroll', null, { params: data }),
  drop: (id) => api.post(`/enrollments/drop/${id}`),
  add: (data) => api.post('/enrollments', data),
  update: (data) => api.put('/enrollments', data),
  delete: (id) => api.delete(`/enrollments/${id}`)
}

// 成绩 API
export const gradeApi = {
  getByStudent: (studentId) => api.get(`/grades/student/${studentId}`),
  earnedCredit: (studentId) => api.get(`/grades/student/${studentId}/earned-credit`),
  creditByType: (studentId) => api.get(`/grades/student/${studentId}/credit-by-type`),
  gpa: (studentId) => api.get(`/grades/student/${studentId}/gpa`),
  failed: (studentId) => api.get(`/grades/student/${studentId}/failed`),
  semesterStats: (studentId) => api.get(`/grades/student/${studentId}/semester-stats`),
  add: (data) => api.post('/grades', data),
  update: (data) => api.put('/grades', data),
  delete: (id) => api.delete(`/grades/${id}`)
}

// 毕业要求 API
export const graduationApi = {
  requirement: (params) => api.get('/graduation/requirement', { params }),
  list: () => api.get('/graduation/list'),
  analyze: (studentId) => api.get(`/graduation/analyze/${studentId}`),
  missingRequired: (studentId) => api.get(`/graduation/missing-required/${studentId}`),
  recommend: (studentId, params) => api.get(`/graduation/recommend/${studentId}`, { params }),
  recovery: (studentId) => api.get(`/graduation/recovery/${studentId}`)
}

// AI Agent API
export const agentApi = {
  chat: (data) => api.post('/agent/chat', data),
  tools: () => api.get('/agent/tools'),
  examples: () => api.get('/agent/examples')
}

export default api
