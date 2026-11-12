/**
 * 水境日记全局底部导航，供四个主工作区切换使用。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
const { startTabNavigation, logTabNavigation } = require('../services/tab-bar')

Component({
  data: {
    selected: 0,
    hidden: false,
    items: [
      { label: '首页', icon: 'home-o', path: '/pages/home/home' },
      { label: '日志', icon: 'records', path: '/pages/log/list' },
      { label: '影像', icon: 'photo-o', path: '/pages/photos/index' },
      { label: '我的', icon: 'manager-o', path: '/pages/settings/index' }
    ]
  },
  methods: {
    /**
     * 切换到用户选择的主工作区。
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     */
    switchTab(event) {
      const item = this.data.items[event.currentTarget.dataset.index]
      if (!item || Number(event.currentTarget.dataset.index) === this.data.selected) return
      // 记录点击时刻，以便和目标页面生命周期耗时对应。
      startTabNavigation(item.path)
      // 使用微信原生 Tab 切换，保留系统返回栈约定。
      wx.switchTab({
        url: item.path,
        success: () => logTabNavigation(item.path, 'switchTab-success'),
        fail: (error) => {
          logTabNavigation(item.path, 'switchTab-fail')
          console.warn('[tab-perf] switchTab error', error)
        }
      })
    }
  }
})
