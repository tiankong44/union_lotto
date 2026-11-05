/**
 * 同步微信自定义底部导航的当前选中项。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
function syncTabBar(page, selected) {
  // 页面未启用自定义 Tab 栏时不处理导航状态。
  if (typeof page.getTabBar !== 'function') return
  const tabBar = page.getTabBar()
  if (tabBar) tabBar.setData({ selected })
}

module.exports = { syncTabBar }
