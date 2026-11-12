/**
 * 分批导出和恢复水族记录，照片逐张读取并写入备份文件。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-12
 */
const reefApi = require('./reef-api')
const reefPhotoMedia = require('./reef-photo-media')
const fs = wx.getFileSystemManager()
const BATCH_SIZE = 25
const BACKUP_FORMAT = 'water-journal-backup'
const RESTORE_STAGES = ['tanks', 'biology', 'equipment', 'biologyEvents', 'logs', 'waterRanges']

/** 读取本机临时文件并返回指定编码的文本。 @Author zhanghao_SMEICS @Date 2026-11-12 */
function readText(filePath, encoding) {
  return new Promise((resolve, reject) => fs.readFile({ filePath, encoding,
    success: (result) => typeof result.data === 'string' ? resolve(result.data) : reject(new Error('备份文件内容无法读取')),
    fail: () => reject(new Error('备份文件无法读取，请重新选择')) }))
}

/**
 * 写入或追加备份文本，避免把所有照片同时装入内存。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-12
 * @param {string} filePath 本机备份路径
 * @param {string} data 待写入文本
 * @param {boolean} append 是否追加
 * @param {'utf8' | 'base64'} encoding 文本编码
 */
function writeText(filePath, data, append = false, encoding = 'utf8') {
  return new Promise((resolve, reject) => {
    const options = { filePath, data, encoding, success: () => resolve(undefined), fail: () => reject(new Error('备份文件写入失败，请检查小程序存储空间')) }
    if (append) fs.appendFile(options)
    else fs.writeFile(options)
  })
}

/** 下载当前用户有权访问的照片，读取服务端返回的图片类型。 @Author zhanghao_SMEICS @Date 2026-11-12 */
async function downloadMedia(url, label) {
  return new Promise((resolve, reject) => {
    let contentType = ''
    const task = wx.downloadFile({ url,
      success: (result) => result.statusCode === 200 ? resolve({ filePath: result.tempFilePath, contentType }) : reject(new Error(`${label}下载失败`)),
      fail: () => reject(new Error(`${label}下载中断，请检查网络`)) })
    if (task.onHeadersReceived) task.onHeadersReceived((result) => {
      const headers = result.header ?? {}
      contentType = String(headers['Content-Type'] ?? headers['content-type'] ?? '').split(';')[0]
    })
  })
}

/** 下载实况照片的静态画面和可选动效，保留服务端实际返回的媒体类型。 @Author zhanghao_SMEICS @Date 2026-11-12 */
async function downloadPhoto(id) {
  const photo = reefPhotoMedia.withMotionUrl(await reefApi.getPhoto(id))
  const image = await downloadMedia(photo.contentUrl, '备份照片')
  const contentType = image.contentType.startsWith('image/') ? image.contentType : await imageType(image.filePath)
  const content = await readText(image.filePath, 'base64')
  if (!photo.motionContentUrl) return { contentType, content }
  const motion = await downloadMedia(photo.motionContentUrl, '备份动态片段')
  if (!motion.contentType.startsWith('video/')) throw new Error('备份动态片段类型无法识别')
  return { contentType, content, motionContentType: motion.contentType, motionContent: await readText(motion.filePath, 'base64') }
}

/** 若下载响应未提供图片类型，使用微信图片信息识别。 @Author zhanghao_SMEICS @Date 2026-11-12 */
function imageType(filePath) {
  return new Promise((resolve, reject) => wx.getImageInfo({ src: filePath,
    success: (result) => result.type === 'unknown' ? reject(new Error('备份照片类型无法识别')) : resolve(`image/${result.type}`),
    fail: () => reject(new Error('备份照片类型无法识别')) }))
}

/**
 * 生成包含记录及原始照片的备份文件，照片逐张追加。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-12
 * @param {(message: string) => void} progress 页面进度回调
 * @returns {Promise<string>} 生成的本地备份路径
 */
async function exportBackup(progress) {
  progress('正在读取记录…')
  const data = await reefApi.exportData()
  const photos = data?.photos ?? []
  const filePath = `${wx.env.USER_DATA_PATH}/水境日记备份-${Date.now()}.json`
  try {
    await writeText(filePath, `{"format":"${BACKUP_FORMAT}","version":3,"data":${JSON.stringify(data)},"images":[`)
    for (let index = 0; index < photos.length; index += 1) {
      progress(`正在备份影像 ${index + 1}/${photos.length}…`)
      const downloaded = /** @type {{contentType: string, content: string, motionContentType?: string, motionContent?: string}} */ (await downloadPhoto(photos[index].id))
      await writeText(filePath, `${index ? ',' : ''}${JSON.stringify({ id: photos[index].id, ...downloaded })}`, true)
    }
    await writeText(filePath, ']}', true)
    return filePath
  } catch (error) {
    // 删除本次未完成的备份文件，避免被误认为完整备份。
    try { fs.unlinkSync(filePath) } catch (ignored) { /* 文件尚未创建时无需清理。 */ }
    throw error
  }
}

/** 选择一份用户主动指定的 JSON 备份。 @Author zhanghao_SMEICS @Date 2026-11-12 */
function chooseBackup() {
  return new Promise((resolve, reject) => wx.chooseMessageFile({ count: 1, type: 'file', extension: ['json'],
    success: (result) => result.tempFiles?.[0]?.path ? resolve(result.tempFiles[0].path) : reject(new Error('没有选中备份文件')),
    fail: () => reject(new Error('未选择备份文件')) }))
}

/** 校验备份完整性后再开始恢复，旧版缺少关联关系的导出不作为完整备份。 @Author zhanghao_SMEICS @Date 2026-11-12 */
function validateBackup(backup) {
  const data = backup?.data
  if (backup?.format !== BACKUP_FORMAT || ![2, 3].includes(backup?.version) || !data?.backupId || !Array.isArray(backup?.images)) {
    throw new Error('请选择新版水境日记完整备份文件')
  }
  for (const key of [...RESTORE_STAGES, 'photos']) if (!Array.isArray(data[key])) throw new Error('备份文件缺少记录，请重新导出')
  if (data.biologyEvents.some((item) => !item?.biologyId)) throw new Error('备份中的生物事件缺少所属生物')
  const imageIds = new Set(backup.images.map((item) => item?.id))
  if (imageIds.size !== backup.images.length || imageIds.size !== data.photos.length || data.photos.some((photo) => !imageIds.has(photo.id))) {
    throw new Error('备份中的照片不完整，请重新导出')
  }
  if (backup.images.some((item) => !item?.content || !String(item?.contentType ?? '').startsWith('image/'))) {
    throw new Error('备份中的照片内容无法读取')
  }
  if (backup.images.some((item) => {
    const hasMotionContent = typeof item?.motionContent === 'string'
    const hasMotionType = typeof item?.motionContentType === 'string'
    return hasMotionContent !== hasMotionType || (hasMotionContent && (!item.motionContent || !item.motionContentType.startsWith('video/')))
  })) throw new Error('备份中的动态片段不完整，请重新导出')
}

/**
 * 按依赖顺序分批恢复记录，再逐张恢复照片；重复执行可继续未完成的部分。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-12
 * @param {(message: string) => void} progress 页面进度回调
 */
async function restoreBackup(progress) {
  const filePath = /** @type {string} */ (await chooseBackup())
  progress('正在检查备份…')
  let backup
  try { backup = JSON.parse(/** @type {string} */ (await readText(filePath, 'utf8'))) }
  catch (error) { throw new Error('备份文件不是有效的 JSON') }
  validateBackup(backup)
  const data = backup.data
  const checkpointKey = `reef-restore-session:${data.backupId}`
  let restoreId = wx.getStorageSync(checkpointKey)
  if (!restoreId) {
    // 同一文件的未完成恢复使用同一个服务端随机编号，完成后可再次创建新副本。
    restoreId = await reefApi.createBackupSession()
    wx.setStorageSync(checkpointKey, restoreId)
  }
  // 先建立恢复批次，空备份或仅含未关联照片的备份也可恢复。
  await reefApi.restoreBackupBatch({ backupId: restoreId })
  for (const stage of RESTORE_STAGES) {
    const rows = data[stage]
    for (let offset = 0; offset < rows.length; offset += BATCH_SIZE) {
      progress(`正在恢复记录 ${Math.min(offset + BATCH_SIZE, rows.length)}/${rows.length}…`)
      await reefApi.restoreBackupBatch({ backupId: restoreId, [stage]: rows.slice(offset, offset + BATCH_SIZE) })
    }
  }
  const photosById = new Map(data.photos.map((photo) => [photo.id, photo]))
  for (let index = 0; index < backup.images.length; index += 1) {
    progress(`正在恢复照片 ${index + 1}/${backup.images.length}…`)
    const image = backup.images[index]
    const photo = photosById.get(image.id)
    const tempPath = `${wx.env.USER_DATA_PATH}/reef-restore-${Date.now()}-${index}.img`
    const motionTempPath = `${wx.env.USER_DATA_PATH}/reef-restore-${Date.now()}-${index}.mov`
    try {
      // 一次只解码一张照片，上传后删除本次临时文件。
      await writeText(tempPath, image.content, false, 'base64')
      const restoredPhotoId = await reefApi.restoreBackupPhoto(restoreId, tempPath, { originalId: image.id, category: photo.category,
        title: photo.title ?? '', referenceType: photo.referenceType ?? '', referenceId: photo.referenceId ?? '',
        recordedAt: photo.recordedAt, note: photo.note ?? '', contentType: image.contentType })
      if (image.motionContent) {
        // 动效通过受保护的照片接口绑定，重复恢复时服务端保持幂等。
        await writeText(motionTempPath, image.motionContent, false, 'base64')
        await reefPhotoMedia.uploadMotion(restoredPhotoId, motionTempPath)
      }
    } finally {
      try { fs.unlinkSync(tempPath) } catch (ignored) { /* 临时文件不存在时无需清理。 */ }
      try { fs.unlinkSync(motionTempPath) } catch (ignored) { /* 没有动态片段时无需清理。 */ }
    }
  }
  // 完整恢复后清除本机续传标记，用户下次可选择再次创建副本。
  wx.removeStorageSync(checkpointKey)
}

module.exports = { exportBackup, restoreBackup }
