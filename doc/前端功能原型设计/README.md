# QuantPF V25 前端产品原型

本目录依据《量化交易系统完整设计方案 V25》设计，视觉和交互风格参考：

- 前端底座：D:\workspace\quant\plus-ui
- 后端底座：D:\workspace\quant\RuoYi-Vue-Plus
- 目标浏览器：Microsoft Edge（Chromium）

## 打开方式

直接使用 Microsoft Edge 打开 `index.html` 即可，不依赖网络或第三方 CDN。

也可以启动静态服务：

```powershell
cd "D:\我的文档\私人\量化\前端功能原型设计"
python -m http.server 8088
```

访问：`http://127.0.0.1:8088/index.html`。

## 文件结构

- `index.html`：完整原型总入口，左侧菜单覆盖全部页面。
- `pages/*.html`：50 个独立页面入口，方便评审和菜单映射。
- `assets/styles.css`：产品视觉、响应式、打印和深色主题。
- `assets/app.js`：页面数据、组件模板和原型交互。
- `PROTOTYPE_PAGE_MAP.md`：页面、路由、权限、API和V25模块映射。

## 原型说明

1. 页面使用模拟数据，不会发起真实交易或网络请求。
2. 真实账户、PAPER/SHADOW/READ_ONLY/LIVE模式在界面中明确区分。
3. XXL-JOB 是唯一调度框架，SnailJob 仅在迁移说明中出现。
4. 正式落地应替换为 Vue 3 + TypeScript + Element Plus + ECharts，并绑定V25 OpenAPI。
5. 高风险操作必须接入二次确认、Sa-Token权限、幂等键和审计日志。
