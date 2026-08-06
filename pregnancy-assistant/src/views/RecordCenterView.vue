<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ArrowUpRight, BookOpen, CalendarClock, Check, FileText, HeartPulse, Plus, Scale, Waves } from 'lucide-vue-next'
import { usePregnancyStore } from '../stores/pregnancy'
import type { HealthRecordType } from '../types/pregnancy'

const store = usePregnancyStore()

type HealthFormType = HealthRecordType

const healthForm = reactive({
  recordType: 'weight' as HealthFormType,
  value: '',
  recordedAt: toLocalInputValue(),
  note: '',
})
const saving = ref(false)
const saveMessage = ref('')
const saveMessageTone = ref<'success' | 'error'>('success')

const healthUnit = computed(() => {
  if (healthForm.recordType === 'weight') return 'kg'
  if (healthForm.recordType === 'blood-pressure') return 'mmHg'
  return ''
})

const valueLabel = computed(() => healthForm.recordType === 'symptom' ? '描述一下今天的感受' : '记录数值')
const valuePlaceholder = computed(() => {
  if (healthForm.recordType === 'weight') return '例如 62.4'
  if (healthForm.recordType === 'blood-pressure') return '例如 118/76'
  return '例如 腰部酸胀'
})

function toLocalInputValue(date: Date = new Date()): string {
  const localDate = new Date(date.getTime() - date.getTimezoneOffset() * 60000)
  return localDate.toISOString().slice(0, 16)
}

function toIsoDate(value: string): string | null {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? null : date.toISOString()
}

function makeRecordId(): string {
  return `health-${typeof crypto !== 'undefined' && 'randomUUID' in crypto ? crypto.randomUUID() : `${Date.now()}-${Math.random().toString(16).slice(2)}`}`
}

function validateForm(): string | null {
  if (!healthForm.value.trim()) return `${valueLabel.value}不能为空`
  if (healthForm.recordType === 'weight') {
    const numericValue = Number(healthForm.value.trim())
    if (!Number.isFinite(numericValue) || numericValue < 0) return '体重请输入不小于 0 的数字'
  }
  if (!toIsoDate(healthForm.recordedAt)) return '请选择有效的记录时间'
  return null
}

function resetForm(): void {
  healthForm.value = ''
  healthForm.recordedAt = toLocalInputValue()
  healthForm.note = ''
}

async function saveHealthRecord(): Promise<void> {
  const validationMessage = validateForm()
  if (validationMessage) {
    saveMessageTone.value = 'error'
    saveMessage.value = validationMessage
    return
  }
  const recordedAt = toIsoDate(healthForm.recordedAt)
  if (!recordedAt) return

  saving.value = true
  saveMessage.value = ''
  try {
    await store.addHealthRecord({
      clientRecordId: makeRecordId(),
      recordType: healthForm.recordType,
      valueJson: JSON.stringify({ value: healthForm.value.trim() }),
      unit: healthUnit.value || undefined,
      recordedAt,
      note: healthForm.note.trim() || undefined,
    })
    resetForm()
    saveMessageTone.value = 'success'
    saveMessage.value = '记录已保存到云端 MySQL'
  } catch {
    saveMessageTone.value = 'error'
    saveMessage.value = '保存失败，当前内容已保留，请重试'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="view-stack">
    <section class="page-intro compact-intro">
      <div>
        <p class="eyebrow">DAILY LOG</p>
        <h1>记录中心</h1>
        <p>把宫缩、体重和身体感受放在同一个入口里。</p>
      </div>
      <div class="profile-badge"><BookOpen :size="21" /><span>身体记录</span></div>
    </section>

    <section class="record-center-grid">
      <article class="panel record-center-actions">
        <div class="panel-heading">
          <div><p class="eyebrow">QUICK ENTRY</p><h2>从这里开始</h2></div>
          <FileText :size="20" class="heading-icon" />
        </div>
        <div class="record-action-list">
          <RouterLink class="record-action blue-action" to="/contractions">
            <span class="record-action-icon"><Waves :size="19" /></span>
            <span><strong>记录宫缩</strong><small>开始、结束、持续时间和间隔</small></span>
            <ArrowUpRight :size="17" />
          </RouterLink>
          <RouterLink class="record-action coral-action" to="/records">
            <span class="record-action-icon"><CalendarClock :size="19" /></span>
            <span><strong>查看历史</strong><small>按类型回看已保存的记录</small></span>
            <ArrowUpRight :size="17" />
          </RouterLink>
        </div>
        <p class="panel-copy">记录只用于个人回看和就医沟通，不替代医疗判断。</p>
      </article>

      <article class="panel form-panel record-form-panel">
        <div class="panel-heading">
          <div><p class="eyebrow">HEALTH CHECK-IN</p><h2>新增一笔健康记录</h2></div>
          <Scale :size="20" class="heading-icon" />
        </div>
        <form class="form-stack" @submit.prevent="saveHealthRecord">
          <div class="segmented-control" role="tablist" aria-label="健康记录类型">
            <button :class="{ active: healthForm.recordType === 'weight' }" type="button" :aria-pressed="healthForm.recordType === 'weight'" @click="healthForm.recordType = 'weight'">体重</button>
            <button :class="{ active: healthForm.recordType === 'blood-pressure' }" type="button" :aria-pressed="healthForm.recordType === 'blood-pressure'" @click="healthForm.recordType = 'blood-pressure'">血压</button>
            <button :class="{ active: healthForm.recordType === 'symptom' }" type="button" :aria-pressed="healthForm.recordType === 'symptom'" @click="healthForm.recordType = 'symptom'">症状</button>
          </div>
          <div class="field-grid two-columns">
            <label class="field"><span>{{ valueLabel }}</span><input v-model="healthForm.value" :type="healthForm.recordType === 'weight' ? 'number' : 'text'" :min="healthForm.recordType === 'weight' ? 0 : undefined" :step="healthForm.recordType === 'weight' ? 0.1 : undefined" :placeholder="valuePlaceholder" required /></label>
            <label class="field"><span>记录时间</span><input v-model="healthForm.recordedAt" type="datetime-local" required /></label>
          </div>
          <label v-if="healthUnit" class="field"><span>单位</span><input :value="healthUnit" type="text" readonly /></label>
          <label class="field"><span>补充备注</span><textarea v-model="healthForm.note" rows="3" placeholder="可选"></textarea></label>
          <div class="form-footer">
            <span :class="['save-hint', { error: saveMessageTone === 'error' } ]" role="status"><Check v-if="saveMessageTone === 'success' && saveMessage" :size="15" />{{ saveMessage || '默认记录当前时间，也可以补录历史时间' }}</span>
            <button class="primary-button" type="submit" :disabled="saving || store.cloudStatus === 'saving'"><Plus :size="17" /> {{ saving ? '保存中' : '添加记录' }}</button>
          </div>
        </form>
      </article>
    </section>
  </div>
</template>
