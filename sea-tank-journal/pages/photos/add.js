/**
 * 添加或编辑影像记录，统一维护照片分类、关联对象和记录时间。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
const reefApi = require('../../services/reef-api')
const { shanghaiDateTimeParts } = require('../../services/date-time')

const CATEGORIES = [
  { value: 'TANK', label: '鱼缸' }, { value: 'FISH', label: '鱼' }, { value: 'CORAL', label: '珊瑚' },
  { value: 'EQUIPMENT', label: '设备' }, { value: 'PLANT', label: '植物' }, { value: 'OTHER', label: '其他生物' }
]
const REFERENCE_TYPES = [
  { value: '', label: '不关联' }, { value: 'TANK', label: '鱼缸' }, { value: 'BIOLOGY', label: '生物' },
  { value: 'EQUIPMENT', label: '设备' }, { value: 'LOG', label: '日志' }, { value: 'BIOLOGY_EVENT', label: '生物事件' }
]
const NEW_REFERENCE_TYPES = REFERENCE_TYPES.filter((item) => item.value !== 'BIOLOGY_EVENT')

Page({
  data: {
    photoId: '',
    editing: false,
    saving: false,
    photoPath: '',
    photoUrl: '',
    tanks: [],
    tankNames: [],
    tankPickerIndex: 0,
    referenceItems: [],
    referenceNames: [],
    referenceIndex: 0,
    categories: CATEGORIES,
    referenceTypes: NEW_REFERENCE_TYPES,
    referenceTypeLabels: NEW_REFERENCE_TYPES.map((item) => item.label),
    referenceTypeIndex: 0,
    recordDate: '',
    recordTime: '',
    form: { category: 'TANK', referenceType: '', referenceTypeLabel: '不关联', referenceId: '', referenceName: '', tankId: '', tankName: '', title: '', note: '' }
  },

  onLoad(query) {
    const now = shanghaiDateTimeParts(new Date())
    this.setData({
      photoId: query.id || '',
      editing: Boolean(query.id),
      referenceTypes: query.id ? REFERENCE_TYPES : NEW_REFERENCE_TYPES,
      referenceTypeLabels: (query.id ? REFERENCE_TYPES : NEW_REFERENCE_TYPES).map((item) => item.label),
      recordDate: now.date,
      recordTime: now.time.slice(0, 5)
    })
    // 先读取鱼缸选项，再回填照片关联对象。
    this.loadTanks().then(() => {
      if (query.id) this.loadExistingPhoto(query.id)
    })
  },

  /**
   * 读取当前用户鱼缸选项，供影像关联对象选择使用。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-05
   */
  async loadTanks() {
    try {
      const result = await reefApi.listTanks()
      const tanks = result?.list ?? result ?? []
      this.setData({ tanks, tankNames: tanks.map((item) => item.name) })
    } catch (error) {
      wx.showToast({ title: error?.message || '鱼缸列表暂时无法读取', icon: 'none' })
    }
  },

  /**
   * 读取已保存影像并回填编辑表单。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-05
   */
  async loadExistingPhoto(id) {
    try {
      const photo = await reefApi.getPhoto(id)
      const recordedAt = String(photo?.recordedAt || '').replace('T', ' ')
      const referenceTypes = this.data.referenceTypes
      const referenceTypeIndex = Math.max(referenceTypes.findIndex((item) => item.value === (photo?.referenceType || '')), 0)
      const biology = photo?.referenceType === 'BIOLOGY' && photo?.referenceId ? await reefApi.getBiology(photo.referenceId) : null
      const referenceType = photo?.referenceType || ''
      const selectedTankId = referenceType === 'TANK' ? photo?.referenceId : biology?.tankId || ''
      const selectedTank = this.data.tanks.find((tank) => tank.id === selectedTankId)
      const tankPickerIndex = selectedTank ? this.data.tanks.findIndex((tank) => tank.id === selectedTank.id) : 0
      this.setData({
        photoUrl: photo?.contentUrl || '',
        recordDate: recordedAt.slice(0, 10) || this.data.recordDate,
        recordTime: recordedAt.slice(11, 16) || this.data.recordTime,
        referenceTypeIndex,
        tankPickerIndex,
        'form.category': photo?.category || 'TANK',
        'form.referenceType': referenceType,
        'form.referenceTypeLabel': referenceTypes[referenceTypeIndex]?.label || '不关联',
        'form.referenceId': photo?.referenceId || '',
        'form.title': photo?.title || '',
        'form.note': photo?.note || '',
        'form.tankId': selectedTankId,
        'form.tankName': selectedTank?.name || ''
      })
      await this.loadReferenceItems(referenceType)
    } catch (error) {
      wx.showToast({ title: error?.message || '这张照片暂时无法读取', icon: 'none' })
    }
  },

  /**
   * 选择一张待上传图片。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-05
   */
  choosePhoto() {
    if (this.data.editing) return
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      success: (result) => {
        const file = result.tempFiles?.[0]
        if (file) this.setData({ photoPath: file.tempFilePath })
      }
    })
  },

  /**
   * 选择影像分类。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-05
   */
  selectCategory(event) {
    this.setData({ 'form.category': event.currentTarget.dataset.value })
  },

  /**
   * 选择关联对象类型并读取对应的当前用户记录。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-05
   */
  async onReferenceTypeChange(event) {
    const referenceTypeIndex = Number(event.detail.value)
    const referenceType = this.data.referenceTypes[referenceTypeIndex]?.value || ''
    this.setData({ referenceTypeIndex, referenceIndex: 0, 'form.referenceType': referenceType, 'form.referenceTypeLabel': this.data.referenceTypes[referenceTypeIndex]?.label || '不关联', 'form.referenceId': '', 'form.referenceName': '', referenceItems: [], referenceNames: [] })
    await this.loadReferenceItems(referenceType)
  },

  /**
   * 按关联类型读取可选对象，编号保持服务端返回的字符串。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-05
   */
  async loadReferenceItems(referenceType) {
    if (!referenceType) return
    // 编辑已有事件照片时保留原关联；事件照片通过生物时间轴添加。
    if (referenceType === 'BIOLOGY_EVENT') {
      this.setData({ 'form.referenceName': this.data.form.referenceId ? '当前关联的生物事件' : '' })
      return
    }
    try {
      let result = []
      if (referenceType === 'TANK') result = this.data.tanks
      if (referenceType === 'BIOLOGY') {
        if (!this.data.form.tankId) {
          wx.showToast({ title: '先选择鱼缸，再选择生物', icon: 'none' })
          return
        }
        result = await reefApi.listBiology(this.data.form.tankId, 1, 100)
      }
      if (referenceType === 'EQUIPMENT') result = await reefApi.listEquipment('', 1, 100)
      if (referenceType === 'LOG') result = await reefApi.listLogs({ pageNum: 1, pageSize: 100 })
      const items = result?.list ?? result ?? []
      const selected = items.find((item) => item.id === this.data.form.referenceId)
      const referenceIndex = selected ? items.findIndex((item) => item.id === selected.id) : 0
      this.setData({ referenceItems: items, referenceNames: items.map((item) => item.name || item.title || item.id), referenceIndex, 'form.referenceName': selected?.name || selected?.title || '' })
    } catch (error) {
      wx.showToast({ title: error?.message || '关联记录暂时无法读取', icon: 'none' })
    }
  },

  /**
   * 为生物关联选择所属鱼缸。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-05
   */
  onTankChange(event) {
    const tank = this.data.tanks[Number(event.detail.value)]
    if (!tank) return
    this.setData({ tankPickerIndex: Number(event.detail.value), 'form.tankId': tank.id, 'form.tankName': tank.name, 'form.referenceId': '', 'form.referenceName': '', referenceItems: [], referenceNames: [], referenceIndex: 0 })
    if (this.data.form.referenceType === 'BIOLOGY') this.loadReferenceItems('BIOLOGY')
  },

  /**
   * 选择具体的鱼缸、生物、设备或日志记录。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-05
   */
  onReferenceChange(event) {
    const item = this.data.referenceItems[Number(event.detail.value)]
    if (!item) return
    const changes = { referenceIndex: Number(event.detail.value), 'form.referenceId': item.id, 'form.referenceName': item.name || item.title || item.id }
    if (this.data.form.referenceType === 'TANK') {
      changes['form.tankId'] = item.id
      changes['form.tankName'] = item.name || ''
    }
    this.setData(changes)
  },

  /**
   * 更新影像记录日期。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-05
   */
  onDateChange(event) {
    this.setData({ recordDate: event.detail.value })
  },

  /**
   * 更新影像记录时刻。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-05
   */
  onTimeChange(event) {
    this.setData({ recordTime: event.detail.value })
  },

  /**
   * 更新影像备注。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-05
   */
  onNoteInput(event) {
    this.setData({ 'form.note': event.detail.value })
  },

  /**
   * 校验表单后上传新照片或保存已有影像资料。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-05
   */
  async savePhoto() {
    if (this.data.saving) return
    if (!this.data.editing && !this.data.photoPath) {
      wx.showToast({ title: '先选择一张照片', icon: 'none' })
      return
    }
    const referenceType = this.data.form.referenceType
    if (referenceType && !this.data.form.referenceId) {
      wx.showToast({ title: '请选择要关联的记录', icon: 'none' })
      return
    }
    this.setData({ saving: true })
    const recordedAt = `${this.data.recordDate} ${this.data.recordTime}:00`
    const payload = {
      category: this.data.form.category,
      title: this.data.form.title || '',
      referenceType: referenceType || '',
      referenceId: this.data.form.referenceId || '',
      recordedAt,
      note: this.data.form.note
    }
    try {
      if (this.data.editing) {
        // 更新影像元数据，不重新上传或替换已有照片文件。
        await reefApi.updatePhoto(this.data.photoId, payload)
      } else {
        // 将照片内容和用户填写的元数据一次提交到共享云端存储。
        await reefApi.uploadPhoto(this.data.photoPath, payload)
      }
      wx.showToast({ title: this.data.editing ? '照片资料已更新' : '照片已保存', icon: 'success' })
      wx.navigateBack({ delta: 1 })
    } catch (error) {
      wx.showToast({ title: error?.message || '照片资料没有保存，请检查网络后重试', icon: 'none' })
    } finally {
      this.setData({ saving: false })
    }
  },

  /**
   * 退出照片表单并返回来源页面。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-05
   */
  cancel() {
    wx.navigateBack({ delta: 1 })
  }
})
