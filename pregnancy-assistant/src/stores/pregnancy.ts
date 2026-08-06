import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { enqueueSync, clearSyncQueue, listSyncQueue, loadSnapshot, saveSnapshot } from '../services/localDb'
import { syncRecords } from '../services/api'
import type {
  AntenatalTask,
  ContractionSession,
  FetalMovementSession,
  HealthRecord,
  PregnancyProfile,
  SyncQueueItem,
  SyncStatus,
} from '../types/pregnancy'

export const usePregnancyStore = defineStore('pregnancy', () => {
  const profile = ref<PregnancyProfile | null>(null)
  const movementSessions = ref<FetalMovementSession[]>([])
  const contractions = ref<ContractionSession[]>([])
  const healthRecords = ref<HealthRecord[]>([])
  const tasks = ref<AntenatalTask[]>([])
  const syncStatus = ref<SyncStatus>('idle')
  const lastSyncedAt = ref<string | null>(null)
  const errorMessage = ref('')
  const hydrated = ref(false)

  const pendingTasks = computed(() => tasks.value.filter((task) => task.status === 'TODO'))
  const todayMovements = computed(() => {
    const today = new Date().toDateString()
    return movementSessions.value.filter((session) => new Date(session.startedAt).toDateString() === today)
  })

  async function persist(): Promise<void> {
    await saveSnapshot({
      profile: profile.value ? ({ ...profile.value } as unknown as Record<string, unknown>) : null,
      movementSessions: movementSessions.value.map((session) => ({ ...session })) as unknown as Record<string, unknown>[],
      contractions: contractions.value.map((session) => ({ ...session })) as unknown as Record<string, unknown>[],
      healthRecords: healthRecords.value.map((record) => ({ ...record })) as unknown as Record<string, unknown>[],
      tasks: tasks.value.map((task) => ({ ...task })) as unknown as Record<string, unknown>[],
      lastSyncedAt: lastSyncedAt.value,
    })
  }

  async function hydrate(): Promise<void> {
    if (hydrated.value) return
    const snapshot = await loadSnapshot()
    if (snapshot) {
      profile.value = snapshot.profile as PregnancyProfile | null
      movementSessions.value = snapshot.movementSessions as unknown as FetalMovementSession[]
      contractions.value = snapshot.contractions as unknown as ContractionSession[]
      healthRecords.value = snapshot.healthRecords as unknown as HealthRecord[]
      tasks.value = snapshot.tasks as unknown as AntenatalTask[]
      lastSyncedAt.value = snapshot.lastSyncedAt
    }
    hydrated.value = true
    syncStatus.value = navigator.onLine ? 'idle' : 'offline'
  }

  async function queue(entityType: string, clientRecordId: string, payload: unknown): Promise<void> {
    const item: SyncQueueItem = {
      entityType,
      clientRecordId,
      payload: JSON.stringify(payload),
      createdAt: new Date().toISOString(),
    }
    await enqueueSync(item)
  }

  async function saveProfile(nextProfile: Omit<PregnancyProfile, 'id' | 'createdAt' | 'updatedAt'>): Promise<void> {
    profile.value = { ...nextProfile }
    await persist()
    await queue('profile', 'profile-singleton', profile.value)
    syncStatus.value = navigator.onLine ? 'idle' : 'offline'
  }

  async function addMovementSession(session: Omit<FetalMovementSession, 'recordStatus'>): Promise<void> {
    movementSessions.value.unshift({ ...session, recordStatus: 'LOCAL' })
    await persist()
    await queue('fetal-movement-session', session.clientRecordId, session)
    syncStatus.value = navigator.onLine ? 'idle' : 'offline'
  }

  async function addContraction(session: Omit<ContractionSession, 'recordStatus'>): Promise<void> {
    contractions.value.unshift({ ...session, recordStatus: 'LOCAL' })
    await persist()
    await queue('contraction-session', session.clientRecordId, session)
    syncStatus.value = navigator.onLine ? 'idle' : 'offline'
  }

  async function addHealthRecord(record: Omit<HealthRecord, 'recordStatus'>): Promise<void> {
    healthRecords.value.unshift({ ...record, recordStatus: 'LOCAL' })
    await persist()
    await queue('health-record', record.clientRecordId, record)
    syncStatus.value = navigator.onLine ? 'idle' : 'offline'
  }

  async function addTask(task: AntenatalTask): Promise<void> {
    tasks.value.push(task)
    await persist()
    await queue('antenatal-task', task.clientRecordId, task)
    syncStatus.value = navigator.onLine ? 'idle' : 'offline'
  }

  async function toggleTask(task: AntenatalTask): Promise<void> {
    task.status = task.status === 'TODO' ? 'DONE' : 'TODO'
    task.completedAt = task.status === 'DONE' ? new Date().toISOString() : undefined
    await persist()
    await queue('antenatal-task', task.clientRecordId, task)
  }

  async function syncNow(): Promise<void> {
    if (!navigator.onLine) {
      syncStatus.value = 'offline'
      errorMessage.value = '当前没有网络，记录已保存在本机。'
      return
    }
    const queueItems = await listSyncQueue()
    if (!queueItems.length) {
      syncStatus.value = 'synced'
      errorMessage.value = ''
      return
    }
    syncStatus.value = 'syncing'
    errorMessage.value = ''
    try {
      await syncRecords(queueItems)
      await clearSyncQueue(queueItems.map((item) => item.id).filter((id): id is number => typeof id === 'number'))
      movementSessions.value = movementSessions.value.map((item) => ({ ...item, recordStatus: 'SYNCED' }))
      contractions.value = contractions.value.map((item) => ({ ...item, recordStatus: 'SYNCED' }))
      healthRecords.value = healthRecords.value.map((item) => ({ ...item, recordStatus: 'SYNCED' }))
      lastSyncedAt.value = new Date().toISOString()
      syncStatus.value = 'synced'
      await persist()
    } catch (error) {
      syncStatus.value = 'error'
      errorMessage.value = error instanceof Error ? error.message : '同步失败，稍后可重试。'
    }
  }

  return {
    profile,
    movementSessions,
    contractions,
    healthRecords,
    tasks,
    pendingTasks,
    todayMovements,
    syncStatus,
    lastSyncedAt,
    errorMessage,
    hydrated,
    hydrate,
    saveProfile,
    addMovementSession,
    addContraction,
    addHealthRecord,
    addTask,
    toggleTask,
    syncNow,
  }
})
