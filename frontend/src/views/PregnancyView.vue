<script setup lang="ts">
import { reactive, watch } from 'vue'
import { ArrowUpRight, Check, HeartPulse, Plus, Save, Scale, Stethoscope, Waves } from 'lucide-vue-next'
import { usePregnancyStore } from '../stores/pregnancy'

const store = usePregnancyStore()
const profileForm = reactive({ lmpDate: '', dueDate: '', babyCount: 1, nickname: '', note: '' })
const healthForm = reactive({ recordType: 'weight' as 'weight' | 'blood-pressure' | 'symptom', value: '', unit: 'kg', note: '' })
const savedMessage = reactive({ profile: '', health: '' })

watch(() => store.profile, (profile) => {
  if (!profile) return
  profileForm.lmpDate = profile.lmpDate ?? ''
  profileForm.dueDate = profile.dueDate ?? ''
  profileForm.babyCount = profile.babyCount ?? 1
  profileForm.nickname = profile.nickname ?? ''
  profileForm.note = profile.note ?? ''
}, { immediate: true })

watch(() => healthForm.recordType, (type) => {
  healthForm.unit = type === 'weight' ? 'kg' : type === 'blood-pressure' ? 'mmHg' : ''
})

async function saveProfile(): Promise<void> {
  await store.saveProfile({ ...profileForm })
  savedMessage.profile = '档案已保存在本机'
  window.setTimeout(() => { savedMessage.profile = '' }, 2400)
}

async function saveHealth(): Promise<void> {
  if (!healthForm.value.trim()) return
  await store.addHealthRecord({
    clientRecordId: `health-${Date.now()}`,
    recordType: healthForm.recordType,
    valueJson: JSON.stringify({ value: healthForm.value.trim() }),
    unit: healthForm.unit || undefined,
    recordedAt: new Date().toISOString(),
    note: healthForm.note.trim() || undefined,
  })
  healthForm.value = ''
  healthForm.note = ''
  savedMessage.health = '健康记录已保存'
  window.setTimeout(() => { savedMessage.health = '' }, 2400)
}
</script>

<template>
  <div class="view-stack">
    <section class="page-intro compact-intro">
      <div><p class="eyebrow">PREGNANCY PROFILE</p><h1>你的孕期档案</h1><p>基础信息只为计算孕周和整理自己的记录服务。</p></div>
      <div class="profile-badge"><HeartPulse :size="22" /><span>{{ store.profile ? '已建立' : '待建立' }}</span></div>
    </section>

    <section class="form-grid">
      <article class="panel form-panel">
        <div class="panel-heading"><div><p class="eyebrow">ABOUT THIS JOURNEY</p><h2>孕期基础信息</h2></div><Stethoscope :size="20" class="heading-icon" /></div>
        <form class="form-stack" @submit.prevent="saveProfile">
          <div class="field-grid two-columns">
            <label class="field"><span>末次月经日期</span><input v-model="profileForm.lmpDate" type="date" /></label>
            <label class="field"><span>预产期</span><input v-model="profileForm.dueDate" type="date" /></label>
          </div>
          <div class="field-grid two-columns">
            <label class="field"><span>胎儿数量</span><input v-model.number="profileForm.babyCount" type="number" min="1" max="4" /></label>
            <label class="field"><span>给这段旅程的称呼</span><input v-model="profileForm.nickname" type="text" maxlength="64" placeholder="例如：小星星" /></label>
          </div>
          <label class="field"><span>档案备注</span><textarea v-model="profileForm.note" rows="3" maxlength="500" placeholder="给自己留一点说明"></textarea></label>
          <div class="form-footer"><span class="save-hint"><Check v-if="savedMessage.profile" :size="15" />{{ savedMessage.profile || '修改只会保存到本机，并进入同步队列' }}</span><button class="primary-button" type="submit"><Save :size="17" /> 保存档案</button></div>
        </form>
      </article>

      <article class="panel form-panel">
        <div class="panel-heading"><div><p class="eyebrow">DAILY CHECK-IN</p><h2>记下一笔健康记录</h2></div><Scale :size="20" class="heading-icon" /></div>
        <form class="form-stack" @submit.prevent="saveHealth">
          <div class="segmented-control" role="tablist" aria-label="健康记录类型">
            <button :class="{ active: healthForm.recordType === 'weight' }" type="button" @click="healthForm.recordType = 'weight'">体重</button>
            <button :class="{ active: healthForm.recordType === 'blood-pressure' }" type="button" @click="healthForm.recordType = 'blood-pressure'">血压</button>
            <button :class="{ active: healthForm.recordType === 'symptom' }" type="button" @click="healthForm.recordType = 'symptom'">症状</button>
          </div>
          <label class="field"><span>{{ healthForm.recordType === 'symptom' ? '描述一下今天的感受' : '记录数值' }}</span><input v-model="healthForm.value" type="text" :placeholder="healthForm.recordType === 'blood-pressure' ? '例如 118/76' : healthForm.recordType === 'weight' ? '例如 62.4' : '例如 腰部酸胀'" /></label>
          <label v-if="healthForm.recordType !== 'symptom'" class="field"><span>单位</span><input v-model="healthForm.unit" type="text" /></label>
          <label class="field"><span>补充备注</span><textarea v-model="healthForm.note" rows="3" placeholder="可选"></textarea></label>
          <div class="form-footer"><span class="save-hint"><Check v-if="savedMessage.health" :size="15" />{{ savedMessage.health || '记录仅用于自我回看和就医沟通' }}</span><button class="primary-button" type="submit" :disabled="!healthForm.value.trim()"><Plus :size="17" /> 添加记录</button></div>
        </form>
      </article>
    </section>
    <section class="panel quick-tool-band"><div><p class="eyebrow">ANOTHER TOOL</p><h2>需要记录宫缩？</h2><p>单独的计时页面会记录持续时间和与上一条的间隔。</p></div><RouterLink class="secondary-button" to="/contractions"><Waves :size="16" /> 打开宫缩计时 <ArrowUpRight :size="15" /></RouterLink></section>
  </div>
</template>
