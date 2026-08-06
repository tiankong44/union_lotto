# union_lotto

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
