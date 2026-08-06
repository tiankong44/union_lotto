# 固定 Workspace 视图切换设计

## 目标

将 `pregnancy-assistant` 调整为固定根地址 `/` 的单页 workspace。侧栏、移动端底栏和页面内部入口只切换内存中的页面视图，不通过路由改变浏览器地址栏。

## 设计

- 使用 `WorkspaceView` 联合类型描述全部页面视图：胎动、总览、记录中心、宫缩、历史记录、分析、孕期、待办和设置。
- `AppShell.vue` 作为唯一导航状态所有者，维护 `activeView` 并通过动态组件注册表渲染当前页面。
- 动态页面由 `<KeepAlive>` 包裹，保留计时器、未提交表单、图表范围和分类状态。
- 新增 `WorkspaceLink.vue`，统一承接所有应用内入口，接收目标视图并通过注入的导航函数切换状态。
- 启动时将非根旧地址用 `history.replaceState` 归一到 `/`；视图切换过程不再操作 History API。
- 删除 `vue-router` 运行时依赖、路由配置和所有 `RouterLink`/`RouterView` 使用，不改变云端接口、Pinia 业务数据和记录字段。

## 交互边界

- 默认视图为记录胎动。
- 所有主导航和页面内入口均保持地址栏不变。
- 浏览器后退/前进不再作为视图切换入口，这是固定 workspace 的明确取舍。
- 视图切换不触发额外云端刷新；现有云端加载、保存、错误和重试逻辑保持原样。

## 验收

- `npm run type-check` 和 `npm run build` 通过。
- 静态检索不存在残留路由组件、路由导航调用和 `.router-link-active` 依赖。
- 动态组件注册覆盖全部 9 个视图，并由 `<KeepAlive>` 缓存。
