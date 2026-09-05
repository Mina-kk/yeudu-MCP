# 阅读书源参考路由

每轮创建、修改或继续调试先读 `SKILL.md` 与本文件，再只加载当前阶段需要的参考。

## MCP-first 基线

默认在书源工坊真实运行时中工作：

1. `list_sources` 按名称、域名查重。
2. 已有源用 `get_source` 读取原文。
3. 用 `eval_js` 做请求与 DOM 探针。
4. 用 `save_source` 保存单个草案。
5. 用 `debug_source` 验证当前阶段。
6. 完成后 `get_source` 回读，并用 `check_source` 整体校验。
7. 不要引导用户打开已删除的「项目列表」。本地书源在底栏「书源」页，可导入至阅读或删除。排查请用 `get_logs` / `get_http_logs` / `get_crash_logs`，不要要求用户导出日志。

MCP 连接故障属于环境故障。先恢复连接，不以本地猜测代替应用内调试。旧 `scripts/legado-debug.py` 仅是用户明确同意后的备用入口。

## 按阶段读取

| 当前阶段 | 必读文件 |
|---|---|
| 初始化、基础字段、详情、搜索、目录、正文 | `references/basics.md` |
| 请求失败、403、验证页、动态页面 | `references/troubleshoot.md`, `references/js-api.md` |
| WebView、webJs、调用网页函数 | `references/webjs.md`, `references/troubleshoot.md` |
| 登录、按钮、回调、变量持久化 | `references/login.md`, `references/patterns.md` |
| 发现分类与布局 | `references/discovery.md` |
| 漫画正文与图片 | `references/comic.md`, `references/basics.md` |
| 多线路、多类型、跨页状态 | `references/patterns.md`, `references/js-api.md` |
| 订阅源/RSS | `references/basics.md`, `references/js-api.md` |

## 按失败现象读取

| 现象 | 读取文件 |
|---|---|
| 选择器无结果、字段为空或错位 | `references/basics.md` |
| 搜索无结果、乱码、分页或 URL 参数异常 | `references/basics.md`, `references/troubleshoot.md` |
| 浏览器有内容但普通请求拿不到 | `references/troubleshoot.md`, `references/webjs.md` |
| 403、验证盾、跳转、UA、Cookie | `references/troubleshoot.md`, `references/js-api.md` |
| JS 报错、Rhino 兼容、java.* 用法 | `references/basics.md`, `references/js-api.md` |
| 发现页 JSON、分类和按钮布局 | `references/discovery.md` |
| 漫画图片不显示、403、懒加载或解密 | `references/comic.md`, `references/troubleshoot.md` |

## 探针原则

在改规则前，用 `eval_js` 输出最小证据：

- 响应码和最终 URL
- HTML 长度与关键片段
- 候选选择器匹配数量
- 第一个节点的文本、属性和链接
- JavaScript 的输入值、输出值和异常

需要网络级证据时：开启 HTTP 日志 → 复现一次 → 读取该次请求详情。不要批量复现制造噪音。

## 每阶段记录

- 当前阶段
- 本轮读取的参考文件
- 测试入口与预期值
- 修改字段
- `debug_source` 结果摘要
- 下一步只处理哪个字段或阶段
