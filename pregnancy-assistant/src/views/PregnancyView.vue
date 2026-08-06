<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { ArrowUpRight, BookOpen, Check, HeartPulse, Save, Stethoscope } from 'lucide-vue-next'
import DateField from '../components/DateField.vue'
import WorkspaceLink from '../components/WorkspaceLink.vue'
import { usePregnancyStore } from '../stores/pregnancy'

const store = usePregnancyStore()
const profileForm = reactive({ lmpDate: '', dueDate: '', babyCount: 1, nickname: '', note: '' })
const savedMessage = ref('')
const savingProfile = ref(false)

watch(() => store.profile, (profile) => {
  if (!profile) return
  profileForm.lmpDate = profile.lmpDate ?? ''
  profileForm.dueDate = profile.dueDate ?? ''
  profileForm.babyCount = profile.babyCount ?? 1
  profileForm.nickname = profile.nickname ?? ''
  profileForm.note = profile.note ?? ''
}, { immediate: true })

async function saveProfile(): Promise<void> {
  savingProfile.value = true
  try {
    await store.saveProfile({ ...profileForm })
    savedMessage.value = '档案已保存到云端 MySQL'
    window.setTimeout(() => { savedMessage.value = '' }, 2400)
  } catch {
    savedMessage.value = '档案保存失败，请重试'
  } finally {
    savingProfile.value = false
  }
}
</script>

<template>
  <div class="view-stack">
    <section class="page-intro compact-intro">
      <div><p class="eyebrow">PREGNANCY PROFILE</p><h1>你的孕期档案</h1><p>基础信息只为计算孕周和整理自己的记录服务。</p></div>
      <div class="profile-badge"><HeartPulse :size="22" /><span>{{ store.profile ? '已建立' : '待建立' }}</span></div>
    </section>

    <section class="profile-grid">
      <article class="panel form-panel">
        <div class="panel-heading"><div><p class="eyebrow">ABOUT THIS JOURNEY</p><h2>孕期基础信息</h2></div><Stethoscope :size="20" class="heading-icon" /></div>
        <form class="form-stack" @submit.prevent="saveProfile">
          <div class="field-grid two-columns">
            <label class="field date-field"><span>末次月经日期</span><DateField v-model="profileForm.lmpDate" /></label>
            <label class="field date-field"><span>预产期</span><DateField v-model="profileForm.dueDate" /></label>
          </div>
          <div class="field-grid two-columns">
            <label class="field"><span>胎儿数量</span><input v-model.number="profileForm.babyCount" type="number" min="1" max="4" /></label>
            <label class="field"><span>给这段旅程的称呼</span><input v-model="profileForm.nickname" type="text" maxlength="64" placeholder="例如：小星星" /></label>
          </div>
          <label class="field"><span>档案备注</span><textarea v-model="profileForm.note" rows="3" maxlength="500" placeholder="给自己留一点说明"></textarea></label>
          <div class="form-footer"><span class="save-hint"><Check v-if="savedMessage" :size="15" />{{ savedMessage || '修改会直接保存到云端 MySQL' }}</span><button class="primary-button" type="submit" :disabled="savingProfile"><Save :size="17" /> 保存档案</button></div>
        </form>
      </article>
    </section>

    <section class="panel quick-tool-band">
      <div><p class="eyebrow">HEALTH RECORDS</p><h2>需要记录体重或身体感受？</h2><p>记录中心支持体重、血压、症状和可修改的记录时间。</p></div>
      <WorkspaceLink class="secondary-button" view="record-center"><BookOpen :size="16" /> 打开记录中心 <ArrowUpRight :size="15" /></WorkspaceLink>
    </section>
  </div>
</template>
