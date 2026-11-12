/**
 * 鱼缸详情场景编排器，切换生物、日志、水质和设备内容。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
const reefApi = require('../../services/reef-api')
const { describeTank } = require('../../services/aquarium-catalog')

Page({
  data: {
    tankId: '', tank: { name: '', archived: false, tankTypeSummary: '', systemType: '' }, tankCoverUrl: '', biology: [], biologyPageNum: 1, biologyHasMore: false, biologyLoading: false, logs: [], equipment: [], waterRanges: [],
    tabs: [{ key: 'biology', label: '生物' }, { key: 'logs', label: '日志' }, { key: 'water', label: '水质' }, { key: 'equipment', label: '设备' }],
    activeTab: 'biology', rangeFormOpen: false, savingRanges: false,
    rangeFields: [
      { metric: 'salinity', label: '盐度', unit: '比重', min: '', max: '' },
      { metric: 'temperature', label: '水温', unit: '℃', min: '', max: '' },
      { metric: 'ph', label: 'pH', unit: '', min: '', max: '' },
      { metric: 'no2', label: 'NO₂', unit: 'mg/L', min: '', max: '' },
      { metric: 'no3', label: 'NO₃', unit: 'mg/L', min: '', max: '' },
      { metric: 'po4', label: 'PO₄', unit: 'mg/L', min: '', max: '' }
    ]
  },

  onLoad(query) {
    this.setData({ tankId: query.id || '' })
  },

  onShow() { if (this.data.tankId) this.loadTank() },

  /** 打开鱼缸编辑表单，归档状态也可在此恢复。 @Author zhanghao_SMEICS @Date 2026-11-12 */
  editTank() { wx.navigateTo({ url: `/pages/tank/add?id=${this.data.tankId}` }) },

  onReachBottom() {
    if (this.data.activeTab === 'biology' && this.data.biologyHasMore) this.loadMoreBiology()
  },

  async loadTank() {
    try {
      const [tank, biology, logs, equipment, photoResult] = await Promise.all([
        reefApi.getTank(this.data.tankId),
        reefApi.listBiology(this.data.tankId),
        reefApi.listLogs({ tankId: this.data.tankId, pageSize: 10 }),
        reefApi.listEquipment('', 1, 50, this.data.tankId),
        reefApi.listPhotos('TANK', 1, 20, 'TANK', this.data.tankId)
      ])
      const waterRanges = tank?.waterRanges ?? []
      const rangeFields = this.data.rangeFields.map((field) => {
        const saved = waterRanges.find((item) => item.metric === field.metric)
        return saved ? { ...field, min: saved.minimum ?? '', max: saved.maximum ?? '' } : field
      })
      const biologyRows = biology?.list ?? biology ?? []
      this.setData({
        tank: describeTank(tank || {}), tankCoverUrl: (photoResult?.list ?? photoResult ?? [])[0]?.contentUrl || '', biology: biologyRows, biologyPageNum: 1, biologyHasMore: biologyRows.length === 50,
        logs: logs?.list ?? logs ?? [], equipment: equipment?.list ?? equipment ?? [], waterRanges, rangeFields
      })
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' })
    }
  },

  selectTab(event) { this.setData({ activeTab: event.currentTarget.dataset.tab }) },

  async loadMoreBiology() {
    if (this.data.biologyLoading || !this.data.biologyHasMore) return
    this.setData({ biologyLoading: true })
    const nextPage = this.data.biologyPageNum + 1
    try {
      const result = await reefApi.listBiology(this.data.tankId, nextPage, 50)
      const rows = result?.list ?? result ?? []
      this.setData({ biology: this.data.biology.concat(rows), biologyPageNum: nextPage, biologyHasMore: rows.length === 50 })
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' })
    } finally {
      this.setData({ biologyLoading: false })
    }
  },

  openBiology(event) {
    wx.navigateTo({ url: `/pages/biology/detail?id=${event.currentTarget.dataset.id}` })
  },

  /** 共用生物新增与编辑表单。 @Author zhanghao_SMEICS @Date 2026-11-12 */
  openBiologyForm() { wx.navigateTo({ url: `/pages/biology/edit?tankId=${this.data.tankId}` }) },

  openLogList() {
    const app = getApp()
    app.globalData.pendingLogTank = { id: this.data.tankId, name: this.data.tank.name || '', archived: Boolean(this.data.tank.archived) }
    // 日志页属于底部 Tab，通过原生切换并带入当前鱼缸筛选。
    wx.switchTab({ url: '/pages/log/list' })
  },
  openWaterTrend() { wx.navigateTo({ url: `/pages/water/trend?tankId=${this.data.tankId}` }) },
  openEquipment() { wx.navigateTo({ url: `/pages/equipment/list?tankId=${this.data.tankId}` }) },
  openRangeForm() { this.setData({ rangeFormOpen: true }) },
  closeRangeForm() { this.setData({ rangeFormOpen: false }) },

  onRangeInput(event) {
    const { index, bound } = event.currentTarget.dataset
    this.setData({ [`rangeFields[${index}].${bound}`]: event.detail.value })
  },

  async saveRanges() {
    this.setData({ savingRanges: true })
    try {
      const ranges = this.data.rangeFields.map((item) => ({
        metric: item.metric,
        minimum: item.min === '' ? null : Number(item.min),
        maximum: item.max === '' ? null : Number(item.max)
      }))
      await reefApi.saveWaterRanges(this.data.tankId, { ranges })
      this.setData({ rangeFormOpen: false })
      await this.loadTank()
      wx.showToast({ title: '参数范围已保存', icon: 'success' })
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' })
    } finally {
      this.setData({ savingRanges: false })
    }
  }
})
