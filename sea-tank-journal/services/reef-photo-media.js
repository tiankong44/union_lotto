/**
 * 上传实况照片动效并转换照片接口返回的动态片段地址。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-12
 */
const reefApi = require('./reef-api')
const { API_BASE_URL } = require('../config/env')
const { ensureLogin } = require('./auth')

/**
 * 将动态片段上传到已保存的静态照片记录。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-12
 * @param {string} photoId 照片字符串编号
 * @param {string} filePath 小程序临时视频路径
 * @returns {Promise<object>} 更新后的照片资料
 */
async function uploadMotion(photoId, filePath) {
  let token
  try {
    token = await ensureLogin()
  } catch (error) {
    throw new Error(error?.message || '登录状态已失效，请重新进入小程序')
  }
  return new Promise((resolve, reject) => {
    // 将动态片段发送到对应照片的受保护上传接口。
    wx.uploadFile({
      url: `${API_BASE_URL}/tabs/reef-journal/photos/${encodeURIComponent(photoId)}/motion`,
      filePath,
      name: 'file',
      header: token ? { Authorization: `Bearer ${token}` } : {},
      success(response) {
        try {
          const body = JSON.parse(response.data || '{}')
          if (response.statusCode < 200 || response.statusCode >= 300 || body.code !== 0) {
            reject(new Error(body.message || '动态片段没有保存，请重试'))
            return
          }
          resolve(body.data)
        } catch (error) {
          reject(new Error('动态片段保存结果无法读取，请重试'))
        }
      },
      fail() { reject(new Error('动态片段上传中断，请检查网络后重试')) }
    })
  })
}

/**
 * 先上传静态画面，再按需上传动态片段；回调让表单能在失败后安全重试。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-12
 * @param {{imagePath:string, motionPath:string, photoId:string, metadata:object, onPhotoCreated:(id:string)=>void}} options 影像文件与关联资料
 * @returns {Promise<string>} 已保存的照片字符串编号
 */
async function saveAttachedLivePhoto(options) {
  if (!options.imagePath) return ''
  let photoId = options.photoId || ''
  if (!photoId) {
    const photo = await reefApi.uploadPhoto(options.imagePath, options.metadata)
    if (typeof photo?.id !== 'string' || !photo.id) throw new Error('静态照片已上传，但没有取得影像编号')
    photoId = photo.id
    // 保存照片编号后再上传动态片段，网络中断时不会重复创建静态记录。
    options.onPhotoCreated?.(photoId)
  }
  if (options.motionPath) await uploadMotion(photoId, options.motionPath)
  return photoId
}

/**
 * 将接口返回的相对动态片段地址转换为小程序可请求的完整地址。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-12
 * @param {object|null} photo 照片接口返回项
 * @returns {object|null} 动态片段地址已补全的照片资料
 */
function withMotionUrl(photo) {
  if (!photo) return null
  const url = photo.motionContentUrl || ''
  return { ...photo, motionContentUrl: url && !/^https?:\/\//i.test(url) ? `${API_BASE_URL}${url}` : url }
}

module.exports = { uploadMotion, saveAttachedLivePhoto, withMotionUrl }
