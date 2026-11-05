/**
 * 首页场景编排器，加载用户鱼缸并提供新增鱼缸和快捷入口。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
const reefApi = require('../../services/reef-api')
const { shanghaiDateTimeParts } = require('../../services/date-time')

Page({
  data: {
    tanks: [],
    greeting: '你好',
    loading: true,
    errorMessage: '',
    menuOpen: false,
    tankFormOpen: false,
    savingTank: false,
    tankForm: { name: '', startedAt: '', capacityLiter: '', systemType: '' }
  },

  onShow() {
    const hour = Number(shanghaiDateTimeParts(new Date()).time.slice(0, 2))
    this.setData({ greeting: hour < 11 ? '早上好' : hour < 18 ? '下午好' : '晚上好' })
    this.loadDashboard()
  },

  async loadDashboard() {
    this.setData({ loading: true, errorMessage: '' })
    try {
      const result = await reefApi.getDashboard()
      this.setData({ tanks: result?.tanks ?? [] })
    } catch (error) {
      this.setData({ tanks: [], errorMessage: error.message || '鱼缸信息暂时无法读取' })
    } finally {
      this.setData({ loading: false })
    }
  },

  toggleMenu() {
    this.setData({ menuOpen: !this.data.menuOpen })
  },

  closeMenu() {
    this.setData({ menuOpen: false })
  },

  openTankForm() {
    this.setData({ menuOpen: false, tankFormOpen: true })
  },

  closeTankForm() {
    this.setData({ tankFormOpen: false })
  },

  openQuickLog() {
    this.setData({ menuOpen: false })
    wx.navigateTo({ url: '/pages/log/add' })
  },

  openLogs() { wx.navigateTo({ url: '/pages/log/list' }) },
  openPhotos() { wx.navigateTo({ url: '/pages/photos/index' }) },
  openSettings() { wx.navigateTo({ url: '/pages/settings/index' }) },

  onTankFieldChange(event) {
    const field = event.currentTarget.dataset.field
    this.setData({ [`tankForm.${field}`]: event.detail })
  },

  async saveTank() {
    const form = this.data.tankForm
    if (!form.name.trim() || !form.startedAt.trim()) {
      wx.showToast({ title: '请填写鱼缸名称和开缸日期', icon: 'none' })
      return
    }
    this.setData({ savingTank: true })
    try {
      await reefApi.saveTank({ ...form, capacityLiter: Number(form.capacityLiter) || null })
      this.setData({ tankFormOpen: false, tankForm: { name: '', startedAt: '', capacityLiter: '', systemType: '' } })
      await this.loadDashboard()
      wx.showToast({ title: '鱼缸已保存', icon: 'success' })
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' })
    } finally {
      this.setData({ savingTank: false })
    }
  }
})
