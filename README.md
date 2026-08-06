# union_lotto

## 孕期助手

前端位于 `pregnancy-assistant/`，使用 Vite + Vue + TypeScript。记录先保存到浏览器 IndexedDB，联网后可通过同步按钮上传到后端。

```powershell
cd pregnancy-assistant
npm install
npm run dev
```

后端继续使用现有 Spring Boot 服务，孕期接口前缀为 `/tabs/pregnancy`。全新环境初始化数据库时执行 `sql/init_pregnancy_assistant.sql`。

前端生产构建：

```powershell
cd pregnancy-assistant
npm run build
```

首版为单用户工具，不建立登录、用户隔离或共享模型；统计只描述个人历史变化，不能替代产科建议。
