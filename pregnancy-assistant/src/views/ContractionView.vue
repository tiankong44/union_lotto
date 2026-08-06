<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { ArrowUpRight, Clock3, RotateCcw, Waves } from 'lucide-vue-next'
import WorkspaceLink from '../components/WorkspaceLink.vue'
import { usePregnancyStore } from '../stores/pregnancy'
import type { ContractionSessionPayload } from '../types/pregnancy'

const store = usePregnancyStore()
const running = ref(false)
const startedAt = ref<number | null>(null)
const clientRecordId = ref('')
const elapsedNow = ref(Date.now())
const intensity = ref(3)
const saveMessage = ref('')
const saveMessageTone = ref<'success' | 'error'>('success')
const isSaving = ref(false)
const pendingSession = ref<ContractionSessionPayload | null>(null)
let timer: number | undefined

const elapsedSeconds = computed(() => startedAt.value ? Math.max(0, Math.floor((elapsedNow.value - startedAt.value) / 1000)) : 0)
const elapsedText = computed(() => `${String(Math.floor(elapsedSeconds.value / 60)).padStart(2, '0')}:${String(elapsedSeconds.value % 60).padStart(2, '0')}`)
const latestContraction = computed(() => store.contractions[0])

function start(): void {
  if (startedAt.value) return
  startedAt.value = Date.now()
  clientRecordId.value = `contraction-${typeof crypto !== 'undefined' && 'randomUUID' in crypto ? crypto.randomUUID() : Date.now()}`
  running.value = true
  elapsedNow.value = Date.now()
  timer = window.setInterval(() => { elapsedNow.value = Date.now() }, 1000)
}

function stopTimer(): void {
  running.value = false
  if (timer) window.clearInterval(timer)
  timer = undefined
}

async function finish(): Promise<void> {
  if (isSaving.value || !startedAt.value) return
  if (!pendingSession.value) {
    const end = new Date()
    const startTime = new Date(startedAt.value)
    const previousEnd = latestContraction.value ? new Date(latestContraction.value.endedAt).getTime() : null
    pendingSession.value = {
      clientRecordId: clientRecordId.value,
      startedAt: startTime.toISOString(),
      endedAt: end.toISOString(),
      durationSeconds: Math.max(0, Math.floor((end.getTime() - startTime.getTime()) / 1000)),
      intervalSeconds: previousEnd ? Math.max(0, Math.floor((startTime.getTime() - previousEnd) / 1000)) : undefined,
      intensity: intensity.value,
    }
  }
  stopTimer()
  isSaving.value = true
  saveMessage.value = ''
  try {
    await store.addContraction(pendingSession.value)
    pendingSession.value = null
    reset()
    saveMessageTone.value = 'success'
    saveMessage.value = '已保存到云端 MySQL。'
  } catch {
    saveMessageTone.value = 'error'
    saveMessage.value = '保存失败，当前计时内容已保留，请重试。'
  } finally {
    isSaving.value = false
  }
}

function reset(): void {
  stopTimer()
  startedAt.value = null
  clientRecordId.value = ''
  pendingSession.value = null
  elapsedNow.value = Date.now()
}

function handleMainAction(): void {
  if (isSaving.value) return
  if (startedAt.value) {
    void finish()
    return
  }
  start()
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }).format(new Date(value))
}

onBeforeUnmount(() => { if (timer) window.clearInterval(timer) })
</script>

<template>
  <div class="view-stack">
    <section class="page-intro compact-intro"><div><p class="eyebrow">CONTRACTION TIMER</p><h1>记录宫缩</h1><p>手动记录开始、结束和间隔，方便回顾自己的时间线。</p></div><div class="profile-badge"><Waves :size="21" /><span>{{ running ? '计时中' : '准备记录' }}</span></div></section>
    <section class="movement-layout">
      <article class="panel movement-console">
        <div class="console-meta"><span class="live-label"><span class="live-dot"></span>{{ running ? '本次宫缩进行中' : '准备好后开始' }}</span><span class="session-mode">第 {{ store.contractions.length + 1 }} 次</span></div>
        <div class="movement-counter"><span class="counter-caption">当前持续时间</span><strong>{{ elapsedText }}</strong><span class="counter-unit">停止后保存为一条记录</span></div>
        <button class="movement-tap contraction-tap" type="button" :disabled="isSaving" @click="handleMainAction"><span><Waves :size="27" /></span><strong>{{ running ? '结束并自动保存' : startedAt ? '重试保存' : '开始计时' }}</strong><small>{{ running ? '再次点按结束本次宫缩并保存' : startedAt ? '上次保存失败，点击重试' : '感受到宫缩时点按开始' }}</small></button>
        <div class="timer-row"><div class="timer-display"><Clock3 :size="17" /> {{ elapsedText }}</div></div>
        <div class="strength-field">
          <div class="field-label"><span>主观强度</span><span>{{ intensity }}/5</span></div>
          <div class="strength-options" role="group" aria-label="宫缩主观强度">
            <button v-for="level in 5" :key="level" :class="['strength-option', `strength-level-${level}`, { active: intensity === level }]" type="button" :aria-pressed="intensity === level" :disabled="isSaving || Boolean(pendingSession)" @click="intensity = level">{{ level }}</button>
          </div>
          <div class="strength-endpoints"><span>轻</span><span>强</span></div>
        </div>
        <div class="console-actions"><button class="text-button" type="button" :disabled="!startedAt || isSaving" @click="reset"><RotateCcw :size="15" /> 重新开始</button></div>
        <p v-if="saveMessage" :class="['save-hint', { error: saveMessageTone === 'error' }]" role="status">{{ saveMessage }}</p>
      </article>
      <aside class="movement-aside"><article class="panel aside-card"><div class="panel-heading small-heading"><div><p class="eyebrow">RECENT</p><h2>最近宫缩</h2></div><WorkspaceLink class="icon-link" view="records" title="查看全部记录"><ArrowUpRight :size="17" /></WorkspaceLink></div><div v-if="store.contractions.length" class="mini-list"><div v-for="record in store.contractions.slice(0, 5)" :key="record.clientRecordId" class="mini-row"><span>{{ formatDate(record.startedAt) }}</span><strong>{{ record.durationSeconds }} 秒</strong></div></div><div v-else class="empty-inline">完成第一次记录后，会显示在这里。</div></article><article class="panel aside-card"><p class="eyebrow">CARE NOTE</p><p class="panel-copy">计时结果只用于个人记录和就医沟通，不判断是否临产。如有担忧，请直接联系医疗机构。</p></article></aside>
    </section>
  </div>
</template>
