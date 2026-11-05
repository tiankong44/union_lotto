/**
 * 日志录入场景，校验鱼缸、生物关联、水质值和照片后保存。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
const reefApi = require('../../services/reef-api')
const { shanghaiDateTimeParts } = require('../../services/date-time')

const LOG_TYPES = [
  { value: 'WATER_TEST', label: '水质检测' },
  { value: 'WATER_CHANGE', label: '换水' },
  { value: 'FEEDING', label: '喂食' },
  { value: 'MEDICATION', label: '用药' },
  { value: 'OBSERVATION', label: '观察' }
]

Page({
  data: {
    logTypes: LOG_TYPES.map((item) => item.label),
    tanks: [], tankNames: [], biology: [], selectedPhotos: [], saving: false, savedLogId: '',
    recordDate: '', recordTime: '',
    waterFields: [
      { key: 'salinity', label: '盐度', placeholder: '1.025' },
      { key: 'temperature', label: '水温℃', placeholder: '26.5' },
      { key: 'ph', label: 'pH', placeholder: '8.2' },
      { key: 'no2', label: 'NO₂ mg/L', placeholder: '0.02' },
      { key: 'no3', label: 'NO₃ mg/L', placeholder: '2.5' },
      { key: 'po4', label: 'PO₄ mg/L', placeholder: '0.03' }
    ],
    form: {
      type: 'WATER_TEST', typeLabel: '水质检测', tankId: '', tankName: '',
      recordedAt: '', note: '', changePercent: '',
      water: { salinity: '', temperature: '', ph: '', no2: '', no3: '', po4: '' }
    }
  },

  onLoad(query) {
    const now = shanghaiDateTimeParts(new Date())
    const recordDate = now.date
    const recordTime = now.time.slice(0, 5)
    this.setData({ recordDate, recordTime, 'form.recordedAt': `${recordDate} ${recordTime}:00` })
    this.loadTanks(query.tankId || '')
  },

  async loadTanks(selectedId) {
    try {
      const result = await reefApi.listTanks()
      const tanks = result?.list ?? result ?? []
      this.setData({ tanks, tankNames: tanks.map((item) => item.name) })
      if (selectedId) {
        const index = tanks.findIndex((item) => item.id === selectedId)
        if (index >= 0) this.setData({ 'form.tankId': tanks[index].id, 'form.tankName': tanks[index].name })
      }
      if (!this.data.form.tankId && tanks.length) this.setData({ 'form.tankId': tanks[0].id, 'form.tankName': tanks[0].name })
      if (this.data.form.tankId) this.loadBiology()
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' })
    }
  },

  async loadBiology() {
    try {
      const result = await reefApi.listBiology(this.data.form.tankId)
      const biology = result?.list ?? result ?? []
      this.setData({ biology: biology.map((item) => ({ ...item, selected: false })) })
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' })
    }
  },

  onTypeChange(event) {
    const selected = LOG_TYPES[Number(event.detail.value)]
    this.setData({ 'form.type': selected.value, 'form.typeLabel': selected.label })
  },

  onTankChange(event) {
    const tank = this.data.tanks[Number(event.detail.value)]
    this.setData({ 'form.tankId': tank.id, 'form.tankName': tank.name, biology: [] })
    this.loadBiology()
  },

  toggleBiology(event) {
    const id = event.currentTarget.dataset.id
    const biology = this.data.biology.map((item) => item.id === id ? { ...item, selected: !item.selected } : item)
    this.setData({ biology })
  },

  onDateChange(event) {
    this.setData({ recordDate: event.detail.value, 'form.recordedAt': `${event.detail.value} ${this.data.recordTime}:00` })
  },

  onTimeChange(event) {
    this.setData({ recordTime: event.detail.value, 'form.recordedAt': `${this.data.recordDate} ${event.detail.value}:00` })
  },

  onFieldChange(event) {
    this.setData({ [`form.${event.currentTarget.dataset.field}`]: event.detail })
  },

  onWaterInput(event) {
    this.setData({ [`form.water.${event.currentTarget.dataset.key}`]: event.detail.value })
  },

  choosePhotos() {
    wx.chooseMedia({ count: 9 - this.data.selectedPhotos.length, mediaType: ['image'], sourceType: ['album', 'camera'], success: (result) => {
      this.setData({ selectedPhotos: this.data.selectedPhotos.concat(result.tempFiles.map((file) => ({ path: file.tempFilePath, name: '海缸照片' }))) })
    } })
  },

  removePhoto(event) {
    const selectedPhotos = this.data.selectedPhotos.slice()
    selectedPhotos.splice(Number(event.currentTarget.dataset.index), 1)
    this.setData({ selectedPhotos })
  },

  async submitLog() {
    if (!this.data.form.tankId) {
      wx.showToast({ title: '请先新增鱼缸', icon: 'none' })
      return
    }
    this.setData({ saving: true })
    try {
      const form = this.data.form
      const payload = {
        tankId: form.tankId,
        type: form.type,
        title: form.typeLabel,
        description: form.type === 'WATER_CHANGE' && form.changePercent ? `换水 ${form.changePercent}%` : form.note,
        recordedAt: form.recordedAt,
        note: form.note,
        biologyIds: this.data.biology.filter((item) => item.selected).map((item) => item.id),
        changePercent: Number(form.changePercent) || null,
        water: this.normalizeWaterValues(form.water)
      }
      let logId = this.data.savedLogId
      if (!logId) {
        const saved = await reefApi.saveLog(payload)
        logId = saved.id
        this.setData({ savedLogId: logId })
      }
      let photoFailure = false
      const pendingPhotos = []
      for (const photo of this.data.selectedPhotos) {
        try {
          await reefApi.uploadPhoto(photo.path, { title: photo.name, category: 'TANK', referenceType: 'LOG', referenceId: logId || '' })
        } catch (error) {
          photoFailure = true
          pendingPhotos.push(photo)
        }
      }
      this.setData({ selectedPhotos: pendingPhotos })
      wx.showToast({ title: photoFailure ? '日志已保存，部分照片未上传' : '日志已保存', icon: photoFailure ? 'none' : 'success' })
      if (!photoFailure) setTimeout(() => wx.navigateBack({ delta: 1 }), 500)
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' })
    } finally {
      this.setData({ saving: false })
    }
  },

  cancel() { wx.navigateBack({ delta: 1 }) },

  normalizeWaterValues(values) {
    const normalized = {}
    Object.keys(values).forEach((key) => { normalized[key] = values[key] === '' ? null : Number(values[key]) })
    return normalized
  }
})
