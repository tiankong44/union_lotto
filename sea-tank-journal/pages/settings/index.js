/**
 * 提醒与通用设置场景，保存用户偏好、订阅结果和 JSON 导出。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
const reefApi = require('../../services/reef-api')
const { API_BASE_URL } = require('../../config/env')
const { resetLogin } = require('../../services/auth')
const { syncTabBar } = require('../../services/tab-bar')
const { exportBackup, restoreBackup } = require('../../services/backup')

const REMINDER_DEFAULTS = [
  { type: 'WATER_CHANGE', label: '换水提醒', frequency: '每 14 天', icon: '↻', enabled: true },
  { type: 'WATER_TEST', label: '水质检测提醒', frequency: '每周', icon: '◉', enabled: true },
  { type: 'FEEDING', label: '喂食提醒', frequency: '每天', icon: '✦', enabled: false },
  { type: 'EQUIPMENT', label: '设备维护提醒', frequency: '按设备维护周期', icon: '⌁', enabled: true }
]

Page({
  data: { reminders: REMINDER_DEFAULTS, sendTime: '', cacheSize: '读取中…', backupBusy: false, backupProgress: '',
    profile: { nickname: '', avatarFileKey: '', avatarUrl: '' }, avatarTempPath: '', profileBusy: false },
  onShow() {
    // 将全局导航选中项同步到“我的”页。
    syncTabBar(this, 3)
    this.loadProfile()
    this.loadSettings()
    this.loadCacheSize()
  },

  /** 读取当前账号的资料并显示头像云端地址。 @Author zhanghao_SMEICS @Date 2026-11-12 */
  async loadProfile() {
    try {
      const profile = await reefApi.getProfile()
      const avatarUrl = profile?.avatarUrl ? `${API_BASE_URL}${profile.avatarUrl}` : ''
      this.setData({ profile: { nickname: profile?.nickname ?? '', avatarFileKey: profile?.avatarFileKey ?? '', avatarUrl } })
    } catch (error) {
      wx.showToast({ title: error?.message || '个人资料暂时无法读取，请重试', icon: 'none' })
    }
  },

  /** 保存用户选择的头像与填写的昵称。 @Author zhanghao_SMEICS @Date 2026-11-12 */
  async saveProfile() {
    if (this.data.profileBusy) return
    const nickname = (this.data.profile?.nickname ?? '').trim()
    if (!nickname) {
      wx.showToast({ title: '请填写昵称后保存', icon: 'none' })
      return
    }
    this.setData({ profileBusy: true })
    try {
      let avatarFileKey = this.data.profile?.avatarFileKey || ''
      if (this.data.avatarTempPath) {
        // 先把用户选中的头像保存到共享云端文件服务。
        const uploaded = await reefApi.uploadAvatar(this.data.avatarTempPath)
        avatarFileKey = uploaded?.fileKey ?? ''
      }
      // 保存昵称和云端文件编号，不将临时本地路径写入服务端。
      const profile = await reefApi.saveProfile({ nickname, avatarFileKey: avatarFileKey || null })
      const avatarUrl = profile?.avatarUrl ? `${API_BASE_URL}${profile.avatarUrl}` : ''
      this.setData({ profile: { nickname: profile?.nickname ?? nickname, avatarFileKey: profile?.avatarFileKey ?? '', avatarUrl }, avatarTempPath: '' })
      wx.showToast({ title: '个人资料已保存', icon: 'success' })
    } catch (error) {
      wx.showToast({ title: error?.message || '资料没有保存，请重试', icon: 'none' })
    } finally {
      this.setData({ profileBusy: false })
    }
  },

  /** 将用户选中的临时头像仅保留在当前页面，等待点击保存后上传。 @Author zhanghao_SMEICS @Date 2026-11-12 */
  onChooseAvatar(event) {
    const avatarTempPath = event?.detail?.avatarUrl ?? ''
    this.setData({ avatarTempPath, 'profile.avatarUrl': avatarTempPath })
  },

  /** 更新昵称输入内容，避免直接修改页面数据对象。 @Author zhanghao_SMEICS @Date 2026-11-12 */
  onNicknameInput(event) {
    this.setData({ 'profile.nickname': event?.detail?.value ?? '' })
  },

  /** 确认后清除本地登录令牌并返回登录页。 @Author zhanghao_SMEICS @Date 2026-11-12 */
  logout() {
    wx.showModal({
      title: '退出登录',
      content: '退出后需要重新登录才能查看账号资料和鱼缸记录。',
      confirmText: '退出登录',
      cancelText: '取消',
      success: (result) => {
        if (!result.confirm) return
        // 清除本地认证令牌和认证请求状态，保留用户业务数据。
        wx.removeStorageSync('reefAccessToken')
        resetLogin()
        this.setData({ profile: { nickname: '', avatarFileKey: '', avatarUrl: '' }, avatarTempPath: '' })
        // 退出后回到不需要令牌的微信登录入口。
        wx.reLaunch({
          url: '/pages/auth/login',
          fail: () => wx.showToast({ title: '登录页暂时无法打开，请重新进入小程序', icon: 'none' })
        })
      }
    })
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
    if (this.data.backupBusy) return
    this.setData({ backupBusy: true, backupProgress: '正在准备备份…' })
    try {
      // 导出记录和原照片，全部完成后再交由用户选择保存位置。
      const filePath = await exportBackup((message) => this.setData({ backupProgress: message }))
      wx.shareFileMessage({ filePath, fileName: '水境日记完整备份.json', fail: () => wx.showToast({ title: '备份已生成，当前页面暂不支持分享', icon: 'none' }) })
    } catch (error) {
      wx.showToast({ title: error?.message ?? '备份没有生成，请重试', icon: 'none' })
    } finally { this.setData({ backupBusy: false, backupProgress: '' }) }
  },

  /** 恢复到当前账号；相同备份可重复选择以继续未完成的照片。 @Author zhanghao_SMEICS @Date 2026-11-12 */
  async restoreData() {
    if (this.data.backupBusy) return
    this.setData({ backupBusy: true, backupProgress: '请选择备份文件…' })
    try {
      await restoreBackup((message) => this.setData({ backupProgress: message }))
      wx.showToast({ title: '备份已恢复', icon: 'success' })
    } catch (error) {
      wx.showModal({ title: '备份尚未全部恢复', content: `${error?.message ?? '请重试'}。已恢复的记录会保留，可再次选择同一备份继续。`, showCancel: false })
    } finally { this.setData({ backupBusy: false, backupProgress: '' }) }
  },

  clearCache() {
    wx.clearStorageSync()
    this.setData({ cacheSize: '0 MB' })
    wx.showToast({ title: '本地缓存已清理，请重新登录', icon: 'none' })
  }
})
