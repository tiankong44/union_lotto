<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { AlertCircle, ArrowUpRight, Check, Clock3, Pause, Play, RotateCcw, Sparkles, TimerReset, Undo2 } from 'lucide-vue-next'
import { usePregnancyStore } from '../stores/pregnancy'

const store = usePregnancyStore()
const online = ref(typeof navigator === 'undefined' ? true : navigator.onLine)
const isRunning = ref(false)
const startedAt = ref<number | null>(null)
const count = ref(0)
const strength = ref(3)
const note = ref('')
const selectedMode = ref<'free' | 'target'>('target')
const elapsedNow = ref(Date.now())
const isSaving = ref(false)
const saveMessage = ref('')
const saveMessageTone = ref<'success' | 'error'>('success')
let timer: number | undefined

const elapsedSeconds = computed(() => startedAt.value ? Math.max(0, Math.floor((elapsedNow.value - startedAt.value) / 1000)) : 0)
const elapsedText = computed(() => `${String(Math.floor(elapsedSeconds.value / 60)).padStart(2, '0')}:${String(elapsedSeconds.value % 60).padStart(2, '0')}`)
const recentSessions = computed(() => store.movementSessions.slice(0, 5))
const targetReached = computed(() => selectedMode.value === 'target' && count.value >= 10)
const todayLabel = computed(() => new Intl.DateTimeFormat('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' }).format(new Date()))
const weekLabel = computed(() => {
  if (!store.profile?.lmpDate) return '未设置孕周'
  const start = new Date(`${store.profile.lmpDate}T00:00:00`)
  if (Number.isNaN(start.getTime())) return '未设置孕周'
  const days = Math.max(0, Math.floor((Date.now() - start.getTime()) / 86400000))
  return `第 ${Math.floor(days / 7) + 1} 周`
})
const cloudStatusLabel = computed(() => {
  if (store.cloudStatus === 'loading') return '加载中'
  if (store.cloudStatus === 'saving') return '保存中'
  if (store.cloudStatus === 'error') return '保存失败'
  if (!online.value) return '等待联网'
  return '云端可用'
})

function startSession(): void {
  if (!startedAt.value) {
    startedAt.value = Date.now()
    saveMessage.value = ''
  }
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
  if (isSaving.value || !startedAt.value || count.value === 0) return
  const end = new Date()
  const start = new Date(startedAt.value)
  pauseSession()
  isSaving.value = true
  saveMessage.value = ''
  try {
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
    saveMessageTone.value = 'success'
    saveMessage.value = '已保存到云端 MySQL。'
  } catch {
    saveMessageTone.value = 'error'
    saveMessage.value = '本次记录保存失败，当前内容已保留，请重试。'
  } finally {
    isSaving.value = false
  }
}

function resetSession(): void {
  pauseSession()
  startedAt.value = null
  count.value = 0
  note.value = ''
  elapsedNow.value = Date.now()
  saveMessage.value = ''
}

function makeRecordId(prefix: string): string {
  return `${prefix}-${typeof crypto !== 'undefined' && 'randomUUID' in crypto ? crypto.randomUUID() : Date.now()}`
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }).format(new Date(value))
}

function updateOnlineState(): void {
  online.value = navigator.onLine
}

onMounted(() => {
  window.addEventListener('online', updateOnlineState)
  window.addEventListener('offline', updateOnlineState)
})

onBeforeUnmount(() => {
  if (timer) window.clearInterval(timer)
  window.removeEventListener('online', updateOnlineState)
  window.removeEventListener('offline', updateOnlineState)
})
</script>

<template>
  <div class="view-stack movement-page">
    <section class="movement-header">
      <div class="movement-heading">
        <p class="eyebrow">FOCUS · MOVEMENT</p>
        <h1>记录胎动</h1>
        <div class="movement-meta"><span>{{ todayLabel }}</span><span class="meta-divider"></span><span>{{ weekLabel }}</span></div>
      </div>
      <div class="movement-header-tools">
        <span class="local-status"><span :class="['status-dot', { muted: store.cloudStatus === 'error' || !online } ]" />{{ cloudStatusLabel }}</span>
        <div class="mode-switch" role="tablist" aria-label="记录模式">
          <button :class="{ active: selectedMode === 'target' }" type="button" :disabled="Boolean(startedAt) || isSaving" @click="selectedMode = 'target'">目标计时</button>
          <button :class="{ active: selectedMode === 'free' }" type="button" :disabled="Boolean(startedAt) || isSaving" @click="selectedMode = 'free'">自由记录</button>
        </div>
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
        <div class="target-hint" :class="{ reached: targetReached }" role="status">
          <Check v-if="targetReached" :size="15" />
          <span>{{ targetReached ? '已达到目标，可以继续记录或手动保存。' : selectedMode === 'target' ? '目标为 10 次，不代表医疗判断。' : '自由记录，不设次数目标。' }}</span>
        </div>
        <button class="movement-tap" type="button" aria-label="记录一次胎动" :disabled="isSaving" @click="addMovement">
          <span><Sparkles :size="27" /></span>
          <strong>点按记录</strong>
          <small>{{ isRunning ? '每次感受到胎动时点按' : '点击后开始计时' }}</small>
        </button>
        <div class="timer-row">
          <div class="timer-display"><Clock3 :size="17" /> {{ elapsedText }}</div>
          <button class="ghost-button" type="button" :disabled="isSaving" @click="isRunning ? pauseSession() : startSession()">
            <Pause v-if="isRunning" :size="16" />
            <Play v-else :size="16" />
            {{ isRunning ? '暂停' : '继续' }}
          </button>
          <button class="icon-button" type="button" title="撤销最近一次点按" :disabled="count === 0 || isSaving" @click="undoMovement"><Undo2 :size="17" /></button>
        </div>
        <div class="strength-field">
          <div class="field-label"><span>主观强度</span><span>{{ strength }}/5</span></div>
          <input v-model.number="strength" type="range" min="1" max="5" step="1" aria-label="胎动主观强度" :disabled="isSaving" />
          <div class="range-labels"><span>轻柔</span><span>明显</span></div>
        </div>
        <textarea v-model="note" class="note-input" rows="2" placeholder="给这次记录留一句备注（可选）" :disabled="isSaving"></textarea>
        <div class="console-actions">
          <button class="primary-button" type="button" :disabled="count === 0 || isSaving" @click="finishSession">
            <span v-if="isSaving" class="button-loader" aria-hidden="true"></span>
            <Check v-else :size="18" />
            {{ isSaving ? '保存中…' : '完成并保存' }}
          </button>
          <button class="text-button" type="button" :disabled="(!startedAt && count === 0) || isSaving" @click="resetSession"><RotateCcw :size="15" /> 重新开始</button>
        </div>
        <p v-if="saveMessage" :class="['save-feedback', saveMessageTone]" role="status">
          <Check v-if="saveMessageTone === 'success'" :size="15" />
          <AlertCircle v-else :size="15" />
          {{ saveMessage }}
        </p>
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
