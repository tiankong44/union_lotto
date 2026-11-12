/**
 * 提供可搜索的分类选择面板，选项由所属页面传入。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-12
 */
Component({
  properties: {
    title: { type: String, value: '类型' },
    label: { type: String, value: '全部' },
    value: { type: String, value: '' },
    options: { type: Array, value: [] },
    searchPlaceholder: { type: String, value: '搜索类型' }
  },
  data: { open: false, keyword: '', visibleOptions: [] },
  methods: {
    /** 打开面板时同步所属页面提供的完整选项。 */
    openFilter() {
      this.setData({ open: true, keyword: '', visibleOptions: this.data.options || [] })
    },

    /** 关闭面板并清除临时搜索内容。 */
    closeFilter() {
      this.setData({ open: false, keyword: '', visibleOptions: this.data.options || [] })
    },

    /** 按选项名称搜索，筛选范围仅限当前选择面板。 */
    searchOptions(event) {
      const keyword = event.detail?.value ?? ''
      const normalizedKeyword = String(keyword).trim().toLowerCase()
      const visibleOptions = (this.data.options || []).filter((item) => String(item.label ?? '').toLowerCase().includes(normalizedKeyword))
      this.setData({ keyword, visibleOptions })
    },

    /** 清空搜索内容并恢复完整选项。 */
    clearSearch() {
      this.setData({ keyword: '', visibleOptions: this.data.options || [] })
    },

    /** 通知所属页面更新筛选值，由页面继续调用原有列表接口。 */
    selectOption(event) {
      const value = event.currentTarget.dataset.value || ''
      const selectedOption = (this.data.options || []).find((item) => item.value === value)
      if (!selectedOption) return
      this.triggerEvent('change', { value: selectedOption.value, label: selectedOption.label })
      this.closeFilter()
    }
  }
})
