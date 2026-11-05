/**
 * 水境日记微信登录服务，使用服务端换发的访问令牌标识当前用户。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
const { API_BASE_URL } = require('../config/env')
let loginPromise = null

/**
 * 获取现有访问令牌或以微信临时凭证建立新的服务端登录态。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 * @returns {Promise<string>} 服务端签发的访问令牌
 */
function ensureLogin() {
  const currentToken = wx.getStorageSync('reefAccessToken')
  if (currentToken) return Promise.resolve(currentToken)
  if (!API_BASE_URL) return Promise.reject(new Error('服务地址待补充，请先配置小程序 API 域名'))
  if (loginPromise) return loginPromise

  loginPromise = new Promise((resolve, reject) => {
    wx.login({
      success(loginResult) {
        if (!loginResult.code) {
          reject(new Error('暂时无法取得微信登录凭证，请稍后重试'))
          return
        }
        wx.request({
          url: `${API_BASE_URL}/tabs/reef-journal/auth/login`,
          method: 'POST',
          data: { code: loginResult.code },
          success(response) {
            const body = /** @type {{code?: number, message?: string, data?: {accessToken?: string}}} */ (/** @type {any} */ (response.data || {}))
            if (response.statusCode < 200 || response.statusCode >= 300 || body.code !== 0 || !body.data?.accessToken) {
              reject(new Error(body.message || '微信登录暂未完成，请稍后重试'))
              return
            }
            wx.setStorageSync('reefAccessToken', body.data.accessToken)
            resolve(body.data.accessToken)
          },
          fail() {
            reject(new Error('登录服务暂时不可用，请稍后重试'))
          }
        })
      },
      fail() {
        reject(new Error('微信登录暂时不可用，请稍后重试'))
      }
    })
  })
  return loginPromise.then((token) => {
    loginPromise = null
    return token
  }, (error) => {
    loginPromise = null
    throw error
  })
}

/**
 * 清除进程内登录请求状态，使失效令牌能够重新换取登录态。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
function resetLogin() {
  loginPromise = null
}

module.exports = { ensureLogin, resetLogin }
