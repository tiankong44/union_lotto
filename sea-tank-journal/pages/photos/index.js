/**
 * 影像记录场景，按分类和上海本地日期分组加载用户照片。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
const reefApi = require('../../services/reef-api')
const { shanghaiDateTimeParts } = require('../../services/date-time')

Page({
  data: {
    categories: [{ value: '', label: '全部' }, { value: 'TANK', label: '鱼缸' }, { value: 'FISH', label: '鱼' }, { value: 'CORAL', label: '珊瑚' }, { value: 'EQUIPMENT', label: '设备' }, { value: 'PLANT', label: '植物' }],
    selectedCategory: '', recent: [], older: [], uploading: false, pageNum: 0, hasMore: true, loading: false, loadVersion: 0
  },

  onLoad() { this.loadPhotos() },
  onShow() { this.loadPhotos() },
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
      wx.showToast({ title: error.message, icon: 'none' })
    } finally {
      if (loadVersion === this.data.loadVersion) this.setData({ loading: false })
    }
  },

  selectCategory(event) {
    this.setData({ selectedCategory: event.currentTarget.dataset.value })
    this.loadPhotos(true)
  },

  previewPhoto(event) {
    const group = event.currentTarget.dataset.group
    const index = Number(event.currentTarget.dataset.index)
    const photos = this.data[group]
    wx.previewImage({ current: photos[index].contentUrl, urls: photos.map((item) => item.contentUrl) })
  },

  addPhoto() {
    if (this.data.uploading) return
    wx.chooseMedia({ count: 9, mediaType: ['image'], sourceType: ['album', 'camera'], success: async (result) => {
      this.setData({ uploading: true })
      let failed = false
      for (const file of result.tempFiles) {
        try {
          await reefApi.uploadPhoto(file.tempFilePath, { title: '海缸记录', category: this.data.selectedCategory || 'TANK' })
        } catch (error) {
          failed = true
        }
      }
      await this.loadPhotos(true)
      this.setData({ uploading: false })
      wx.showToast({ title: failed ? '部分照片没有上传，请检查网络后重试' : '照片已添加', icon: failed ? 'none' : 'success' })
    } })
  }
})
