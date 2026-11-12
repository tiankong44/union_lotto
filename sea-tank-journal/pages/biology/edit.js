/**
 * 生物与植物资料表单，支持个体、批次和状态维护。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-12
 */
const reefApi = require('../../services/reef-api')
const { shanghaiDateTimeParts } = require('../../services/date-time')
const { BIOLOGY_CATEGORIES, BIOLOGY_STATUSES } = require('../../services/aquarium-catalog')

Page({
  data: {
    biologyId: '', tankId: '', loading: false, saving: false,
    categories: BIOLOGY_CATEGORIES, categoryLabels: BIOLOGY_CATEGORIES.map((item) => item.label),
    statuses: BIOLOGY_STATUSES, statusLabels: BIOLOGY_STATUSES.map((item) => item.label),
    modeLabels: ['个体', '批次'],
    form: { name: '', scientificName: '', category: 'FISH', categoryLabel: '鱼', status: 'NORMAL', statusLabel: '正常',
      addedAt: '', bodyLengthCm: '', source: '', trackingMode: 'SINGLE', modeLabel: '个体', quantity: '1', positionNote: '', note: '' }
  },

  onLoad(query) {
    this.setData({ biologyId: query?.id ?? '', tankId: query?.tankId ?? '', 'form.addedAt': shanghaiDateTimeParts(new Date()).date })
    if (query?.id) this.loadBiology(query.id)
  },

  /** 回填已有档案，保留原生物编号和当前鱼缸。 @Author zhanghao_SMEICS @Date 2026-11-12 */
  async loadBiology(id) {
    this.setData({ loading: true })
    try {
      const biology = await reefApi.getBiology(id)
      this.setData({ tankId: biology?.tankId ?? '', form: {
        name: biology?.name ?? '', scientificName: biology?.scientificName ?? '', category: biology?.category ?? 'OTHER',
        categoryLabel: BIOLOGY_CATEGORIES.find((item) => item.value === biology?.category)?.label ?? '其他',
        status: biology?.status ?? 'NORMAL', statusLabel: BIOLOGY_STATUSES.find((item) => item.value === biology?.status)?.label ?? '正常',
        addedAt: biology?.addedAt ?? '', bodyLengthCm: biology?.bodyLengthCm ?? '', source: biology?.source ?? '',
        trackingMode: biology?.trackingMode ?? 'SINGLE', modeLabel: biology?.trackingMode === 'BATCH' ? '批次' : '个体',
        quantity: String(biology?.quantity ?? 1), positionNote: biology?.positionNote ?? '', note: biology?.note ?? ''
      } })
    } catch (error) {
      wx.showToast({ title: error?.message ?? '生物资料暂时无法读取', icon: 'none' })
    } finally { this.setData({ loading: false }) }
  },

  onFieldInput(event) { this.setData({ [`form.${event.currentTarget.dataset.field}`]: event.detail?.value ?? event.detail }) },
  onDateChange(event) { this.setData({ 'form.addedAt': event.detail.value }) },
  onCategoryChange(event) {
    const selected = BIOLOGY_CATEGORIES[Number(event.detail.value)]
    this.setData({ 'form.category': selected.value, 'form.categoryLabel': selected.label })
  },
  onStatusChange(event) {
    const selected = BIOLOGY_STATUSES[Number(event.detail.value)]
    this.setData({ 'form.status': selected.value, 'form.statusLabel': selected.label })
  },
  onModeChange(event) {
    const batch = Number(event.detail.value) === 1
    this.setData({ 'form.trackingMode': batch ? 'BATCH' : 'SINGLE', 'form.modeLabel': batch ? '批次' : '个体',
      'form.quantity': batch ? this.data.form.quantity : '1' })
  },

  /** 保存档案；失败时保留表单，状态变化由服务端追加事件。 @Author zhanghao_SMEICS @Date 2026-11-12 */
  async saveBiology() {
    if (this.data.saving || this.data.loading) return
    const form = this.data.form
    const quantity = Number(form.quantity)
    if (!form.name.trim() || !form.addedAt || !this.data.tankId) return wx.showToast({ title: '请填写名称和入缸日期', icon: 'none' })
    if (!Number.isInteger(quantity) || quantity < 0 || (form.trackingMode === 'SINGLE' && quantity > 1)) {
      return wx.showToast({ title: '请填写有效数量，个体数量为 0 或 1', icon: 'none' })
    }
    this.setData({ saving: true })
    try {
      // 提交当前账号的鱼缸及生物编号，编号始终使用字符串。
      await reefApi.saveBiology({ id: this.data.biologyId || undefined, tankId: this.data.tankId, name: form.name,
        scientificName: form.scientificName, category: form.category, status: form.status, addedAt: form.addedAt,
        bodyLengthCm: form.bodyLengthCm === '' ? null : Number(form.bodyLengthCm), source: form.source,
        trackingMode: form.trackingMode, quantity, positionNote: form.positionNote, note: form.note })
      wx.showToast({ title: '生物资料已保存', icon: 'success' })
      wx.navigateBack({ delta: 1 })
    } catch (error) {
      wx.showToast({ title: error?.message ?? '生物资料没有保存，请重试', icon: 'none' })
    } finally { this.setData({ saving: false }) }
  }
})
