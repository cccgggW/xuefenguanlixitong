<template>
  <el-container class="app-container">
    <el-aside width="230px" class="app-aside">
      <div class="logo">
        <div class="logo-icon">
          <el-icon size="26"><Reading /></el-icon>
        </div>
        <div class="logo-text">
          <div class="logo-title">学业规划</div>
          <div class="logo-sub">Academic Planner</div>
        </div>
      </div>
      <div class="aside-divider"></div>
      <el-menu
        :default-active="activeMenu"
        router
        class="aside-menu"
      >
        <el-menu-item index="/">
          <el-icon><DataAnalysis /></el-icon>
          <span>数据概览</span>
        </el-menu-item>
        <el-menu-item index="/courses">
          <el-icon><Reading /></el-icon>
          <span>课程管理</span>
        </el-menu-item>
        <el-menu-item index="/students">
          <el-icon><User /></el-icon>
          <span>学生管理</span>
        </el-menu-item>
        <el-menu-item index="/enrollments">
          <el-icon><Tickets /></el-icon>
          <span>选课管理</span>
        </el-menu-item>
        <el-menu-item index="/grades">
          <el-icon><Document /></el-icon>
          <span>成绩管理</span>
        </el-menu-item>
        <el-menu-item index="/agent">
          <el-icon><MagicStick /></el-icon>
          <span>AI 智能问答</span>
        </el-menu-item>
      </el-menu>
      <div class="aside-footer">
        <div class="footer-line"></div>
        <div class="footer-text">© 2024 校园学业规划</div>
        <div class="footer-text-sub">智能助手系统 v1.0</div>
      </div>
    </el-aside>
    <el-container>
      <el-header class="app-header">
        <div class="header-left">
          <span class="header-title">{{ pageTitle }}</span>
          <span class="header-divider"></span>
          <span class="header-sub">{{ pageSubtitle }}</span>
        </div>
        <div class="header-right">
          <el-icon class="header-icon"><School /></el-icon>
          <span class="header-dept">教务处 · 学业指导中心</span>
        </div>
      </el-header>
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const activeMenu = computed(() => route.path)
const pageTitle = computed(() => route.meta.title || '学业规划助手')

const subtitleMap = {
  '数据概览': 'Dashboard · 学业数据全景',
  '课程管理': 'Course Management · 课程库维护',
  '学生管理': 'Student Management · 学籍信息',
  '选课管理': 'Enrollment Management · 选课与退课',
  '成绩管理': 'Grade Management · 成绩录入与查询',
  'AI 智能问答': 'AI Assistant · 智能学业咨询'
}
const pageSubtitle = computed(() => subtitleMap[route.meta.title] || 'Academic Planning System')
</script>

<style>
/* ========== 全局学术风设计系统 ========== */
:root {
  --ac-primary: #1a365d;
  --ac-primary-light: #2c5282;
  --ac-primary-dark: #0f1f3a;
  --ac-gold: #b08d57;
  --ac-gold-light: #c9a96e;
  --ac-ink: #2d3748;
  --ac-ink-light: #4a5568;
  --ac-gray: #718096;
  --ac-bg: #faf8f5;
  --ac-bg-card: #ffffff;
  --ac-border: #e2e8f0;
  --ac-border-warm: #e8e4dc;
  --ac-success: #2d6a4f;
  --ac-warning: #b7791f;
  --ac-danger: #c53030;
  --ac-font-serif: 'Noto Serif SC', 'Source Han Serif SC', 'SimSun', 'Songti SC', serif;
  --ac-font-sans: 'Noto Sans SC', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  --ac-font-num: 'Georgia', 'Times New Roman', serif;
}

* { margin: 0; padding: 0; box-sizing: border-box; }

html, body {
  font-family: var(--ac-font-sans);
  background: var(--ac-bg);
  color: var(--ac-ink);
  -webkit-font-smoothing: antialiased;
}

/* 滚动条 */
::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: #cbd5e0; border-radius: 3px; }
::-webkit-scrollbar-thumb:hover { background: #a0aec0; }

/* ========== 布局 ========== */
.app-container { height: 100vh; }

/* 侧边栏 */
.app-aside {
  background: linear-gradient(180deg, var(--ac-primary-dark) 0%, var(--ac-primary) 100%);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.logo {
  padding: 22px 20px 18px;
  display: flex;
  align-items: center;
  gap: 12px;
}
.logo-icon {
  width: 44px; height: 44px;
  background: var(--ac-gold);
  border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  color: #fff;
  box-shadow: 0 2px 8px rgba(176,141,87,0.4);
}
.logo-text { display: flex; flex-direction: column; }
.logo-title {
  font-family: var(--ac-font-serif);
  font-size: 17px; font-weight: 700; color: #fff;
  letter-spacing: 2px;
}
.logo-sub {
  font-size: 10px; color: rgba(255,255,255,0.5);
  letter-spacing: 1px; margin-top: 2px;
  font-family: 'Georgia', serif;
}
.aside-divider {
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(176,141,87,0.5), transparent);
  margin: 0 20px 8px;
}
.aside-menu {
  --el-menu-bg-color: transparent;
  --el-menu-text-color: rgba(255,255,255,0.65);
  --el-menu-active-color: var(--ac-gold-light);
  --el-menu-hover-bg-color: rgba(255,255,255,0.06);
  border-right: none;
  flex: 1;
}
.aside-menu .el-menu-item {
  height: 48px;
  margin: 2px 12px;
  border-radius: 6px;
  font-size: 14px;
  letter-spacing: 0.5px;
}
.aside-menu .el-menu-item.is-active {
  background: rgba(176,141,87,0.15);
  border-left: 3px solid var(--ac-gold);
  color: var(--ac-gold-light);
  font-weight: 600;
}
.aside-footer {
  padding: 16px 20px;
  text-align: center;
}
.footer-line {
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.15), transparent);
  margin-bottom: 12px;
}
.footer-text {
  font-size: 11px; color: rgba(255,255,255,0.35);
  font-family: var(--ac-font-serif);
}
.footer-text-sub {
  font-size: 10px; color: rgba(255,255,255,0.2);
  margin-top: 2px;
}

/* 顶部 */
.app-header {
  background: var(--ac-bg-card);
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--ac-border-warm);
  box-shadow: 0 1px 3px rgba(0,0,0,0.03);
  padding: 0 28px;
}
.header-left { display: flex; align-items: center; gap: 14px; }
.header-title {
  font-family: var(--ac-font-serif);
  font-size: 20px; font-weight: 700;
  color: var(--ac-primary);
  letter-spacing: 1px;
}
.header-divider {
  width: 3px; height: 20px;
  background: var(--ac-gold);
  border-radius: 2px;
}
.header-sub {
  font-size: 12px; color: var(--ac-gray);
  font-family: 'Georgia', serif;
  letter-spacing: 0.5px;
}
.header-right {
  display: flex; align-items: center; gap: 8px;
}
.header-icon { color: var(--ac-gold); font-size: 16px; }
.header-dept {
  font-size: 12px; color: var(--ac-gray);
  font-family: var(--ac-font-serif);
}

/* 主内容区 */
.app-main {
  background: var(--ac-bg);
  padding: 24px 28px;
  overflow-y: auto;
}

/* ========== 全局组件学术风覆盖 ========== */
.el-card {
  border-radius: 8px;
  border: 1px solid var(--ac-border-warm);
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}
.el-card__header {
  border-bottom: 1px solid var(--ac-border-warm);
  font-family: var(--ac-font-serif);
  font-weight: 600;
  color: var(--ac-primary);
}
.el-button--primary {
  --el-button-bg-color: var(--ac-primary);
  --el-button-border-color: var(--ac-primary);
  --el-button-hover-bg-color: var(--ac-primary-light);
  --el-button-hover-border-color: var(--ac-primary-light);
}
.el-button--success {
  --el-button-bg-color: var(--ac-success);
  --el-button-border-color: var(--ac-success);
  --el-button-hover-bg-color: #2f855a;
  --el-button-hover-border-color: #2f855a;
}
.el-button--warning {
  --el-button-bg-color: var(--ac-warning);
  --el-button-border-color: var(--ac-warning);
}
.el-button--danger {
  --el-button-bg-color: var(--ac-danger);
  --el-button-border-color: var(--ac-danger);
}
.el-table th.el-table__cell {
  background: #f7f5f0 !important;
  color: var(--ac-primary);
  font-family: var(--ac-font-serif);
  font-weight: 600;
}
.el-table .el-table__row:hover > td {
  background: #faf8f5 !important;
}
.el-tag {
  --el-tag-border-radius: 4px;
  font-family: var(--ac-font-sans);
}
.el-input__wrapper {
  border-radius: 6px;
}
.el-dialog {
  border-radius: 10px;
}
.el-dialog__title {
  font-family: var(--ac-font-serif);
  color: var(--ac-primary);
}
.el-pagination {
  --el-pagination-button-color: var(--ac-ink-light);
}
.el-pagination .is-active {
  background: var(--ac-primary) !important;
}
</style>
