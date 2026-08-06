import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  getProfile,
  listContractions,
  listFetalMovements,
  listHealthRecords,
  listTasks,
  saveContraction as saveContractionRequest,
  saveFetalMovement as saveFetalMovementRequest,
  saveHealthRecord as saveHealthRecordRequest,
  saveProfile as saveProfileRequest,
  saveTask as saveTaskRequest,
  deleteRecord as deleteRecordRequest,
  updateRecordNote as updateRecordNoteRequest,
} from '../services/api'
import type {
  AntenatalTask,
  CloudStatus,
  ContractionSession,
  ContractionSessionPayload,
  FetalMovementSession,
  FetalMovementSessionPayload,
  HealthRecord,
  HealthRecordPayload,
  PregnancyProfile,
  PregnancyProfilePayload,
  RecordDeletePayload,
  RecordNoteUpdatePayload,
} from '../types/pregnancy'

export const usePregnancyStore = defineStore('pregnancy', () => {
  const profile = ref<PregnancyProfile | null>(null)
  const movementSessions = ref<FetalMovementSession[]>([])
  const contractions = ref<ContractionSession[]>([])
  const healthRecords = ref<HealthRecord[]>([])
  const tasks = ref<AntenatalTask[]>([])
  const cloudStatus = ref<CloudStatus>('idle')
  const lastLoadedAt = ref<string | null>(null)
  const errorMessage = ref('')
  const hydrated = ref(false)

  const pendingTasks = computed(() => tasks.value.filter((task) => task.status === 'TODO'))
  const todayMovements = computed(() => {
    const today = new Date().toDateString()
    return movementSessions.value.filter((session) => new Date(session.startedAt).toDateString() === today)
  })

  function getError(error: unknown, fallback: string): Error {
    return error instanceof Error ? error : new Error(fallback)
  }

  function isOnline(): boolean {
    return typeof navigator === 'undefined' || navigator.onLine
  }

  async function saveToCloud<T>(request: () => Promise<T>): Promise<T> {
    if (!isOnline()) {
      const error = new Error('当前没有网络，无法保存到云端。')
      cloudStatus.value = 'error'
      errorMessage.value = error.message
      throw error
    }
    cloudStatus.value = 'saving'
    errorMessage.value = ''
    try {
      const result = await request()
      cloudStatus.value = 'idle'
      return result
    } catch (error) {
      const cloudError = getError(error, '云端保存失败，请稍后重试。')
      cloudStatus.value = 'error'
      errorMessage.value = cloudError.message
      throw cloudError
    }
  }

  async function hydrate(force = false): Promise<void> {
    if (hydrated.value && !force) return
    if (!isOnline()) {
      cloudStatus.value = 'error'
      errorMessage.value = '当前没有网络，无法加载云端记录。'
      return
    }
    cloudStatus.value = 'loading'
    errorMessage.value = ''
    try {
      const [nextProfile, nextMovements, nextContractions, nextHealthRecords, nextTasks] = await Promise.all([
        getProfile(),
        listFetalMovements(),
        listContractions(),
        listHealthRecords(),
        listTasks(),
      ])
      profile.value = nextProfile
      movementSessions.value = nextMovements
      contractions.value = nextContractions
      healthRecords.value = nextHealthRecords
      tasks.value = nextTasks
      hydrated.value = true
      lastLoadedAt.value = new Date().toISOString()
      cloudStatus.value = 'idle'
    } catch (error) {
      const cloudError = getError(error, '云端加载失败，请稍后重试。')
      cloudStatus.value = 'error'
      errorMessage.value = cloudError.message
    }
  }

  async function saveProfile(nextProfile: PregnancyProfilePayload): Promise<void> {
    const savedProfile = await saveToCloud(() => saveProfileRequest(nextProfile))
    profile.value = savedProfile
  }

  async function addMovementSession(session: FetalMovementSessionPayload): Promise<void> {
    const savedSession = await saveToCloud(() => saveFetalMovementRequest(session))
    movementSessions.value.unshift(savedSession)
  }

  async function addContraction(session: ContractionSessionPayload): Promise<void> {
    const savedSession = await saveToCloud(() => saveContractionRequest(session))
    contractions.value.unshift(savedSession)
  }

  async function addHealthRecord(record: HealthRecordPayload): Promise<void> {
    const savedRecord = await saveToCloud(() => saveHealthRecordRequest(record))
    healthRecords.value.unshift(savedRecord)
  }

  async function addTask(task: AntenatalTask): Promise<void> {
    const savedTask = await saveToCloud(() => saveTaskRequest(task))
    const existingIndex = tasks.value.findIndex((item) => item.clientRecordId === savedTask.clientRecordId)
    if (existingIndex === -1) {
      tasks.value.push(savedTask)
      return
    }
    tasks.value[existingIndex] = savedTask
  }

  async function toggleTask(task: AntenatalTask): Promise<void> {
    const nextTask: AntenatalTask = {
      ...task,
      status: task.status === 'TODO' ? 'DONE' : 'TODO',
      completedAt: task.status === 'TODO' ? new Date().toISOString() : undefined,
    }
    const savedTask = await saveToCloud(() => saveTaskRequest(nextTask))
    const existingIndex = tasks.value.findIndex((item) => item.clientRecordId === savedTask.clientRecordId)
    if (existingIndex !== -1) tasks.value[existingIndex] = savedTask
  }

  async function updateRecordNote(payload: RecordNoteUpdatePayload): Promise<void> {
    await saveToCloud(() => updateRecordNoteRequest(payload))
    const note = payload.note.trim() || undefined
    if (payload.recordType === 'movement') {
      const record = movementSessions.value.find((item) => item.clientRecordId === payload.clientRecordId)
      if (record) record.note = note
      return
    }
    if (payload.recordType === 'contraction') {
      const record = contractions.value.find((item) => item.clientRecordId === payload.clientRecordId)
      if (record) record.note = note
      return
    }
    if (payload.recordType === 'health') {
      const record = healthRecords.value.find((item) => item.clientRecordId === payload.clientRecordId)
      if (record) record.note = note
      return
    }
    const record = tasks.value.find((item) => item.clientRecordId === payload.clientRecordId)
    if (record) record.note = note
  }

  async function deleteRecord(payload: RecordDeletePayload): Promise<void> {
    await saveToCloud(() => deleteRecordRequest(payload))
    if (payload.recordType === 'movement') {
      movementSessions.value = movementSessions.value.filter((item) => item.clientRecordId !== payload.clientRecordId)
      return
    }
    if (payload.recordType === 'contraction') {
      contractions.value = contractions.value.filter((item) => item.clientRecordId !== payload.clientRecordId)
      return
    }
    if (payload.recordType === 'health') {
      healthRecords.value = healthRecords.value.filter((item) => item.clientRecordId !== payload.clientRecordId)
      return
    }
    tasks.value = tasks.value.filter((item) => item.clientRecordId !== payload.clientRecordId)
  }

  async function refreshCloudData(): Promise<void> {
    await hydrate(true)
  }

  return {
    profile,
    movementSessions,
    contractions,
    healthRecords,
    tasks,
    pendingTasks,
    todayMovements,
    cloudStatus,
    lastLoadedAt,
    errorMessage,
    hydrated,
    hydrate,
    refreshCloudData,
    saveProfile,
    addMovementSession,
    addContraction,
    addHealthRecord,
    addTask,
    toggleTask,
    updateRecordNote,
    deleteRecord,
  }
})
