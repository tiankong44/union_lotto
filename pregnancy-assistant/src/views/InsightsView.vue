<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { Activity, ArrowDownRight, ArrowUpRight, BarChart3, Info, Minus, Scale, TrendingUp, Waves } from 'lucide-vue-next'
import { usePregnancyStore } from '../stores/pregnancy'
import type { HealthRecord, RecordTimelineItem, WeightTrendPoint } from '../types/pregnancy'

type ReportPeriodDays = 7 | 30 | 90
type ChartPoint = WeightTrendPoint & { x: number; y: number }

const store = usePregnancyStore()
const periodDays = ref<ReportPeriodDays>(30)
const periodOptions: { value: ReportPeriodDays; label: string }[] = [
  { value: 7, label: '7 天' },
  { value: 30, label: '30 天' },
  { value: 90, label: '90 天' },
]
const movementChartScroll = ref<HTMLElement | null>(null)

function pad(value: number): string {
  return String(value).padStart(2, '0')
}

function dateKey(value: Date): string {
  return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}`
}

function parseDate(value: string): Date | null {
  const parsed = new Date(value)
  return Number.isNaN(parsed.getTime()) ? null : parsed
}

function timestamp(value: string): number {
  return parseDate(value)?.getTime() ?? 0
}

function formatShortDate(value: string): string {
  const date = parseDate(value)
  return date ? `${date.getMonth() + 1}/${date.getDate()}` : '时间未知'
}

function formatDate(value: string): string {
  const date = parseDate(value)
  return date ? new Intl.DateTimeFormat('zh-CN', { month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit' }).format(date) : '时间未知'
}

function healthLabel(type: HealthRecord['recordType']): string {
  return { weight: '体重', 'blood-pressure': '血压', symptom: '症状' }[type]
}

function healthValue(record: HealthRecord): string {
  try {
    const parsed: unknown = JSON.parse(record.valueJson)
    if (record.recordType === 'blood-pressure' && typeof parsed === 'object' && parsed !== null && 'systolic' in parsed && 'diastolic' in parsed) {
      const systolic = (parsed as { systolic?: unknown }).systolic
      const diastolic = (parsed as { diastolic?: unknown }).diastolic
      if ((typeof systolic === 'string' || typeof systolic === 'number') && (typeof diastolic === 'string' || typeof diastolic === 'number')) return `${systolic} / ${diastolic}`
    }
    if (typeof parsed === 'object' && parsed !== null && 'value' in parsed) {
      const value = (parsed as { value?: unknown }).value
      if (typeof value === 'string' || typeof value === 'number') return String(value)
    }
  } catch {
    return record.valueJson
  }
  return record.valueJson
}

function numericHealthValue(record: HealthRecord): number | null {
  const value = Number(healthValue(record))
  return Number.isFinite(value) ? value : null
}

const today = computed(() => {
  const value = new Date()
  value.setHours(0, 0, 0, 0)
  return value
})

const periodStart = computed(() => {
  const value = new Date(today.value)
  value.setDate(value.getDate() - periodDays.value + 1)
  return value
})

function isWithinPeriod(value: string): boolean {
  const parsed = parseDate(value)
  if (!parsed) return false
  const end = new Date(today.value)
  end.setDate(end.getDate() + 1)
  return parsed >= periodStart.value && parsed < end
}

const movementRecords = computed(() => [...store.movementSessions].filter((item) => isWithinPeriod(item.startedAt)).sort((a, b) => timestamp(b.startedAt) - timestamp(a.startedAt)))
const movementDays = computed(() => {
  const result: { label: string; date: string; count: number; sessions: number }[] = []
  for (let index = periodDays.value - 1; index >= 0; index -= 1) {
    const date = new Date(today.value)
    date.setDate(date.getDate() - index)
    const currentKey = dateKey(date)
    const records = movementRecords.value.filter((session) => {
      const sessionDate = parseDate(session.startedAt)
      return sessionDate ? dateKey(sessionDate) === currentKey : false
    })
    result.push({ label: `${date.getMonth() + 1}/${date.getDate()}`, date: currentKey, count: records.reduce((sum, record) => sum + record.movementCount, 0), sessions: records.length })
  }
  return result
})

async function scrollMovementChartToLatest(): Promise<void> {
  await nextTick()
  const chartScroll = movementChartScroll.value
  if (chartScroll) chartScroll.scrollLeft = chartScroll.scrollWidth
}

onMounted(() => {
  void scrollMovementChartToLatest()
})

watch(movementDays, () => {
  void scrollMovementChartToLatest()
}, { flush: 'post' })

const maxCount = computed(() => Math.max(1, ...movementDays.value.map((day) => day.count)))
const averageCount = computed(() => movementRecords.value.length ? Math.round(movementRecords.value.reduce((sum, item) => sum + item.movementCount, 0) / movementRecords.value.length) : 0)
const latestCount = computed(() => movementRecords.value[0]?.movementCount ?? 0)
const previousCount = computed(() => movementRecords.value[1]?.movementCount ?? latestCount.value)
const changeText = computed(() => latestCount.value === previousCount.value ? '持平' : `${Math.abs(latestCount.value - previousCount.value)} 次`)
const changeDirection = computed(() => latestCount.value === previousCount.value ? 'same' : latestCount.value > previousCount.value ? 'up' : 'down')

function barHeight(count: number): string {
  return count > 0 ? `${Math.max(12, Math.round((count / maxCount.value) * 100))}%` : '0%'
}

const weightRecords = computed(() => store.healthRecords
  .filter((record) => record.recordType === 'weight' && isWithinPeriod(record.recordedAt) && numericHealthValue(record) !== null)
  .sort((a, b) => timestamp(a.recordedAt) - timestamp(b.recordedAt)))

const weightTrend = computed<WeightTrendPoint[]>(() => {
  const latestByDate = new Map<string, HealthRecord>()
  weightRecords.value.forEach((record) => {
    const parsed = parseDate(record.recordedAt)
    if (parsed) latestByDate.set(dateKey(parsed), record)
  })
  return [...latestByDate.entries()]
    .sort(([left], [right]) => left.localeCompare(right))
    .map(([currentKey, record]) => ({ dateKey: currentKey, label: formatShortDate(record.recordedAt), value: numericHealthValue(record) ?? 0 }))
})

const chartWidth = computed(() => Math.max(560, weightTrend.value.length * 64))
const chartPoints = computed<ChartPoint[]>(() => {
  const values = weightTrend.value.map((point) => point.value)
  const minValue = values.length ? Math.min(...values) : 0
  const maxValue = values.length ? Math.max(...values) : 1
  const valueRange = Math.max(1, maxValue - minValue)
  const xStep = weightTrend.value.length > 1 ? (chartWidth.value - 48) / (weightTrend.value.length - 1) : 0
  return weightTrend.value.map((point, index) => ({
    ...point,
    x: 24 + index * xStep,
    y: 188 - ((point.value - minValue) / valueRange) * 132,
  }))
})
const polylinePoints = computed(() => chartPoints.value.map((point) => `${point.x},${point.y}`).join(' '))

const timelineItems = computed<RecordTimelineItem[]>(() => {
  const items: RecordTimelineItem[] = []
  movementRecords.value.forEach((record) => items.push({ id: record.clientRecordId, recordType: 'movement', label: '胎动', recordedAt: record.startedAt, detail: `${record.movementCount} 次 · ${Math.max(0, Math.floor((new Date(record.endedAt).getTime() - new Date(record.startedAt).getTime()) / 60000))} 分钟`, note: record.note }))
  store.contractions.filter((record) => isWithinPeriod(record.startedAt)).forEach((record) => items.push({ id: record.clientRecordId, recordType: 'contraction', label: '宫缩', recordedAt: record.startedAt, detail: `持续 ${record.durationSeconds} 秒 · 间隔 ${record.intervalSeconds ?? '--'} 秒`, note: record.note }))
  store.healthRecords.filter((record) => isWithinPeriod(record.recordedAt)).forEach((record) => items.push({ id: record.clientRecordId, recordType: record.recordType, label: healthLabel(record.recordType), recordedAt: record.recordedAt, detail: `${healthValue(record)}${record.unit ? ` ${record.unit}` : ''}`, note: record.note }))
  return items.sort((left, right) => timestamp(right.recordedAt) - timestamp(left.recordedAt))
})
</script>

<template>
  <div class="view-stack">
    <section class="page-intro compact-intro">
      <div><p class="eyebrow">PERSONAL PATTERNS</p><h1>看见自己的节奏</h1><p>只和自己的历史比较，不把数据变成答案。</p></div>
      <div class="insight-mark"><TrendingUp :size="26" /></div>
    </section>

    <section class="report-toolbar panel">
      <div><p class="eyebrow">REPORT WINDOW</p><strong>查看最近一段时间</strong></div>
      <div class="segmented-control" role="tablist" aria-label="报表时间范围">
        <button v-for="option in periodOptions" :key="option.value" :class="{ active: periodDays === option.value }" type="button" :aria-pressed="periodDays === option.value" @click="periodDays = option.value">{{ option.label }}</button>
      </div>
    </section>

    <section class="insight-grid">
      <article class="panel chart-panel">
        <div class="panel-heading"><div><p class="eyebrow">MOVEMENT DISTRIBUTION</p><h2>胎动记录分布</h2></div><BarChart3 :size="20" class="heading-icon" /></div>
        <div class="chart-legend" aria-label="胎动分布图例">
          <span class="legend-item"><i class="legend-swatch recorded"></i>1 次会话</span>
          <span class="legend-item"><i class="legend-swatch repeated"></i>多次会话</span>
          <span class="legend-item"><i class="legend-swatch empty"></i>无记录</span>
        </div>
        <div v-if="movementRecords.length" ref="movementChartScroll" class="chart-scroll"><div class="chart-area report-chart-area" :style="{ minWidth: `${Math.max(560, movementDays.length * 30)}px` }" aria-label="选择时间范围内的胎动记录分布">
          <div v-for="day in movementDays" :key="day.date" :class="['chart-column', { 'has-records': day.count > 0, 'multiple-sessions': day.sessions > 1, 'empty-day': day.count === 0 }]" :title="`${day.label}：${day.count} 次胎动，${day.sessions} 次会话`" :aria-label="`${day.label}：${day.count} 次胎动，${day.sessions} 次会话`"><span class="chart-value">{{ day.count }}</span><div class="bar-track"><span class="bar-fill" :style="{ height: barHeight(day.count) }"></span></div><span class="chart-label">{{ day.label }}</span></div>
        </div></div>
        <div v-else class="empty-report chart-empty"><BarChart3 :size="24" /><strong>当前范围还没有胎动记录</strong><span>完成一条记录后，这里会按日期显示胎动总数。</span></div>
        <div class="chart-caption"><Info :size="15" /> 柱高表示当日胎动总数，颜色表示当日会话次数；仅用于个人回看，不代表医学标准或健康结论。</div>
      </article>

      <article class="panel insight-summary">
        <div class="panel-heading"><div><p class="eyebrow">AT A GLANCE</p><h2>这段时间</h2></div><Activity :size="20" class="heading-icon" /></div>
        <div class="big-stat"><strong>{{ averageCount }}</strong><span>每次记录平均胎动</span></div>
        <div class="summary-divider"></div>
        <div class="summary-row"><span>最近一次</span><strong>{{ latestCount }} 次</strong></div>
        <div class="summary-row"><span>与前一次相比</span><strong :class="changeDirection"><ArrowUpRight v-if="changeDirection === 'up'" :size="15" /><ArrowDownRight v-else-if="changeDirection === 'down'" :size="15" /><Minus v-else :size="15" /> {{ changeText }}</strong></div>
        <p class="summary-note">当前范围内没有记录时，统计会显示为 0。</p>
      </article>
    </section>

    <section class="panel weight-report-panel">
      <div class="panel-heading"><div><p class="eyebrow">WEIGHT TREND</p><h2>体重变化</h2><p class="report-subtitle">按自然日展示每天最后一次测量，明细保留全部记录。</p></div><Scale :size="20" class="heading-icon" /></div>
      <div v-if="chartPoints.length" class="weight-chart-scroll">
        <svg class="weight-chart" :viewBox="`0 0 ${chartWidth} 230`" :width="chartWidth" height="230" role="img" aria-label="体重变化折线图">
          <line x1="24" y1="188" :x2="chartWidth - 24" y2="188" class="weight-axis" />
          <polyline :points="polylinePoints" class="weight-line" fill="none" />
          <g v-for="point in chartPoints" :key="point.dateKey">
            <circle :cx="point.x" :cy="point.y" r="4" class="weight-point" />
            <text :x="point.x" :y="point.y - 10" text-anchor="middle" class="weight-value">{{ point.value.toFixed(1) }}</text>
            <text :x="point.x" y="210" text-anchor="middle" class="weight-label">{{ point.label }}</text>
          </g>
        </svg>
      </div>
      <div v-else class="empty-report"><Scale :size="24" /><strong>当前范围还没有体重记录</strong><span>从记录中心添加第一笔体重数据。</span></div>
      <div v-if="weightRecords.length" class="weight-detail-list">
        <div v-for="record in [...weightRecords].reverse()" :key="record.clientRecordId" class="weight-detail-row"><span>{{ formatDate(record.recordedAt) }}</span><strong>{{ healthValue(record) }} {{ record.unit || 'kg' }}</strong><small>{{ record.note || '无备注' }}</small></div>
      </div>
      <div class="chart-caption"><Info :size="15" /> 体重记录只用于个人回看和就医沟通，不输出医疗判断。</div>
    </section>

    <section class="panel timeline-report-panel">
      <div class="panel-heading"><div><p class="eyebrow">BODY LOG TIMELINE</p><h2>身体记录时间线</h2><p class="report-subtitle">按时间回看胎动、宫缩、体重、血压和症状。</p></div><Waves :size="20" class="heading-icon" /></div>
      <div v-if="timelineItems.length" class="report-timeline">
        <div v-for="item in timelineItems" :key="item.id" class="report-timeline-row">
          <time>{{ formatDate(item.recordedAt) }}</time>
          <span :class="['timeline-marker', `${item.recordType}-marker`]" aria-hidden="true"></span>
          <div><strong>{{ item.label }}</strong><span>{{ item.detail }}{{ item.note ? ` · ${item.note}` : '' }}</span></div>
        </div>
      </div>
      <div v-else class="empty-report"><FileText :size="24" /><strong>当前范围还没有身体记录</strong><span>从记录中心开始留下第一条记录。</span></div>
    </section>

    <section class="panel trend-note"><span class="trend-note-mark"></span><div><p class="eyebrow">A GENTLE NOTE</p><h2>记录帮助你描述自己的变化。</h2><p>如果最近的感受与平时明显不同，请直接联系产科或医疗机构，不要等待应用判断。</p></div></section>
  </div>
</template>
