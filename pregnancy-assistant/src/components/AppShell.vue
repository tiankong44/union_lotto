<script setup lang="ts">
import { computed, onMounted, provide, ref, type Component } from 'vue'
import { Activity, BarChart3, BookOpen, CalendarDays, ClipboardList, HeartPulse, Home, RefreshCw, Settings, Waves } from 'lucide-vue-next'
import ContractionView from '../views/ContractionView.vue'
import DashboardView from '../views/DashboardView.vue'
import InsightsView from '../views/InsightsView.vue'
import MovementView from '../views/MovementView.vue'
import PregnancyView from '../views/PregnancyView.vue'
import RecordCenterView from '../views/RecordCenterView.vue'
import RecordsView from '../views/RecordsView.vue'
import SettingsView from '../views/SettingsView.vue'
import TasksView from '../views/TasksView.vue'
import { usePregnancyStore } from '../stores/pregnancy'
import { WORKSPACE_NAVIGATE_KEY, type WorkspaceView } from '../types/workspace'
import WorkspaceLink from './WorkspaceLink.vue'

const store = usePregnancyStore()
const activeView = ref<WorkspaceView>('movement')

const viewComponents: Record<WorkspaceView, Component> = {
  movement: MovementView,
  overview: DashboardView,
  'record-center': RecordCenterView,
  contractions: ContractionView,
  records: RecordsView,
  insights: InsightsView,
  pregnancy: PregnancyView,
  tasks: TasksView,
  settings: SettingsView,
}

const activeComponent = computed(() => viewComponents[activeView.value])

function navigateTo(view: WorkspaceView): void {
  activeView.value = view
}

provide(WORKSPACE_NAVIGATE_KEY, navigateTo)

onMounted(() => {
  if (window.location.pathname !== '/') {
    // 旧地址只作为启动兼容入口，进入 workspace 后统一使用根地址。
    window.history.replaceState(null, document.title, '/')
  }
})

const navItems = [
  { view: 'movement' as const, label: '记录胎动', icon: Activity },
  { view: 'record-center' as const, label: '记录中心', icon: BookOpen },
  { view: 'contractions' as const, label: '宫缩', icon: Waves },
  { view: 'overview' as const, label: '总览', icon: Home },
  { view: 'records' as const, label: '历史记录', icon: CalendarDays },
  { view: 'insights' as const, label: '分析', icon: BarChart3 },
  { view: 'pregnancy' as const, label: '孕期', icon: HeartPulse },
  { view: 'tasks' as const, label: '待办', icon: ClipboardList },
]

const mobileNavItems = [
  { view: 'movement' as const, label: '胎动', icon: Activity },
  { view: 'overview' as const, label: '总览', icon: Home },
  { view: 'record-center' as const, label: '记录', icon: BookOpen },
  { view: 'insights' as const, label: '分析', icon: BarChart3 },
  { view: 'tasks' as const, label: '待办', icon: ClipboardList },
]

const syncLabel = computed(() => {
  if (store.cloudStatus === 'loading') return '加载云端'
  if (store.cloudStatus === 'saving') return '保存中'
  if (store.cloudStatus === 'error') return '重试加载'
  return '刷新云端'
})
</script>

<template>
  <div class="app-frame">
    <aside class="side-rail">
      <WorkspaceLink class="brand-lockup" view="overview" aria-label="回到总览">
        <span class="brand-mark"><Waves :size="20" /></span>
        <span>
          <strong>拾光孕记</strong>
          <small>quiet pregnancy log</small>
        </span>
      </WorkspaceLink>

      <nav class="primary-nav" aria-label="主要导航">
        <WorkspaceLink v-for="item in navItems" :key="item.view" :view="item.view" :class="['nav-item', { active: activeView === item.view }]" :aria-current="activeView === item.view ? 'page' : undefined">
          <component :is="item.icon" :size="18" stroke-width="1.8" />
          <span>{{ item.label }}</span>
        </WorkspaceLink>
      </nav>

      <div class="rail-footer">
        <div class="privacy-note"><span class="status-dot"></span> 云端 MySQL 保存</div>
        <WorkspaceLink class="nav-item nav-item-muted" view="settings" :class="{ active: activeView === 'settings' }" :aria-current="activeView === 'settings' ? 'page' : undefined">
          <Settings :size="18" />
          <span>设置</span>
        </WorkspaceLink>
      </div>
    </aside>

    <main class="main-column">
      <div v-if="store.errorMessage" class="sync-alert" role="status">
        {{ store.errorMessage }}
      </div>

      <section class="page-content">
        <div class="page-toolbar">
          <button class="sync-button" type="button" :disabled="store.cloudStatus === 'loading' || store.cloudStatus === 'saving'" title="刷新云端数据" @click="store.refreshCloudData">
            <RefreshCw :size="16" :class="{ spin: store.cloudStatus === 'loading' || store.cloudStatus === 'saving' }" />
            <span>{{ syncLabel }}</span>
          </button>
        </div>
        <KeepAlive>
          <component :is="activeComponent" />
        </KeepAlive>
      </section>

      <nav class="bottom-nav" aria-label="移动端主要导航">
        <WorkspaceLink v-for="item in mobileNavItems" :key="item.view" :view="item.view" :class="['bottom-nav-item', { active: activeView === item.view }]" :aria-current="activeView === item.view ? 'page' : undefined">
          <component :is="item.icon" :size="18" />
          <span>{{ item.label }}</span>
        </WorkspaceLink>
      </nav>
    </main>
  </div>
</template>
