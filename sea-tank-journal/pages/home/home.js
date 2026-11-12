/**
 * 首页场景编排器，加载用户鱼缸并提供新增鱼缸和快捷入口。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
const reefApi = require('../../services/reef-api')
const { shanghaiDateTimeParts } = require('../../services/date-time')
const { syncTabBar, setTabBarHidden } = require('../../services/tab-bar')
const { describeTank } = require('../../services/aquarium-catalog')

Page({
  data: {
    tanks: [],
    allTanks: [], showArchived: false, archivedCount: 0,
    greeting: '你好',
    loading: true,
    errorMessage: '',
    menuOpen: false
  },

  onShow() {
    // 将全局导航选中项同步到首页。
    syncTabBar(this, 0)
    // 离开表单返回首页时恢复底部导航，并关闭残留菜单。
    setTabBarHidden(this, false)
    this.setData({ menuOpen: false })
    const hour = Number(shanghaiDateTimeParts(new Date()).time.slice(0, 2))
    this.setData({ greeting: hour < 11 ? '早上好' : hour < 18 ? '下午好' : '晚上好' })
    this.loadDashboard()
  },

  async loadDashboard() {
    this.setData({ loading: true, errorMessage: '' })
    try {
      const result = await reefApi.getDashboard()
      const allTanks = (result?.tanks ?? []).map(describeTank)
      this.setData({ allTanks, tanks: allTanks.filter((tank) => Boolean(tank.archived) === this.data.showArchived),
        archivedCount: allTanks.filter((tank) => tank.archived).length })
    } catch (error) {
      this.setData({ allTanks: [], tanks: [], archivedCount: 0, errorMessage: error.message || '鱼缸信息暂时无法读取' })
    } finally {
      this.setData({ loading: false })
    }
  },

  /** 在使用中和已归档鱼缸之间切换，归档记录仍可打开恢复。 @Author zhanghao_SMEICS @Date 2026-11-12 */
  toggleArchived() {
    const showArchived = !this.data.showArchived
    this.setData({ showArchived, tanks: this.data.allTanks.filter((tank) => Boolean(tank.archived) === showArchived) })
  },

  toggleMenu() {
    const menuOpen = !this.data.menuOpen
    this.setData({ menuOpen })
    // 菜单展开时隐藏最上层的底部导航，避免遮住菜单项。
    setTabBarHidden(this, menuOpen)
  },

  closeMenu() {
    this.setData({ menuOpen: false })
    // 点击遮罩或关闭弹层后恢复底部导航。
    setTabBarHidden(this, false)
  },

  openTankForm() {
    this.setData({ menuOpen: false })
    // 跳转到独立鱼缸表单，保存后返回首页刷新列表。
    wx.navigateTo({
      url: '/pages/tank/add',
      fail: () => {
        setTabBarHidden(this, false)
        wx.showToast({ title: '鱼缸表单暂时无法打开', icon: 'none' })
      }
    })
  },

  openQuickLog() {
    this.setData({ menuOpen: false })
    wx.navigateTo({
      url: '/pages/log/add',
      fail: () => {
        setTabBarHidden(this, false)
        wx.showToast({ title: '日志表单暂时无法打开', icon: 'none' })
      }
    })
  },

})
