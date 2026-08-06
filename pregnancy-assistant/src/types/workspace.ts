import type { InjectionKey } from 'vue'

export type WorkspaceView =
  | 'movement'
  | 'overview'
  | 'record-center'
  | 'contractions'
  | 'records'
  | 'insights'
  | 'pregnancy'
  | 'tasks'
  | 'settings'

export type WorkspaceNavigate = (view: WorkspaceView) => void

export const WORKSPACE_NAVIGATE_KEY: InjectionKey<WorkspaceNavigate> = Symbol('workspace-navigate')
