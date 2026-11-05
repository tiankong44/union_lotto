/**
 * 将设备绝对时间转换为项目约定的上海本地日期时间片段。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
const SHANGHAI_OFFSET_MILLISECONDS = 8 * 60 * 60 * 1000

/**
 * 返回上海本地日期和时分秒，用于日志录入和照片分组。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 * @param {Date} date 设备当前时间
 * @returns {{date: string, time: string}} 上海本地时间文本
 */
function shanghaiDateTimeParts(date) {
  const shifted = new Date(date.getTime() + SHANGHAI_OFFSET_MILLISECONDS)
  const year = shifted.getUTCFullYear()
  const month = String(shifted.getUTCMonth() + 1).padStart(2, '0')
  const day = String(shifted.getUTCDate()).padStart(2, '0')
  const hours = String(shifted.getUTCHours()).padStart(2, '0')
  const minutes = String(shifted.getUTCMinutes()).padStart(2, '0')
  const seconds = String(shifted.getUTCSeconds()).padStart(2, '0')
  return { date: `${year}-${month}-${day}`, time: `${hours}:${minutes}:${seconds}` }
}

module.exports = { shanghaiDateTimeParts }
