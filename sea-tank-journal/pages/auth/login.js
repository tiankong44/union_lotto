/**
 * 微信登录入口，用户点击后使用微信临时凭证换取水境日记访问令牌。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
const { ensureLogin } = require('../../services/auth')

Page({
  data: {
    loggingIn: false,
    errorMessage: ''
  },

  onLoad() {
    // 已有登录令牌时直接进入首页，失效令牌由接口请求统一刷新。
    if (wx.getStorageSync('reefAccessToken')) this.openHome()
  },

  /**
   * 发起微信登录并进入首页，失败时保留登录入口供用户重试。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-05
   */
  async login() {
    if (this.data.loggingIn) return
    this.setData({ loggingIn: true, errorMessage: '' })
    try {
      // 通过现有认证服务换取服务端令牌，不读取微信用户资料。
      await ensureLogin()
      this.openHome()
    } catch (error) {
      const errorMessage = error?.message || '微信登录暂时无法完成，请检查网络后重试'
      this.setData({ errorMessage })
      wx.showToast({ title: errorMessage, icon: 'none' })
    } finally {
      this.setData({ loggingIn: false })
    }
  },

  /**
   * 进入底部导航首页。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-05
   */
  openHome() {
    wx.switchTab({
      url: '/pages/home/home',
      fail: () => this.setData({ errorMessage: '首页暂时无法打开，请重试' })
    })
  }
})
