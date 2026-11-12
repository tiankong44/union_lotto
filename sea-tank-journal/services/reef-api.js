/**
 * 水境日记前端业务 API，统一对接后端模块端点。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
const { request } = require('./request')
const { API_BASE_URL } = require('../config/env')

/** @param {'POST' | 'PUT'} method @param {object} data 请求方式和 DTO */
const json = (method, data) => ({ method, data })

// 按业务页面导出接口操作，页面不直接拼装服务端路径。
module.exports = {
  getDashboard: () => request('/dashboard'),
  listTanks: (pageNum = 1, pageSize = 20) => request(`/tanks?pageNum=${pageNum}&pageSize=${pageSize}`),
  saveTank: (data) => request('/tanks', json('POST', data)),
  getTank: (id) => request(`/tanks/${id}`),
  saveWaterRanges: (id, data) => request(`/tanks/${id}/water-ranges`, json('PUT', data)),
  listBiology: (tankId, pageNum = 1, pageSize = 50) => request(`/biology?tankId=${tankId}&pageNum=${pageNum}&pageSize=${pageSize}`),
  saveBiology: (data) => request('/biology', json('POST', data)),
  getBiology: (id) => request(`/biology/${id}`),
  listBiologyEvents: (biologyId) => request(`/biology/${biologyId}/events`),
  saveBiologyEvent: (biologyId, data) => request(`/biology/${biologyId}/events`, json('POST', data)),
  createBiologyEvent: (biologyId, data) => request(`/biology/${biologyId}/events/entry`, json('POST', data)),
  listLogs: (filters = {}) => request(`/logs?pageNum=${filters.pageNum || 1}&pageSize=${filters.pageSize || 50}&type=${encodeURIComponent(filters.type || '')}&tankId=${encodeURIComponent(filters.tankId || '')}`),
  saveLog: (data) => request('/logs', json('POST', data)),
  getLog: (id) => request(`/logs/${id}`),
  deleteLog: (id) => request(`/logs/${id}`, { method: 'DELETE' }),
  /**
   * 查询按鱼缸和日期范围整理的水质历史总览及当前参数趋势。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-09
   */
  getWaterDashboard: (tankId, metric, days) => request(`/water/dashboard?tankId=${encodeURIComponent(tankId)}&metric=${encodeURIComponent(metric)}&days=${days}`),
  listEquipment: (category = '', pageNum = 1, pageSize = 50, tankId = '') => request(`/equipment?pageNum=${pageNum}&pageSize=${pageSize}&category=${encodeURIComponent(category)}&tankId=${encodeURIComponent(tankId)}`),
  saveEquipment: (data) => request('/equipment', json('POST', data)),
  getEquipment: (id) => request(`/equipment/${id}`),
  listPhotos: async (category = '', pageNum = 1, pageSize = 60, referenceType = '', referenceId = '') => {
    const result = await request(`/photos?pageNum=${pageNum}&pageSize=${pageSize}&category=${encodeURIComponent(category)}&referenceType=${encodeURIComponent(referenceType)}&referenceId=${encodeURIComponent(referenceId)}`)
    const withContentUrls = (items) => (items ?? []).map((item) => ({ ...item, contentUrl: `${API_BASE_URL}${item.contentUrl}` }))
    return Array.isArray(result) ? withContentUrls(result) : { ...result, list: withContentUrls(result?.list) }
  },
  getPhoto: async (id) => {
    const photo = await request(`/photos/${id}`)
    return photo ? { ...photo, contentUrl: `${API_BASE_URL}${photo.contentUrl}` } : null
  },
  updatePhoto: (id, data) => request(`/photos/${id}`, json('PUT', data)),
  deletePhoto: (id) => request(`/photos/${id}`, { method: 'DELETE' }),
  uploadPhoto: async (filePath, metadata) => {
    const { API_BASE_URL } = require('../config/env')
    const { ensureLogin } = require('./auth')
    const token = await ensureLogin()
    return new Promise((resolve, reject) => wx.uploadFile({
      url: `${API_BASE_URL}/tabs/reef-journal/photos`,
      filePath,
      name: 'file',
      formData: metadata,
      header: token ? { Authorization: `Bearer ${token}` } : {},
      success(response) {
        try {
          const body = JSON.parse(response.data || '{}')
          if (response.statusCode < 200 || response.statusCode >= 300 || body.code !== 0) {
            reject(new Error(body.message || '照片暂时没有保存，请重试'))
            return
          }
          resolve(body.data)
        } catch (error) {
          reject(new Error('照片返回内容无法读取，请重试'))
        }
      },
      fail() {
        reject(new Error('照片上传中断，请检查网络后重试'))
      }
    }))
  },
  getReminders: () => request('/reminders'),
  saveReminders: (data) => request('/reminders', json('PUT', data)),
  saveSubscriptionConsent: (data) => request('/reminders/consent', json('POST', data)),
  exportData: () => request('/export'),
  createBackupSession: () => request('/backup/session', { method: 'POST' }),
  restoreBackupBatch: (data) => request('/backup/restore', json('POST', data)),
  restoreBackupPhoto: async (backupId, filePath, metadata) => {
    const { ensureLogin } = require('./auth')
    const token = await ensureLogin()
    return new Promise((resolve, reject) => wx.uploadFile({
      url: `${API_BASE_URL}/tabs/reef-journal/backup/${encodeURIComponent(backupId)}/photos`, filePath, name: 'file', formData: metadata,
      header: token ? { Authorization: `Bearer ${token}` } : {},
      success(response) {
        try {
          const body = JSON.parse(response.data || '{}')
          if (response.statusCode < 200 || response.statusCode >= 300 || body.code !== 0) return reject(new Error(body.message || '备份照片没有恢复'))
          resolve(body.data)
        } catch (error) { reject(new Error('备份照片恢复结果无法读取')) }
      },
      fail() { reject(new Error('备份照片上传中断，请检查网络后重试')) }
    }))
  }
}
