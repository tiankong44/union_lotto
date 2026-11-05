/**
 * 空状态组件，用页面标题和提示表达当前无记录的情况。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
Component({
  properties: {
    icon: { type: String, value: '◌' },
    title: { type: String, value: '暂时没有记录' },
    description: { type: String, value: '' }
  }
})
