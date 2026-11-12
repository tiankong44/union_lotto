/**
 * 照片详情场景，展示云端影像、关联对象和可编辑的记录资料。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
const reefApi = require('../../services/reef-api')

const CATEGORY_LABELS = { TANK: '鱼缸', FISH: '鱼', CORAL: '珊瑚', EQUIPMENT: '设备', PLANT: '植物', OTHER: '其他生物' }
const REFERENCE_LABELS = { TANK: '鱼缸', BIOLOGY: '生物', BIOLOGY_EVENT: '生物事件', EQUIPMENT: '设备', LOG: '日志' }

Page({
  data: { photoId: '', photo: {}, categoryLabel: '', referenceLabel: '不关联', referenceName: '不关联', loading: true, deleting: false },

  onLoad(query) {
    this.setData({ photoId: query.id || '' })
  },

  onShow() {
    this.loadPhoto()
  },

  /**
   * 读取照片和关联对象名称，详情缺失时保留字符串编号。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-05
   */
  async loadPhoto() {
    this.setData({ loading: true })
    try {
      const photo = await reefApi.getPhoto(this.data.photoId)
      const referenceName = await this.resolveReferenceName(photo?.referenceType, photo?.referenceId)
      this.setData({
        photo: photo || {},
        categoryLabel: CATEGORY_LABELS[photo?.category] || '其他',
        referenceLabel: REFERENCE_LABELS[photo?.referenceType] || '不关联',
        referenceName
      })
    } catch (error) {
      wx.showToast({ title: error?.message || '这张照片暂时无法读取', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  },

  /**
   * 根据照片保存的关联类型读取展示名称。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-05
   */
  async resolveReferenceName(type, id) {
    if (!type || !id) return '不关联'
    try {
      let item
      if (type === 'TANK') item = await reefApi.getTank(id)
      if (type === 'BIOLOGY') item = await reefApi.getBiology(id)
      if (type === 'EQUIPMENT') item = await reefApi.getEquipment(id)
      if (type === 'LOG') item = await reefApi.getLog(id)
      return item?.name || item?.title || id
    } catch (error) {
      return id
    }
  },

  /**
   * 打开照片元数据编辑表单。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-05
   */
  editPhoto() {
    wx.navigateTo({ url: `/pages/photos/add?id=${this.data.photoId}` })
  },

  /**
   * 确认后移除当前用户照片记录和云端文件。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-05
   */
  deletePhoto() {
    if (this.data.deleting) return
    wx.showModal({
      title: '删除这张照片？',
      content: '照片文件和记录都会删除，无法恢复。',
      confirmText: '删除照片',
      confirmColor: '#d76565',
      success: async (result) => {
        if (!result.confirm || this.data.deleting) return
        this.setData({ deleting: true })
        try {
          // 服务端同步删除照片元数据和对应云端文件内容。
          await reefApi.deletePhoto(this.data.photoId)
          wx.showToast({ title: '照片已删除', icon: 'success' })
          wx.navigateBack({ delta: 1 })
        } catch (error) {
          wx.showToast({ title: error?.message || '照片没有删除，请刷新后重试', icon: 'none' })
        } finally {
          this.setData({ deleting: false })
        }
      }
    })
  }
})
