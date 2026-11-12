/**
 * 生物详情和事件时间轴，记录状态、转缸、观察和关联照片。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-12
 */
const reefApi = require('../../services/reef-api')
const reefPhotoMedia = require('../../services/reef-photo-media')
const { shanghaiDateTimeParts } = require('../../services/date-time')
const { EVENT_TYPES, BIOLOGY_STATUSES } = require('../../services/aquarium-catalog')

Page({
  data: {
    biologyId: '', biology: { category: '' }, events: [], photos: [], tanks: [], targetTanks: [], targetTankNames: [], imageIndex: 0,
    eventFormOpen: false, savingEvent: false, savedEventId: '', eventPhotos: [],
    eventTypes: EVENT_TYPES, eventTypeLabels: EVENT_TYPES.map((item) => item.label),
    statusOptions: [{ value: '', label: '不改变状态' }, ...BIOLOGY_STATUSES],
    statusLabels: ['不改变状态', ...BIOLOGY_STATUSES.map((item) => item.label)], recordDate: '', recordTime: '',
    eventForm: { title: '观察', eventType: 'OBSERVATION', eventTypeLabel: '观察', status: '', statusLabel: '不改变状态',
      description: '', toTankId: '', toTankName: '', quantity: '' }
  },
  onLoad(query) {
    const now = shanghaiDateTimeParts(new Date())
    this.setData({ biologyId: query?.id ?? '', recordDate: now.date, recordTime: now.time.slice(0, 5) })
  },
  onShow() { if (this.data.biologyId) this.loadBiology() },

  /** 读取生物资料、事件和照片，转缸目标只展示当前用户的其他鱼缸。 @Author zhanghao_SMEICS @Date 2026-11-12 */
  async loadBiology() {
    try {
      const [biology, events, photoResult, tankResult] = await Promise.all([
        reefApi.getBiology(this.data.biologyId), reefApi.listBiologyEvents(this.data.biologyId),
        reefApi.listPhotos('', 1, 100, 'BIOLOGY', this.data.biologyId), reefApi.listTanks(1, 100)
      ])
      const tanks = tankResult?.list ?? tankResult ?? []
      const targetTanks = tanks.filter((tank) => tank.id !== biology?.tankId && !tank.archived)
      const decoratedEvents = (events ?? []).map((event) => ({ ...event,
        fromTankName: tanks.find((tank) => tank.id === event.fromTankId)?.name ?? '',
        toTankName: tanks.find((tank) => tank.id === event.toTankId)?.name ?? '',
        quantityLabel: event.quantity === null || event.quantity === undefined ? '' : `数量：${event.quantity}` }))
      const photos = (photoResult?.list ?? photoResult ?? []).map(reefPhotoMedia.withMotionUrl)
      this.setData({ biology: biology ?? {}, events: decoratedEvents, photos,
        tanks, targetTanks, targetTankNames: targetTanks.map((tank) => tank.name), imageIndex: 0 })
    } catch (error) { wx.showToast({ title: error?.message ?? '生物资料暂时无法读取', icon: 'none' }) }
  },
  onPhotoChange(event) { this.setData({ imageIndex: event.detail.current }) },
  editBiology() { wx.navigateTo({ url: `/pages/biology/edit?id=${this.data.biologyId}` }) },
  openRelatedBiology(event) { wx.navigateTo({ url: `/pages/biology/detail?id=${event.currentTarget.dataset.id}` }) },
  openEventForm() { this.setData({ eventFormOpen: true }) },
  closeEventForm() { if (!this.data.savingEvent) this.setData({ eventFormOpen: false }) },
  onEventFieldChange(event) { this.setData({ [`eventForm.${event.currentTarget.dataset.field}`]: event.detail?.value ?? event.detail }) },
  onDateChange(event) { this.setData({ recordDate: event.detail.value }) },
  onTimeChange(event) { this.setData({ recordTime: event.detail.value }) },
  onEventTypeChange(event) {
    const selected = EVENT_TYPES[Number(event.detail.value)]
    this.setData({ 'eventForm.eventType': selected.value, 'eventForm.eventTypeLabel': selected.label, 'eventForm.title': selected.label })
  },
  onStatusChange(event) {
    const selected = this.data.statusOptions[Number(event.detail.value)]
    this.setData({ 'eventForm.status': selected.value, 'eventForm.statusLabel': selected.label })
  },
  onTargetTankChange(event) {
    const tank = this.data.targetTanks[Number(event.detail.value)]
    if (tank) this.setData({ 'eventForm.toTankId': tank.id, 'eventForm.toTankName': tank.name })
  },

  /** 事件保存后将所选照片绑定到服务端返回的事件编号。 @Author zhanghao_SMEICS @Date 2026-11-12 */
  chooseEventPhotos() {
    if (this.data.eventPhotos.length >= 9) return
    wx.chooseMedia({ count: 9 - this.data.eventPhotos.length, mediaType: ['image'], sourceType: ['album', 'camera'],
      success: (result) => this.setData({ eventPhotos: this.data.eventPhotos.concat(result.tempFiles.map((file) => file.tempFilePath)) }) })
  },
  removeEventPhoto(event) {
    const paths = this.data.eventPhotos.slice()
    paths.splice(Number(event.currentTarget.dataset.index), 1)
    this.setData({ eventPhotos: paths })
  },

  /** 创建事件并上传照片，部分照片失败时只重试未上传部分。 @Author zhanghao_SMEICS @Date 2026-11-12 */
  async saveEvent() {
    if (this.data.savingEvent) return
    const form = this.data.eventForm
    if (!form.title.trim()) return wx.showToast({ title: '请填写事件标题', icon: 'none' })
    if (form.eventType === 'TRANSFER' && !form.toTankId) return wx.showToast({ title: '请选择转入的鱼缸', icon: 'none' })
    const quantity = form.quantity === '' ? null : Number(form.quantity)
    if (quantity !== null && (!Number.isInteger(quantity) || quantity < 0 || (form.eventType === 'TRANSFER' && quantity === 0))) return wx.showToast({ title: '请填写有效的事件数量', icon: 'none' })
    this.setData({ savingEvent: true })
    try {
      let eventId = this.data.savedEventId
      const recordedAt = `${this.data.recordDate} ${this.data.recordTime}:00`
      if (!eventId) {
        // 获取新事件编号，避免把照片关联到并发创建的其他事件。
        const saved = await reefApi.createBiologyEvent(this.data.biologyId, { title: form.title, eventType: form.eventType,
          eventAt: recordedAt, description: form.description, status: form.status || null,
          toTankId: form.eventType === 'TRANSFER' ? form.toTankId : null, quantity })
        eventId = saved.id
        this.setData({ savedEventId: eventId })
      }
      const pending = []
      const category = ['FISH', 'CORAL', 'PLANT'].includes(this.data.biology.category) ? this.data.biology.category : 'OTHER'
      for (const path of this.data.eventPhotos) {
        try { await reefApi.uploadPhoto(path, { title: form.title, category, referenceType: 'BIOLOGY_EVENT', referenceId: eventId, recordedAt }) }
        catch (error) { pending.push(path) }
      }
      this.setData({ eventPhotos: pending })
      if (pending.length) return wx.showToast({ title: '事件已保存，部分照片未上传，可重试', icon: 'none' })
      this.setData({ eventFormOpen: false, savedEventId: '', eventForm: { title: '观察', eventType: 'OBSERVATION', eventTypeLabel: '观察',
        status: '', statusLabel: '不改变状态', description: '', toTankId: '', toTankName: '', quantity: '' } })
      await this.loadBiology()
      wx.showToast({ title: '事件已保存', icon: 'success' })
    } catch (error) { wx.showToast({ title: error?.message ?? '事件没有保存，请重试', icon: 'none' }) }
    finally { this.setData({ savingEvent: false }) }
  },

  /** 按事件编号读取照片，不为每条事件预先请求图片。 @Author zhanghao_SMEICS @Date 2026-11-12 */
  async showEventPhotos(event) {
    try {
      const result = await reefApi.listPhotos('', 1, 100, 'BIOLOGY_EVENT', event.currentTarget.dataset.id)
      const urls = (result?.list ?? result ?? []).map((photo) => photo.contentUrl).filter(Boolean)
      if (!urls.length) return wx.showToast({ title: '这个事件还没有照片', icon: 'none' })
      wx.previewImage({ urls })
    } catch (error) { wx.showToast({ title: error?.message ?? '事件照片暂时无法读取', icon: 'none' }) }
  }
})
