<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { ArrowUpRight, Check, Clock3, Pause, Play, RotateCcw, Sparkles, TimerReset, Undo2 } from 'lucide-vue-next'
import { usePregnancyStore } from '../stores/pregnancy'

const store = usePregnancyStore()
const isRunning = ref(false)
const startedAt = ref<number | null>(null)
const count = ref(0)
const strength = ref(3)
const note = ref('')
const selectedMode = ref<'free' | 'target'>('target')
const elapsedNow = ref(Date.now())
let timer: number | undefined

const elapsedSeconds = computed(() => startedAt.value ? Math.max(0, Math.floor((elapsedNow.value - startedAt.value) / 1000)) : 0)
const elapsedText = computed(() => `${String(Math.floor(elapsedSeconds.value / 60)).padStart(2, '0')}:${String(elapsedSeconds.value % 60).padStart(2, '0')}`)
const recentSessions = computed(() => store.movementSessions.slice(0, 5))

function startSession(): void {
  if (!startedAt.value) startedAt.value = Date.now()
  isRunning.value = true
  timer = window.setInterval(() => { elapsedNow.value = Date.now() }, 1000)
}

function pauseSession(): void {
  isRunning.value = false
  if (timer) window.clearInterval(timer)
  timer = undefined
}

function addMovement(): void {
  if (!startedAt.value) startSession()
  count.value += 1
  elapsedNow.value = Date.now()
}

function undoMovement(): void {
  count.value = Math.max(0, count.value - 1)
}

async function finishSession(): Promise<void> {
  if (!startedAt.value || count.value === 0) return
  const end = new Date()
  const start = new Date(startedAt.value)
  pauseSession()
  await store.addMovementSession({
    clientRecordId: makeRecordId('movement'),
    sessionMode: selectedMode.value,
    startedAt: start.toISOString(),
    endedAt: end.toISOString(),
    movementCount: count.value,
    targetCount: selectedMode.value === 'target' ? 10 : undefined,
    averageStrength: strength.value,
    note: note.value.trim() || undefined,
  })
  resetSession()
}

function resetSession(): void {
  pauseSession()
  startedAt.value = null
  count.value = 0
  note.value = ''
  elapsedNow.value = Date.now()
}

function makeRecordId(prefix: string): string {
  return `${prefix}-${typeof crypto !== 'undefined' && 'randomUUID' in crypto ? crypto.randomUUID() : Date.now()}`
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }).format(new Date(value))
}

onBeforeUnmount(() => {
  if (timer) window.clearInterval(timer)
})
</script>

<template>
  <div class="view-stack movement-page">
    <section class="page-intro compact-intro">
      <div>
        <p class="eyebrow">MOVEMENT SESSION</p>
        <h1>记录胎动</h1>
        <p>保持舒适，按感受到的每一次轻动点按记录。</p>
      </div>
      <div class="mode-switch" role="tablist" aria-label="记录模式">
        <button :class="{ active: selectedMode === 'target' }" type="button" @click="selectedMode = 'target'">目标计时</button>
        <button :class="{ active: selectedMode === 'free' }" type="button" @click="selectedMode = 'free'">自由记录</button>
      </div>
    </section>

    <section class="movement-layout">
      <article class="panel movement-console">
        <div class="console-meta">
          <span class="live-label"><span class="live-dot"></span>{{ isRunning ? '记录进行中' : '准备好后开始' }}</span>
          <span class="session-mode">{{ selectedMode === 'target' ? '10 次目标' : '不设目标' }}</span>
        </div>
        <div class="movement-counter">
          <span class="counter-caption">本次已记录</span>
          <strong>{{ count }}</strong>
          <span class="counter-unit">次胎动</span>
        </div>
        <button class="movement-tap" type="button" aria-label="记录一次胎动" @click="addMovement">
          <span><Sparkles :size="27" /></span>
          <strong>点按记录</strong>
          <small>{{ isRunning ? '每次感受到胎动时点按' : '点击后开始计时' }}</small>
        </button>
        <div class="timer-row">
          <div class="timer-display"><Clock3 :size="17" /> {{ elapsedText }}</div>
          <button class="ghost-button" type="button" @click="isRunning ? pauseSession() : startSession()">
            <Pause v-if="isRunning" :size="16" />
            <Play v-else :size="16" />
            {{ isRunning ? '暂停' : '继续' }}
          </button>
          <button class="icon-button" type="button" title="撤销最近一次点按" :disabled="count === 0" @click="undoMovement"><Undo2 :size="17" /></button>
        </div>
        <div class="strength-field">
          <div class="field-label"><span>主观强度</span><span>{{ strength }}/5</span></div>
          <input v-model.number="strength" type="range" min="1" max="5" step="1" aria-label="胎动主观强度" />
          <div class="range-labels"><span>轻柔</span><span>明显</span></div>
        </div>
        <textarea v-model="note" class="note-input" rows="2" placeholder="给这次记录留一句备注（可选）"></textarea>
        <div class="console-actions">
          <button class="primary-button" type="button" :disabled="count === 0" @click="finishSession"><Check :size="18" /> 完成并保存</button>
          <button class="text-button" type="button" :disabled="!startedAt && count === 0" @click="resetSession"><RotateCcw :size="15" /> 重新开始</button>
        </div>
      </article>

      <aside class="movement-aside">
        <article class="panel aside-card">
          <div class="panel-heading small-heading"><div><p class="eyebrow">HOW IT FEELS</p><h2>记下你的感觉</h2></div><TimerReset :size="19" /></div>
          <p class="panel-copy">强度是你的主观感受，不是检测结果。持续几次记录后，你会更熟悉自己的日常节奏。</p>
          <div class="strength-scale"><span v-for="level in 5" :key="level" :class="{ selected: level <= strength }"></span></div>
        </article>
        <article class="panel aside-card history-mini">
          <div class="panel-heading small-heading"><div><p class="eyebrow">RECENT</p><h2>最近几次</h2></div><RouterLink class="icon-link" to="/records" title="查看全部记录"><ArrowUpRight :size="17" /></RouterLink></div>
          <div v-if="recentSessions.length" class="mini-list">
            <div v-for="session in recentSessions" :key="session.clientRecordId" class="mini-row"><span>{{ formatDate(session.startedAt) }}</span><strong>{{ session.movementCount }} 次</strong></div>
          </div>
          <div v-else class="empty-inline">完成第一次记录后，会显示在这里。</div>
        </article>
      </aside>
    </section>
  </div>
</template>
