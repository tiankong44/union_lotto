/**
 * 设备管理场景，维护用户设备状态和逐设备维护周期。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
const reefApi = require('../../services/reef-api')

Page({
  data: {
    categories: [{ value: '', label: '全部' }, { value: 'LIGHT', label: '灯光' }, { value: 'FILTER', label: '过滤' }, { value: 'TEMPERATURE', label: '温控' }, { value: 'OTHER', label: '其他' }],
    equipmentCategories: [{ value: 'LIGHT', label: '灯光' }, { value: 'FILTER', label: '过滤' }, { value: 'TEMPERATURE', label: '温控' }, { value: 'OTHER', label: '其他' }],
    equipmentCategoryLabels: ['灯光', '过滤', '温控', '其他'],
    equipmentStatuses: [{ value: 'RUNNING', label: '运行中' }, { value: 'DIRTY', label: '待清洗' }, { value: 'STANDBY', label: '待机' }],
    equipmentStatusLabels: ['运行中', '待清洗', '待机'],
    selectedCategory: '', tankId: '', tanks: [], tankNames: [], equipment: [], formOpen: false, saving: false, pageNum: 0, hasMore: true, loading: false, loadVersion: 0,
    form: { id: '', name: '', model: '', category: 'LIGHT', categoryLabel: '灯光', status: 'RUNNING', statusLabel: '运行中', tankId: '', tankName: '', powerWatt: '', dailyHours: '', startedAt: '', maintenanceIntervalDays: '', lastMaintainedAt: '' }
  },

  onLoad(query) {
    this.setData({ tankId: query.tankId || '' })
    this.loadTanks()
  },
  onShow() { this.loadEquipment(true) },
  onReachBottom() { if (this.data.hasMore) this.loadEquipment(false) },

  async loadTanks() {
    try {
      const result = await reefApi.listTanks()
      const tanks = result?.list ?? result ?? []
      const selected = tanks.find((item) => item.id === this.data.tankId)
      this.setData({ tanks, tankNames: ['不绑定鱼缸', ...tanks.map((item) => item.name)], 'form.tankId': selected?.id || '', 'form.tankName': selected?.name || '' })
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' })
    }
  },

  async loadEquipment(reset = true) {
    if (!reset && (this.data.loading || !this.data.hasMore)) return
    const loadVersion = this.data.loadVersion + 1
    const pageNum = reset ? 1 : this.data.pageNum + 1
    this.setData({ loading: true, loadVersion })
    try {
      const result = await reefApi.listEquipment(this.data.selectedCategory, pageNum, 50, this.data.tankId)
      if (loadVersion !== this.data.loadVersion) return
      const incoming = result?.list ?? result ?? []
      this.setData({ equipment: reset ? incoming : this.data.equipment.concat(incoming), pageNum, hasMore: incoming.length === 50 })
    } catch (error) {
      if (loadVersion !== this.data.loadVersion) return
      wx.showToast({ title: error.message, icon: 'none' })
    } finally {
      if (loadVersion === this.data.loadVersion) this.setData({ loading: false })
    }
  },

  selectCategory(event) {
    this.setData({ selectedCategory: event.currentTarget.dataset.value })
    this.loadEquipment(true)
  },

  openForm() {
    // 新增设备使用独立表单页，管理列表只保留维护状态编辑。
    wx.navigateTo({ url: `/pages/equipment/add?tankId=${this.data.tankId}` })
  },
  closeForm() { this.setData({ formOpen: false }) },
  onFieldChange(event) { this.setData({ [`form.${event.currentTarget.dataset.field}`]: event.detail }) },

  onFormTankChange(event) {
    const selectedIndex = Number(event.detail.value)
    const tank = selectedIndex === 0 ? null : this.data.tanks[selectedIndex - 1]
    this.setData({ 'form.tankId': tank?.id || '', 'form.tankName': tank?.name || '' })
  },

  onCategoryChange(event) {
    const category = this.data.equipmentCategories[Number(event.detail.value)]
    this.setData({ 'form.category': category.value, 'form.categoryLabel': category.label })
  },

  onStatusChange(event) {
    const status = this.data.equipmentStatuses[Number(event.detail.value)]
    this.setData({ 'form.status': status.value, 'form.statusLabel': status.label })
  },

  editMaintenance(event) {
    const equipment = this.data.equipment.find((item) => item.id === event.currentTarget.dataset.id)
    if (!equipment) return
    const category = this.data.equipmentCategories.find((item) => item.value === equipment.category)
    const status = this.data.equipmentStatuses.find((item) => item.value === equipment.status)
    const tank = this.data.tanks.find((item) => item.id === equipment.tankId)
    this.setData({ formOpen: true, form: { ...equipment, categoryLabel: category?.label || '其他', statusLabel: status?.label || '待机', tankName: tank?.name || '' } })
  },

  async saveEquipment() {
    if (!this.data.form.name.trim()) {
      wx.showToast({ title: '请填写设备名称', icon: 'none' })
      return
    }
    this.setData({ saving: true })
    try {
      const form = this.data.form
      await reefApi.saveEquipment({
        id: form.id,
        tankId: form.tankId || null,
        category: form.category,
        name: form.name,
        model: form.model,
        status: form.status,
        powerWatt: Number(form.powerWatt) || null,
        dailyHours: Number(form.dailyHours) || null,
        startedAt: form.startedAt,
        maintenanceIntervalDays: Number(form.maintenanceIntervalDays) || null,
        lastMaintainedAt: form.lastMaintainedAt
      })
      const defaultTank = this.data.tanks.find((item) => item.id === this.data.tankId)
      const cleanForm = { id: '', name: '', model: '', category: 'LIGHT', categoryLabel: '灯光', status: 'RUNNING', statusLabel: '运行中', tankId: defaultTank?.id || '', tankName: defaultTank?.name || '', powerWatt: '', dailyHours: '', startedAt: '', maintenanceIntervalDays: '', lastMaintainedAt: '' }
      this.setData({ formOpen: false, form: cleanForm })
      await this.loadEquipment(true)
      wx.showToast({ title: '设备信息已保存', icon: 'success' })
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' })
    } finally {
      this.setData({ saving: false })
    }
  }
})
