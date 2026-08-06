<script setup lang="ts">
import { computed, nextTick, onMounted, onUpdated, ref } from 'vue'
import { Activity, BarChart3, CheckCircle2, Info, Scale, Timer, Waves } from 'lucide-vue-next'
import type {
  AntenatalTask,
  ContractionSession,
  FetalMovementSession,
  HealthRecord,
  RecordChartType,
} from '../types/pregnancy'

export interface RecordChartPanelProps {
  recordType: RecordChartType
  movementSessions: FetalMovementSession[]
  contractions: ContractionSession[]
  healthRecords: HealthRecord[]
  tasks: AntenatalTask[]
}

type ReportPeriodDays = 7 | 30 | 90
type NumericPoint = { dateKey: string; label: string; value: number }

interface DateBucket {
  dateKey: string
  label: string
}

interface MovementDay extends DateBucket {
  count: number
  sessions: number
}

interface ContractionDay extends DateBucket {
  count: number
  averageDuration: number | null
  averageInterval: number | null
}

interface HealthDay extends DateBucket {
  weight: number | null
  systolic: number | null
  diastolic: number | null
  symptomCount: number
}

interface TaskDay extends DateBucket {
  todo: number
  done: number
}

interface LinePoint extends NumericPoint {
  x: number
  y: number
}

interface LineChartModel {
  width: number
  minValue: number
  maxValue: number
  points: LinePoint[]
  polyline: string
}

interface DualLineChartModel {
  width: number
  minValue: number
  maxValue: number
  firstPoints: LinePoint[]
  secondPoints: LinePoint[]
  firstPolyline: string
  secondPolyline: string
}

const props = defineProps<RecordChartPanelProps>()
const periodDays = ref<ReportPeriodDays>(30)
const chartPanelRoot = ref<HTMLElement | null>(null)

const periodOptions: { value: ReportPeriodDays; label: string }[] = [
  { value: 7, label: '7 天' },
  { value: 30, label: '30 天' },
  { value: 90, label: '90 天' },
]

const chartTitles: Record<RecordChartType, string> = {
  movement: '胎动记录图表',
  contraction: '宫缩记录图表',
  health: '健康记录图表',
  tasks: '待办记录图表',
}

const chartTitle = computed(() => chartTitles[props.recordType])

function pad(value: number): string {
  return String(value).padStart(2, '0')
}

function parseDate(value: string | undefined): Date | null {
  if (!value) return null
  const parsed = new Date(value)
  return Number.isNaN(parsed.getTime()) ? null : parsed
}

function timestamp(value: string): number {
  return parseDate(value)?.getTime() ?? 0
}

function dateKey(value: Date): string {
  return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}`
}

function dateLabel(value: Date): string {
  return `${value.getMonth() + 1}/${value.getDate()}`
}

function createDateBuckets(): DateBucket[] {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const result: DateBucket[] = []
  for (let index = periodDays.value - 1; index >= 0; index -= 1) {
    const current = new Date(today)
    current.setDate(current.getDate() - index)
    result.push({ dateKey: dateKey(current), label: dateLabel(current) })
  }
  return result
}

function isWithinPeriod(value: string | undefined): boolean {
  const parsed = parseDate(value)
  if (!parsed) return false
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const start = new Date(today)
  start.setDate(start.getDate() - periodDays.value + 1)
  const end = new Date(today)
  end.setDate(end.getDate() + 1)
  return parsed >= start && parsed < end
}

function average(values: number[]): number | null {
  return values.length ? values.reduce((sum, value) => sum + value, 0) / values.length : null
}

function toNonNegativeNumber(value: unknown): number | null {
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed >= 0 ? parsed : null
}

function parseHealthJson(record: HealthRecord): Record<string, unknown> | null {
  try {
    const parsed: unknown = JSON.parse(record.valueJson)
    return typeof parsed === 'object' && parsed !== null ? parsed as Record<string, unknown> : null
  } catch {
    return null
  }
}

function healthValue(record: HealthRecord): number | null {
  const parsed = parseHealthJson(record)
  return toNonNegativeNumber(parsed?.value)
}

function bloodPressureValue(record: HealthRecord): { systolic: number; diastolic: number } | null {
  const parsed = parseHealthJson(record)
  const systolic = toNonNegativeNumber(parsed?.systolic)
  const diastolic = toNonNegativeNumber(parsed?.diastolic)
  if (systolic === null || diastolic === null) return null
  return { systolic, diastolic }
}

function buildLineChart(points: NumericPoint[]): LineChartModel {
  const width = Math.max(560, points.length * 72)
  const values = points.map((point) => point.value)
  const minValue = values.length ? Math.min(...values) : 0
  const maxValue = values.length ? Math.max(...values) : 1
  const valueRange = Math.max(1, maxValue - minValue)
  const xStep = points.length > 1 ? (width - 48) / (points.length - 1) : 0
  const chartPoints = points.map((point, index) => ({
    ...point,
    x: 24 + index * xStep,
    y: 176 - ((point.value - minValue) / valueRange) * 124,
  }))
  return {
    width,
    minValue,
    maxValue,
    points: chartPoints,
    polyline: chartPoints.map((point) => `${point.x},${point.y}`).join(' '),
  }
}

function buildDualLineChart(first: NumericPoint[], second: NumericPoint[]): DualLineChartModel {
  const width = Math.max(560, Math.max(first.length, second.length) * 72)
  const values = [...first, ...second].map((point) => point.value)
  const minValue = values.length ? Math.min(...values) : 0
  const maxValue = values.length ? Math.max(...values) : 1
  const valueRange = Math.max(1, maxValue - minValue)
  const createPoints = (points: NumericPoint[]): LinePoint[] => {
    const xStep = points.length > 1 ? (width - 48) / (points.length - 1) : 0
    return points.map((point, index) => ({
      ...point,
      x: 24 + index * xStep,
      y: 176 - ((point.value - minValue) / valueRange) * 124,
    }))
  }
  const firstPoints = createPoints(first)
  const secondPoints = createPoints(second)
  return {
    width,
    minValue,
    maxValue,
    firstPoints,
    secondPoints,
    firstPolyline: firstPoints.map((point) => `${point.x},${point.y}`).join(' '),
    secondPolyline: secondPoints.map((point) => `${point.x},${point.y}`).join(' '),
  }
}

const movementDays = computed<MovementDay[]>(() => {
  const buckets = createDateBuckets()
  const records = props.movementSessions.filter((record) => isWithinPeriod(record.startedAt))
  return buckets.map((bucket) => {
    const matching = records.filter((record) => {
      const parsed = parseDate(record.startedAt)
      return parsed ? dateKey(parsed) === bucket.dateKey : false
    })
    return {
      ...bucket,
      count: matching.reduce((sum, record) => sum + (Number.isFinite(record.movementCount) && record.movementCount >= 0 ? record.movementCount : 0), 0),
      sessions: matching.length,
    }
  })
})

const maxMovementCount = computed(() => Math.max(1, ...movementDays.value.map((day) => day.count)))
const hasMovementData = computed(() => movementDays.value.some((day) => day.sessions > 0))
const movementTotal = computed(() => movementDays.value.reduce((sum, day) => sum + day.count, 0))
const movementSessionsTotal = computed(() => movementDays.value.reduce((sum, day) => sum + day.sessions, 0))

const contractionDays = computed<ContractionDay[]>(() => {
  const buckets = createDateBuckets()
  const records = props.contractions.filter((record) => isWithinPeriod(record.startedAt))
  return buckets.map((bucket) => {
    const matching = records.filter((record) => {
      const parsed = parseDate(record.startedAt)
      return parsed ? dateKey(parsed) === bucket.dateKey : false
    })
    return {
      ...bucket,
      count: matching.length,
      averageDuration: average(matching.map((record) => record.durationSeconds).filter((value) => Number.isFinite(value) && value >= 0)),
      averageInterval: average(matching.map((record) => record.intervalSeconds).filter((value): value is number => value !== undefined && Number.isFinite(value) && value >= 0)),
    }
  })
})

const maxContractionCount = computed(() => Math.max(1, ...contractionDays.value.map((day) => day.count)))
const hasContractionData = computed(() => contractionDays.value.some((day) => day.count > 0))
const contractionTotal = computed(() => contractionDays.value.reduce((sum, day) => sum + day.count, 0))
const contractionDurationPoints = computed<NumericPoint[]>(() => contractionDays.value.filter((day) => day.averageDuration !== null).map((day) => ({ dateKey: day.dateKey, label: day.label, value: day.averageDuration as number })))
const contractionIntervalPoints = computed<NumericPoint[]>(() => contractionDays.value.filter((day) => day.averageInterval !== null).map((day) => ({ dateKey: day.dateKey, label: day.label, value: day.averageInterval as number })))
const contractionDurationChart = computed(() => buildLineChart(contractionDurationPoints.value))
const contractionIntervalChart = computed(() => buildLineChart(contractionIntervalPoints.value))

const healthDays = computed<HealthDay[]>(() => {
  const buckets = createDateBuckets()
  const records = props.healthRecords.filter((record) => isWithinPeriod(record.recordedAt))
  return buckets.map((bucket) => {
    const matching = records
      .filter((record) => {
        const parsed = parseDate(record.recordedAt)
        return parsed ? dateKey(parsed) === bucket.dateKey : false
      })
      .sort((left, right) => timestamp(left.recordedAt) - timestamp(right.recordedAt))
    const weight = [...matching].reverse().find((record) => record.recordType === 'weight' && healthValue(record) !== null)
    const bloodPressure = [...matching].reverse().find((record) => record.recordType === 'blood-pressure' && bloodPressureValue(record) !== null)
    return {
      ...bucket,
      weight: weight ? healthValue(weight) : null,
      systolic: bloodPressure ? bloodPressureValue(bloodPressure)?.systolic ?? null : null,
      diastolic: bloodPressure ? bloodPressureValue(bloodPressure)?.diastolic ?? null : null,
      symptomCount: matching.filter((record) => record.recordType === 'symptom').length,
    }
  })
})

const weightPoints = computed<NumericPoint[]>(() => healthDays.value.filter((day) => day.weight !== null).map((day) => ({ dateKey: day.dateKey, label: day.label, value: day.weight as number })))
const systolicPoints = computed<NumericPoint[]>(() => healthDays.value.filter((day) => day.systolic !== null).map((day) => ({ dateKey: day.dateKey, label: day.label, value: day.systolic as number })))
const diastolicPoints = computed<NumericPoint[]>(() => healthDays.value.filter((day) => day.diastolic !== null).map((day) => ({ dateKey: day.dateKey, label: day.label, value: day.diastolic as number })))
const weightChart = computed(() => buildLineChart(weightPoints.value))
const bloodPressureChart = computed(() => buildDualLineChart(systolicPoints.value, diastolicPoints.value))
const maxSymptomCount = computed(() => Math.max(1, ...healthDays.value.map((day) => day.symptomCount)))
const hasWeightData = computed(() => weightPoints.value.length > 0)
const hasBloodPressureData = computed(() => systolicPoints.value.length > 0 || diastolicPoints.value.length > 0)
const hasSymptomData = computed(() => healthDays.value.some((day) => day.symptomCount > 0))
const hasHealthData = computed(() => hasWeightData.value || hasBloodPressureData.value || hasSymptomData.value)

const taskDays = computed<TaskDay[]>(() => {
  const buckets = createDateBuckets()
  const records = props.tasks.filter((task) => isWithinPeriod(task.plannedAt))
  return buckets.map((bucket) => {
    const matching = records.filter((task) => {
      const parsed = parseDate(task.plannedAt)
      return parsed ? dateKey(parsed) === bucket.dateKey : false
    })
    return {
      ...bucket,
      todo: matching.filter((task) => task.status === 'TODO').length,
      done: matching.filter((task) => task.status === 'DONE').length,
    }
  })
})

const maxTaskCount = computed(() => Math.max(1, ...taskDays.value.map((day) => Math.max(day.todo, day.done))))
const hasTaskData = computed(() => taskDays.value.some((day) => day.todo > 0 || day.done > 0))
const todoTotal = computed(() => taskDays.value.reduce((sum, day) => sum + day.todo, 0))
const doneTotal = computed(() => taskDays.value.reduce((sum, day) => sum + day.done, 0))

function barHeight(value: number, maxValue: number): string {
  return `${Math.max(value ? 12 : 4, Math.round((value / maxValue) * 100))}%`
}

function formatMetric(value: number | null, digits = 0): string {
  return value === null ? '--' : value.toFixed(digits)
}

function chartLabel(type: RecordChartType): string {
  return chartTitles[type]
}

async function scrollChartsToLatest(): Promise<void> {
  await nextTick()
  const root = chartPanelRoot.value
  if (!root) return
  root.querySelectorAll<HTMLElement>('.chart-scroll').forEach((chartScroll) => {
    chartScroll.scrollLeft = chartScroll.scrollWidth
  })
}

onMounted(() => {
  void scrollChartsToLatest()
})

onUpdated(() => {
  void scrollChartsToLatest()
})
</script>

<template>
  <section ref="chartPanelRoot" class="record-chart-panel" :aria-label="chartLabel(recordType)">
    <div class="record-chart-header">
      <div>
        <p class="eyebrow">HISTORY CHART</p>
        <h2>{{ chartTitle }}</h2>
        <p class="record-chart-subtitle">只展示最近一段时间的个人记录，不输出医疗判断。</p>
      </div>
      <div class="segmented-control" role="tablist" aria-label="图表时间范围">
        <button v-for="option in periodOptions" :key="option.value" :class="{ active: periodDays === option.value }" type="button" :aria-pressed="periodDays === option.value" @click="periodDays = option.value">{{ option.label }}</button>
      </div>
    </div>

    <div v-if="recordType === 'movement'" class="record-chart-content">
      <div v-if="hasMovementData" class="record-chart-section">
        <div class="record-chart-stat-grid">
          <div><strong>{{ movementTotal }}</strong><span>胎动总次数</span></div>
          <div><strong>{{ movementSessionsTotal }}</strong><span>记录会话数</span></div>
        </div>
        <div class="chart-scroll">
          <div class="record-bar-chart" :style="{ minWidth: `${Math.max(560, movementDays.length * 30)}px` }" role="img" aria-label="胎动每日总次数柱状图">
            <div v-for="day in movementDays" :key="day.dateKey" class="record-bar-column"><span class="record-bar-value">{{ day.count || '' }}</span><div class="record-bar-track"><span class="record-bar-fill coral-fill" :style="{ height: barHeight(day.count, maxMovementCount) }"></span></div><span class="record-bar-label">{{ day.label }}</span></div>
          </div>
        </div>
        <div class="record-chart-caption"><Activity :size="14" /> 每日总次数按用户本地自然日汇总。</div>
      </div>
      <div v-else class="record-chart-empty"><BarChart3 :size="24" /><strong>当前范围还没有胎动数据</strong><span>保留历史列表，切换时间范围或新增记录后可查看。</span></div>
    </div>

    <div v-else-if="recordType === 'contraction'" class="record-chart-content">
      <div v-if="hasContractionData" class="record-chart-section">
        <div class="record-chart-stat-grid">
          <div><strong>{{ contractionTotal }}</strong><span>记录次数</span></div>
          <div><strong>{{ formatMetric(contractionDurationPoints.length ? average(contractionDurationPoints.map((point) => point.value)) : null) }} 秒</strong><span>平均持续时间</span></div>
          <div><strong>{{ formatMetric(contractionIntervalPoints.length ? average(contractionIntervalPoints.map((point) => point.value)) : null) }} 秒</strong><span>平均间隔</span></div>
        </div>
        <div class="chart-scroll">
          <div class="record-bar-chart" :style="{ minWidth: `${Math.max(560, contractionDays.length * 30)}px` }" role="img" aria-label="宫缩每日记录次数柱状图">
            <div v-for="day in contractionDays" :key="day.dateKey" class="record-bar-column"><span class="record-bar-value">{{ day.count || '' }}</span><div class="record-bar-track"><span class="record-bar-fill blue-fill" :style="{ height: barHeight(day.count, maxContractionCount) }"></span></div><span class="record-bar-label">{{ day.label }}</span></div>
          </div>
        </div>
        <div class="record-line-grid">
          <div v-if="contractionDurationPoints.length" class="record-line-section"><div class="record-line-title"><Timer :size="14" /> 平均持续时间（秒）</div><div class="chart-scroll"><svg class="record-line-chart" :viewBox="`0 0 ${contractionDurationChart.width} 210`" :width="contractionDurationChart.width" height="210" role="img" aria-label="宫缩平均持续时间折线图"><line x1="24" y1="176" :x2="contractionDurationChart.width - 24" y2="176" class="record-chart-axis" /><polyline :points="contractionDurationChart.polyline" class="record-line blue-line" fill="none" /><g v-for="point in contractionDurationChart.points" :key="point.dateKey"><circle :cx="point.x" :cy="point.y" r="4" class="record-point blue-point" /><text :x="point.x" :y="point.y - 10" text-anchor="middle" class="record-line-value">{{ point.value.toFixed(0) }}</text><text :x="point.x" y="198" text-anchor="middle" class="record-line-label">{{ point.label }}</text></g></svg></div></div>
          <div v-if="contractionIntervalPoints.length" class="record-line-section"><div class="record-line-title"><Waves :size="14" /> 平均间隔（秒）</div><div class="chart-scroll"><svg class="record-line-chart" :viewBox="`0 0 ${contractionIntervalChart.width} 210`" :width="contractionIntervalChart.width" height="210" role="img" aria-label="宫缩平均间隔折线图"><line x1="24" y1="176" :x2="contractionIntervalChart.width - 24" y2="176" class="record-chart-axis" /><polyline :points="contractionIntervalChart.polyline" class="record-line mint-line" fill="none" /><g v-for="point in contractionIntervalChart.points" :key="point.dateKey"><circle :cx="point.x" :cy="point.y" r="4" class="record-point mint-point" /><text :x="point.x" :y="point.y - 10" text-anchor="middle" class="record-line-value">{{ point.value.toFixed(0) }}</text><text :x="point.x" y="198" text-anchor="middle" class="record-line-label">{{ point.label }}</text></g></svg></div></div>
        </div>
        <div class="record-chart-caption"><Waves :size="14" /> 图表只描述已保存的个人记录，不表示临产或其他医疗结论。</div>
      </div>
      <div v-else class="record-chart-empty"><BarChart3 :size="24" /><strong>当前范围还没有宫缩数据</strong><span>保留历史列表，切换时间范围或新增记录后可查看。</span></div>
    </div>

    <div v-else-if="recordType === 'health'" class="record-chart-content">
      <div v-if="hasHealthData" class="record-chart-section health-chart-section">
        <div v-if="hasWeightData" class="record-line-section"><div class="record-line-title"><Scale :size="14" /> 体重变化</div><div class="chart-scroll"><svg class="record-line-chart" :viewBox="`0 0 ${weightChart.width} 210`" :width="weightChart.width" height="210" role="img" aria-label="体重变化折线图"><line x1="24" y1="176" :x2="weightChart.width - 24" y2="176" class="record-chart-axis" /><polyline :points="weightChart.polyline" class="record-line coral-line" fill="none" /><g v-for="point in weightChart.points" :key="point.dateKey"><circle :cx="point.x" :cy="point.y" r="4" class="record-point coral-point" /><text :x="point.x" :y="point.y - 10" text-anchor="middle" class="record-line-value">{{ point.value.toFixed(1) }}</text><text :x="point.x" y="198" text-anchor="middle" class="record-line-label">{{ point.label }}</text></g></svg></div></div>
        <div v-if="hasBloodPressureData" class="record-line-section"><div class="record-line-title"><Activity :size="14" /> 血压变化（mmHg）</div><div class="chart-scroll"><svg class="record-line-chart" :viewBox="`0 0 ${bloodPressureChart.width} 210`" :width="bloodPressureChart.width" height="210" role="img" aria-label="血压高压低压折线图"><line x1="24" y1="176" :x2="bloodPressureChart.width - 24" y2="176" class="record-chart-axis" /><polyline :points="bloodPressureChart.firstPolyline" class="record-line coral-line" fill="none" /><polyline :points="bloodPressureChart.secondPolyline" class="record-line blue-line" fill="none" /><g v-for="point in bloodPressureChart.firstPoints" :key="`systolic-${point.dateKey}`"><circle :cx="point.x" :cy="point.y" r="4" class="record-point coral-point" /><text :x="point.x" :y="point.y - 10" text-anchor="middle" class="record-line-value">{{ point.value.toFixed(0) }}</text><text :x="point.x" y="198" text-anchor="middle" class="record-line-label">{{ point.label }}</text></g><g v-for="point in bloodPressureChart.secondPoints" :key="`diastolic-${point.dateKey}`"><circle :cx="point.x" :cy="point.y" r="4" class="record-point blue-point" /></g></svg></div><div class="record-chart-legend"><span><i class="record-legend-swatch coral-fill"></i>高压</span><span><i class="record-legend-swatch blue-fill"></i>低压</span></div></div>
        <div v-if="hasSymptomData" class="record-line-section"><div class="record-line-title"><Info :size="14" /> 症状记录数</div><div class="chart-scroll"><div class="record-bar-chart" :style="{ minWidth: `${Math.max(560, healthDays.length * 30)}px` }" role="img" aria-label="症状每日记录数柱状图"><div v-for="day in healthDays" :key="day.dateKey" class="record-bar-column"><span class="record-bar-value">{{ day.symptomCount || '' }}</span><div class="record-bar-track"><span class="record-bar-fill yellow-fill" :style="{ height: barHeight(day.symptomCount, maxSymptomCount) }"></span></div><span class="record-bar-label">{{ day.label }}</span></div></div></div></div>
        <div class="record-chart-caption"><Info :size="14" /> 健康图表仅用于个人回看和就医沟通，不包含医学阈值。</div>
      </div>
      <div v-else class="record-chart-empty"><BarChart3 :size="24" /><strong>当前范围还没有有效健康数据</strong><span>非法值不会阻断历史列表，补充有效记录后可查看。</span></div>
    </div>

    <div v-else class="record-chart-content">
      <div v-if="hasTaskData" class="record-chart-section">
        <div class="record-chart-stat-grid"><div><strong>{{ todoTotal }}</strong><span>待处理事项</span></div><div><strong>{{ doneTotal }}</strong><span>已完成事项</span></div></div>
        <div class="chart-scroll"><div class="record-bar-chart task-bar-chart" :style="{ minWidth: `${Math.max(560, taskDays.length * 30)}px` }" role="img" aria-label="待办每日状态数量柱状图"><div v-for="day in taskDays" :key="day.dateKey" class="record-bar-column"><span class="record-bar-value">{{ Math.max(day.todo, day.done) || '' }}</span><div class="task-bar-track"><span class="task-bar-fill mint-fill" :style="{ height: barHeight(day.todo, maxTaskCount) }"></span><span class="task-bar-fill green-fill" :style="{ height: barHeight(day.done, maxTaskCount) }"></span></div><span class="record-bar-label">{{ day.label }}</span></div></div></div>
        <div class="record-chart-legend"><span><i class="record-legend-swatch mint-fill"></i>待处理</span><span><i class="record-legend-swatch green-fill"></i>已完成</span></div>
        <div class="record-chart-caption"><CheckCircle2 :size="14" /> 按事项计划日期和当前状态统计，不模拟状态变更历史。</div>
      </div>
      <div v-else class="record-chart-empty"><BarChart3 :size="24" /><strong>当前范围还没有待办数据</strong><span>保留历史列表，切换时间范围或新增事项后可查看。</span></div>
    </div>
  </section>
</template>
