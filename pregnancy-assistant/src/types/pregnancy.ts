export interface PregnancyProfile {
  id?: number
  lmpDate?: string
  dueDate?: string
  babyCount: number
  nickname?: string
  note?: string
  createdAt?: string
  updatedAt?: string
}

export interface FetalMovementSession {
  id?: number
  clientRecordId: string
  sessionMode: 'free' | 'target'
  startedAt: string
  endedAt: string
  movementCount: number
  targetCount?: number
  averageStrength?: number
  note?: string
  recordStatus: 'LOCAL' | 'SYNCED'
}

export interface ContractionSession {
  id?: number
  clientRecordId: string
  startedAt: string
  endedAt: string
  durationSeconds: number
  intervalSeconds?: number
  intensity?: number
  note?: string
  recordStatus: 'LOCAL' | 'SYNCED'
}

export interface HealthRecord {
  id?: number
  clientRecordId: string
  recordType: 'weight' | 'blood-pressure' | 'symptom'
  valueJson: string
  unit?: string
  recordedAt: string
  note?: string
  recordStatus: 'LOCAL' | 'SYNCED'
}

export interface AntenatalTask {
  id?: number
  clientRecordId: string
  title: string
  taskType: 'checkup' | 'todo' | 'custom'
  plannedAt: string
  status: 'TODO' | 'DONE'
  note?: string
  completedAt?: string
}

export interface SyncQueueItem {
  id?: number
  entityType: string
  clientRecordId: string
  payload: string
  createdAt: string
}

export type SyncStatus = 'idle' | 'syncing' | 'synced' | 'offline' | 'error'
