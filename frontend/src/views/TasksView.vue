<script setup lang="ts">
import { computed, reactive } from 'vue'
import { BellRing, CalendarPlus, Check, Circle, Clock3, Plus } from 'lucide-vue-next'
import { usePregnancyStore } from '../stores/pregnancy'

const store = usePregnancyStore()
const form = reactive({ title: '', plannedAt: '', taskType: 'checkup' as 'checkup' | 'todo' | 'custom', note: '' })
const upcomingTasks = computed(() => [...store.tasks].sort((a, b) => a.plannedAt.localeCompare(b.plannedAt)))

function addTask(): void {
  if (!form.title.trim() || !form.plannedAt) return
  store.addTask({
    clientRecordId: `task-${Date.now()}`,
    title: form.title.trim(),
    taskType: form.taskType,
    plannedAt: new Date(form.plannedAt).toISOString(),
    status: 'TODO',
    note: form.note.trim() || undefined,
  })
  form.title = ''
  form.plannedAt = ''
  form.note = ''
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat('zh-CN', { month: 'long', day: 'numeric', weekday: 'short', hour: '2-digit', minute: '2-digit' }).format(new Date(value))
}
</script>

<template>
  <div class="view-stack">
    <section class="page-intro compact-intro"><div><p class="eyebrow">CALENDAR & CARE</p><h1>产检与待办</h1><p>把重要的事情放在眼前，给自己少一点记忆负担。</p></div><div class="profile-badge"><BellRing :size="21" /><span>本机提醒</span></div></section>
    <section class="tasks-layout">
      <article class="panel task-form-panel">
        <div class="panel-heading"><div><p class="eyebrow">ADD A PLAN</p><h2>安排一件事</h2></div><CalendarPlus :size="20" class="heading-icon" /></div>
        <form class="form-stack" @submit.prevent="addTask">
          <label class="field"><span>事项名称</span><input v-model="form.title" type="text" placeholder="例如：预约下次产检" /></label>
          <div class="field-grid two-columns"><label class="field"><span>计划时间</span><input v-model="form.plannedAt" type="datetime-local" /></label><label class="field"><span>类型</span><select v-model="form.taskType"><option value="checkup">产检</option><option value="todo">待办</option><option value="custom">自定义</option></select></label></div>
          <label class="field"><span>备注</span><textarea v-model="form.note" rows="3" placeholder="带上要问医生的问题"></textarea></label>
          <button class="primary-button full-button" type="submit" :disabled="!form.title.trim() || !form.plannedAt"><Plus :size="18" /> 加入时间线</button>
        </form>
      </article>
      <article class="panel task-list-panel">
        <div class="panel-heading"><div><p class="eyebrow">UP NEXT</p><h2>接下来的安排</h2></div><span class="panel-index">{{ store.pendingTasks.length }}</span></div>
        <div v-if="upcomingTasks.length" class="task-list">
          <button v-for="task in upcomingTasks" :key="task.clientRecordId" :class="['task-item', { done: task.status === 'DONE' }]" type="button" @click="store.toggleTask(task)">
            <span class="task-check"><Check v-if="task.status === 'DONE'" :size="14" /><Circle v-else :size="17" /></span>
            <span class="task-content"><strong>{{ task.title }}</strong><span><Clock3 :size="13" /> {{ formatDate(task.plannedAt) }}</span></span>
            <span class="task-type">{{ task.taskType === 'checkup' ? '产检' : '待办' }}</span>
          </button>
        </div>
        <div v-else class="empty-block"><CalendarPlus :size="24" /><span>还没有安排</span><small>从左侧添加第一件事</small></div>
      </article>
    </section>
  </div>
</template>
