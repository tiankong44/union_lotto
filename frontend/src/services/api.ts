import type { SyncQueueItem } from '../types/pregnancy'

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
    throw new Error(`同步服务返回 ${response.status}`)
  }
  const body = (await response.json()) as ApiResponse<T>
  if (body.code !== 0) {
    throw new Error(body.message || '同步服务处理失败')
  }
  return body.data
}

export async function syncRecords(items: SyncQueueItem[]): Promise<void> {
  await request('/sync', {
    method: 'POST',
    body: JSON.stringify({
      items: items.map(({ entityType, clientRecordId, payload }) => ({ entityType, clientRecordId, payload })),
    }),
  })
}
