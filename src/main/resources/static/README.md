# 一起听歌吧 Modern UI

独立实现的 B「唱片书房」前端，不依赖原 Vue 2 / Muse UI 页面。

## 运行

安装依赖后，在仓库根目录启动：

```bash
npm install
npm start
```

然后访问：

```text
http://127.0.0.1:4174/modern-ui/
```

## 打包与预览

```bash
npm run build
npm run preview
```

打包产物位于 `dist-modern/`，Windows 下同时生成 `一起听歌吧-前端.zip`。`dist-modern` 是可直接部署到网站根目录的完整静态站点，预览地址为 `http://127.0.0.1:4175/`。

原 Vue 2 页面仍可使用 `npm run serve` 启动、使用 `npm run build:legacy` 打包。

## 架构

- `index.html`：语义化页面结构
- `design-v2.css`：完全重建的桌面端与移动端设计系统，旧样式不再加载
- `app.js`：播放器、队列、聊天、房间及所有功能面板状态
- `backend-adapter.js`：真实后端适配层，负责 HTTP、SockJS、STOMP、事件解析与断线重连

## 后端

本地访问时默认连接 `http://localhost:8888`，线上访问时默认连接 `https://tx.alang.run/api`。也可以使用查询参数覆盖：

```text
http://127.0.0.1:4174/modern-ui/?api=http://localhost:8888
```

页面会读取真实房间、自动连接 SockJS/STOMP，并同步在线人数、聊天、队列、当前歌曲、音量、搜索结果、歌词和管理员状态。浏览器的自动播放策略可能阻止首次自动播放，此时点击一次播放按钮即可。

## 系统管理员控制台

先进入房间，在聊天框输入 `root 密码` 完成 Root 验证，再从房管控制台进入“管理员控制台”。系统级 HTTP 接口另需后端 Basic Auth 用户名与密码；登录验证使用 `/house/showRetainKey`，两种身份不能互相替代。系统账号仅保存在当前页面内存，刷新页面或点击退出即清除。控制台按房间管理、永存码、房间用户、系统与音乐分类，房间编辑仅提交实际修改的字段。
