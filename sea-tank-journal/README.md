# 水境日记

微信小程序原生项目，使用 WXML、WXSS 和 JavaScript；UI 控件使用 Vant Weapp `1.11.7`，不使用跨端框架。

## 开发配置

1. 在微信公众平台创建小程序后，将 `project.config.json` 的 `appid` 填为真实 AppID。
2. 在 `config/env.js` 设置已备案并配置到小程序 request 合法域名的 HTTPS API 地址。
3. 本机开发时，在仓库根目录 `config/application-out.yml` 配置微信 AppID、AppSecret 和令牌签名密钥；该文件已加入 Git 忽略。生产环境继续通过服务端环境变量注入这些配置，不要将密钥提交到仓库。
4. 在本目录执行 `npm install`；安装脚本会把 Vant 小程序组件准备到 `miniprogram_npm/`。
5. 执行 `npm run typecheck` 和 `npm run build`。源目录与生成的 `dist/` 均包含 DevTools 可解析的组件目录。
6. 将项目根目录或 `dist/` 导入微信开发者工具。

## 检查命令

```powershell
npm run typecheck
npm run build
npm run audit
npm run check:performance-budget
```

`build` 生成可导入的原生小程序文件并拷贝使用到的 Vant Weapp 库。微信开发者工具的 WXML/WXSS 编译仍需在工具中完成。初始数据为空，不会自动写入原型示例。
