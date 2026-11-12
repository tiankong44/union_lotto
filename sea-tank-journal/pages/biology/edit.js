/**
 * 生物与植物资料表单，支持个体、批次和状态维护。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-12
 */
const reefApi = require('../../services/reef-api')
const reefPhotoMedia = require('../../services/reef-photo-media')
const { shanghaiDateTimeParts } = require('../../services/date-time')
const { BIOLOGY_CATEGORIES, BIOLOGY_STATUSES } = require('../../services/aquarium-catalog')

Page({
  data: {
    biologyId: '', tankId: '', loading: false, saving: false, imagePath: '', motionPath: '', pendingPhotoId: '',
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
        quantity: biology?.trackingMode === 'SINGLE' ? '1' : String(biology?.quantity ?? 1),
        positionNote: biology?.positionNote ?? '', note: biology?.note ?? ''
      } })
    } catch (error) {
      wx.showToast({ title: error?.message ?? '生物资料暂时无法读取', icon: 'none' })
    } finally { this.setData({ loading: false }) }
  },

  /** 更新可编辑字段；个体数量固定为1，不接受控件事件覆盖。 @Author zhanghao_SMEICS @Date 2026-11-12 */
  onFieldInput(event) {
    const field = event.currentTarget.dataset.field
    if (field === 'quantity' && this.data.form.trackingMode === 'SINGLE') return
    this.setData({ [`form.${field}`]: event.detail?.value ?? event.detail })
  },
  onDateChange(event) { this.setData({ 'form.addedAt': event.detail.value }) },
  onCategoryChange(event) {
    const selected = BIOLOGY_CATEGORIES[Number(event.detail.value)]
    this.setData({ 'form.category': selected.value, 'form.categoryLabel': selected.label })
  },
  onStatusChange(event) {
    const selected = BIOLOGY_STATUSES[Number(event.detail.value)]
    this.setData({ 'form.status': selected.value, 'form.statusLabel': selected.label })
  },
  /** 切换个体或批次记录；个体始终保留一个档案单位。 @Author zhanghao_SMEICS @Date 2026-11-12 */
  onModeChange(event) {
    const batch = Number(event.detail.value) === 1
    this.setData({ 'form.trackingMode': batch ? 'BATCH' : 'SINGLE', 'form.modeLabel': batch ? '批次' : '个体',
      'form.quantity': batch ? this.data.form.quantity : '1' })
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

  /** 保存档案；失败时保留表单，状态变化由服务端追加事件。 @Author zhanghao_SMEICS @Date 2026-11-12 */
  async saveBiology() {
    if (this.data.saving || this.data.loading) return
    const form = this.data.form
    const quantity = form.trackingMode === 'SINGLE' ? 1 : Number(form.quantity)
    if (!form.name.trim() || !form.addedAt || !this.data.tankId) return wx.showToast({ title: '请填写名称和入缸日期', icon: 'none' })
    if (this.data.motionPath && !this.data.imagePath) return wx.showToast({ title: '先选择静态照片，再添加动态片段', icon: 'none' })
    if (!Number.isInteger(quantity) || quantity < 0) {
      return wx.showToast({ title: '请填写不小于0的整数数量', icon: 'none' })
    }
    this.setData({ saving: true })
    let biologySaved = false
    try {
      // 提交当前账号的鱼缸及生物编号，编号始终使用字符串。
      const savedBiology = await reefApi.saveBiology({ id: this.data.biologyId || undefined, tankId: this.data.tankId, name: form.name,
        scientificName: form.scientificName, category: form.category, status: form.status, addedAt: form.addedAt,
        bodyLengthCm: form.bodyLengthCm === '' ? null : Number(form.bodyLengthCm), source: form.source,
        trackingMode: form.trackingMode, quantity, positionNote: form.positionNote, note: form.note })
      biologySaved = true
      const biologyId = savedBiology?.id ?? this.data.biologyId
      if (this.data.imagePath && !biologyId) throw new Error('生物资料已保存，请重新打开编辑页后添加影像')
      this.setData({ biologyId: biologyId || '' })
      if (this.data.imagePath) {
        const now = shanghaiDateTimeParts(new Date())
        const photoCategory = ['FISH', 'CORAL', 'PLANT'].includes(form.category) ? form.category : 'OTHER'
        // 将静态照片和动态片段关联到已保存的生物档案。
        await reefPhotoMedia.saveAttachedLivePhoto({ imagePath: this.data.imagePath, motionPath: this.data.motionPath,
          photoId: this.data.pendingPhotoId, metadata: { title: `${form.name} 实况照片`, category: photoCategory, referenceType: 'BIOLOGY',
            referenceId: biologyId, recordedAt: `${now.date} ${now.time}`, note: '' },
          onPhotoCreated: (photoId) => this.setData({ pendingPhotoId: photoId }) })
      }
      this.setData({ imagePath: '', motionPath: '', pendingPhotoId: '' })
      wx.showToast({ title: '生物资料已保存', icon: 'success' })
      wx.navigateBack({ delta: 1 })
    } catch (error) {
      wx.showToast({ title: biologySaved ? '生物资料已保存，影像可重试' : error?.message ?? '生物资料没有保存，请重试', icon: 'none' })
    } finally { this.setData({ saving: false }) }
  }
})
