/**
 * 水质趋势场景，读取采样点并使用微信 Canvas 绘制折线。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
const reefApi = require('../../services/reef-api')

Page({
  data: {
    tanks: [], selectedTankId: '', selectedMetric: 'salinity', selectedDays: 30,
    currentMetric: { key: 'salinity', label: '盐度', unit: '比重' }, rangeDaysLabel: '30天',
    metrics: [
      { key: 'salinity', label: '盐度', unit: '比重' },
      { key: 'ph', label: 'pH', unit: '' },
      { key: 'no3', label: 'NO₃', unit: 'mg/L' },
      { key: 'no2', label: 'NO₂', unit: 'mg/L' },
      { key: 'po4', label: 'PO₄', unit: 'mg/L' }
    ],
    ranges: [{ days: 7, label: '7天' }, { days: 30, label: '30天' }, { days: 90, label: '90天' }, { days: 0, label: '全部' }],
    stats: {}, points: [], alerts: []
  },

  onLoad(query) {
    this.setData({ selectedTankId: query.tankId || '' })
    this.loadTanks()
  },

  async loadTanks() {
    try {
      const result = await reefApi.listTanks()
      const tanks = result?.list ?? result ?? []
      const selectedTankId = this.data.selectedTankId || tanks[0]?.id || ''
      this.setData({ tanks, selectedTankId })
      if (selectedTankId) this.loadTrend()
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' })
    }
  },

  async loadTrend() {
    try {
      const result = await reefApi.getWaterTrend(this.data.selectedTankId, this.data.selectedMetric, this.data.selectedDays)
      this.setData({ stats: result?.stats || {}, points: result?.points ?? [], alerts: result?.alerts ?? [] })
      this.drawChart()
    } catch (error) {
      this.setData({ stats: {}, points: [], alerts: [] })
      wx.showToast({ title: error.message, icon: 'none' })
    }
  },

  selectTank(event) { this.setData({ selectedTankId: event.currentTarget.dataset.id }); this.loadTrend() },
  selectMetric(event) {
    const selectedMetric = event.currentTarget.dataset.key
    const currentMetric = this.data.metrics.find((item) => item.key === selectedMetric) || this.data.metrics[0]
    this.setData({ selectedMetric, currentMetric })
    this.loadTrend()
  },
  selectRange(event) {
    const selectedDays = Number(event.currentTarget.dataset.days)
    this.setData({ selectedDays, rangeDaysLabel: selectedDays ? `${selectedDays}天` : '全部记录' })
    this.loadTrend()
  },

  drawChart() {
    const points = this.data.points
    const query = wx.createSelectorQuery().in(this)
    query.select('.chart-canvas').boundingClientRect((rect) => {
      if (!rect || !points.length) return
      const context = wx.createCanvasContext('trendChart', this)
      const width = rect.width
      const height = rect.height
      const values = points.map((point) => Number(point.value)).filter((value) => Number.isFinite(value))
      if (!values.length) return
      const minValue = Math.min(...values)
      const maxValue = Math.max(...values)
      const spread = maxValue - minValue || 1
      const plotTop = 24
      const plotBottom = height - 32
      const plotHeight = plotBottom - plotTop
      context.setStrokeStyle('#8bcffc')
      context.setLineWidth(3)
      context.beginPath()
      values.forEach((value, index) => {
        const x = values.length === 1 ? width / 2 : 8 + (width - 16) * index / (values.length - 1)
        const y = plotBottom - (value - minValue) / spread * plotHeight
        if (index === 0) context.moveTo(x, y)
        else context.lineTo(x, y)
      })
      context.stroke()
      values.forEach((value, index) => {
        const x = values.length === 1 ? width / 2 : 8 + (width - 16) * index / (values.length - 1)
        const y = plotBottom - (value - minValue) / spread * plotHeight
        context.beginPath()
        context.setFillStyle('#69baf0')
        context.arc(x, y, 5, 0, Math.PI * 2)
        context.fill()
      })
      context.draw()
    }).exec()
  }
})
