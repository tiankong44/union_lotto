/**
 * 日志详情场景，展示水质、关联生物和关联影像。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
const reefApi = require('../../services/reef-api')

const LOG_LABELS = {
  WATER_TEST: '水质检测', WATER_CHANGE: '换水', FEEDING: '喂食', MEDICATION: '用药', OBSERVATION: '观察'
}

Page({
  data: {
    logId: '',
    log: {},
    typeLabel: '',
    waterValues: [],
    biology: [],
    photos: [],
    loading: true,
    deleting: false
  },

  onLoad(query) {
    this.setData({ logId: query.id || '' })
  },

  onShow() {
    this.loadLog()
  },

  /**
   * 读取日志、关联生物和引用该日志的影像。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-05
   */
  async loadLog() {
    this.setData({ loading: true })
    try {
      const log = await reefApi.getLog(this.data.logId)
      const [tank, biologyResult, photoResult] = await Promise.all([
        reefApi.getTank(log?.tankId || ''),
        reefApi.listBiology(log?.tankId || '', 1, 100),
        reefApi.listPhotos('', 1, 100, 'LOG', this.data.logId)
      ])
      const biologyRows = biologyResult?.list ?? biologyResult ?? []
      const biologyIds = log?.biologyIds ?? []
      const photos = photoResult?.list ?? photoResult ?? []
      const waterValues = [
        { label: '盐度', value: log?.salinity, unit: '比重' },
        { label: '水温', value: log?.temperature, unit: '℃' },
        { label: 'pH', value: log?.ph, unit: '' },
        { label: 'NO₂', value: log?.no2, unit: 'mg/L' },
        { label: 'NO₃', value: log?.no3, unit: 'mg/L' },
        { label: 'PO₄', value: log?.po4, unit: 'mg/L' }
      ].filter((item) => item.value !== null && item.value !== undefined && item.value !== '')
      this.setData({
        log: { ...log, tankName: tank?.name || '' },
        typeLabel: LOG_LABELS[log?.type] || '日志',
        waterValues,
        biology: biologyRows.filter((item) => biologyIds.includes(item.id)),
        photos
      })
    } catch (error) {
      wx.showToast({ title: error?.message || '这条日志暂时无法读取', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  },

  /**
   * 使用原日志编号打开可回填的编辑表单。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-05
   */
  editLog() {
    wx.navigateTo({ url: `/pages/log/add?id=${this.data.logId}` })
  },

  /**
   * 确认后删除日志，服务端会检查用户归属和照片引用。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-05
   */
  deleteLog() {
    if (this.data.deleting) return
    wx.showModal({
      title: '删除这条日志？',
      content: '删除后无法恢复；仍关联照片时，需要先在照片详情中解除关联。',
      confirmText: '删除日志',
      confirmColor: '#d76565',
      success: async (result) => {
        if (!result.confirm || this.data.deleting) return
        this.setData({ deleting: true })
        try {
          // 调用服务端按用户归属执行删除并清理生物关联。
          await reefApi.deleteLog(this.data.logId)
          wx.showToast({ title: '日志已删除', icon: 'success' })
          wx.navigateBack({ delta: 1 })
        } catch (error) {
          wx.showToast({ title: error?.message || '日志没有删除，请刷新后重试', icon: 'none' })
        } finally {
          this.setData({ deleting: false })
        }
      }
    })
  },

  /**
   * 打开照片详情查看影像备注与关联对象。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-05
   */
  openPhoto(event) {
    wx.navigateTo({ url: `/pages/photos/detail?id=${event.currentTarget.dataset.id}` })
  },

  /**
   * 打开所关联生物的详情页。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-05
   */
  openBiology(event) {
    wx.navigateTo({ url: `/pages/biology/detail?id=${event.currentTarget.dataset.id}` })
  }
})
