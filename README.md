# 阅读书源 MCP

本机 **Legado 运行时 + MCP Server**（原「书源工坊」）。App 内不调用任何模型、不保存任何模型密钥。书源的制作、修复和调试全部由外部 MCP 客户端完成。

- 包名：`com.mina.legadostudio`
- 当前版本：`1.0.105`（versionCode 125）
- 许可证：GPL-3.0
- 上游致谢：DandanLLab/legadoSkill、LegadoTeam/legado

## 连接 MCP

1. 打开 App，底栏 **MCP** 页开启服务（默认端口 **58823**）
2. 外部 MCP 客户端配置：
   - URL：`http://127.0.0.1:58823/mcp`
   - Header：`X-Studio-Token: <你的 Token>`（在 App 的 MCP 页可查看/复制）
3. 调用 `get_app_info`，应返回 `"ai": false`、`"role": "mcp-runtime"`

`save_source` 默认按书源 URL 覆盖更新同站记录；需要保留历史版本时传 `newVersion=true` 追加。底栏为 **MCP / 书源 / 技能 / 日志**。书源页按站点域名分组，组内按保存时间倒序展开。技能页可查看内置 Skill 并控制启用/停用（停用后对 MCP 不可见）；自定义 Skill 支持新增、导入、导出和删除。`save_skill` / `delete_skill` 不能改写内置 Skill。

## 制作流程

```
list_skills / search_knowledge
  → 编写 BookSource JSON
  → validate_source
  → debug_source / inspect_rule / eval_js
  → check_source
  → save_source
```

验证码 / 登录 / WAF 用 `browser_verify`：通过系统通知或 MCP 页顶部横幅，在应用内完成站点验证。

本 App **不提供** `list_models`、`ai_generate_source`、`start_job` 等模型或任务工具。

## 日志与诊断

底栏 **日志** 分四段：操作日志 / HTTP / 崩溃 / 诊断快照。可多选删除。诊断快照只含版本、MCP 状态和前置条件，不含 HTTP 或崩溃正文。排查请用 MCP：`get_logs` / `get_log` / `get_http_logs` / `get_http_log` / `get_crash_logs` / `get_crash_log` / `get_diagnostic_snapshots` / `get_diagnostic_snapshot`。

构建说明见 `BUILDING.md`。
