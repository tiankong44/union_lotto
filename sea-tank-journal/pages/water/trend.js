/**
 * 水质历史看板，展示六项参数概览和选中参数的完整趋势。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-09
 */
const reefApi = require('../../services/reef-api')

/** 按现有水质记录字段组织参数名称、单位和图表识别色。 */
const METRICS = [
  { key: 'salinity', label: '盐度', unit: '比重', accent: '#2f83b8' },
  { key: 'temperature', label: '温度', unit: '°C', accent: '#d8795b' },
  { key: 'ph', label: 'pH', unit: '', accent: '#62a47b' },
  { key: 'no2', label: 'NO₂', unit: 'mg/L', accent: '#a181b3' },
  { key: 'no3', label: 'NO₃', unit: 'mg/L', accent: '#c59535' },
  { key: 'po4', label: 'PO₄', unit: 'mg/L', accent: '#5f9994' }
]

/** 支持的趋势统计范围，零表示不限制历史起始日期。 */
const RANGES = [
  { days: 7, label: '7天' },
  { days: 30, label: '30天' },
  { days: 90, label: '90天' },
  { days: 0, label: '全部' }
]

/**
 * 将后端返回的历史抽样点整理为总览卡片中的柱形小图。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-09
 * @param {Array<{value?: string}>} points 后端均匀抽样的历史点
 * @returns {Array<{height: number}>} 归一化后的柱高
 */
function buildSparkline(points) {
  const values = (Array.isArray(points) ? points : [])
    .map((point) => Number(point?.value))
    .filter((value) => Number.isFinite(value))
  if (!values.length) return []

  const minimum = Math.min(...values)
  const spread = Math.max(...values) - minimum || 1
  return values.map((value) => ({
    height: Math.round(18 + ((value - minimum) / spread) * 82)
  }))
}

/**
 * 按固定参数清单映射后端概览，缺失参数保留为空数据卡片。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-09
 * @param {Array<object>} rawSeries 后端返回的参数汇总
 * @returns {Array<object>} 可直接展示的参数卡片
 */
function buildMetricCards(rawSeries) {
  const sourceSeries = Array.isArray(rawSeries) ? rawSeries : []
  return METRICS.map((metric) => {
    const source = sourceSeries.find((item) => item?.metric === metric.key)
    const stats = source?.stats ?? {}
    const samplePoints = Array.isArray(source?.samplePoints) ? source.samplePoints : []
    return {
      ...metric,
      currentValue: stats?.currentValue ?? '',
      average7Days: stats?.average7Days ?? '',
      trendKey: stats?.trendKey ?? '',
      trendLabel: stats?.trendLabel ?? '',
      sampleCount: source?.sampleCount ?? 0,
      sparkline: buildSparkline(samplePoints)
    }
  })
}

/**
 * 将接口时间格式转换为图表两端的紧凑标记。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-09
 * @param {string} value yyyy-MM-dd HH:mm:ss 格式的记录时间
 * @returns {string} MM-dd HH:mm 格式的图表标记
 */
function chartDateLabel(value) {
  return typeof value === 'string' && value.length >= 16 ? value.slice(5, 16) : ''
}

Page({
  data: {
    tanks: [],
    selectedTankId: '',
    selectedMetric: 'salinity',
    selectedDays: 30,
    currentMetric: METRICS[0],
    rangeDaysLabel: '30天',
    ranges: RANGES,
    series: [],
    points: [],
    selectedPointCount: 0,
    chartStartLabel: '',
    chartEndLabel: '',
    alerts: [],
    isTankLoading: true,
    isDashboardLoading: false,
    loadError: '',
    requestVersion: 0,
    pageClosed: false
  },

  /**
   * 页面进入时恢复鱼缸入口参数并加载可查看的鱼缸。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-09
   */
  onLoad(query) {
    this.data.requestVersion = 0
    this.data.pageClosed = false
    this.setData({ selectedTankId: query?.tankId ?? '' })
    // 先读取当前用户的鱼缸列表，再加载对应的历史趋势。
    this.loadTanks()
  },

  /**
   * 离开页面时使未完成请求的结果失效。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-09
   */
  onUnload() {
    this.data.pageClosed = true
    this.data.requestVersion += 1
  },

  /**
   * 读取用户鱼缸，并优先保留从鱼缸详情传入的有效鱼缸编号。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-09
   */
  async loadTanks() {
    this.setData({ isTankLoading: true, loadError: '' })
    try {
      const result = await reefApi.listTanks()
      if (this.data.pageClosed) return
      const tanks = Array.isArray(result?.list)
        ? result.list
        : Array.isArray(result)
          ? result
          : []
      const requestedTankId = this.data.selectedTankId
      const hasRequestedTank = tanks.some((tank) => tank?.id === requestedTankId)
      const selectedTankId = hasRequestedTank ? requestedTankId : tanks[0]?.id ?? ''
      this.setData({ tanks, selectedTankId, isTankLoading: false })
      if (selectedTankId) {
        // 鱼缸列表确认后读取对应参数的历史看板。
        this.loadDashboard()
      }
    } catch (error) {
      if (this.data.pageClosed) return
      this.setData({
        isTankLoading: false,
        loadError: error?.message ?? '鱼缸列表暂时无法读取，请稍后重试'
      })
    }
  },

  /**
   * 一次读取六项参数的摘要和当前指标完整点集，并忽略过期响应。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-09
   */
  async loadDashboard() {
    if (!this.data.selectedTankId) return
    const requestId = ++this.data.requestVersion
    this.setData({ isDashboardLoading: true, loadError: '' })
    try {
      // 使用聚合接口避免逐个参数请求历史记录。
      const result = await reefApi.getWaterDashboard(
        this.data.selectedTankId,
        this.data.selectedMetric,
        this.data.selectedDays
      )
      if (this.data.pageClosed || requestId !== this.data.requestVersion) return

      const series = buildMetricCards(result?.series ?? [])
      const currentMetric = METRICS.find((item) => item.key === this.data.selectedMetric) ?? METRICS[0]
      const selectedSeries = series.find((item) => item.key === currentMetric.key)
      const points = Array.isArray(result?.points) ? result.points : []
      this.setData({
        series,
        currentMetric,
        points,
        selectedPointCount: selectedSeries?.sampleCount ?? 0,
        chartStartLabel: chartDateLabel(points[0]?.recordedAt ?? ''),
        chartEndLabel: chartDateLabel(points[points.length - 1]?.recordedAt ?? ''),
        alerts: result?.alerts ?? [],
        isDashboardLoading: false
      }, () => {
        // 数据渲染完成后绘制当前参数的详细折线。
        this.drawChart(requestId)
      })
    } catch (error) {
      if (this.data.pageClosed || requestId !== this.data.requestVersion) return
      this.setData({
        series: [],
        points: [],
        selectedPointCount: 0,
        chartStartLabel: '',
        chartEndLabel: '',
        alerts: [],
        isDashboardLoading: false,
        loadError: error?.message ?? '水质趋势暂时无法读取，请稍后重试'
      })
    }
  },

  /**
   * 切换鱼缸后重新读取该鱼缸的历史参数。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-09
   */
  selectTank(event) {
    const selectedTankId = event.currentTarget.dataset.id
    if (!selectedTankId || selectedTankId === this.data.selectedTankId) return
    this.setData({ selectedTankId })
    // 重新按目标鱼缸加载范围内的水质趋势。
    this.loadDashboard()
  },

  /**
   * 切换参数后更新总览选中态，并读取该参数的详细采样点。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-09
   */
  selectMetric(event) {
    const selectedMetric = event.currentTarget.dataset.key
    if (!METRICS.some((item) => item.key === selectedMetric) || selectedMetric === this.data.selectedMetric) return
    this.setData({ selectedMetric })
    // 聚合接口返回所选参数完整点集，同时更新六项参数概览。
    this.loadDashboard()
  },

  /**
   * 切换日期范围后刷新六项参数及当前参数折线。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-09
   */
  selectRange(event) {
    const selectedDays = Number(event.currentTarget.dataset.days)
    const range = RANGES.find((item) => item.days === selectedDays)
    if (!range || selectedDays === this.data.selectedDays) return
    this.setData({ selectedDays, rangeDaysLabel: selectedDays ? `${selectedDays}天` : '全部记录' })
    // 让统计摘要、总览小图和详细折线使用同一时间范围。
    this.loadDashboard()
  },

  /**
   * 失败后按当前鱼缸状态重试对应的数据读取。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-09
   */
  retry() {
    if (this.data.tanks.length) {
      // 鱼缸列表已就绪时只重试趋势请求。
      this.loadDashboard()
    } else {
      // 鱼缸列表尚未读取成功时重新加载入口数据。
      this.loadTanks()
    }
  },

  /**
   * 从趋势空状态进入新增鱼缸表单。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-09
   */
  createTank() {
    // 帮助用户从空状态直接进入鱼缸资料录入。
    wx.navigateTo({ url: '/pages/tank/add' })
  },

  /**
   * 按当前历史点和参数色绘制详细折线，过期视图不覆盖新图。
   * @Author zhanghao_SMEICS
   * @Date 2026-11-09
   * @param {number} requestId 当前看板请求编号
   */
  drawChart(requestId) {
    const points = Array.isArray(this.data.points) ? this.data.points : []
    if (!points.length || requestId !== this.data.requestVersion) return

    const query = wx.createSelectorQuery().in(this)
    query.select('.chart-canvas').boundingClientRect((rect) => {
      if (!rect || this.data.pageClosed || requestId !== this.data.requestVersion) return
      const values = points
        .map((point) => Number(point?.value))
        .filter((value) => Number.isFinite(value))
      if (!values.length) return

      const context = wx.createCanvasContext('trendChart', this)
      const width = rect.width
      const height = rect.height
      const plotLeft = 12
      const plotRight = width - 12
      const plotTop = 16
      const plotBottom = height - 18
      const plotHeight = plotBottom - plotTop
      const minimum = Math.min(...values)
      const spread = Math.max(...values) - minimum || 1

      context.setStrokeStyle('#e9eff2')
      context.setLineWidth(1)
      for (let line = 0; line < 3; line += 1) {
        const y = plotTop + (plotHeight * line) / 2
        context.beginPath()
        context.moveTo(plotLeft, y)
        context.lineTo(plotRight, y)
        context.stroke()
      }

      context.setStrokeStyle(this.data.currentMetric.accent)
      context.setLineWidth(2)
      context.beginPath()
      values.forEach((value, index) => {
        const x = values.length === 1
          ? width / 2
          : plotLeft + ((plotRight - plotLeft) * index) / (values.length - 1)
        const y = plotBottom - ((value - minimum) / spread) * plotHeight
        if (index === 0) context.moveTo(x, y)
        else context.lineTo(x, y)
      })
      context.stroke()
      values.forEach((value, index) => {
        const x = values.length === 1
          ? width / 2
          : plotLeft + ((plotRight - plotLeft) * index) / (values.length - 1)
        const y = plotBottom - ((value - minimum) / spread) * plotHeight
        context.beginPath()
        context.setFillStyle(this.data.currentMetric.accent)
        context.arc(x, y, 3, 0, Math.PI * 2)
        context.fill()
      })
      // 将折线和参考线一次提交到当前 Canvas。
      context.draw()
    }).exec()
  }
})
