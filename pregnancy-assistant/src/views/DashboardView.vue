<script setup lang="ts">
import { computed } from 'vue'
import { ArrowUpRight, CalendarClock, ChevronRight, CirclePlus, ClipboardCheck, HeartPulse } from 'lucide-vue-next'
import { RouterLink } from 'vue-router'
import { usePregnancyStore } from '../stores/pregnancy'

const store = usePregnancyStore()

const todayMovementTotal = computed(() => store.todayMovements.reduce((total, session) => total + session.movementCount, 0))
const weekNumber = computed(() => {
  if (!store.profile?.lmpDate) return null
  const start = new Date(`${store.profile.lmpDate}T00:00:00`)
  const days = Math.max(0, Math.floor((Date.now() - start.getTime()) / 86400000))
  return Math.floor(days / 7) + 1
})
const dueText = computed(() => store.profile?.dueDate ? formatShortDate(store.profile.dueDate) : '设置预产期')
const latestMovement = computed(() => store.movementSessions[0])

function formatShortDate(value?: string): string {
  if (!value) return '未设置'
  const date = new Date(value)
  return `${date.getMonth() + 1}月${date.getDate()}日`
}

function formatTime(value: string): string {
  return new Intl.DateTimeFormat('zh-CN', { hour: '2-digit', minute: '2-digit' }).format(new Date(value))
}
</script>

<template>
  <div class="view-stack">
    <section class="hero-band">
      <div>
        <p class="eyebrow">GOOD MORNING, {{ store.profile?.nickname || '准妈妈' }}</p>
        <h1>把每一次感受，留在今天。</h1>
        <p class="hero-copy">记录自己的节奏，回看自己的变化。数据只属于你，也只为更好地和产科沟通。</p>
      </div>
    </section>

    <section v-if="!store.profile" class="onboarding-strip">
      <div class="strip-icon"><HeartPulse :size="20" /></div>
      <div>
        <strong>先建立一份孕期档案</strong>
        <p>设置预产期后，总览会显示孕周、阶段和你的记录节奏。</p>
      </div>
      <RouterLink class="text-action" to="/pregnancy">去设置 <ArrowUpRight :size="16" /></RouterLink>
    </section>

    <section class="metric-grid">
      <article class="metric-card metric-card-accent">
        <div class="metric-label"><span class="metric-dot coral"></span> 今日胎动</div>
        <strong>{{ todayMovementTotal }}</strong>
        <span class="metric-foot">{{ store.todayMovements.length }} 次记录</span>
      </article>
      <article class="metric-card">
        <div class="metric-label"><span class="metric-dot blue"></span> 当前孕周</div>
        <strong>{{ weekNumber ? `第 ${weekNumber} 周` : '--' }}</strong>
        <span class="metric-foot">预产期 {{ dueText }}</span>
      </article>
      <article class="metric-card">
        <div class="metric-label"><span class="metric-dot yellow"></span> 待办事项</div>
        <strong>{{ store.pendingTasks.length }}</strong>
        <span class="metric-foot">{{ store.pendingTasks.length ? '还有事项要处理' : '今天很轻盈' }}</span>
      </article>
    </section>

    <section class="dashboard-grid">
      <article class="panel focus-panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">FOCUS TODAY</p>
            <h2>现在，记录一次胎动</h2>
          </div>
          <span class="panel-index">01</span>
        </div>
        <p class="panel-copy">坐下来，给自己一段安静的时间。每一次点按都会形成你的个人记录。</p>
        <RouterLink class="primary-button" to="/">
          <CirclePlus :size="18" /> 开始记录
        </RouterLink>
        <div v-if="latestMovement" class="last-session">
          <div>
            <span>上一次记录</span>
            <strong>{{ latestMovement.movementCount }} 次胎动 · {{ formatTime(latestMovement.startedAt) }}</strong>
          </div>
          <RouterLink to="/records" class="icon-link" title="查看记录"><ChevronRight :size="18" /></RouterLink>
        </div>
      </article>

      <article class="panel timeline-panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">YOUR DAY</p>
            <h2>今天的安排</h2>
          </div>
          <CalendarClock :size="20" class="heading-icon" />
        </div>
        <div v-if="store.pendingTasks.length" class="timeline-list">
          <div v-for="task in store.pendingTasks.slice(0, 3)" :key="task.clientRecordId" class="timeline-row">
            <span class="timeline-time">{{ formatTime(task.plannedAt) }}</span>
            <span class="timeline-line"></span>
            <span class="timeline-title">{{ task.title }}</span>
          </div>
        </div>
        <div v-else class="empty-block">
          <ClipboardCheck :size="22" />
          <span>今天还没有安排</span>
          <RouterLink to="/tasks" class="text-action">添加一项 <ArrowUpRight :size="15" /></RouterLink>
        </div>
      </article>
    </section>

    <section class="notice-band">
      <span class="notice-marker"></span>
      <p>这里的趋势只描述你自己的历史记录。如果感觉胎动明显减少或模式发生变化，请及时联系产科，不要等待应用给出判断。</p>
    </section>
  </div>
</template>
