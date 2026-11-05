/**
 * 海缸日记全局底部导航，供四个主工作区切换使用。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
Component({
  data: {
    selected: 0,
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
      // 使用微信原生 Tab 切换，保留系统返回栈约定。
      wx.switchTab({ url: item.path })
    }
  }
})
