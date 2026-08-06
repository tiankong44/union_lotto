<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { ArrowUpRight, Check, Clock3, Pause, Play, RotateCcw, Waves } from 'lucide-vue-next'
import { usePregnancyStore } from '../stores/pregnancy'

const store = usePregnancyStore()
const running = ref(false)
const startedAt = ref<number | null>(null)
const elapsedNow = ref(Date.now())
const intensity = ref(3)
const note = ref('')
const saveMessage = ref('')
const saveMessageTone = ref<'success' | 'error'>('success')
let timer: number | undefined

const elapsedSeconds = computed(() => startedAt.value ? Math.max(0, Math.floor((elapsedNow.value - startedAt.value) / 1000)) : 0)
const elapsedText = computed(() => `${String(Math.floor(elapsedSeconds.value / 60)).padStart(2, '0')}:${String(elapsedSeconds.value % 60).padStart(2, '0')}`)
const latestContraction = computed(() => store.contractions[0])

function start(): void {
  startedAt.value = Date.now()
  running.value = true
  timer = window.setInterval(() => { elapsedNow.value = Date.now() }, 1000)
}

function pause(): void {
  running.value = false
  if (timer) window.clearInterval(timer)
  timer = undefined
}

async function finish(): Promise<void> {
  if (!startedAt.value) return
  const end = new Date()
  const startTime = new Date(startedAt.value)
  const previousEnd = latestContraction.value ? new Date(latestContraction.value.endedAt).getTime() : null
  pause()
  saveMessage.value = ''
  try {
    await store.addContraction({
      clientRecordId: `contraction-${Date.now()}`,
      startedAt: startTime.toISOString(),
      endedAt: end.toISOString(),
      durationSeconds: Math.max(0, Math.floor((end.getTime() - startTime.getTime()) / 1000)),
      intervalSeconds: previousEnd ? Math.max(0, Math.floor((startTime.getTime() - previousEnd) / 1000)) : undefined,
      intensity: intensity.value,
      note: note.value.trim() || undefined,
    })
    reset()
    saveMessageTone.value = 'success'
    saveMessage.value = '已保存到云端 MySQL。'
  } catch {
    saveMessageTone.value = 'error'
    saveMessage.value = '保存失败，当前计时内容已保留，请重试。'
  }
}

function reset(): void {
  pause()
  startedAt.value = null
  elapsedNow.value = Date.now()
  note.value = ''
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
        <button class="movement-tap contraction-tap" type="button" @click="running ? pause() : start()"><span><Waves :size="27" /></span><strong>{{ running ? '结束计时' : '开始计时' }}</strong><small>{{ running ? '再次点按结束本次宫缩' : '感受到宫缩时点按开始' }}</small></button>
        <div class="timer-row"><div class="timer-display"><Clock3 :size="17" /> {{ elapsedText }}</div><button class="ghost-button" type="button" :disabled="!startedAt" @click="running ? pause() : start()"><Pause v-if="running" :size="16" /><Play v-else :size="16" />{{ running ? '暂停' : '继续' }}</button></div>
        <div class="strength-field"><div class="field-label"><span>主观强度</span><span>{{ intensity }}/5</span></div><input v-model.number="intensity" type="range" min="1" max="5" step="1" aria-label="宫缩主观强度" /><div class="range-labels"><span>轻</span><span>强</span></div></div>
        <textarea v-model="note" class="note-input" rows="2" placeholder="给这次记录留一句备注（可选）"></textarea>
        <div class="console-actions"><button class="primary-button" type="button" :disabled="!startedAt || running || store.cloudStatus === 'saving'" @click="finish"><Check :size="18" /> 完成并保存</button><button class="text-button" type="button" :disabled="!startedAt || store.cloudStatus === 'saving'" @click="reset"><RotateCcw :size="15" /> 重新开始</button></div>
        <p v-if="saveMessage" :class="['save-hint', { error: saveMessageTone === 'error' }]" role="status">{{ saveMessage }}</p>
      </article>
      <aside class="movement-aside"><article class="panel aside-card"><div class="panel-heading small-heading"><div><p class="eyebrow">RECENT</p><h2>最近宫缩</h2></div><RouterLink class="icon-link" to="/records" title="查看全部记录"><ArrowUpRight :size="17" /></RouterLink></div><div v-if="store.contractions.length" class="mini-list"><div v-for="record in store.contractions.slice(0, 5)" :key="record.clientRecordId" class="mini-row"><span>{{ formatDate(record.startedAt) }}</span><strong>{{ record.durationSeconds }} 秒</strong></div></div><div v-else class="empty-inline">完成第一次记录后，会显示在这里。</div></article><article class="panel aside-card"><p class="eyebrow">CARE NOTE</p><p class="panel-copy">计时结果只用于个人记录和就医沟通，不判断是否临产。如有担忧，请直接联系医疗机构。</p></article></aside>
    </section>
  </div>
</template>
