<script setup lang="ts">
import { computed, ref } from 'vue'
import { Activity, ArrowUpRight, CalendarDays, CheckCircle2, Clock3, FileText, HeartPulse, ListFilter, Waves } from 'lucide-vue-next'
import { usePregnancyStore } from '../stores/pregnancy'

const store = usePregnancyStore()
const activeTab = ref<'movement' | 'contraction' | 'health' | 'tasks'>('movement')

const tabs = [
  { key: 'movement' as const, label: '胎动', icon: Activity },
  { key: 'contraction' as const, label: '宫缩', icon: Waves },
  { key: 'health' as const, label: '健康', icon: HeartPulse },
  { key: 'tasks' as const, label: '待办', icon: CheckCircle2 },
]

const activeCount = computed(() => {
  if (activeTab.value === 'movement') return store.movementSessions.length
  if (activeTab.value === 'contraction') return store.contractions.length
  if (activeTab.value === 'health') return store.healthRecords.length
  return store.tasks.length
})

function formatDate(value: string): string {
  return new Intl.DateTimeFormat('zh-CN', { month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit' }).format(new Date(value))
}

function healthLabel(type: string): string {
  return { weight: '体重', 'blood-pressure': '血压', symptom: '症状' }[type] ?? type
}
</script>

<template>
  <div class="view-stack">
    <section class="page-intro compact-intro">
      <div>
        <p class="eyebrow">YOUR ARCHIVE</p>
        <h1>所有记录</h1>
        <p>按类型回看每一个被认真记下的瞬间。</p>
      </div>
      <div class="archive-actions"><div class="archive-count"><strong>{{ activeCount }}</strong><span>条记录</span></div><RouterLink class="secondary-button" to="/record-center"><ArrowUpRight :size="15" /> 去记录中心</RouterLink></div>
    </section>

    <section class="panel archive-panel">
      <div class="archive-toolbar">
        <div class="tab-list" role="tablist" aria-label="记录分类">
          <button v-for="tab in tabs" :key="tab.key" :class="['tab-button', { active: activeTab === tab.key }]" type="button" @click="activeTab = tab.key">
            <component :is="tab.icon" :size="16" /> {{ tab.label }}
          </button>
        </div>
        <button class="icon-button" type="button" title="筛选记录"><ListFilter :size="17" /></button>
      </div>

      <div v-if="activeTab === 'movement'" class="record-list">
        <div v-for="session in store.movementSessions" :key="session.clientRecordId" class="record-row">
          <div class="record-symbol coral-symbol"><Activity :size="18" /></div>
          <div class="record-main"><strong>{{ session.movementCount }} 次胎动</strong><span>{{ formatDate(session.startedAt) }} · {{ session.sessionMode === 'target' ? '目标计时' : '自由记录' }}</span></div>
          <div class="record-detail"><strong>{{ Math.max(0, Math.floor((new Date(session.endedAt).getTime() - new Date(session.startedAt).getTime()) / 60000)) }} 分钟</strong><span>云端记录</span></div>
        </div>
      </div>

      <div v-else-if="activeTab === 'contraction'" class="record-list">
        <div v-for="session in store.contractions" :key="session.clientRecordId" class="record-row">
          <div class="record-symbol blue-symbol"><Waves :size="18" /></div>
          <div class="record-main"><strong>宫缩记录</strong><span>{{ formatDate(session.startedAt) }} · 持续 {{ session.durationSeconds }} 秒</span></div>
          <div class="record-detail"><strong>{{ session.intervalSeconds ?? '--' }} 秒</strong><span>间隔</span></div>
        </div>
      </div>

      <div v-else-if="activeTab === 'health'" class="record-list">
        <div v-for="record in store.healthRecords" :key="record.clientRecordId" class="record-row">
          <div class="record-symbol yellow-symbol"><HeartPulse :size="18" /></div>
          <div class="record-main"><strong>{{ healthLabel(record.recordType) }}</strong><span>{{ formatDate(record.recordedAt) }}{{ record.note ? ` · ${record.note}` : '' }}</span></div>
          <div class="record-detail"><strong>{{ record.valueJson.replace(/[{}\"\[\]]/g, ' ') }}</strong><span>{{ record.unit || '记录' }}</span></div>
        </div>
      </div>

      <div v-else class="record-list">
        <div v-for="task in store.tasks" :key="task.clientRecordId" class="record-row">
          <div class="record-symbol green-symbol"><CalendarDays :size="18" /></div>
          <div class="record-main"><strong :class="{ struck: task.status === 'DONE' }">{{ task.title }}</strong><span>{{ formatDate(task.plannedAt) }} · {{ task.taskType === 'checkup' ? '产检' : '个人待办' }}</span></div>
          <div class="record-detail"><strong>{{ task.status === 'DONE' ? '已完成' : '待处理' }}</strong><span>{{ task.note || '无备注' }}</span></div>
        </div>
      </div>

      <div v-if="activeCount === 0" class="empty-state"><FileText :size="28" /><strong>还没有这类记录</strong><span>从记录页开始，给自己留下一点可回看的时间。</span></div>
    </section>
  </div>
</template>
