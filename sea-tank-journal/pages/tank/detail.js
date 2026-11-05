/**
 * 鱼缸详情场景编排器，切换生物、日志、水质和设备内容。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
const reefApi = require('../../services/reef-api')

Page({
  data: {
    tankId: '', tank: {}, biology: [], biologyPageNum: 1, biologyHasMore: false, biologyLoading: false, logs: [], equipment: [], waterRanges: [],
    tabs: [{ key: 'biology', label: '生物' }, { key: 'logs', label: '日志' }, { key: 'water', label: '水质' }, { key: 'equipment', label: '设备' }],
    biologyCategories: [{ value: 'FISH', label: '鱼' }, { value: 'CORAL', label: '珊瑚' }, { value: 'PLANT', label: '植物' }, { value: 'OTHER', label: '其他' }],
    biologyStatuses: [{ value: 'NORMAL', label: '正常' }, { value: 'QUARANTINE', label: '检疫中' }, { value: 'ISOLATED', label: '隔离中' }, { value: 'SICK', label: '生病' }, { value: 'DEAD', label: '阵亡' }],
    biologyCategoryLabels: ['鱼', '珊瑚', '植物', '其他'],
    biologyStatusLabels: ['正常', '检疫中', '隔离中', '生病', '阵亡'],
    activeTab: 'biology', biologyFormOpen: false, savingBiology: false, rangeFormOpen: false, savingRanges: false,
    biologyForm: { name: '', scientificName: '', addedAt: '', bodyLengthCm: '', source: '', category: 'FISH', categoryLabel: '鱼', status: 'NORMAL', statusLabel: '正常' },
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
    this.loadTank()
  },

  onReachBottom() {
    if (this.data.activeTab === 'biology' && this.data.biologyHasMore) this.loadMoreBiology()
  },

  async loadTank() {
    try {
      const [tank, biology, logs, equipment] = await Promise.all([
        reefApi.getTank(this.data.tankId),
        reefApi.listBiology(this.data.tankId),
        reefApi.listLogs({ tankId: this.data.tankId, pageSize: 10 }),
        reefApi.listEquipment('', 1, 50, this.data.tankId)
      ])
      const waterRanges = tank?.waterRanges ?? []
      const rangeFields = this.data.rangeFields.map((field) => {
        const saved = waterRanges.find((item) => item.metric === field.metric)
        return saved ? { ...field, min: saved.minimum ?? '', max: saved.maximum ?? '' } : field
      })
      const biologyRows = biology?.list ?? biology ?? []
      this.setData({
        tank: tank || {}, biology: biologyRows, biologyPageNum: 1, biologyHasMore: biologyRows.length === 50,
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

  openBiologyForm() { this.setData({ biologyFormOpen: true }) },
  closeBiologyForm() { this.setData({ biologyFormOpen: false }) },
  onBiologyFieldChange(event) { this.setData({ [`biologyForm.${event.currentTarget.dataset.field}`]: event.detail }) },

  onBiologyCategoryChange(event) {
    const selected = this.data.biologyCategories[Number(event.detail.value)]
    this.setData({ 'biologyForm.category': selected.value, 'biologyForm.categoryLabel': selected.label })
  },

  onBiologyStatusChange(event) {
    const selected = this.data.biologyStatuses[Number(event.detail.value)]
    this.setData({ 'biologyForm.status': selected.value, 'biologyForm.statusLabel': selected.label })
  },

  async saveBiology() {
    if (!this.data.biologyForm.name.trim() || !this.data.biologyForm.addedAt.trim()) {
      wx.showToast({ title: '请填写名称和入缸日期', icon: 'none' })
      return
    }
    this.setData({ savingBiology: true })
    try {
      const form = this.data.biologyForm
      await reefApi.saveBiology({
        name: form.name,
        scientificName: form.scientificName,
        category: form.category,
        addedAt: form.addedAt,
        bodyLengthCm: Number(form.bodyLengthCm) || null,
        source: form.source,
        status: form.status,
        tankId: this.data.tankId
      })
      this.setData({
        biologyFormOpen: false,
        biologyForm: { name: '', scientificName: '', addedAt: '', bodyLengthCm: '', source: '', category: 'FISH', categoryLabel: '鱼', status: 'NORMAL', statusLabel: '正常' }
      })
      await this.loadTank()
      wx.showToast({ title: '生物已保存', icon: 'success' })
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' })
    } finally {
      this.setData({ savingBiology: false })
    }
  },

  openLogList() { wx.navigateTo({ url: '/pages/log/list' }) },
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
