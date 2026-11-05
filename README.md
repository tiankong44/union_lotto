# union_lotto

## 水境日记

原生微信小程序位于 `sea-tank-journal/`，后端接口位于 `/tabs/reef-journal/**`。小程序使用 WXML、WXSS、JavaScript 和 Vant Weapp；导入微信开发者工具前需填写小程序 AppID，并在 `sea-tank-journal/config/env.js` 配置 HTTPS API 域名。

后端运行前需在 `application*.yml` 对应环境中提供 `REEF_WECHAT_APP_ID`、`REEF_WECHAT_APP_SECRET`、`REEF_TOKEN_SIGNING_KEY`、API 订阅模板 ID 及模板字段配置。密钥只保留在服务端环境。新环境执行 `src/main/resources/sql/init/init.sql`；已有环境执行 `src/main/resources/sql/fix/fix.sql`。

海缸业务规则与数据结构见 `docs/business/modules/reef-journal.md`。

## 孕期助手

前端位于 `pregnancy-assistant/`，使用 Vite + Vue + TypeScript。记录直接通过后端接口保存到云端 MySQL，浏览器不保存业务快照或同步队列。

```powershell
cd pregnancy-assistant
npm install
npm run dev
```

后端继续使用现有 Spring Boot 服务，孕期接口前缀为 `/tabs/pregnancy`，文件接口前缀为 `/tabs/common/files`。全新环境初始化数据库时执行 `sql/init_pregnancy_assistant.sql`，已有环境新增文件表时执行 `sql/fix_cloud_file.sql`。

前端生产构建：

```powershell
cd pregnancy-assistant
npm run build
```

首版为单用户工具，不建立登录、用户隔离或共享模型；网络不可用时不保存记录；统计只描述个人历史变化，不能替代产科建议。
