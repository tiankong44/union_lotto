<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { Activity, AlertCircle, ArrowUpRight, CalendarDays, CheckCircle2, FileText, HeartPulse, ListFilter, Pencil, Save, Waves, X } from 'lucide-vue-next'
import { usePregnancyStore } from '../stores/pregnancy'
import type { RecordNoteType } from '../types/pregnancy'

const store = usePregnancyStore()
const activeTab = ref<'movement' | 'contraction' | 'health' | 'tasks'>('movement')
const editingNoteKey = ref<string | null>(null)
const savingNoteKey = ref<string | null>(null)
const noteDrafts = reactive<Record<string, string>>({})
const noteErrors = reactive<Record<string, string>>({})

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

function noteKey(recordType: RecordNoteType, clientRecordId: string): string {
  return `${recordType}:${clientRecordId}`
}

function isEditingNote(recordType: RecordNoteType, clientRecordId: string): boolean {
  return editingNoteKey.value === noteKey(recordType, clientRecordId)
}

function beginNoteEdit(recordType: RecordNoteType, clientRecordId: string, note?: string): void {
  const key = noteKey(recordType, clientRecordId)
  editingNoteKey.value = key
  noteDrafts[key] = note ?? ''
  delete noteErrors[key]
}

function cancelNoteEdit(recordType: RecordNoteType, clientRecordId: string): void {
  const key = noteKey(recordType, clientRecordId)
  delete noteDrafts[key]
  delete noteErrors[key]
  if (editingNoteKey.value === key) editingNoteKey.value = null
}

async function saveNote(recordType: RecordNoteType, clientRecordId: string): Promise<void> {
  const key = noteKey(recordType, clientRecordId)
  if (savingNoteKey.value) return
  savingNoteKey.value = key
  delete noteErrors[key]
  try {
    await store.updateRecordNote({ recordType, clientRecordId, note: noteDrafts[key] ?? '' })
    delete noteDrafts[key]
    if (editingNoteKey.value === key) editingNoteKey.value = null
  } catch (error) {
    noteErrors[key] = error instanceof Error ? error.message : '备注保存失败，请重试'
  } finally {
    savingNoteKey.value = null
  }
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
          <div class="record-detail"><strong>{{ Math.max(0, Math.floor((new Date(session.endedAt).getTime() - new Date(session.startedAt).getTime()) / 60000)) }} 分钟</strong><span>{{ session.note || '无备注' }}</span></div>
          <button class="icon-button record-note-action" type="button" :title="session.note ? '编辑备注' : '添加备注'" :disabled="Boolean(savingNoteKey)" @click="beginNoteEdit('movement', session.clientRecordId, session.note)"><Pencil :size="15" /></button>
          <div v-if="isEditingNote('movement', session.clientRecordId)" class="record-note-editor">
            <textarea v-model="noteDrafts[noteKey('movement', session.clientRecordId)]" rows="2" maxlength="500" placeholder="补充这次胎动记录的备注"></textarea>
            <div class="record-note-editor-footer"><span v-if="noteErrors[noteKey('movement', session.clientRecordId)]" class="record-note-error"><AlertCircle :size="14" />{{ noteErrors[noteKey('movement', session.clientRecordId)] }}</span><span v-else></span><span class="record-note-editor-actions"><button class="text-button" type="button" :disabled="savingNoteKey === noteKey('movement', session.clientRecordId)" @click="cancelNoteEdit('movement', session.clientRecordId)"><X :size="14" /> 取消</button><button class="secondary-button" type="button" :disabled="savingNoteKey === noteKey('movement', session.clientRecordId)" @click="saveNote('movement', session.clientRecordId)"><Save :size="14" /> {{ savingNoteKey === noteKey('movement', session.clientRecordId) ? '保存中' : '保存备注' }}</button></span></div>
          </div>
        </div>
      </div>

      <div v-else-if="activeTab === 'contraction'" class="record-list">
        <div v-for="session in store.contractions" :key="session.clientRecordId" class="record-row">
          <div class="record-symbol blue-symbol"><Waves :size="18" /></div>
          <div class="record-main"><strong>宫缩记录</strong><span>{{ formatDate(session.startedAt) }} · 持续 {{ session.durationSeconds }} 秒</span></div>
          <div class="record-detail"><strong>{{ session.intervalSeconds ?? '--' }} 秒</strong><span>{{ session.note || '无备注' }}</span></div>
          <button class="icon-button record-note-action" type="button" :title="session.note ? '编辑备注' : '添加备注'" :disabled="Boolean(savingNoteKey)" @click="beginNoteEdit('contraction', session.clientRecordId, session.note)"><Pencil :size="15" /></button>
          <div v-if="isEditingNote('contraction', session.clientRecordId)" class="record-note-editor">
            <textarea v-model="noteDrafts[noteKey('contraction', session.clientRecordId)]" rows="2" maxlength="500" placeholder="补充这次宫缩记录的备注"></textarea>
            <div class="record-note-editor-footer"><span v-if="noteErrors[noteKey('contraction', session.clientRecordId)]" class="record-note-error"><AlertCircle :size="14" />{{ noteErrors[noteKey('contraction', session.clientRecordId)] }}</span><span v-else></span><span class="record-note-editor-actions"><button class="text-button" type="button" :disabled="savingNoteKey === noteKey('contraction', session.clientRecordId)" @click="cancelNoteEdit('contraction', session.clientRecordId)"><X :size="14" /> 取消</button><button class="secondary-button" type="button" :disabled="savingNoteKey === noteKey('contraction', session.clientRecordId)" @click="saveNote('contraction', session.clientRecordId)"><Save :size="14" /> {{ savingNoteKey === noteKey('contraction', session.clientRecordId) ? '保存中' : '保存备注' }}</button></span></div>
          </div>
        </div>
      </div>

      <div v-else-if="activeTab === 'health'" class="record-list">
        <div v-for="record in store.healthRecords" :key="record.clientRecordId" class="record-row">
          <div class="record-symbol yellow-symbol"><HeartPulse :size="18" /></div>
          <div class="record-main"><strong>{{ healthLabel(record.recordType) }}</strong><span>{{ formatDate(record.recordedAt) }}</span></div>
          <div class="record-detail"><strong>{{ record.valueJson.replace(/[{}\"\[\]]/g, ' ') }}</strong><span>{{ record.note || '无备注' }} · {{ record.unit || '记录' }}</span></div>
          <button class="icon-button record-note-action" type="button" :title="record.note ? '编辑备注' : '添加备注'" :disabled="Boolean(savingNoteKey)" @click="beginNoteEdit('health', record.clientRecordId, record.note)"><Pencil :size="15" /></button>
          <div v-if="isEditingNote('health', record.clientRecordId)" class="record-note-editor">
            <textarea v-model="noteDrafts[noteKey('health', record.clientRecordId)]" rows="2" maxlength="500" placeholder="补充这条健康记录的备注"></textarea>
            <div class="record-note-editor-footer"><span v-if="noteErrors[noteKey('health', record.clientRecordId)]" class="record-note-error"><AlertCircle :size="14" />{{ noteErrors[noteKey('health', record.clientRecordId)] }}</span><span v-else></span><span class="record-note-editor-actions"><button class="text-button" type="button" :disabled="savingNoteKey === noteKey('health', record.clientRecordId)" @click="cancelNoteEdit('health', record.clientRecordId)"><X :size="14" /> 取消</button><button class="secondary-button" type="button" :disabled="savingNoteKey === noteKey('health', record.clientRecordId)" @click="saveNote('health', record.clientRecordId)"><Save :size="14" /> {{ savingNoteKey === noteKey('health', record.clientRecordId) ? '保存中' : '保存备注' }}</button></span></div>
          </div>
        </div>
      </div>

      <div v-else class="record-list">
        <div v-for="task in store.tasks" :key="task.clientRecordId" class="record-row">
          <div class="record-symbol green-symbol"><CalendarDays :size="18" /></div>
          <div class="record-main"><strong :class="{ struck: task.status === 'DONE' }">{{ task.title }}</strong><span>{{ formatDate(task.plannedAt) }} · {{ task.taskType === 'checkup' ? '产检' : '个人待办' }}</span></div>
          <div class="record-detail"><strong>{{ task.status === 'DONE' ? '已完成' : '待处理' }}</strong><span>{{ task.note || '无备注' }}</span></div>
          <button class="icon-button record-note-action" type="button" :title="task.note ? '编辑备注' : '添加备注'" :disabled="Boolean(savingNoteKey)" @click="beginNoteEdit('task', task.clientRecordId, task.note)"><Pencil :size="15" /></button>
          <div v-if="isEditingNote('task', task.clientRecordId)" class="record-note-editor">
            <textarea v-model="noteDrafts[noteKey('task', task.clientRecordId)]" rows="2" maxlength="500" placeholder="补充这条待办的备注"></textarea>
            <div class="record-note-editor-footer"><span v-if="noteErrors[noteKey('task', task.clientRecordId)]" class="record-note-error"><AlertCircle :size="14" />{{ noteErrors[noteKey('task', task.clientRecordId)] }}</span><span v-else></span><span class="record-note-editor-actions"><button class="text-button" type="button" :disabled="savingNoteKey === noteKey('task', task.clientRecordId)" @click="cancelNoteEdit('task', task.clientRecordId)"><X :size="14" /> 取消</button><button class="secondary-button" type="button" :disabled="savingNoteKey === noteKey('task', task.clientRecordId)" @click="saveNote('task', task.clientRecordId)"><Save :size="14" /> {{ savingNoteKey === noteKey('task', task.clientRecordId) ? '保存中' : '保存备注' }}</button></span></div>
          </div>
        </div>
      </div>

      <div v-if="activeCount === 0" class="empty-state"><FileText :size="28" /><strong>还没有这类记录</strong><span>从记录页开始，给自己留下一点可回看的时间。</span></div>
    </section>
  </div>
</template>
