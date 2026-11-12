/**
 * 新增鱼缸表单，保存用户填写的容量、系统类型、开缸日期和备注。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
const reefApi = require('../../services/reef-api')
const { shanghaiDateTimeParts } = require('../../services/date-time')
const { WATER_TYPES, THEMES, METRICS, LEGACY_METRICS, suggestedMetrics, splitSelection } = require('../../services/aquarium-catalog')

Page({
  data: {
    saving: false, loading: false, tankId: '',
    waterTypeLabels: WATER_TYPES.map((item) => item.label), waterTypeLabel: '待补充',
    themeOptions: THEMES.map((item) => ({ ...item, checked: false })),
    metricOptions: METRICS.map((item) => ({ ...item, checked: LEGACY_METRICS.includes(item.key) })),
    selectedThemes: [], selectedMetrics: LEGACY_METRICS,
    form: { name: '', capacityLiter: '', systemType: '', startedAt: '', note: '', waterType: '', archived: false }
  },

  onLoad(query) {
    // 新鱼缸默认使用当前上海本地日期。
    this.setData({ 'form.startedAt': shanghaiDateTimeParts(new Date()).date })
    this.setData({ tankId: query?.id ?? '' })
    if (query?.id) this.loadTank(query.id)
  },

  /** 读取已有鱼缸并保留未分类的历史资料。 @Author zhanghao_SMEICS @Date 2026-11-12 */
  async loadTank(id) {
    this.setData({ loading: true })
    try {
      const tank = await reefApi.getTank(id)
      const selectedThemes = splitSelection(tank?.themes)
      const selectedMetrics = tank?.enabledMetrics == null ? LEGACY_METRICS : splitSelection(tank.enabledMetrics)
      this.setData({
        form: { name: tank?.name ?? '', capacityLiter: tank?.capacityLiter ?? '', systemType: tank?.systemType ?? '',
          startedAt: tank?.startedAt ?? '', note: tank?.note ?? '', waterType: tank?.waterType ?? '', archived: Boolean(tank?.archived) },
        waterTypeLabel: WATER_TYPES.find((item) => item.value === tank?.waterType)?.label ?? '待补充'
      })
      this.setSelections(selectedThemes, selectedMetrics)
    } catch (error) {
      wx.showToast({ title: error?.message ?? '鱼缸资料暂时无法读取', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  },

  /** 水体、饲养主题与可测指标都由用户主动选择。 @Author zhanghao_SMEICS @Date 2026-11-12 */
  onWaterTypeChange(event) {
    const selected = WATER_TYPES[Number(event.detail.value)] ?? WATER_TYPES[0]
    this.setData({ 'form.waterType': selected.value, waterTypeLabel: selected.label })
  },
  onThemesChange(event) { this.setSelections(event.detail.value, this.data.selectedMetrics) },
  onMetricsChange(event) { this.setSelections(this.data.selectedThemes, event.detail.value) },
  onArchivedChange(event) { this.setData({ 'form.archived': Boolean(event.detail.value) }) },
  useSuggestedMetrics() { this.setSelections(this.data.selectedThemes, suggestedMetrics(this.data.form.waterType, this.data.selectedThemes)) },

  /** 将选中项映射到小程序可渲染的复选状态。 @Author zhanghao_SMEICS @Date 2026-11-12 */
  setSelections(themes, metrics) {
    this.setData({ selectedThemes: themes, selectedMetrics: metrics,
      themeOptions: THEMES.map((item) => ({ ...item, checked: themes.includes(item.value) })),
      metricOptions: METRICS.map((item) => ({ ...item, checked: metrics.includes(item.key) })) })
  },

  /**
   * 更新鱼缸表单中的文本字段。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-05
   */
  onFieldInput(event) {
    const field = event.currentTarget.dataset.field
    this.setData({ [`form.${field}`]: event.detail.value })
  },

  /**
   * 选择鱼缸开缸日期。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-05
   */
  onDateChange(event) {
    this.setData({ 'form.startedAt': event.detail.value })
  },

  /**
   * 校验后提交新鱼缸并返回首页。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-05
   */
  async saveTank() {
    const form = this.data.form
    if (!form.name.trim()) {
      wx.showToast({ title: '先填写鱼缸名称', icon: 'none' })
      return
    }
    if (this.data.saving) return
    this.setData({ saving: true })
    try {
      // 仅将用户填写的容量转换为数值，编号始终按字符串传递。
      await reefApi.saveTank({ ...form, id: this.data.tankId || undefined, capacityLiter: form.capacityLiter ? Number(form.capacityLiter) : null,
        themes: this.data.selectedThemes.join(','), enabledMetrics: this.data.selectedMetrics.join(',') })
      wx.showToast({ title: '鱼缸已保存', icon: 'success' })
      // 返回首页后由首页 onShow 刷新鱼缸列表。
      wx.navigateBack({ delta: 1 })
    } catch (error) {
      wx.showToast({ title: error?.message || '鱼缸资料没有保存，请检查网络后重试', icon: 'none' })
    } finally {
      this.setData({ saving: false })
    }
  },

  /**
   * 退出鱼缸表单并返回来源页面。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-05
   */
  cancel() {
    wx.navigateBack({ delta: 1 })
  }
})
