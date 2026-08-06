import type {
  AntenatalTask,
  ContractionSession,
  ContractionSessionPayload,
  FetalMovementSession,
  FetalMovementSessionPayload,
  HealthRecord,
  HealthRecordPayload,
  PregnancyProfile,
  PregnancyProfilePayload,
} from '../types/pregnancy'

const API_ROOT = '/tabs/pregnancy'

interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(`${API_ROOT}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers ?? {}),
    },
  })
  if (!response.ok) {
    throw new Error(`云端服务返回 ${response.status}`)
  }
  const body = (await response.json()) as ApiResponse<T>
  if (body.code !== 0) {
    throw new Error(body.message || '云端服务处理失败')
  }
  return body.data
}

function buildDateQuery(startDate?: string, endDate?: string): string {
  const params = new URLSearchParams()
  if (startDate) params.set('startDate', startDate)
  if (endDate) params.set('endDate', endDate)
  const query = params.toString()
  return query ? `?${query}` : ''
}

function normalizeDateTime(value: string | undefined): string | undefined {
  if (!value) return value
  const date = new Date(value)
  const pad = (part: number, length = 2) => String(part).padStart(length, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}.${pad(date.getMilliseconds(), 3)}`
}

export function getProfile(): Promise<PregnancyProfile | null> {
  return request('/profile')
}

export function saveProfile(profile: PregnancyProfilePayload): Promise<PregnancyProfile> {
  return request('/profile', { method: 'PUT', body: JSON.stringify(profile) })
}

export function listFetalMovements(startDate?: string, endDate?: string): Promise<FetalMovementSession[]> {
  return request(`/fetal-movement/sessions${buildDateQuery(startDate, endDate)}`)
}

export function saveFetalMovement(session: FetalMovementSessionPayload): Promise<FetalMovementSession> {
  return request('/fetal-movement/sessions', {
    method: 'POST',
    body: JSON.stringify({ ...session, startedAt: normalizeDateTime(session.startedAt), endedAt: normalizeDateTime(session.endedAt) }),
  })
}

export function listContractions(startDate?: string, endDate?: string): Promise<ContractionSession[]> {
  return request(`/contractions${buildDateQuery(startDate, endDate)}`)
}

export function saveContraction(session: ContractionSessionPayload): Promise<ContractionSession> {
  return request('/contractions', {
    method: 'POST',
    body: JSON.stringify({ ...session, startedAt: normalizeDateTime(session.startedAt), endedAt: normalizeDateTime(session.endedAt) }),
  })
}

export function listHealthRecords(recordType?: string): Promise<HealthRecord[]> {
  const query = recordType ? `?recordType=${encodeURIComponent(recordType)}` : ''
  return request(`/health-records${query}`)
}

export function saveHealthRecord(record: HealthRecordPayload): Promise<HealthRecord> {
  return request('/health-records', {
    method: 'POST',
    body: JSON.stringify({ ...record, recordedAt: normalizeDateTime(record.recordedAt) }),
  })
}

export function listTasks(status?: string): Promise<AntenatalTask[]> {
  const query = status ? `?status=${encodeURIComponent(status)}` : ''
  return request(`/tasks${query}`)
}

export function saveTask(task: AntenatalTask): Promise<AntenatalTask> {
  return request('/tasks', {
    method: 'POST',
    body: JSON.stringify({ ...task, plannedAt: normalizeDateTime(task.plannedAt), completedAt: normalizeDateTime(task.completedAt) }),
  })
}
