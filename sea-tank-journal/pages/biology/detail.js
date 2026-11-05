/**
 * 生物详情场景编排器，展示生物资料和用户维护的事件时间轴。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
const reefApi = require('../../services/reef-api')

Page({
  data: {
    biologyId: '', biology: {}, events: [], eventFormOpen: false, savingEvent: false,
    eventForm: { title: '', eventAt: '', description: '' }
  },

  onLoad(query) {
    this.setData({ biologyId: query.id || '' })
    this.loadBiology()
  },

  async loadBiology() {
    try {
      const [biology, events] = await Promise.all([
        reefApi.getBiology(this.data.biologyId),
        reefApi.listBiologyEvents(this.data.biologyId)
      ])
      this.setData({ biology: biology || {}, events: events ?? [] })
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' })
    }
  },

  openEventForm() { this.setData({ eventFormOpen: true }) },
  closeEventForm() { this.setData({ eventFormOpen: false }) },
  onEventFieldChange(event) { this.setData({ [`eventForm.${event.currentTarget.dataset.field}`]: event.detail }) },

  async saveEvent() {
    if (!this.data.eventForm.title.trim() || !this.data.eventForm.eventAt.trim()) {
      wx.showToast({ title: '请填写事件标题和记录时间', icon: 'none' })
      return
    }
    this.setData({ savingEvent: true })
    try {
      await reefApi.saveBiologyEvent(this.data.biologyId, this.data.eventForm)
      this.setData({ eventFormOpen: false, eventForm: { title: '', eventAt: '', description: '' } })
      await this.loadBiology()
      wx.showToast({ title: '事件已保存', icon: 'success' })
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' })
    } finally {
      this.setData({ savingEvent: false })
    }
  }
})
