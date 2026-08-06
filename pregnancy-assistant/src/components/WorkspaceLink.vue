<script setup lang="ts">
import { inject } from 'vue'
import { WORKSPACE_NAVIGATE_KEY, type WorkspaceView } from '../types/workspace'

const props = defineProps<{ view: WorkspaceView }>()
const navigate = inject(WORKSPACE_NAVIGATE_KEY)

if (!navigate) {
  throw new Error('WorkspaceLink 必须在 AppShell workspace 上下文中使用')
}

function handleClick(): void {
  // 统一切换内存视图，避免页面入口修改浏览器地址栏。
  navigate!(props.view)
}
</script>

<template>
  <button class="workspace-link" type="button" @click="handleClick">
    <slot />
  </button>
</template>
