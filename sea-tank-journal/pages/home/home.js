/**
 * 首页场景编排器，加载用户鱼缸并提供新增鱼缸和快捷入口。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
const reefApi = require('../../services/reef-api')
const { shanghaiDateTimeParts } = require('../../services/date-time')
const { syncTabBar } = require('../../services/tab-bar')

Page({
  data: {
    tanks: [],
    greeting: '你好',
    loading: true,
    errorMessage: '',
    menuOpen: false
  },

  onShow() {
    // 将全局导航选中项同步到首页。
    syncTabBar(this, 0)
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
    this.setData({ menuOpen: false })
    // 跳转到独立鱼缸表单，保存后返回首页刷新列表。
    wx.navigateTo({ url: '/pages/tank/add' })
  },

  openQuickLog() {
    this.setData({ menuOpen: false })
    wx.navigateTo({ url: '/pages/log/add' })
  },

})
