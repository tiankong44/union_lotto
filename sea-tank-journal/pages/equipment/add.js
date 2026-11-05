/**
 * 添加设备表单，维护设备分类、功率、安装日期和维护周期。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
const reefApi = require('../../services/reef-api')
const { shanghaiDateTimeParts } = require('../../services/date-time')

Page({
  data: {
    saving: false,
    tanks: [],
    tankNames: ['不绑定鱼缸'],
    tankPickerIndex: 0,
    categories: [
      { value: 'LIGHT', label: '灯光' },
      { value: 'FILTER', label: '过滤' },
      { value: 'TEMPERATURE', label: '温控' },
      { value: 'OTHER', label: '其他' }
    ],
    categoryLabels: ['灯光', '过滤', '温控', '其他'],
    form: { name: '', model: '', category: 'LIGHT', categoryLabel: '灯光', powerWatt: '', startedAt: '', maintenanceIntervalDays: '', tankId: '', tankName: '' }
  },

  onLoad(query) {
    // 新设备默认关联来源鱼缸并使用当前上海日期。
    this.setData({ 'form.tankId': query.tankId || '', 'form.startedAt': shanghaiDateTimeParts(new Date()).date })
    this.loadTanks()
  },

  /**
   * 读取鱼缸选项并回填已选关联项。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-05
   */
  async loadTanks() {
    try {
      const result = await reefApi.listTanks()
      const tanks = result?.list ?? result ?? []
      const selectedTank = tanks.find((tank) => tank.id === this.data.form.tankId)
      const tankPickerIndex = selectedTank ? tanks.findIndex((tank) => tank.id === selectedTank.id) + 1 : 0
      this.setData({ tanks, tankNames: ['不绑定鱼缸', ...tanks.map((tank) => tank.name)], tankPickerIndex, 'form.tankName': selectedTank?.name || '' })
    } catch (error) {
      wx.showToast({ title: error?.message || '鱼缸列表暂时无法读取', icon: 'none' })
    }
  },

  /**
   * 更新设备名称、型号、功率和维护周期文本。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-05
   */
  onFieldInput(event) {
    const field = event.currentTarget.dataset.field
    this.setData({ [`form.${field}`]: event.detail.value })
  },

  /**
   * 选择设备分类。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-05
   */
  onCategoryChange(event) {
    const category = this.data.categories[Number(event.detail.value)]
    if (category) this.setData({ 'form.category': category.value, 'form.categoryLabel': category.label })
  },

  /**
   * 选择关联鱼缸。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-05
   */
  onTankChange(event) {
    const index = Number(event.detail.value)
    const tank = index === 0 ? null : this.data.tanks[index - 1]
    this.setData({ tankPickerIndex: index, 'form.tankId': tank?.id || '', 'form.tankName': tank?.name || '' })
  },

  /**
   * 选择设备安装日期。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-05
   */
  onDateChange(event) {
    this.setData({ 'form.startedAt': event.detail.value })
  },

  /**
   * 校验后提交设备资料并返回设备列表。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-05
   */
  async saveEquipment() {
    const form = this.data.form
    if (!form.name.trim()) {
      wx.showToast({ title: '先填写设备名称', icon: 'none' })
      return
    }
    if (this.data.saving) return
    this.setData({ saving: true })
    try {
      // 设备编号与鱼缸编号保持字符串，数值字段只转换用户输入。
      await reefApi.saveEquipment({
        tankId: form.tankId || null,
        category: form.category,
        name: form.name.trim(),
        model: form.model.trim(),
        powerWatt: form.powerWatt ? Number(form.powerWatt) : null,
        startedAt: form.startedAt,
        maintenanceIntervalDays: form.maintenanceIntervalDays ? Number(form.maintenanceIntervalDays) : null
      })
      wx.showToast({ title: '设备已保存', icon: 'success' })
      // 返回设备管理页后由 onShow 刷新列表。
      wx.navigateBack({ delta: 1 })
    } catch (error) {
      wx.showToast({ title: error?.message || '设备资料没有保存，请检查网络后重试', icon: 'none' })
    } finally {
      this.setData({ saving: false })
    }
  },

  /**
   * 退出设备表单并返回来源页面。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-05
   */
  cancel() {
    wx.navigateBack({ delta: 1 })
  }
})
