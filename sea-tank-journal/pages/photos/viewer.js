/**
 * 全屏影像浏览页，支持在照片间切换并进入影像详情。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
const reefApi = require('../../services/reef-api')
const reefPhotoMedia = require('../../services/reef-photo-media')

Page({
  data: {
    photoId: '', photos: [], currentIndex: 0,
    currentPhoto: { id: '', contentUrl: '', title: '', recordedAt: '' }, loading: true
  },

  onLoad(query) {
    this.setData({ photoId: query.id || '' })
  },

  onShow() {
    this.loadPhotos()
  },

  /**
   * 读取最近影像并定位到用户选择的照片。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-05
   */
  async loadPhotos() {
    this.setData({ loading: true })
    try {
      const listResult = await reefApi.listPhotos('', 1, 100)
      const photos = (listResult?.list ?? listResult ?? []).map(reefPhotoMedia.withMotionUrl)
      let currentIndex = photos.findIndex((photo) => photo.id === this.data.photoId)
      if (currentIndex < 0) {
        try {
          const selectedPhoto = reefPhotoMedia.withMotionUrl(await reefApi.getPhoto(this.data.photoId))
          if (selectedPhoto) {
            photos.unshift(selectedPhoto)
            currentIndex = 0
          }
        } catch (error) {
          // 已删除照片不阻止浏览其他当前用户影像。
        }
      }
      if (currentIndex < 0 && photos.length) currentIndex = Math.min(this.data.currentIndex, photos.length - 1)
      this.setData({ photos, currentIndex, currentPhoto: photos[currentIndex] || {} })
    } catch (error) {
      wx.showToast({ title: error?.message || '照片暂时无法读取', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  },

  /**
   * 切换照片时同步底部信息。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-05
   */
  onSwiperChange(event) {
    const currentIndex = event.detail.current
    this.setData({ currentIndex, currentPhoto: this.data.photos[currentIndex] || {} })
  },

  /**
   * 从全屏浏览进入照片详情。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-05
   */
  openDetail() {
    wx.navigateTo({ url: `/pages/photos/detail?id=${this.data.currentPhoto.id}` })
  },

  /**
   * 返回影像列表。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-05
   */
  backToList() {
    wx.navigateBack({ delta: 1 })
  }
})
