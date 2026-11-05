/**
 * 首页鱼缸摘要卡片组件，展示用户鱼缸的开缸和维护摘要。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
Component({
  properties: {
    tank: { type: Object, value: {} }
  },
  methods: {
    openTank() {
      wx.navigateTo({ url: `/pages/tank/detail?id=${this.data.tank.id}` })
    }
  }
})
