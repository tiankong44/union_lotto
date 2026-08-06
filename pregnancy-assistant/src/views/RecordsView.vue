<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { Activity, AlertCircle, AlertTriangle, ArrowUpRight, BarChart3, CalendarDays, CheckCircle2, FileText, HeartPulse, Pencil, Save, Trash2, Waves, X } from 'lucide-vue-next'
import RecordChartPanel from '../components/RecordChartPanel.vue'
import WorkspaceLink from '../components/WorkspaceLink.vue'
import { usePregnancyStore } from '../stores/pregnancy'
import type { HealthRecord, RecordChartType, RecordNoteType } from '../types/pregnancy'

interface PendingDelete {
  recordType: RecordNoteType
  clientRecordId: string
}

const store = usePregnancyStore()
const activeTab = ref<RecordChartType>('movement')
const showChart = ref(false)
const editingNoteKey = ref<string | null>(null)
const savingNoteKey = ref<string | null>(null)
const deletingRecordKey = ref<string | null>(null)
const pendingDelete = ref<PendingDelete | null>(null)
const noteDrafts = reactive<Record<string, string>>({})
const noteErrors = reactive<Record<string, string>>({})
const deleteErrors = reactive<Record<string, string>>({})

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

const activeTabLabel = computed(() => tabs.find((tab) => tab.key === activeTab.value)?.label ?? '记录')

const pendingDeleteLabel = computed(() => tabs.find((tab) => tab.key === pendingDelete.value?.recordType)?.label ?? '这条')

function selectTab(tab: RecordChartType): void {
  activeTab.value = tab
  showChart.value = false
}

function toggleChart(): void {
  showChart.value = !showChart.value
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat('zh-CN', { month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit' }).format(new Date(value))
}

function formatDurationSeconds(value: number | undefined): string {
  if (value === undefined || !Number.isFinite(value) || value < 0) return '未记录'
  const totalSeconds = Math.floor(value)
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  if (minutes === 0) return `${seconds} 秒`
  return `${minutes} 分 ${seconds} 秒`
}

function healthLabel(type: string): string {
  return { weight: '体重', 'blood-pressure': '血压', symptom: '症状' }[type] ?? type
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

function openDeleteDialog(recordType: RecordNoteType, clientRecordId: string): void {
  const key = noteKey(recordType, clientRecordId)
  if (deletingRecordKey.value) return
  delete deleteErrors[key]
  pendingDelete.value = { recordType, clientRecordId }
}

function closeDeleteDialog(): void {
  if (deletingRecordKey.value) return
  pendingDelete.value = null
}

async function confirmDelete(): Promise<void> {
  const target = pendingDelete.value
  if (!target || deletingRecordKey.value) return
  const { recordType, clientRecordId } = target
  const key = noteKey(recordType, clientRecordId)
  deletingRecordKey.value = key
  delete deleteErrors[key]
  try {
    await store.deleteRecord({ recordType, clientRecordId })
    cancelNoteEdit(recordType, clientRecordId)
    pendingDelete.value = null
  } catch (error) {
    deleteErrors[key] = error instanceof Error ? error.message : '删除失败，请重试'
    pendingDelete.value = null
  } finally {
    deletingRecordKey.value = null
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
      <div class="archive-actions"><div class="archive-count"><strong>{{ activeCount }}</strong><span>条记录</span></div><WorkspaceLink class="secondary-button" view="record-center"><ArrowUpRight :size="15" /> 去记录中心</WorkspaceLink></div>
    </section>

    <section class="panel archive-panel">
      <div class="archive-toolbar">
        <div class="archive-toolbar-main">
          <span class="archive-toolbar-label">记录类型</span>
          <div class="tab-list" role="tablist" aria-label="记录分类">
            <button v-for="tab in tabs" :key="tab.key" :class="['tab-button', { active: activeTab === tab.key }]" type="button" @click="selectTab(tab.key)">
              <component :is="tab.icon" :size="16" /> {{ tab.label }}
            </button>
          </div>
          <button class="icon-button archive-chart-toggle" type="button" :class="{ active: showChart }" :title="showChart ? `收起${activeTabLabel}图表` : `查看${activeTabLabel}图表`" :aria-label="showChart ? `收起${activeTabLabel}图表` : `查看${activeTabLabel}图表`" :aria-expanded="showChart" @click="toggleChart"><BarChart3 :size="17" /></button>
        </div>
        <span class="archive-toolbar-summary">当前 {{ activeCount }} 条</span>
      </div>

      <RecordChartPanel v-if="showChart" :key="activeTab" :record-type="activeTab" :movement-sessions="store.movementSessions" :contractions="store.contractions" :health-records="store.healthRecords" :tasks="store.tasks" />

      <div v-if="activeTab === 'movement'" class="record-list">
        <div v-for="session in store.movementSessions" :key="session.clientRecordId" class="record-row">
          <div class="record-symbol coral-symbol"><Activity :size="18" /></div>
          <div class="record-main"><strong>{{ session.movementCount }} 次胎动</strong><span>{{ formatDate(session.startedAt) }} · {{ session.sessionMode === 'target' ? '目标计时' : '自由记录' }}</span></div>
          <div class="record-detail"><strong>{{ Math.max(0, Math.floor((new Date(session.endedAt).getTime() - new Date(session.startedAt).getTime()) / 60000)) }} 分钟</strong><span>{{ session.note || '无备注' }}</span></div>
          <span class="record-row-actions"><button class="icon-button record-note-action" type="button" :title="session.note ? '编辑备注' : '添加备注'" :disabled="Boolean(savingNoteKey) || Boolean(deletingRecordKey)" @click="beginNoteEdit('movement', session.clientRecordId, session.note)"><Pencil :size="15" /></button><button class="icon-button record-delete-action" type="button" title="删除记录" :disabled="Boolean(savingNoteKey) || Boolean(deletingRecordKey)" @click="openDeleteDialog('movement', session.clientRecordId)"><Trash2 :size="15" /></button></span>
          <div v-if="isEditingNote('movement', session.clientRecordId)" class="record-note-editor">
            <textarea v-model="noteDrafts[noteKey('movement', session.clientRecordId)]" rows="2" maxlength="500" placeholder="补充这次胎动记录的备注"></textarea>
            <div class="record-note-editor-footer"><span v-if="noteErrors[noteKey('movement', session.clientRecordId)]" class="record-note-error"><AlertCircle :size="14" />{{ noteErrors[noteKey('movement', session.clientRecordId)] }}</span><span v-else></span><span class="record-note-editor-actions"><button class="text-button" type="button" :disabled="savingNoteKey === noteKey('movement', session.clientRecordId)" @click="cancelNoteEdit('movement', session.clientRecordId)"><X :size="14" /> 取消</button><button class="secondary-button" type="button" :disabled="savingNoteKey === noteKey('movement', session.clientRecordId)" @click="saveNote('movement', session.clientRecordId)"><Save :size="14" /> {{ savingNoteKey === noteKey('movement', session.clientRecordId) ? '保存中' : '保存备注' }}</button></span></div>
          </div>
          <div v-if="deleteErrors[noteKey('movement', session.clientRecordId)]" class="record-delete-error"><AlertCircle :size="14" />{{ deleteErrors[noteKey('movement', session.clientRecordId)] }}</div>
        </div>
      </div>

      <div v-else-if="activeTab === 'contraction'" class="record-list">
        <div v-for="session in store.contractions" :key="session.clientRecordId" class="record-row">
          <div class="record-symbol blue-symbol"><Waves :size="18" /></div>
          <div class="record-main"><strong>宫缩记录</strong><span>{{ formatDate(session.startedAt) }} · 持续 {{ formatDurationSeconds(session.durationSeconds) }}</span></div>
          <div class="record-detail"><strong>间隔 {{ formatDurationSeconds(session.intervalSeconds) }}</strong><span>{{ session.note || '无备注' }}</span></div>
          <span class="record-row-actions"><button class="icon-button record-note-action" type="button" :title="session.note ? '编辑备注' : '添加备注'" :disabled="Boolean(savingNoteKey) || Boolean(deletingRecordKey)" @click="beginNoteEdit('contraction', session.clientRecordId, session.note)"><Pencil :size="15" /></button><button class="icon-button record-delete-action" type="button" title="删除记录" :disabled="Boolean(savingNoteKey) || Boolean(deletingRecordKey)" @click="openDeleteDialog('contraction', session.clientRecordId)"><Trash2 :size="15" /></button></span>
          <div v-if="isEditingNote('contraction', session.clientRecordId)" class="record-note-editor">
            <textarea v-model="noteDrafts[noteKey('contraction', session.clientRecordId)]" rows="2" maxlength="500" placeholder="补充这次宫缩记录的备注"></textarea>
            <div class="record-note-editor-footer"><span v-if="noteErrors[noteKey('contraction', session.clientRecordId)]" class="record-note-error"><AlertCircle :size="14" />{{ noteErrors[noteKey('contraction', session.clientRecordId)] }}</span><span v-else></span><span class="record-note-editor-actions"><button class="text-button" type="button" :disabled="savingNoteKey === noteKey('contraction', session.clientRecordId)" @click="cancelNoteEdit('contraction', session.clientRecordId)"><X :size="14" /> 取消</button><button class="secondary-button" type="button" :disabled="savingNoteKey === noteKey('contraction', session.clientRecordId)" @click="saveNote('contraction', session.clientRecordId)"><Save :size="14" /> {{ savingNoteKey === noteKey('contraction', session.clientRecordId) ? '保存中' : '保存备注' }}</button></span></div>
          </div>
          <div v-if="deleteErrors[noteKey('contraction', session.clientRecordId)]" class="record-delete-error"><AlertCircle :size="14" />{{ deleteErrors[noteKey('contraction', session.clientRecordId)] }}</div>
        </div>
      </div>

      <div v-else-if="activeTab === 'health'" class="record-list">
        <div v-for="record in store.healthRecords" :key="record.clientRecordId" class="record-row">
          <div class="record-symbol yellow-symbol"><HeartPulse :size="18" /></div>
          <div class="record-main"><strong>{{ healthLabel(record.recordType) }}</strong><span>{{ formatDate(record.recordedAt) }}</span></div>
          <div class="record-detail"><strong>{{ healthValue(record) }}</strong><span>{{ record.note || '无备注' }} · {{ record.unit || '记录' }}</span></div>
          <span class="record-row-actions"><button class="icon-button record-note-action" type="button" :title="record.note ? '编辑备注' : '添加备注'" :disabled="Boolean(savingNoteKey) || Boolean(deletingRecordKey)" @click="beginNoteEdit('health', record.clientRecordId, record.note)"><Pencil :size="15" /></button><button class="icon-button record-delete-action" type="button" title="删除记录" :disabled="Boolean(savingNoteKey) || Boolean(deletingRecordKey)" @click="openDeleteDialog('health', record.clientRecordId)"><Trash2 :size="15" /></button></span>
          <div v-if="isEditingNote('health', record.clientRecordId)" class="record-note-editor">
            <textarea v-model="noteDrafts[noteKey('health', record.clientRecordId)]" rows="2" maxlength="500" placeholder="补充这条健康记录的备注"></textarea>
            <div class="record-note-editor-footer"><span v-if="noteErrors[noteKey('health', record.clientRecordId)]" class="record-note-error"><AlertCircle :size="14" />{{ noteErrors[noteKey('health', record.clientRecordId)] }}</span><span v-else></span><span class="record-note-editor-actions"><button class="text-button" type="button" :disabled="savingNoteKey === noteKey('health', record.clientRecordId)" @click="cancelNoteEdit('health', record.clientRecordId)"><X :size="14" /> 取消</button><button class="secondary-button" type="button" :disabled="savingNoteKey === noteKey('health', record.clientRecordId)" @click="saveNote('health', record.clientRecordId)"><Save :size="14" /> {{ savingNoteKey === noteKey('health', record.clientRecordId) ? '保存中' : '保存备注' }}</button></span></div>
          </div>
          <div v-if="deleteErrors[noteKey('health', record.clientRecordId)]" class="record-delete-error"><AlertCircle :size="14" />{{ deleteErrors[noteKey('health', record.clientRecordId)] }}</div>
        </div>
      </div>

      <div v-else class="record-list">
        <div v-for="task in store.tasks" :key="task.clientRecordId" class="record-row">
          <div class="record-symbol green-symbol"><CalendarDays :size="18" /></div>
          <div class="record-main"><strong :class="{ struck: task.status === 'DONE' }">{{ task.title }}</strong><span>{{ formatDate(task.plannedAt) }} · {{ task.taskType === 'checkup' ? '产检' : '个人待办' }}</span></div>
          <div class="record-detail"><strong>{{ task.status === 'DONE' ? '已完成' : '待处理' }}</strong><span>{{ task.note || '无备注' }}</span></div>
          <span class="record-row-actions"><button class="icon-button record-note-action" type="button" :title="task.note ? '编辑备注' : '添加备注'" :disabled="Boolean(savingNoteKey) || Boolean(deletingRecordKey)" @click="beginNoteEdit('task', task.clientRecordId, task.note)"><Pencil :size="15" /></button><button class="icon-button record-delete-action" type="button" title="删除记录" :disabled="Boolean(savingNoteKey) || Boolean(deletingRecordKey)" @click="openDeleteDialog('task', task.clientRecordId)"><Trash2 :size="15" /></button></span>
          <div v-if="isEditingNote('task', task.clientRecordId)" class="record-note-editor">
            <textarea v-model="noteDrafts[noteKey('task', task.clientRecordId)]" rows="2" maxlength="500" placeholder="补充这条待办的备注"></textarea>
            <div class="record-note-editor-footer"><span v-if="noteErrors[noteKey('task', task.clientRecordId)]" class="record-note-error"><AlertCircle :size="14" />{{ noteErrors[noteKey('task', task.clientRecordId)] }}</span><span v-else></span><span class="record-note-editor-actions"><button class="text-button" type="button" :disabled="savingNoteKey === noteKey('task', task.clientRecordId)" @click="cancelNoteEdit('task', task.clientRecordId)"><X :size="14" /> 取消</button><button class="secondary-button" type="button" :disabled="savingNoteKey === noteKey('task', task.clientRecordId)" @click="saveNote('task', task.clientRecordId)"><Save :size="14" /> {{ savingNoteKey === noteKey('task', task.clientRecordId) ? '保存中' : '保存备注' }}</button></span></div>
          </div>
          <div v-if="deleteErrors[noteKey('task', task.clientRecordId)]" class="record-delete-error"><AlertCircle :size="14" />{{ deleteErrors[noteKey('task', task.clientRecordId)] }}</div>
        </div>
      </div>

      <div v-if="activeCount === 0" class="empty-state"><FileText :size="28" /><strong>还没有这类记录</strong><span>从记录页开始，给自己留下一点可回看的时间。</span></div>
    </section>

    <div v-if="pendingDelete" class="delete-dialog-backdrop" role="presentation" @click.self="closeDeleteDialog">
      <section class="delete-dialog" role="dialog" aria-modal="true" aria-labelledby="delete-dialog-title" aria-describedby="delete-dialog-description">
        <button class="delete-dialog-close icon-button" type="button" title="关闭确认框" aria-label="关闭确认框" :disabled="Boolean(deletingRecordKey)" @click="closeDeleteDialog"><X :size="18" /></button>
        <div class="delete-dialog-icon" aria-hidden="true"><AlertTriangle :size="22" /></div>
        <p class="eyebrow">DELETE RECORD</p>
        <h2 id="delete-dialog-title">删除{{ pendingDeleteLabel }}记录？</h2>
        <p id="delete-dialog-description">删除后无法恢复，确认要移除这条历史记录吗？</p>
        <div class="delete-dialog-actions">
          <button class="secondary-button" type="button" :disabled="Boolean(deletingRecordKey)" @click="closeDeleteDialog">取消</button>
          <button class="primary-button delete-dialog-confirm" type="button" :disabled="Boolean(deletingRecordKey)" @click="confirmDelete"><span v-if="deletingRecordKey" class="button-loader" aria-hidden="true"></span>{{ deletingRecordKey ? '删除中' : '确认删除' }}</button>
        </div>
      </section>
    </div>
  </div>
</template>
