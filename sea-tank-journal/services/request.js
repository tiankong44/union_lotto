/**
 * 海缸日记统一请求封装，负责登录、令牌头和统一错误处理。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
const { API_BASE_URL } = require('../config/env')
const { ensureLogin, resetLogin } = require('./auth')
const API_PREFIX = '/tabs/reef-journal'

/**
 * 发起已认证的小程序接口请求，遇到一次过期令牌时重新登录后重试。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 * @param {string} path 模块接口相对路径
 * @param {{method?: 'GET' | 'POST' | 'PUT' | 'DELETE', data?: object, contentType?: string}} options 请求方式和数据
 * @param {boolean} retryAfterLogin 是否允许过期后重试一次
 * @returns {Promise<any>} 服务端 data 数据
 */
function request(path, options = {}, retryAfterLogin = true) {
  if (!API_BASE_URL) {
    return Promise.reject(new Error('服务地址待补充，请先配置小程序 API 域名'))
  }

  return ensureLogin().then((token) => new Promise((resolve, reject) => {
    wx.request({
      url: `${API_BASE_URL}${API_PREFIX}${path}`,
      method: options.method || 'GET',
      data: options.data || {},
      header: {
        'content-type': options.contentType || 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      success(response) {
        const body = /** @type {{code?: number, message?: string, data?: any}} */ (/** @type {any} */ (response.data || {}))
        if (response.statusCode === 401 || body.code === 999) {
          wx.removeStorageSync('reefAccessToken')
          resetLogin()
          if (retryAfterLogin) {
            request(path, options, false).then(resolve).catch(reject)
            return
          }
          reject(new Error('登录状态已过期，请重新进入小程序'))
          return
        }
        if (response.statusCode < 200 || response.statusCode >= 300 || body.code !== 0) {
          reject(new Error(body.message || '暂时无法完成请求，请稍后重试'))
          return
        }
        resolve(body.data)
      },
      fail() {
        reject(new Error('网络暂时不可用，请检查连接后重试'))
      }
    })
  })).catch((error) => Promise.reject(error))
}

module.exports = { request }
