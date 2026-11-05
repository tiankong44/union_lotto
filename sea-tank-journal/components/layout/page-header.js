/**
 * 页面统一标题栏组件，提供居中标题和返回动作。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
Component({
  properties: {
    title: { type: String, value: '' },
    showBack: { type: Boolean, value: false }
  },
  methods: {
    goBack() {
      wx.navigateBack({ delta: 1 })
    }
  }
})
