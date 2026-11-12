/**
 * 水族档案和水质记录使用的选项目录，不包含水质阈值。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-12
 */
const WATER_TYPES = [
  { value: '', label: '待补充' }, { value: 'FRESH', label: '淡水' },
  { value: 'MARINE', label: '海水' }, { value: 'BRACKISH', label: '汽水' }, { value: 'OTHER', label: '其他' }
]
const THEMES = [
  { value: 'FISH', label: '观赏鱼' }, { value: 'PLANTED', label: '水草' }, { value: 'CORAL', label: '珊瑚' },
  { value: 'SHRIMP', label: '虾缸' }, { value: 'QUARANTINE', label: '检疫' },
  { value: 'BREEDING', label: '繁殖' }, { value: 'OTHER', label: '其他' }
]
const METRICS = [
  { key: 'salinity', label: '盐度（比重）', unit: '比重', accent: '#2f83b8' },
  { key: 'salinity_ppt', label: '盐度', unit: '‰', accent: '#2f83b8' },
  { key: 'temperature', label: '水温', unit: '℃', accent: '#d8795b' },
  { key: 'ph', label: 'pH', unit: '', accent: '#62a47b' },
  { key: 'no2', label: 'NO₂', unit: 'mg/L', accent: '#a181b3' },
  { key: 'no3', label: 'NO₃', unit: 'mg/L', accent: '#c59535' },
  { key: 'po4', label: 'PO₄', unit: 'mg/L', accent: '#5f9994' },
  { key: 'total_ammonia_n', label: '总氨氮', unit: 'mg/L N', accent: '#b67e69' },
  { key: 'free_ammonia', label: '游离氨', unit: 'mg/L NH₃', accent: '#b67e69' },
  { key: 'gh', label: '总硬度 GH', unit: '°dGH', accent: '#7c9c82' },
  { key: 'kh', label: '碱度 KH', unit: '°dKH', accent: '#78949a' },
  { key: 'oxygen', label: '溶氧', unit: 'mg/L', accent: '#608cb7' },
  { key: 'tds', label: 'TDS', unit: 'mg/L', accent: '#8a9298' },
  { key: 'co2', label: '二氧化碳', unit: 'mg/L', accent: '#6d9c88' },
  { key: 'iron', label: '铁', unit: 'mg/L', accent: '#b88a76' },
  { key: 'potassium', label: '钾', unit: 'mg/L', accent: '#9c84a9' },
  { key: 'calcium', label: '钙', unit: 'mg/L', accent: '#72a7a7' },
  { key: 'magnesium', label: '镁', unit: 'mg/L', accent: '#a49a6b' }
]
const LEGACY_METRICS = ['salinity', 'temperature', 'ph', 'no2', 'no3', 'po4']
const BIOLOGY_CATEGORIES = [
  { value: 'FISH', label: '鱼' }, { value: 'CORAL', label: '珊瑚' }, { value: 'PLANT', label: '植物' },
  { value: 'SHRIMP', label: '虾' }, { value: 'SNAIL', label: '螺' }, { value: 'CRAB', label: '蟹' }, { value: 'OTHER', label: '其他' }
]
const BIOLOGY_STATUSES = [
  { value: 'NORMAL', label: '正常' }, { value: 'QUARANTINE', label: '检疫中' },
  { value: 'ISOLATED', label: '隔离中' }, { value: 'SICK', label: '生病' }, { value: 'DEAD', label: '死亡' },
  { value: 'OBSERVING', label: '待观察' }, { value: 'GROWING', label: '生长中' },
  { value: 'POOR', label: '状态欠佳' }, { value: 'CLOSED', label: '收缩' }, { value: 'FADED', label: '颜色变浅' }
]
const EVENT_TYPES = [
  { value: 'OBSERVATION', label: '观察' }, { value: 'STATUS', label: '状态变化' },
  { value: 'TRANSFER', label: '转缸' }, { value: 'GROWTH', label: '生长' },
  { value: 'BREEDING', label: '繁殖' }, { value: 'PRUNING', label: '修剪' },
  { value: 'LOSS', label: '损失' }, { value: 'OTHER', label: '其他' }
]
const LOG_TYPES = [
  { value: 'WATER_TEST', label: '水质检测' }, { value: 'WATER_CHANGE', label: '换水' },
  { value: 'FEEDING', label: '喂食' }, { value: 'MEDICATION', label: '用药' },
  { value: 'OBSERVATION', label: '观察' }, { value: 'FERTILIZING', label: '施肥' },
  { value: 'SUPPLEMENT', label: '补剂' }, { value: 'PRUNING', label: '修剪' },
  { value: 'TOP_OFF', label: '补水' }, { value: 'MAINTENANCE', label: '设备维护' },
  { value: 'LIGHTING', label: '光照记录' }, { value: 'SALT_MIX', label: '海水配制' }
]

/**
 * 按用户选定的水体和主题给出可调整的指标清单，不设置数值范围。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-12
 * @param {string} waterType 水体编码
 * @param {string[]} themes 饲养主题
 * @returns {string[]} 建议选择的指标编码
 */
function suggestedMetrics(waterType, themes) {
  const selected = new Set(['temperature', 'ph', 'no2', 'no3', 'total_ammonia_n'])
  if (waterType === 'FRESH' || waterType === 'BRACKISH') ['gh', 'kh', 'oxygen'].forEach((key) => selected.add(key))
  if (waterType === 'MARINE' || waterType === 'BRACKISH') selected.add('salinity')
  if (themes.includes('PLANTED')) ['po4', 'co2', 'iron', 'potassium'].forEach((key) => selected.add(key))
  if (themes.includes('CORAL')) ['po4', 'kh', 'calcium', 'magnesium'].forEach((key) => selected.add(key))
  return METRICS.filter((item) => selected.has(item.key)).map((item) => item.key)
}

/**
 * 将逗号分隔的用户选择还原为编码列表。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-12
 * @param {string | null | undefined} value 已保存的编码
 * @returns {string[]} 已选编码
 */
function splitSelection(value) {
  return typeof value === 'string' && value ? value.split(',').filter(Boolean) : []
}

/**
 * 将鱼缸编码转换为面向用户的类型和主题标签。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-12
 * @param {object} tank 鱼缸资料
 * @returns {object} 用于首页与详情显示的鱼缸资料
 */
function describeTank(tank) {
  const waterType = WATER_TYPES.find((item) => item.value === tank?.waterType)
  const themes = splitSelection(tank?.themes).map((code) => THEMES.find((item) => item.value === code)?.label ?? code)
  return { ...tank, waterTypeLabel: waterType?.label ?? '待补充', themeLabels: themes, tankTypeSummary: [waterType?.label ?? '待补充', ...themes].filter(Boolean).join(' · ') }
}

module.exports = { WATER_TYPES, THEMES, METRICS, LEGACY_METRICS, BIOLOGY_CATEGORIES, BIOLOGY_STATUSES, EVENT_TYPES, LOG_TYPES, suggestedMetrics, splitSelection, describeTank }
