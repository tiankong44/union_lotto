<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { Activity, BarChart3, CalendarDays, ClipboardList, HeartPulse, Home, RefreshCw, Settings, Waves } from 'lucide-vue-next'
import { usePregnancyStore } from '../stores/pregnancy'

const store = usePregnancyStore()
const online = ref(typeof navigator === 'undefined' ? true : navigator.onLine)

const navItems = [
  { to: '/', label: '记录胎动', icon: Activity },
  { to: '/overview', label: '总览', icon: Home },
  { to: '/records', label: '历史记录', icon: CalendarDays },
  { to: '/insights', label: '分析', icon: BarChart3 },
  { to: '/pregnancy', label: '孕期', icon: HeartPulse },
  { to: '/tasks', label: '待办', icon: ClipboardList },
]

const mobileNavItems = [
  { to: '/', label: '胎动', icon: Activity },
  { to: '/overview', label: '总览', icon: Home },
  { to: '/records', label: '历史', icon: CalendarDays },
  { to: '/insights', label: '分析', icon: BarChart3 },
  { to: '/tasks', label: '待办', icon: ClipboardList },
]

const syncLabel = computed(() => {
  if (store.syncStatus === 'syncing') return '同步中'
  if (store.syncStatus === 'synced') return '已同步'
  if (store.syncStatus === 'error') return '重试同步'
  if (!online.value) return '离线记录'
  return '待同步'
})

function updateOnlineState(): void {
  online.value = navigator.onLine
}

onMounted(() => {
  window.addEventListener('online', updateOnlineState)
  window.addEventListener('offline', updateOnlineState)
})

onBeforeUnmount(() => {
  window.removeEventListener('online', updateOnlineState)
  window.removeEventListener('offline', updateOnlineState)
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
        <div class="privacy-note"><span class="status-dot"></span> 本机优先保存</div>
        <RouterLink class="nav-item nav-item-muted" to="/settings">
          <Settings :size="18" />
          <span>设置</span>
        </RouterLink>
      </div>
    </aside>

    <main class="main-column">
      <header class="topbar">
        <div class="topbar-context">
          <span class="eyebrow">PREGNANCY LOG / 2026</span>
          <span class="online-state"><span :class="['status-dot', { muted: !online }]" /> {{ online ? '在线' : '离线' }}</span>
        </div>
        <button class="sync-button" type="button" :disabled="store.syncStatus === 'syncing'" title="同步本机记录" @click="store.syncNow">
          <RefreshCw :size="16" :class="{ spin: store.syncStatus === 'syncing' }" />
          <span>{{ syncLabel }}</span>
        </button>
      </header>

      <div v-if="store.errorMessage" class="sync-alert" role="status">
        {{ store.errorMessage }}
      </div>

      <section class="page-content">
        <RouterView />
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
