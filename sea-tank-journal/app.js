/**
 * 水境日记小程序应用入口，负责启动登录态初始化。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
const { ensureLogin } = require('./services/auth')

App({
  globalData: {
    loginPromise: null,
    pendingLogTank: null
  },

  onLaunch() {
    // 启动时建立服务端登录态，页面请求仍会在令牌失效时重新登录。
    this.globalData.loginPromise = ensureLogin().catch(() => null)
  }
})
