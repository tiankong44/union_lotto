<script setup lang="ts">
import { computed } from 'vue'
import { Activity, BarChart3, BookOpen, CalendarDays, ClipboardList, HeartPulse, Home, RefreshCw, Settings, Waves } from 'lucide-vue-next'
import { usePregnancyStore } from '../stores/pregnancy'

const store = usePregnancyStore()

const navItems = [
  { to: '/', label: '记录胎动', icon: Activity },
  { to: '/record-center', label: '记录中心', icon: BookOpen },
  { to: '/contractions', label: '宫缩', icon: Waves },
  { to: '/overview', label: '总览', icon: Home },
  { to: '/records', label: '历史记录', icon: CalendarDays },
  { to: '/insights', label: '分析', icon: BarChart3 },
  { to: '/pregnancy', label: '孕期', icon: HeartPulse },
  { to: '/tasks', label: '待办', icon: ClipboardList },
]

const mobileNavItems = [
  { to: '/', label: '胎动', icon: Activity },
  { to: '/overview', label: '总览', icon: Home },
  { to: '/record-center', label: '记录', icon: BookOpen },
  { to: '/insights', label: '分析', icon: BarChart3 },
  { to: '/tasks', label: '待办', icon: ClipboardList },
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
      <RouterLink class="brand-lockup" to="/overview" aria-label="回到总览">
        <span class="brand-mark"><Waves :size="20" /></span>
        <span>
          <strong>拾光孕记</strong>
          <small>quiet pregnancy log</small>
        </span>
      </RouterLink>

      <nav class="primary-nav" aria-label="主要导航">
        <RouterLink v-for="item in navItems" :key="item.to" :to="item.to" class="nav-item">
          <component :is="item.icon" :size="18" stroke-width="1.8" />
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>

      <div class="rail-footer">
        <div class="privacy-note"><span class="status-dot"></span> 云端 MySQL 保存</div>
        <RouterLink class="nav-item nav-item-muted" to="/settings">
          <Settings :size="18" />
          <span>设置</span>
        </RouterLink>
      </div>
    </aside>

    <main class="main-column">
      <div v-if="store.errorMessage" class="sync-alert" role="status">
        {{ store.errorMessage }}
      </div>

      <section class="page-content">
        <RouterView />
        <div class="page-toolbar">
          <button class="sync-button" type="button" :disabled="store.cloudStatus === 'loading' || store.cloudStatus === 'saving'" title="刷新云端数据" @click="store.refreshCloudData">
            <RefreshCw :size="16" :class="{ spin: store.cloudStatus === 'loading' || store.cloudStatus === 'saving' }" />
            <span>{{ syncLabel }}</span>
          </button>
        </div>
      </section>

      <nav class="bottom-nav" aria-label="移动端主要导航">
        <RouterLink v-for="item in mobileNavItems" :key="item.to" :to="item.to" class="bottom-nav-item">
          <component :is="item.icon" :size="18" />
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>
    </main>
  </div>
</template>
