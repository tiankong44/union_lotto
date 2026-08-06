<script setup lang="ts">
import { computed } from 'vue'
import { VueDatePicker } from '@vuepic/vue-datepicker'
import '@vuepic/vue-datepicker/dist/main.css'
import { zhCN } from 'date-fns/locale'

type DateFieldMode = 'date' | 'datetime'

const props = withDefaults(defineProps<{
  modelValue: string
  mode?: DateFieldMode
  placeholder?: string
  required?: boolean
  disabled?: boolean
  clearable?: boolean
}>(), {
  mode: 'date',
  placeholder: '请选择日期',
  required: false,
  disabled: false,
  clearable: true,
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const pickerValue = computed(() => parseLocalDate(props.modelValue, props.mode))
const displayFormat = computed(() => props.mode === 'datetime' ? 'yyyy年MM月dd日 HH:mm' : 'yyyy年MM月dd日')
const timeConfig = computed(() => ({
  enableTimePicker: props.mode === 'datetime',
  enableSeconds: false,
  enableMinutes: true,
  is24: true,
}))

function parseLocalDate(value: string, mode: DateFieldMode): Date | null {
  const pattern = mode === 'datetime'
    ? /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})$/
    : /^(\d{4})-(\d{2})-(\d{2})$/
  const matches = value.match(pattern)
  if (!matches) return null

  const date = new Date(
    Number(matches[1]),
    Number(matches[2]) - 1,
    Number(matches[3]),
    Number(matches[4] ?? 0),
    Number(matches[5] ?? 0),
  )
  return Number.isNaN(date.getTime()) ? null : date
}

function pad(value: number): string {
  return String(value).padStart(2, '0')
}

function formatLocalDate(value: Date, mode: DateFieldMode): string {
  const date = `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}`
  if (mode === 'date') return date
  return `${date}T${pad(value.getHours())}:${pad(value.getMinutes())}`
}

function handleUpdate(value: unknown): void {
  if (!(value instanceof Date)) {
    emit('update:modelValue', '')
    return
  }
  emit('update:modelValue', formatLocalDate(value, props.mode))
}
</script>

<template>
  <VueDatePicker
    :model-value="pickerValue"
    :format="displayFormat"
    :time-config="timeConfig"
    :input-attrs="{ required }"
    :ui="{ input: 'date-picker-input' }"
    :placeholder="placeholder"
    :clearable="clearable"
    :disabled="disabled"
    :auto-apply="true"
    :locale="zhCN"
    :week-start="1"
    @update:model-value="handleUpdate"
  />
</template>
