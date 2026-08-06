<script setup lang="ts">
import { computed } from 'vue'
import { ArrowDownRight, ArrowUpRight, BarChart3, Info, Minus, TrendingUp } from 'lucide-vue-next'
import { usePregnancyStore } from '../stores/pregnancy'

const store = usePregnancyStore()

const days = computed(() => {
  const result: { label: string; date: string; count: number; sessions: number }[] = []
  for (let index = 6; index >= 0; index -= 1) {
    const date = new Date()
    date.setHours(0, 0, 0, 0)
    date.setDate(date.getDate() - index)
    const dateKey = date.toISOString().slice(0, 10)
    const records = store.movementSessions.filter((session) => session.startedAt.slice(0, 10) === dateKey)
    result.push({ label: `${date.getMonth() + 1}/${date.getDate()}`, date: dateKey, count: records.reduce((sum, record) => sum + record.movementCount, 0), sessions: records.length })
  }
  return result
})

const maxCount = computed(() => Math.max(1, ...days.value.map((day) => day.count)))
const averageCount = computed(() => store.movementSessions.length ? Math.round(store.movementSessions.reduce((sum, item) => sum + item.movementCount, 0) / store.movementSessions.length) : 0)
const latestCount = computed(() => store.movementSessions[0]?.movementCount ?? 0)
const previousCount = computed(() => store.movementSessions[1]?.movementCount ?? latestCount.value)
const changeText = computed(() => latestCount.value === previousCount.value ? '持平' : `${Math.abs(latestCount.value - previousCount.value)} 次`)
const changeDirection = computed(() => latestCount.value === previousCount.value ? 'same' : latestCount.value > previousCount.value ? 'up' : 'down')

function barHeight(count: number): string {
  return `${Math.max(count ? 12 : 4, Math.round((count / maxCount.value) * 100))}%`
}
</script>

<template>
  <div class="view-stack">
    <section class="page-intro compact-intro">
      <div>
        <p class="eyebrow">PERSONAL PATTERNS</p>
        <h1>看见自己的节奏</h1>
        <p>只和自己的历史比较，不把数据变成答案。</p>
      </div>
      <div class="insight-mark"><TrendingUp :size="26" /></div>
    </section>

    <section class="insight-grid">
      <article class="panel chart-panel">
        <div class="panel-heading">
          <div><p class="eyebrow">LAST 7 DAYS</p><h2>胎动记录趋势</h2></div>
          <BarChart3 :size="20" class="heading-icon" />
        </div>
        <div class="chart-area" aria-label="最近七天胎动记录柱状图">
          <div v-for="day in days" :key="day.date" class="chart-column">
            <span class="chart-value">{{ day.count || '' }}</span>
            <div class="bar-track"><span class="bar-fill" :style="{ height: barHeight(day.count) }"></span></div>
            <span class="chart-label">{{ day.label }}</span>
          </div>
        </div>
        <div class="chart-caption"><Info :size="15" /> 图表反映已记录的数据量，不代表医学标准或健康结论。</div>
      </article>

      <article class="panel insight-summary">
        <div class="panel-heading"><div><p class="eyebrow">AT A GLANCE</p><h2>这段时间</h2></div></div>
        <div class="big-stat"><strong>{{ averageCount }}</strong><span>每次记录平均胎动</span></div>
        <div class="summary-divider"></div>
        <div class="summary-row"><span>最近一次</span><strong>{{ latestCount }} 次</strong></div>
        <div class="summary-row"><span>与前一次相比</span><strong :class="changeDirection"><ArrowUpRight v-if="changeDirection === 'up'" :size="15" /><ArrowDownRight v-else-if="changeDirection === 'down'" :size="15" /><Minus v-else :size="15" /> {{ changeText }}</strong></div>
        <p class="summary-note">继续保持记录，至少几次之后，个人趋势才更有参考价值。</p>
      </article>
    </section>

    <section class="panel trend-note">
      <span class="trend-note-mark"></span>
      <div><p class="eyebrow">A GENTLE NOTE</p><h2>{{ store.movementSessions.length > 1 ? '你的最近记录已经有了轮廓。' : '从第一条记录开始建立自己的轮廓。' }}</h2><p>{{ store.movementSessions.length > 1 ? '如果最近的感受与平时明显不同，请直接联系产科或医疗机构。不要等待应用判断。' : '记录不是考试，也没有统一的每日标准。它只帮助你更清楚地描述自己的日常变化。' }}</p></div>
    </section>
  </div>
</template>
