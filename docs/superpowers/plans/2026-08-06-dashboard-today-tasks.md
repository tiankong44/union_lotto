# 总览当天安排与待办跳转 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use inline execution in this session. Do not create a worktree or dispatch a subagent.

**Goal:** 让总览只显示本地当天的待处理事项，并让待办摘要与事项条目进入完整待办页面。

**Architecture:** 继续复用 Pinia store 的 `pendingTasks`，在 `DashboardView.vue` 内用用户本地日期派生并排序 `todayTasks`；总览入口统一使用现有 `/tasks` 路由。待办页面、接口、数据库和状态流转保持不变。

**Tech Stack:** Vue 3、TypeScript、Pinia、Vue Router、Vite。

---

### Task 1: 总览当天待办数据与跳转

**Files:**
- Modify: `pregnancy-assistant/src/views/DashboardView.vue`

- [ ] **Step 1: 增加本地日期判断和当天待办计算**

在 `pendingTasks` 之后增加以下局部逻辑，过滤无效时间、使用浏览器本地日期比较，并按计划时间升序排列：

```ts
const todayTasks = computed(() => store.pendingTasks
  .filter((task) => isSameLocalDate(task.plannedAt))
  .sort((a, b) => a.plannedAt.localeCompare(b.plannedAt)))

function isSameLocalDate(value: string): boolean {
  const date = new Date(value)
  const today = new Date()
  if (Number.isNaN(date.getTime())) return false
  return date.getFullYear() === today.getFullYear()
    && date.getMonth() === today.getMonth()
    && date.getDate() === today.getDate()
}
```

- [ ] **Step 2: 将总览待办摘要改为 `/tasks` 路由链接**

把待办指标的 `article` 改为 `RouterLink`，数量使用 `todayTasks.length`，保留现有指标卡样式并增加可聚焦的链接 class：

```vue
<RouterLink class="metric-card metric-card-accent metric-card-link" to="/tasks">
  <div class="metric-label"><span class="metric-dot coral"></span> 待办事项</div>
  <strong>{{ todayTasks.length }}</strong>
  <span class="metric-foot">{{ todayTasks.length ? '今天还有事项要处理' : '今天很轻盈' }}</span>
</RouterLink>
```

- [ ] **Step 3: 让当天安排使用同一数据并支持逐条跳转**

将“今天的安排”列表改为遍历 `todayTasks`，每行使用 `RouterLink` 指向 `/tasks`，空态继续保留现有添加入口；这样总览数量、列表和空态使用同一筛选口径。

- [ ] **Step 4: 静态检查总览引用**

运行 `rg -n "pendingTasks|todayTasks|to=\"/tasks\"" pregnancy-assistant/src/views/DashboardView.vue`，确认当天区域不再直接使用未筛选的 `pendingTasks`，且摘要和列表均存在 `/tasks` 入口。

### Task 2: 补充路由链接的可用状态样式

**Files:**
- Modify: `pregnancy-assistant/src/styles.css`

- [ ] **Step 1: 增加指标链接的交互和键盘焦点样式**

在 `.metric-card` 后增加以下样式，保留现有卡片尺寸，补充 hover 和 focus-visible 状态：

```css
.metric-card-link { color: inherit; transition: border-color 180ms ease, transform 180ms ease, box-shadow 180ms ease; }

.metric-card-link:hover { border-color: var(--coral); transform: translateY(-1px); box-shadow: var(--shadow); }

.metric-card-link:focus-visible { outline: 2px solid var(--coral); outline-offset: 3px; }
```

- [ ] **Step 2: 增加当天事项行的 hover 和焦点反馈**

给现有 `.timeline-row` 增加链接状态，确保整行是可点击目标且不改变网格布局：

```css
.timeline-row { color: inherit; text-decoration: none; transition: color 180ms ease; }

.timeline-row:hover .timeline-title { color: var(--coral); }

.timeline-row:focus-visible { outline: 2px solid var(--coral); outline-offset: 3px; }
```

- [ ] **Step 3: 检查响应式样式未被新增规则覆盖**

运行 `rg -n "metric-card-link|timeline-row" pregnancy-assistant/src/styles.css`，确认新增规则只增强交互，不修改已有移动端网格和固定尺寸规则。

### Task 3: 前端构建验证与提交

**Files:**
- Modify: `pregnancy-assistant/src/views/DashboardView.vue`
- Modify: `pregnancy-assistant/src/styles.css`

- [ ] **Step 1: 执行 TypeScript 检查**

运行：`npm run type-check`（工作目录：`pregnancy-assistant`）。

预期：命令退出码为 0，无 Vue 模板或 TypeScript 类型错误。

- [ ] **Step 2: 执行生产构建**

运行：`npm run build`（工作目录：`pregnancy-assistant`）。

预期：`vue-tsc` 与 Vite 构建均通过并生成 `dist`。

- [ ] **Step 3: 检查差异并提交实现**

运行：`git diff --check`，确认无空白错误；再运行 `git diff -- pregnancy-assistant/src/views/DashboardView.vue pregnancy-assistant/src/styles.css`，确认没有修改接口、待办页或数据模型；最后提交：

```bash
git add pregnancy-assistant/src/views/DashboardView.vue pregnancy-assistant/src/styles.css
git commit -m "feat: 串联总览当天待办入口"
```

