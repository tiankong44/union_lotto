/**
 * 新增鱼缸表单，保存用户填写的容量、系统类型、开缸日期和备注。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
const reefApi = require('../../services/reef-api')
const { shanghaiDateTimeParts } = require('../../services/date-time')

Page({
  data: {
    saving: false,
    form: { name: '', capacityLiter: '', systemType: '', startedAt: '', note: '' }
  },

  onLoad() {
    // 新鱼缸默认使用当前上海本地日期。
    this.setData({ 'form.startedAt': shanghaiDateTimeParts(new Date()).date })
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
      await reefApi.saveTank({ ...form, capacityLiter: form.capacityLiter ? Number(form.capacityLiter) : null })
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
