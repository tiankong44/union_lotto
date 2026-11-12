/**
 * 新增鱼缸表单，估算容量并保存用户填写的鱼缸资料。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
const reefApi = require('../../services/reef-api')
const reefPhotoMedia = require('../../services/reef-photo-media')
const { shanghaiDateTimeParts } = require('../../services/date-time')
const { WATER_TYPES, THEMES, METRICS, LEGACY_METRICS, suggestedMetrics, splitSelection } = require('../../services/aquarium-catalog')
const { estimateRectangularTankVolume } = require('../../services/aquarium-volume')

Page({
  data: {
    saving: false, loading: false, tankId: '', imagePath: '', motionPath: '', pendingPhotoId: '',
    waterTypeLabels: WATER_TYPES.map((item) => item.label), waterTypeLabel: '待补充',
    themeOptions: THEMES.map((item) => ({ ...item, checked: false })),
    metricOptions: METRICS.map((item) => ({ ...item, checked: LEGACY_METRICS.includes(item.key) })),
    selectedThemes: [], selectedMetrics: LEGACY_METRICS,
    volumeForm: { length: '', width: '', height: '', waterGapCm: '2', substrateDepthCm: '4', displacementPercent: '10' },
    volumeEstimate: null,
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

  /** 接收通用实况照片选择器返回的静态画面或动态片段。 @Author zhanghao_SMEICS @Date 2026-11-12 */
  onPhotoInput(event) {
    const field = event.detail?.field
    const value = event.detail?.value ?? ''
    if (field === 'imagePath') {
      if (this.data.pendingPhotoId) return
      this.setData({ imagePath: value, motionPath: '', pendingPhotoId: '' })
      return
    }
    if (field === 'motionPath') this.setData({ motionPath: value })
  },

  /** 更新鱼缸尺寸和经验扣减输入。 @Author zhanghao_SMEICS @Date 2026-11-12 */
  onVolumeInput(event) {
    const field = event.currentTarget.dataset.field
    const volumeForm = { ...this.data.volumeForm, [field]: event.detail.value }
    // 每次修改尺寸或经验参数时，按最新输入实时刷新估算结果。
    const volumeEstimate = estimateRectangularTankVolume(volumeForm)
    this.setData({ volumeForm, volumeEstimate })
  },

  /** 将经验估算值回填到鱼缸容量字段，用户仍可继续修改。 @Author zhanghao_SMEICS @Date 2026-11-12 */
  useEstimatedVolume() {
    // 回填前按当前输入重新计算，避免容量字段使用过期估算值。
    const estimate = this.data.volumeEstimate || estimateRectangularTankVolume(this.data.volumeForm)
    if (!estimate) {
      wx.showToast({ title: '请填写有效尺寸和底砂参数', icon: 'none' })
      return
    }
    this.setData({ volumeEstimate: estimate })
    this.setData({ volumeEstimate: estimate, 'form.capacityLiter': String(estimate.estimatedLiters) })
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
    if (this.data.motionPath && !this.data.imagePath) {
      wx.showToast({ title: '先选择静态照片，再添加动态片段', icon: 'none' })
      return
    }
    this.setData({ saving: true })
    let tankSaved = false
    try {
      // 仅将用户填写的容量转换为数值，编号始终按字符串传递。
      const savedTank = await reefApi.saveTank({ ...form, id: this.data.tankId || undefined, capacityLiter: form.capacityLiter ? Number(form.capacityLiter) : null,
        themes: this.data.selectedThemes.join(','), enabledMetrics: this.data.selectedMetrics.join(',') })
      tankSaved = true
      const tankId = savedTank?.id ?? this.data.tankId
      if (this.data.imagePath && !tankId) throw new Error('鱼缸已保存，请重新打开编辑页后添加影像')
      this.setData({ tankId: tankId || '' })
      if (this.data.imagePath) {
        const now = shanghaiDateTimeParts(new Date())
        // 将静态照片及可选动效关联到已保存的鱼缸记录。
        await reefPhotoMedia.saveAttachedLivePhoto({ imagePath: this.data.imagePath, motionPath: this.data.motionPath,
          photoId: this.data.pendingPhotoId, metadata: { title: `${form.name} 实况照片`, category: 'TANK', referenceType: 'TANK',
            referenceId: tankId, recordedAt: `${now.date} ${now.time}`, note: '' },
          onPhotoCreated: (photoId) => this.setData({ pendingPhotoId: photoId }) })
      }
      this.setData({ imagePath: '', motionPath: '', pendingPhotoId: '' })
      wx.showToast({ title: '鱼缸已保存', icon: 'success' })
      // 返回首页后由首页 onShow 刷新鱼缸列表。
      wx.navigateBack({ delta: 1 })
    } catch (error) {
      wx.showToast({ title: tankSaved ? '鱼缸已保存，影像可重试' : error?.message || '鱼缸资料没有保存，请检查网络后重试', icon: 'none' })
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
