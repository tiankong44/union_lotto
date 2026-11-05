/**
 * 日志列表场景，按日志类型筛选并按月份分组展示。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
const reefApi = require('../../services/reef-api')

const FILTERS = [
  { value: '', label: '全部' }, { value: 'WATER_TEST', label: '水质' }, { value: 'WATER_CHANGE', label: '换水' },
  { value: 'FEEDING', label: '喂食' }, { value: 'MEDICATION', label: '用药' }, { value: 'OBSERVATION', label: '观察' }
]

Page({
  data: { filters: FILTERS, selectedType: '', groups: [], logs: [], pageNum: 0, hasMore: true, loading: false, loadVersion: 0 },
  onLoad() { this.loadLogs() },
  onShow() { this.loadLogs() },
  onReachBottom() { if (this.data.hasMore) this.loadLogs(false) },

  async loadLogs(reset = true) {
    if (!reset && (this.data.loading || !this.data.hasMore)) return
    const loadVersion = this.data.loadVersion + 1
    const pageNum = reset ? 1 : this.data.pageNum + 1
    this.setData({ loading: true, loadVersion })
    try {
      const result = await reefApi.listLogs({ type: this.data.selectedType, pageNum, pageSize: 100 })
      if (loadVersion !== this.data.loadVersion) return
      const incoming = result?.list ?? result ?? []
      const logs = reset ? incoming : this.data.logs.concat(incoming)
      const grouped = logs.reduce((groups, item) => {
        const date = String(item.recordedAt || '').replace('T', ' ')
        const month = date.slice(0, 7) || '未记录时间'
        const shortTime = date.slice(5, 16)
        if (!groups[month]) groups[month] = []
        groups[month].push({ ...item, timeLabel: shortTime, icon: this.iconFor(item.type) })
        return groups
      }, {})
      this.setData({
        logs,
        pageNum,
        hasMore: incoming.length === 100,
        groups: Object.keys(grouped).sort().reverse().map((month) => ({ month, items: grouped[month] }))
      })
    } catch (error) {
      if (loadVersion !== this.data.loadVersion) return
      if (reset) this.setData({ groups: [], logs: [], hasMore: false })
      wx.showToast({ title: error.message, icon: 'none' })
    } finally {
      if (loadVersion === this.data.loadVersion) this.setData({ loading: false })
    }
  },

  iconFor(type) {
    return { WATER_TEST: '◉', WATER_CHANGE: '↻', FEEDING: '✦', MEDICATION: '＋', OBSERVATION: '◎' }[type] || '◉'
  },

  selectFilter(event) {
    this.setData({ selectedType: event.currentTarget.dataset.value })
    this.loadLogs(true)
  },

  addLog() { wx.navigateTo({ url: '/pages/log/add' }) }
})
