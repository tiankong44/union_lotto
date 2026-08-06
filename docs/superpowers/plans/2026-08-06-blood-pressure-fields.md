# 血压双输入框 Implementation Plan

> **For agentic workers:** 本计划在当前仓库内由主会话逐项执行；不创建 worktree、不使用子 Agent、不编写单元测试。

**Goal:** 在记录中心使用高压、低压两个输入框录入血压，并将结构化值正确保存和展示。

**Architecture:** 复用现有 Vue 表单、Pinia 健康记录保存方法和 `valueJson` 字段。记录中心负责两个输入值的局部状态、校验和序列化；历史页与时间线各自负责将结构化血压值转换为可读文本，后端和数据库保持不变。

**Tech Stack:** Vue 3、TypeScript、Vite、Pinia、现有 CSS 和 Spring Boot 健康记录接口。

---

### Task 1: 更新记录中心血压表单

**Files:**
- Modify: `pregnancy-assistant/src/views/RecordCenterView.vue`

- [x] **Step 1: 扩展表单状态**

在现有 `HealthFormState` 中增加 `systolic` 和 `diastolic`，类型使用 `string | number` 以兼容数字输入控件的 `v-model` 结果；初始化和重置时都设为空字符串。

- [x] **Step 2: 调整血压分支校验和序列化**

血压模式分别归一化高压、低压文本，校验两项非空且为可解析非负数；保存时提交 `JSON.stringify({ systolic, diastolic })`。体重和症状仍提交 `JSON.stringify({ value })`，保存失败继续保留表单。

- [x] **Step 3: 调整模板输入框**

血压模式渲染两个 `type="number"` 输入框，标签分别为“高压”和“低压”，设置 `min="0"`、`step="1"`、必填和数字占位提示；其他记录类型继续使用原单值输入框，记录时间字段继续复用 `DateField`。

### Task 2: 更新健康记录展示解析

**Files:**
- Modify: `pregnancy-assistant/src/views/RecordsView.vue`
- Modify: `pregnancy-assistant/src/views/InsightsView.vue`

- [x] **Step 1: 格式化历史列表血压值**

增加血压 JSON 解析分支，读取 `systolic` 和 `diastolic` 后显示为 `高压 / 低压`；保留其他记录当前的 `valueJson` 展示逻辑和无效 JSON 的降级文本。

- [x] **Step 2: 格式化身体记录时间线血压值**

在时间线使用的 `healthValue` 中加入同样的血压结构解析，使时间线与历史列表展示一致；不改变体重趋势的数值解析规则。

### Task 3: 同步业务记录并验证

**Files:**
- Modify: `docs/business/_index.md`
- Modify: `docs/business/modules/pregnancy-health.md`
- Modify: `memory.md`
- Modify: `D:\file\工作日报\2026\工作日报@zhanghao_SMEICS@20260806.md`
- Modify: `D:\file\工作周报\2026\2026年08月第1周工作周报@zhanghao_SMEICS.md`

- [x] **Step 1: 更新业务规则和数据说明**

将血压记录说明改为高压、低压两个用户输入值，并明确 `value_json` 的 `systolic`、`diastolic` 键；保持模块文档的 8 个一级板块、索引登记和未知规则说明不变。

- [x] **Step 2: 更新结构化记忆和工作记录**

在 `memory.md` 汇总表和实现结论中记录本次字段拆分；在日报、周报已有孕期助手条目中合并本次变更，不新增重复条目，日报不出现指定禁用词。

- [x] **Step 3: 执行前端验证**

在 `pregnancy-assistant` 目录执行 `npm run type-check` 和 `npm run build`；再静态检查 `systolic`、`diastolic`、血压展示格式及业务文档八大板块。预期类型检查和生产构建通过，真实数据库联调不在本次验证范围。

### Self-review

- 设计目标覆盖 Task 1 的双输入、Task 2 的展示闭环和 Task 3 的业务记录与验证。
- 无数据库结构、后端 DTO、医疗范围或历史数据修复的未定义步骤。
- 所有字段名统一使用 `systolic`、`diastolic`，与设计文档和 JSON 契约一致。
