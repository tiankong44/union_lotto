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
}

export interface HealthRecord {
  id?: number
  clientRecordId: string
  recordType: 'weight' | 'blood-pressure' | 'symptom'
  valueJson: string
  unit?: string
  recordedAt: string
  note?: string
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

export type PregnancyProfilePayload = Omit<PregnancyProfile, 'id' | 'createdAt' | 'updatedAt'>
export type FetalMovementSessionPayload = Omit<FetalMovementSession, 'id'>
export type ContractionSessionPayload = Omit<ContractionSession, 'id'>
export type HealthRecordPayload = Omit<HealthRecord, 'id'>
export type CloudStatus = 'idle' | 'loading' | 'saving' | 'error'
