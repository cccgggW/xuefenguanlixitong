<template>
  <div class="course-picker">
    <!-- 筛选栏：类型 + 关键词 -->
    <div class="cp-toolbar">
      <el-radio-group v-model="filterType" size="small" @change="currentPage = 1">
        <el-radio-button label="">全部</el-radio-button>
        <el-radio-button v-for="t in allTypes" :key="t" :label="t">{{ t }}</el-radio-button>
      </el-radio-group>
      <el-input
        v-model="keyword"
        placeholder="搜索课程名 / 编号"
        clearable
        size="small"
        style="width: 180px"
        @input="currentPage = 1"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
    </div>

    <!-- 课程列表 -->
    <div class="cp-list">
      <div v-if="filtered.length === 0" class="cp-empty">
        <el-icon size="28" color="#ccc"><Document /></el-icon>
        <p>没有匹配的课程</p>
      </div>
      <div
        v-for="c in paged"
        :key="c.id"
        class="cp-item"
        :class="{ selected: modelValue === c.id }"
        @click="handleSelect(c)"
      >
        <div class="cp-item-top">
          <span class="cp-name">{{ c.courseName }}</span>
          <el-tag size="small" :type="tagType(c.courseType)" effect="plain">{{ c.courseType }}</el-tag>
        </div>
        <div class="cp-item-bottom">
          <span class="cp-code">{{ c.courseCode }}</span>
          <span class="cp-credit">{{ c.credit }}学分</span>
          <span v-if="c.courseCategory" class="cp-cat">{{ c.courseCategory }}</span>
        </div>
        <el-icon v-if="modelValue === c.id" class="cp-check"><CircleCheckFilled /></el-icon>
      </div>
    </div>

    <!-- 分页 -->
    <div class="cp-footer">
      <span class="cp-count">共 {{ filtered.length }} 门课程</span>
      <el-pagination
        small
        layout="prev, pager, next"
        :total="filtered.length"
        :page-size="pageSize"
        v-model:current-page="currentPage"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Search, Document, CircleCheckFilled } from '@element-plus/icons-vue'

const props = defineProps({
  /** 当前选中的课程 ID（v-model） */
  modelValue: { type: [Number, String], default: null },
  /** 全部课程数据 */
  courses: { type: Array, default: () => [] },
  /** 排除的课程 ID 列表（如已选过的课） */
  excludeIds: { type: Array, default: () => [] },
  /** 仅显示的课程 ID 列表（如仅已选课程）；null 表示不限制 */
  includeIds: { type: Array, default: null },
  /** 每页显示数量 */
  pageSize: { type: Number, default: 8 }
})

const emit = defineEmits(['update:modelValue', 'change'])

const filterType = ref('')
const keyword = ref('')
const currentPage = ref(1)

/** 从课程数据中提取所有课程类型 */
const allTypes = computed(() => {
  const set = new Set()
  props.courses.forEach(c => { if (c.courseType) set.add(c.courseType) })
  return Array.from(set).sort()
})

/** 筛选后的课程列表 */
const filtered = computed(() => {
  let list = props.courses

  // includeIds 限制（仅显示指定课程）
  if (props.includeIds && props.includeIds.length > 0) {
    list = list.filter(c => props.includeIds.includes(c.id))
  }
  // excludeIds 排除
  if (props.excludeIds && props.excludeIds.length > 0) {
    list = list.filter(c => !props.excludeIds.includes(c.id))
  }
  // 类型筛选
  if (filterType.value) {
    list = list.filter(c => c.courseType === filterType.value)
  }
  // 关键词搜索
  if (keyword.value.trim()) {
    const kw = keyword.value.trim().toLowerCase()
    list = list.filter(c =>
      (c.courseName && c.courseName.toLowerCase().includes(kw)) ||
      (c.courseCode && c.courseCode.toLowerCase().includes(kw))
    )
  }
  return list
})

/** 当前页课程 */
const paged = computed(() => {
  const start = (currentPage.value - 1) * props.pageSize
  return filtered.value.slice(start, start + props.pageSize)
})

const handleSelect = (course) => {
  emit('update:modelValue', course.id)
  emit('change', course)
}

const tagType = (type) => {
  const map = {
    '必修': 'danger',
    '选修': 'success',
    '公共基础': 'info',
    '专业基础': 'warning',
    '专业核心': 'danger',
    '实践': 'warning'
  }
  return map[type] || 'info'
}
</script>

<style scoped>
.course-picker {
  border: 1px solid var(--ac-border-warm);
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}
.cp-toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 10px 12px;
  background: #faf8f5;
  border-bottom: 1px solid var(--ac-border-warm);
  flex-wrap: wrap;
}
.cp-list {
  max-height: 280px;
  overflow-y: auto;
  padding: 6px;
}
.cp-empty {
  text-align: center;
  padding: 32px 0;
  color: var(--ac-gray);
}
.cp-empty p { margin: 8px 0 0; font-size: 13px; }
.cp-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;
  position: relative;
  border: 1px solid transparent;
}
.cp-item:hover { background: rgba(26,54,93,0.04); }
.cp-item.selected {
  background: rgba(26,54,93,0.06);
  border-color: var(--ac-primary);
}
.cp-item-top {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}
.cp-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--ac-ink);
  font-family: var(--ac-font-sans);
}
.cp-item-bottom {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: var(--ac-gray);
  margin-top: 3px;
}
.cp-check {
  color: var(--ac-primary);
  font-size: 18px;
  margin-left: 8px;
}
.cp-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-top: 1px solid var(--ac-border-warm);
  background: #faf8f5;
}
.cp-count { font-size: 12px; color: var(--ac-gray); font-family: 'Georgia', serif; }
</style>
