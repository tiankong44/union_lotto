/**
 * 提醒与通用设置场景，保存用户偏好、订阅结果和 JSON 导出。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
const reefApi = require('../../services/reef-api')
const { syncTabBar } = require('../../services/tab-bar')

const REMINDER_DEFAULTS = [
  { type: 'WATER_CHANGE', label: '换水提醒', frequency: '每 14 天', icon: '↻', enabled: true },
  { type: 'WATER_TEST', label: '水质检测提醒', frequency: '每周', icon: '◉', enabled: true },
  { type: 'FEEDING', label: '喂食提醒', frequency: '每天', icon: '✦', enabled: false },
  { type: 'EQUIPMENT', label: '设备维护提醒', frequency: '按设备维护周期', icon: '⌁', enabled: true }
]

Page({
  data: { reminders: REMINDER_DEFAULTS, sendTime: '', cacheSize: '读取中…' },
  onShow() {
    // 将全局导航选中项同步到“我的”页。
    syncTabBar(this, 3)
    this.loadSettings()
    this.loadCacheSize()
  },

  async loadSettings() {
    try {
      const result = await reefApi.getReminders()
      const configured = result?.items ?? []
      const reminders = REMINDER_DEFAULTS.map((item) => ({ ...item, ...(configured.find((value) => value.type === item.type) || {}) }))
      this.setData({ reminders, sendTime: result?.sendTime || '' })
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' })
    }
  },

  loadCacheSize() {
    try {
      const info = wx.getStorageInfoSync()
      this.setData({ cacheSize: `${(info.currentSize / 1024).toFixed(1)} MB` })
    } catch (error) {
      this.setData({ cacheSize: '暂不可用' })
    }
  },

  async onSendTimeChange(event) {
    this.setData({ sendTime: event.detail.value })
    await this.saveSettings()
  },

  async onReminderToggle(event) {
    const type = event.currentTarget.dataset.type
    const enabled = Boolean(event.detail)
    const reminders = this.data.reminders.map((item) => item.type === type ? { ...item, enabled } : item)
    this.setData({ reminders })
    if (enabled) await this.requestSubscription(type)
    await this.saveSettings()
  },

  async requestSubscription(type) {
    const selected = this.data.reminders.find((item) => item.type === type)
    if (!selected?.templateId) {
      wx.showToast({ title: '订阅模板待管理员配置', icon: 'none' })
      return
    }
    wx.requestSubscribeMessage({
      tmplIds: [selected.templateId],
      success: async (result) => {
        const accepted = result[selected.templateId] === 'accept'
        try {
          await reefApi.saveSubscriptionConsent({ type, accepted })
          wx.showToast({ title: accepted ? '已记录本次订阅' : '未获得本次提醒授权', icon: 'none' })
        } catch (error) {
          wx.showToast({ title: error.message, icon: 'none' })
        }
      },
      fail: () => wx.showToast({ title: '订阅授权暂时无法打开', icon: 'none' })
    })
  },

  async saveSettings() {
    try {
      await reefApi.saveReminders({ sendTime: this.data.sendTime, items: this.data.reminders.map(({ type, enabled }) => ({ type, enabled })) })
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' })
    }
  },

  async exportData() {
    try {
      const data = await reefApi.exportData()
      const filePath = `${wx.env.USER_DATA_PATH}/reef-journal-export.json`
      wx.getFileSystemManager().writeFile({
        filePath,
        data: JSON.stringify(data, null, 2),
        encoding: 'utf8',
        success: () => wx.shareFileMessage({ filePath, fileName: '海缸日记备份.json', fail: () => wx.showToast({ title: '文件已生成，但当前页面暂不支持分享', icon: 'none' }) }),
        fail: () => wx.showToast({ title: '备份文件暂时无法生成', icon: 'none' })
      })
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' })
    }
  },

  clearCache() {
    wx.clearStorageSync()
    this.setData({ cacheSize: '0 MB' })
    wx.showToast({ title: '本地缓存已清理，请重新登录', icon: 'none' })
  }
})
