<template>
  <div class="agent-page">
    <el-row :gutter="16">
      <!-- 左侧聊天区 -->
      <el-col :span="16">
        <el-card shadow="never" class="chat-card">
          <div class="chat-header">
            <el-avatar :size="44" class="agent-avatar">
              <el-icon size="24"><MagicStick /></el-icon>
            </el-avatar>
            <div>
              <div class="chat-title">学业规划 AI 助手</div>
              <div class="chat-sub">Academic AI Assistant · 多工具调用 · 学分计算</div>
            </div>
            <div class="header-badge">
              <span class="badge-dot"></span>
              <span>在线</span>
            </div>
          </div>

          <div class="chat-messages" ref="messagesRef">
            <div v-for="(msg, idx) in messages" :key="idx" class="message-item" :class="msg.role">
              <div v-if="msg.role === 'user'" class="msg-bubble user">{{ msg.content }}</div>
              <div v-else class="msg-bubble agent">
                <div class="agent-answer">{{ msg.answer }}</div>
                <div v-if="msg.toolCalls && msg.toolCalls.length > 0" class="tool-calls">
                  <div class="tool-calls-title">
                    <el-icon><Connection /></el-icon> 工具调用链 ({{ msg.toolCalls.length }}个工具)
                  </div>
                  <div v-for="(tc, i) in msg.toolCalls" :key="i" class="tool-call-item">
                    <el-tag :type="tc.success ? 'success' : 'danger'" size="small" effect="plain">{{ i+1 }}. {{ tc.tool }}</el-tag>
                    <span class="tool-msg">{{ tc.message }}</span>
                  </div>
                </div>
                <div v-if="msg.thoughtChain && msg.thoughtChain.length > 0" class="thought-chain">
                  <el-collapse>
                    <el-collapse-item title="查看 Agent 思考过程">
                      <div v-for="(t, i) in msg.thoughtChain" :key="i" class="thought-item">{{ t }}</div>
                    </el-collapse-item>
                  </el-collapse>
                </div>
              </div>
            </div>
            <div v-if="loading" class="message-item agent">
              <div class="msg-bubble agent typing">
                <el-icon class="is-loading"><Loading /></el-icon> Agent 正在调用工具分析...
              </div>
            </div>
          </div>

          <div class="chat-input">
            <el-input v-model="inputText" placeholder="输入你的问题，如：我还差多少学分才能毕业？" @keyup.enter="sendMessage" :disabled="loading">
              <template #append>
                <el-button type="primary" @click="sendMessage" :disabled="loading || !inputText.trim()">
                  <el-icon><Promotion /></el-icon> 发送
                </el-button>
              </template>
            </el-input>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧栏 -->
      <el-col :span="8">
        <!-- 学生查询 -->
        <el-card shadow="never" style="margin-bottom:16px;">
          <template #header><span style="font-weight:600;"><el-icon><User /></el-icon> 学生查询</span></template>
          <div class="stu-search">
            <el-input
              v-model="searchKeyword"
              placeholder="输入学号或姓名"
              size="default"
              clearable
              @keyup.enter="handleSearch"
            >
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-button type="primary" @click="handleSearch" :icon="Search">查询</el-button>
          </div>
          <div v-if="currentStudent" class="stu-info">
            <div class="stu-info-name">{{ currentStudent.name }}</div>
            <div class="stu-info-no">{{ currentStudent.studentNo }}</div>
            <el-tag size="small" type="info">{{ currentStudent.grade }}{{ currentStudent.major }}</el-tag>
            <div class="stu-info-dept">{{ currentStudent.department }}</div>
          </div>
          <div v-else class="stu-empty">未查询学生时，仅能回答课程查询、闲聊类问题</div>
        </el-card>

        <!-- 快捷问题 -->
        <el-card shadow="never" style="margin-bottom:16px;">
          <template #header><span style="font-weight:600;"><el-icon><Lightning /></el-icon> 快捷问题</span></template>
          <div class="quick-questions">
            <el-button v-for="(q, i) in quickQuestions" :key="i" class="quick-btn" @click="useQuestion(q)" :disabled="loading">
              {{ q }}
            </el-button>
          </div>
        </el-card>

        <!-- 工具说明 -->
        <el-card shadow="never">
          <template #header><span style="font-weight:600;"><el-icon><Tools /></el-icon> Agent 工具说明</span></template>
          <div class="tool-desc-list">
            <div v-for="tool in toolDescs" :key="tool.name" class="tool-desc-item">
              <div class="tool-desc-name"><el-icon color="#409eff"><Cpu /></el-icon> {{ tool.name }}</div>
              <div class="tool-desc-text">{{ tool.desc }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, User } from '@element-plus/icons-vue'
import { agentApi, studentApi } from '../api'

const students = ref([])
const searchKeyword = ref('')
const currentStudent = ref(null)
const messages = ref([])
const inputText = ref('')
const loading = ref(false)
const messagesRef = ref(null)

const quickQuestions = [
  '帮我看看我现在已修学分还差多少才能毕业，哪些必修课还没选？',
  '大三软件工程，下一学期怎么选课，兼顾考研和项目实践？',
  '我高数挂科了，哪些选修课可以弥补学分缺口？',
  '我的GPA是多少？各类型学分完成情况怎么样？',
  '有哪些选修课可以选？推荐一些新文科类的课程'
]

const toolDescs = [
  { name: 'student_info_query', desc: '查询学生基本信息、专业、年级、班级' },
  { name: 'course_query', desc: '课程库查询、类型筛选、先修依赖检查' },
  { name: 'credit_analysis', desc: '多表联合+数学计算：已修学分、毕业要求、未修必修课、GPA、挂科分析' },
  { name: 'course_recommendation', desc: '基于学分约束+先修依赖+侧重点（考研/实践），推理生成选课方案' },
  { name: 'credit_recovery', desc: '挂科记录分析+学分缺口计算+选修课匹配+弥补方案生成' }
]

/** 根据学号或姓名查询学生 */
const handleSearch = () => {
  const kw = searchKeyword.value.trim()
  if (!kw) { ElMessage.warning('请输入学号或姓名'); return }
  const found = students.value.find(s =>
    (s.studentNo && s.studentNo.includes(kw)) ||
    (s.name && s.name.includes(kw))
  )
  if (!found) {
    ElMessage.warning('未找到匹配的学生')
    currentStudent.value = null
    return
  }
  currentStudent.value = found
  ElMessage.success(`已切换到学生：${found.name}`)
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

const sendMessage = async () => {
  const text = inputText.value.trim()
  if (!text || loading.value) return

  messages.value.push({ role: 'user', content: text })
  inputText.value = ''
  loading.value = true
  scrollToBottom()

  try {
    const res = await agentApi.chat({ question: text, studentId: currentStudent.value?.id || null })
    const data = res.data
    messages.value.push({
      role: 'agent',
      answer: data.answer,
      toolCalls: data.toolCalls,
      thoughtChain: data.thoughtChain
    })
  } catch (e) {
    messages.value.push({ role: 'agent', answer: '抱歉，处理你的问题时出现了错误，请稍后重试。' })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

const useQuestion = (q) => {
  if (loading.value) return
  if (!currentStudent.value) {
    ElMessage.warning('请选中具体要询问的同学')
    messages.value.push({
      role: 'agent',
      answer: '请先在右侧「学生查询」中选中具体要询问的同学（输入学号或姓名后点击「查询」），再点击快捷问题，我才能针对该同学分析学分、成绩和选课情况。',
      toolCalls: [],
      thoughtChain: []
    })
    scrollToBottom()
    return
  }
  inputText.value = q
  sendMessage()
}

onMounted(async () => {
  const res = await studentApi.list()
  students.value = res.data || []

  messages.value.push({
    role: 'agent',
    answer: '你好！我是学业规划 AI 助手 🎓\n\n我可以帮你：\n1. 📊 分析学分进度，计算还差多少学分毕业\n2. 📚 智能推荐选课方案（兼顾考研/项目实践）\n3. 🔧 挂科学分弥补方案\n4. 📖 课程查询和先修依赖分析\n\n请先在右侧「学生查询」中输入学号或姓名查询学生，然后点击快捷问题或直接输入你的问题！',
    toolCalls: [],
    thoughtChain: []
  })
})
</script>

<style scoped>
.agent-page { height: calc(100vh - 110px); }
.chat-card { height: 100%; display: flex; flex-direction: column; border-radius: 10px; }
.chat-card :deep(.el-card__body) { display: flex; flex-direction: column; height: 100%; padding: 0; }
.chat-header {
  display: flex; align-items: center; gap: 14px;
  padding: 18px 24px;
  border-bottom: 1px solid var(--ac-border-warm);
  background: linear-gradient(135deg, #faf8f5, #fff);
}
.agent-avatar {
  background: linear-gradient(135deg, var(--ac-primary), var(--ac-primary-light));
  color: #fff;
  box-shadow: 0 2px 8px rgba(26,54,93,0.3);
}
.chat-title {
  font-family: var(--ac-font-serif);
  font-weight: 700; font-size: 17px;
  color: var(--ac-primary);
  letter-spacing: 1px;
}
.chat-sub { font-size: 11px; color: var(--ac-gray); margin-top: 3px; font-family: 'Georgia', serif; }
.header-badge {
  margin-left: auto;
  display: flex; align-items: center; gap: 6px;
  font-size: 12px; color: var(--ac-success);
  background: rgba(45,106,79,0.08);
  padding: 4px 12px; border-radius: 20px;
}
.badge-dot { width: 6px; height: 6px; background: var(--ac-success); border-radius: 50%; }

.chat-messages { flex: 1; overflow-y: auto; padding: 24px; background: #faf8f5; }
.message-item { margin-bottom: 20px; display: flex; }
.message-item.user { justify-content: flex-end; }
.msg-bubble {
  max-width: 82%; padding: 14px 18px;
  border-radius: 12px; line-height: 1.7;
  white-space: pre-wrap; word-break: break-word;
  font-size: 14px;
}
.msg-bubble.user {
  background: var(--ac-primary);
  color: #fff;
  border-bottom-right-radius: 4px;
  box-shadow: 0 2px 8px rgba(26,54,93,0.2);
}
.msg-bubble.agent {
  background: #fff;
  border: 1px solid var(--ac-border-warm);
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}
.agent-answer { color: var(--ac-ink); }
.typing { color: var(--ac-gray); display: flex; align-items: center; gap: 8px; }

.tool-calls { margin-top: 14px; padding-top: 12px; border-top: 1px dashed var(--ac-border-warm); }
.tool-calls-title {
  font-size: 12px; font-weight: 600;
  color: var(--ac-gold); margin-bottom: 8px;
  display: flex; align-items: center; gap: 6px;
  font-family: var(--ac-font-serif);
}
.tool-call-item { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; font-size: 12px; }
.tool-msg { color: var(--ac-gray); flex: 1; }
.thought-chain { margin-top: 10px; }
.thought-item { font-size: 12px; color: #a0aec0; padding: 2px 0; font-family: monospace; }

.chat-input { padding: 16px 24px; border-top: 1px solid var(--ac-border-warm); background: #fff; }

/* 学生查询卡片 */
.stu-search { display: flex; gap: 8px; margin-bottom: 12px; }
.stu-info {
  padding: 14px;
  background: linear-gradient(135deg, rgba(26,54,93,0.04), rgba(176,141,87,0.06));
  border-radius: 8px;
  border: 1px solid rgba(26,54,93,0.1);
  text-align: center;
}
.stu-info-name { font-family: var(--ac-font-serif); font-size: 17px; font-weight: 700; color: var(--ac-primary); }
.stu-info-no { font-size: 13px; color: var(--ac-ink-light); font-family: monospace; margin: 3px 0 6px; }
.stu-info-dept { font-size: 12px; color: var(--ac-gray); margin-top: 6px; }
.stu-empty { font-size: 12px; color: #a0aec0; text-align: center; padding: 8px 0; }

.quick-questions { display: flex; flex-direction: column; gap: 8px; }
.quick-btn {
  width: 100%; text-align: left; justify-content: flex-start;
  white-space: normal; height: auto; padding: 10px 14px;
  font-size: 13px; border-radius: 8px;
  border: 1px solid var(--ac-border-warm);
  background: #faf8f5;
  transition: all 0.2s;
}
.quick-btn:hover {
  background: rgba(26,54,93,0.04);
  border-color: var(--ac-primary-light);
  color: var(--ac-primary);
}

.tool-desc-list { display: flex; flex-direction: column; gap: 10px; }
.tool-desc-item {
  padding: 10px 12px;
  background: #faf8f5;
  border-radius: 6px;
  border-left: 3px solid var(--ac-gold);
}
.tool-desc-name {
  font-weight: 600; font-size: 13px;
  display: flex; align-items: center; gap: 6px;
  margin-bottom: 4px;
  color: var(--ac-primary);
  font-family: 'Consolas', monospace;
}
.tool-desc-text { font-size: 12px; color: var(--ac-gray); line-height: 1.5; }
</style>
