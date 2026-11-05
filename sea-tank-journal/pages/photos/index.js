/**
 * 影像记录场景，按分类和上海本地日期分组加载用户照片。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
const reefApi = require('../../services/reef-api')
const { shanghaiDateTimeParts } = require('../../services/date-time')
const { syncTabBar } = require('../../services/tab-bar')

Page({
  data: {
    categories: [{ value: '', label: '全部' }, { value: 'TANK', label: '鱼缸' }, { value: 'FISH', label: '鱼' }, { value: 'CORAL', label: '珊瑚' }, { value: 'EQUIPMENT', label: '设备' }, { value: 'PLANT', label: '植物' }],
    selectedCategory: '', recent: [], older: [], pageNum: 0, hasMore: true, loading: false, loadVersion: 0
  },

  onShow() {
    // 将全局导航选中项同步到影像页。
    syncTabBar(this, 2)
    this.loadPhotos()
  },
  onReachBottom() { if (this.data.hasMore) this.loadPhotos(false) },

  async loadPhotos(reset = true) {
    if (!reset && (this.data.loading || !this.data.hasMore)) return
    const loadVersion = this.data.loadVersion + 1
    const pageNum = reset ? 1 : this.data.pageNum + 1
    this.setData({ loading: true, loadVersion })
    try {
      const result = await reefApi.listPhotos(this.data.selectedCategory, pageNum, 60)
      if (loadVersion !== this.data.loadVersion) return
      const incoming = result?.list ?? result ?? []
      const photos = reset ? incoming : this.data.recent.concat(this.data.older).concat(incoming)
      const cutoffDate = shanghaiDateTimeParts(new Date(Date.now() - 30 * 24 * 60 * 60 * 1000)).date
      const decorated = photos.map((item) => ({ ...item, dateLabel: String(item.recordedAt || '').slice(5, 10) }))
      this.setData({
        pageNum,
        hasMore: incoming.length === 60,
        recent: decorated.filter((item) => String(item.recordedAt || '').slice(0, 10) >= cutoffDate),
        older: decorated.filter((item) => String(item.recordedAt || '').slice(0, 10) < cutoffDate)
      })
    } catch (error) {
      if (loadVersion !== this.data.loadVersion) return
      if (reset) this.setData({ recent: [], older: [], hasMore: false })
      wx.showToast({ title: error?.message || '影像记录暂时无法读取', icon: 'none' })
    } finally {
      if (loadVersion === this.data.loadVersion) this.setData({ loading: false })
    }
  },

  selectCategory(event) {
    this.setData({ selectedCategory: event.currentTarget.dataset.value })
    this.loadPhotos(true)
  },

  previewPhoto(event) {
    // 使用自定义全屏浏览页保留底部记录信息和详情入口。
    wx.navigateTo({ url: `/pages/photos/viewer?id=${event.currentTarget.dataset.id}` })
  },

  addPhoto() {
    wx.navigateTo({ url: '/pages/photos/add' })
  }
})
