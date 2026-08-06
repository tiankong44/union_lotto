<script setup lang="ts">
import { computed } from 'vue'
import { Check, CloudOff, Database, Download, LockKeyhole, RefreshCw, ShieldCheck } from 'lucide-vue-next'
import { usePregnancyStore } from '../stores/pregnancy'

const store = usePregnancyStore()
const syncText = computed(() => store.lastLoadedAt ? new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(store.lastLoadedAt)) : '还没有加载云端记录')

function exportData(): void {
  const payload = JSON.stringify({ exportedAt: new Date().toISOString(), profile: store.profile, movementSessions: store.movementSessions, contractions: store.contractions, healthRecords: store.healthRecords, tasks: store.tasks }, null, 2)
  const url = URL.createObjectURL(new Blob([payload], { type: 'application/json' }))
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = `拾光孕记-${new Date().toISOString().slice(0, 10)}.json`
  anchor.click()
  URL.revokeObjectURL(url)
}

</script>

<template>
  <div class="view-stack settings-page">
    <section class="page-intro compact-intro"><div><p class="eyebrow">PRIVATE BY DEFAULT</p><h1>设置</h1><p>简单、安静地管理你的数据和云端连接。</p></div><div class="profile-badge"><ShieldCheck :size="21" /><span>隐私优先</span></div></section>
    <section class="settings-grid">
      <article class="panel setting-panel"><div class="setting-icon mint-icon"><LockKeyhole :size="20" /></div><div><p class="eyebrow">CLOUD STORAGE</p><h2>记录直接保存到云端</h2><p>胎动、健康记录和待办会直接写入云端 MySQL，不在浏览器保留业务副本。</p></div><div class="setting-state"><Check :size="15" /> 已启用</div></article>
      <article class="panel setting-panel"><div class="setting-icon blue-icon"><Database :size="20" /></div><div><p class="eyebrow">SINGLE USER MODE</p><h2>只为一人保存</h2><p>首版不建立账号和用户隔离，记录直接服务于当前使用者。</p></div><span class="setting-state"><Check :size="15" /> 已启用</span></article>
      <article class="panel setting-panel"><div class="setting-icon yellow-icon"><RefreshCw :size="20" /></div><div><p class="eyebrow">CLOUD STATUS</p><h2>{{ store.cloudStatus === 'error' ? '云端连接需要重试' : '云端数据' }}</h2><p>上次加载：{{ syncText }}</p></div><button class="secondary-button" type="button" :disabled="store.cloudStatus === 'loading' || store.cloudStatus === 'saving'" @click="store.refreshCloudData"><RefreshCw :size="16" /> 刷新</button></article>
      <article class="panel setting-panel"><div class="setting-icon coral-icon"><CloudOff :size="20" /></div><div><p class="eyebrow">CARE NOTE</p><h2>记录不是诊断</h2><p>趋势只帮助你描述自己的变化。如果感觉胎动明显减少或模式改变，请及时联系产科。</p></div></article>
      <article class="panel setting-panel"><div class="setting-icon mint-icon"><Download :size="20" /></div><div><p class="eyebrow">YOUR COPY</p><h2>导出一份记录</h2><p>下载 JSON 文件，方便备份或在就医沟通时整理资料。</p></div><button class="secondary-button" type="button" @click="exportData"><Download :size="16" /> 导出</button></article>
    </section>
  </div>
</template>
