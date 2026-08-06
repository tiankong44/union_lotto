import type { SyncQueueItem } from '../types/pregnancy'

const DB_NAME = 'pregnancy-assistant'
const DB_VERSION = 1
const STATE_STORE = 'state'
const QUEUE_STORE = 'sync_queue'

export interface LocalSnapshot {
  profile: Record<string, unknown> | null
  movementSessions: Record<string, unknown>[]
  contractions: Record<string, unknown>[]
  healthRecords: Record<string, unknown>[]
  tasks: Record<string, unknown>[]
  lastSyncedAt: string | null
}

let databasePromise: Promise<IDBDatabase> | null = null

function getDatabase(): Promise<IDBDatabase> {
  if (databasePromise) return databasePromise
  databasePromise = new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION)
    request.onupgradeneeded = () => {
      const database = request.result
      if (!database.objectStoreNames.contains(STATE_STORE)) {
        database.createObjectStore(STATE_STORE, { keyPath: 'id' })
      }
      if (!database.objectStoreNames.contains(QUEUE_STORE)) {
        database.createObjectStore(QUEUE_STORE, { keyPath: 'id', autoIncrement: true })
      }
    }
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error ?? new Error('无法打开本地数据存储'))
  })
  return databasePromise
}

export async function loadSnapshot(): Promise<LocalSnapshot | null> {
  const database = await getDatabase()
  return new Promise((resolve, reject) => {
    const transaction = database.transaction(STATE_STORE, 'readonly')
    const request = transaction.objectStore(STATE_STORE).get('snapshot')
    request.onsuccess = () => resolve((request.result?.value as LocalSnapshot | undefined) ?? null)
    request.onerror = () => reject(request.error ?? new Error('读取本地记录失败'))
  })
}

export async function saveSnapshot(snapshot: LocalSnapshot): Promise<void> {
  const database = await getDatabase()
  return new Promise((resolve, reject) => {
    const transaction = database.transaction(STATE_STORE, 'readwrite')
    transaction.objectStore(STATE_STORE).put({ id: 'snapshot', value: snapshot })
    transaction.oncomplete = () => resolve()
    transaction.onerror = () => reject(transaction.error ?? new Error('保存本地记录失败'))
  })
}

export async function enqueueSync(item: SyncQueueItem): Promise<void> {
  const database = await getDatabase()
  return new Promise((resolve, reject) => {
    const transaction = database.transaction(QUEUE_STORE, 'readwrite')
    transaction.objectStore(QUEUE_STORE).add(item)
    transaction.oncomplete = () => resolve()
    transaction.onerror = () => reject(transaction.error ?? new Error('加入同步队列失败'))
  })
}

export async function listSyncQueue(): Promise<SyncQueueItem[]> {
  const database = await getDatabase()
  return new Promise((resolve, reject) => {
    const transaction = database.transaction(QUEUE_STORE, 'readonly')
    const request = transaction.objectStore(QUEUE_STORE).getAll()
    request.onsuccess = () => resolve(request.result as SyncQueueItem[])
    request.onerror = () => reject(request.error ?? new Error('读取同步队列失败'))
  })
}

export async function clearSyncQueue(ids: number[]): Promise<void> {
  if (!ids.length) return
  const database = await getDatabase()
  return new Promise((resolve, reject) => {
    const transaction = database.transaction(QUEUE_STORE, 'readwrite')
    const store = transaction.objectStore(QUEUE_STORE)
    ids.forEach((id) => store.delete(id))
    transaction.oncomplete = () => resolve()
    transaction.onerror = () => reject(transaction.error ?? new Error('清理同步队列失败'))
  })
}
