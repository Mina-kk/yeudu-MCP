package com.mina.legadostudio.mcp

import android.content.Context
import com.google.gson.JsonParser
import com.mina.legadostudio.BuildConfig
import com.mina.legadostudio.StudioApplication
import com.mina.legadostudio.network.HttpFetcher
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ReadResourceResult
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.TextResourceContents
import io.modelcontextprotocol.kotlin.sdk.types.ToolAnnotations
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

class StudioMcpServer(context: Context) {
    private val app = context.applicationContext as StudioApplication
    private val readOnly = ToolAnnotations(readOnlyHint = true, openWorldHint = false)
    private val write = ToolAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = true, openWorldHint = false)
    private val openWrite = ToolAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = false, openWorldHint = true)

    fun create(): Server = Server(
        serverInfo = Implementation("legado-source-studio", BuildConfig.VERSION_NAME),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = false),
                resources = ServerCapabilities.Resources(),
            )
        )
    ).also { server ->
        server.onConnect { McpStats.connected(); StudioLog.add("mcp connect", category = "mcp") }
        server.onClose { McpStats.disconnected(); StudioLog.add("mcp disconnect", category = "mcp") }
        registerResources(server)
        registerTools(server)
    }

    private fun registerResources(server: Server) {
        app.skills.list().filter { it.enabled }.forEach { skill ->
            val uri = "studio://skills/${skill.id}"
            server.addResource(uri, skill.name, "阅读书源MCP Skill：${skill.name}", "text/markdown") { _ ->
                ReadResourceResult(listOf(TextResourceContents(app.skills.read(skill.id), uri, "text/markdown")))
            }
        }
    }

    private fun registerTools(server: Server) {
        server.tool("get_app_info", "读取阅读书源MCP版本和能力", ToolSchema(properties = buildJsonObject {}, required = emptyList()), toolAnnotations = readOnly) {
            ok(app.gson.toJson(mapOf(
                "name" to "阅读书源MCP",
                "version" to BuildConfig.VERSION_NAME,
                "mcpPath" to McpAccess.PATH,
                "license" to "GPL-3.0",
                "ai" to false,
                "role" to "mcp-runtime",
                "runtime" to listOf("official-css", "official-xpath", "official-jsonpath", "official-regex", "official-rhino", "webview"),
                "ui" to listOf("mcp", "sources", "skills", "logs"),
            )))
        }
        server.tool("app_status", "读取 MCP、权限、省电、局域网和验证会话状态", ToolSchema(properties = buildJsonObject {}, required = emptyList()), toolAnnotations = readOnly) {
            val mcp = com.mina.legadostudio.service.McpService.status(app, includeToken = false)
            val readiness = com.mina.legadostudio.device.DeviceReadiness(app).inspect((mcp["port"] as? Int) ?: McpConfigStore.DEFAULT_PORT, mcp["running"] == true)
            val waiting = app.database.dao().observeVerificationSessions().first().count { it.status == "WAITING" }
            ok(app.gson.toJson(mapOf(
                "mcp" to mcp,
                "readiness" to readiness,
                "waitingVerifications" to waiting,
                "ai" to false,
                "role" to "mcp-runtime",
            )))
        }
        server.tool("list_projects", "列出书源项目摘要", ToolSchema(properties = buildJsonObject {}, required = emptyList()), toolAnnotations = readOnly) {
            ok(app.gson.toJson(app.projects.list()))
        }
        server.tool("get_project", "按项目 ID 读取完整项目", schema(mapOf("id" to "项目 ID"), listOf("id")), toolAnnotations = readOnly) { req ->
            val id = req.arguments.str("id") ?: return@tool err("id 不能为空")
            app.projects.get(id)?.let { ok(app.gson.toJson(it)) } ?: err("项目不存在")
        }
        server.tool("save_project", "保存项目 JSON", schema(mapOf("project" to "Project JSON"), listOf("project")), toolAnnotations = write) { req ->
            runCatching {
                val root = JsonParser.parseString(req.arguments.str("project") ?: error("project 不能为空")).asJsonObject
                val id = root.get("id")?.takeUnless { it.isJsonNull }?.asString?.takeIf { it.isNotBlank() } ?: java.util.UUID.randomUUID().toString()
                val old = app.projects.get(id)
                val now = System.currentTimeMillis()
                val project = com.mina.legadostudio.data.db.ProjectEntity(
                    id = id,
                    name = root.get("name")?.takeUnless { it.isJsonNull }?.asString.orEmpty().ifBlank { "未命名书源" },
                    siteUrl = root.get("siteUrl")?.takeUnless { it.isJsonNull }?.asString.orEmpty(),
                    sourceJson = root.get("sourceJson")?.takeUnless { it.isJsonNull }?.asString ?: old?.sourceJson ?: "{}",
                    stage = root.get("stage")?.takeUnless { it.isJsonNull }?.asString ?: old?.stage ?: "DRAFT",
                    notes = root.get("notes")?.takeUnless { it.isJsonNull }?.asString ?: old?.notes.orEmpty(),
                    createdAt = old?.createdAt ?: now,
                    updatedAt = now,
                )
                ok(app.gson.toJson(app.projects.save(project)))
            }.getOrElse { err(it.message.orEmpty()) }
        }
        server.tool("delete_projects", "删除项目", arraySchema("ids", "项目 ID 列表"), toolAnnotations = write) { req ->
            val ids = (req.arguments?.get("ids") as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()
            ok("已删除 ${app.projects.delete(ids)} 个项目")
        }
        server.tool("save_source", "校验并写入 BookSource JSON。同一 bookSourceUrl 默认覆盖当前成品（内部保留修订历史）；只有 newVersion=true 才追加一条新版本，供下一轮修复使用。", ToolSchema(properties = buildJsonObject {
            put("source", stringProp("BookSource JSON"))
            putJsonObject("newVersion") { put("type", "boolean"); put("description", "true 时追加新版本；默认 false 覆盖同 URL 当前成品") }
        }, required = listOf("source")), toolAnnotations = write) { req ->
            runCatching {
                val source = req.arguments.str("source") ?: error("source 不能为空")
                val newVersion = req.arguments.bool("newVersion") ?: false
                val report = app.validator.validate(source); require(report.isValid) { "书源验证未通过：${app.gson.toJson(report.issues)}" }
                val obj = JsonParser.parseString(source).asJsonObject
                val siteUrl = obj.get("bookSourceUrl").asString.trim().trimEnd('/')
                val now = System.currentTimeMillis()
                val existing = if (newVersion) null else app.database.dao().projectBySiteUrl(siteUrl)
                val project = com.mina.legadostudio.data.db.ProjectEntity(
                    id = existing?.id ?: java.util.UUID.randomUUID().toString(),
                    name = obj.get("bookSourceName").asString,
                    siteUrl = siteUrl,
                    sourceJson = source,
                    stage = "VALIDATED",
                    notes = existing?.notes.orEmpty(),
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now,
                )
                ok(app.gson.toJson(app.projects.save(project)))
            }.getOrElse { err(it.message.orEmpty()) }
        }
        server.tool("list_sources", "列出已保存 BookSource 摘要（默认每 URL 一条成品，按 updatedAt 倒序）", ToolSchema(properties = buildJsonObject {}, required = emptyList()), toolAnnotations = readOnly) {
            ok(app.gson.toJson(app.projects.list().groupBy { it.siteUrl.trim().trimEnd('/') }.values.map { group -> group.maxBy { it.updatedAt } }.sortedByDescending { it.updatedAt }.map {
                mapOf(
                    "projectId" to it.id,
                    "bookSourceName" to it.name,
                    "bookSourceUrl" to it.siteUrl,
                    "stage" to it.stage,
                    "createdAt" to it.createdAt,
                    "updatedAt" to it.updatedAt,
                )
            }))
        }
        server.tool("get_source", "按 projectId 读取指定版本；若传入 bookSourceUrl 则返回该 URL 最近一次保存的版本", schema(mapOf("key" to "项目 ID 或 bookSourceUrl"), listOf("key")), toolAnnotations = readOnly) { req ->
            val key = req.arguments.str("key") ?: return@tool err("key 不能为空")
            val project = app.projects.get(key) ?: app.database.dao().projectBySiteUrl(key)
            project?.let { ok(it.sourceJson) } ?: err("未找到书源")
        }
        server.tool("validate_source", "验证 BookSource JSON", schema(mapOf("source" to "BookSource JSON"), listOf("source")), toolAnnotations = readOnly) { req ->
            ok(app.gson.toJson(app.validator.validate(req.arguments.str("source").orEmpty())))
        }
        server.tool("export_source", "校验并返回项目中的 BookSource JSON", schema(mapOf("projectId" to "项目 ID"), listOf("projectId")), toolAnnotations = readOnly) { req ->
            val project = app.projects.get(req.arguments.str("projectId") ?: return@tool err("projectId 不能为空"))
                ?: return@tool err("项目不存在")
            val report = app.validator.validate(project.sourceJson)
            if (report.isValid) ok(project.sourceJson) else err("书源验证未通过：${app.gson.toJson(report.issues)}")
        }
        server.tool("fetch_page", "抓取真实网页并返回响应", fetchSchema(), toolAnnotations = openWrite) { req ->
            runCatching {
                val input = HttpFetcher.FetchRequest(
                    url = req.arguments.str("url") ?: error("url 不能为空"),
                    method = req.arguments.str("method") ?: "GET",
                    body = req.arguments.str("body"),
                    charset = req.arguments.str("charset"),
                    timeoutSec = req.arguments.int("timeoutSec") ?: 30,
                )
                val result = withContext(Dispatchers.IO) { app.fetcher.fetch(input) }
                ok(app.gson.toJson(result.copy(body = result.body.take(200_000))))
            }.getOrElse { err(it.message.orEmpty()) }
        }
        server.tool("analyze_html", "分析 HTML 或测试 CSS 选择器", schema(mapOf("html" to "HTML", "baseUrl" to "基础 URL", "selector" to "可选 CSS 选择器"), listOf("html")), toolAnnotations = readOnly) { req ->
            runCatching {
                val html = req.arguments.str("html").orEmpty()
                val base = req.arguments.str("baseUrl").orEmpty()
                val selector = req.arguments.str("selector").orEmpty()
                ok(app.gson.toJson(if (selector.isBlank()) app.analyzer.analyze(html, base) else app.analyzer.testSelector(html, base, selector)))
            }.getOrElse { err(it.message.orEmpty()) }
        }
        server.tool("inspect_rule", "真实请求网页并使用 Legado 官方 CSS/XPath/JSONPath/正则解析器运行规则", schema(mapOf("url" to "网页 URL", "rule" to "Legado 规则", "method" to "GET/POST/HEAD", "charset" to "可选编码"), listOf("url", "rule")), toolAnnotations = openWrite) { req ->
            runCatching { ok(app.gson.toJson(app.runtime.inspect(com.mina.legadostudio.runtime.LegadoRuntime.InspectRequest(req.arguments.str("url")!!, req.arguments.str("method") ?: "GET", charset = req.arguments.str("charset"), rule = req.arguments.str("rule")!!)))) }.getOrElse { verificationAware(it) }
        }
        server.tool("debug_source", "使用 App 内运行时调试 BookSource JSON；entry 支持关键词、详情 URL、++目录、--正文、分类::URL", schema(mapOf("source" to "BookSource JSON", "entry" to "调试入口"), listOf("source", "entry")), toolAnnotations = openWrite) { req ->
            runCatching { ok(app.gson.toJson(app.runtime.debug(req.arguments.str("source")!!, req.arguments.str("entry")!!))) }.getOrElse { verificationAware(it) }
        }
        server.tool("eval_js", "在 Legado 官方 Rhino 环境执行 JavaScript，并返回结果和 java.log", schema(mapOf("js" to "JavaScript", "baseUrl" to "可选基础 URL"), listOf("js")), toolAnnotations = openWrite) { req ->
            runCatching { ok(app.gson.toJson(app.runtime.evaluate(req.arguments.str("js")!!, req.arguments.str("baseUrl").orEmpty()))) }.getOrElse { verificationAware(it) }
        }
        server.tool("browser_verify", "创建站点验证会话（验证码/登录/WAF）。完成后通过系统通知或 MCP 页顶部横幅进入应用内 WebView", schema(mapOf("url" to "验证 URL", "purpose" to "用途说明"), listOf("url")), toolAnnotations = openWrite) { req ->
            runCatching {
                val session = app.verification.create("mcp", req.arguments.str("url")!!, req.arguments.str("purpose") ?: "MCP 请求验证")
                val payload = app.gson.toJsonTree(session).asJsonObject
                payload.addProperty("message", VERIFY_MESSAGE)
                ok(payload.toString())
            }.getOrElse { err(it.message.orEmpty()) }
        }
        server.tool("get_verification_status", "读取 App 内验证会话状态", schema(mapOf("sessionId" to "验证会话 ID"), listOf("sessionId")), toolAnnotations = readOnly) { req ->
            app.database.dao().verificationSession(req.arguments.str("sessionId") ?: return@tool err("sessionId 不能为空"))?.let { ok(app.gson.toJson(it)) } ?: err("验证会话不存在")
        }
        server.tool("get_cookies", "读取指定 URL 所属域的 Runtime Cookie", schema(mapOf("url" to "URL"), listOf("url")), toolAnnotations = readOnly) { req ->
            ok(app.cookieStore.headerFor(req.arguments.str("url") ?: return@tool err("url 不能为空")) ?: "（空）")
        }
        server.tool("set_cookie", "写入指定 URL 所属域的 Runtime/WebView Cookie", schema(mapOf("url" to "URL", "cookie" to "name=value; ..."), listOf("url", "cookie")), toolAnnotations = write) { req ->
            runCatching { app.cookieStore.set(req.arguments.str("url")!!, req.arguments.str("cookie")!!); ok("Cookie 已写入") }.getOrElse { err(it.message.orEmpty()) }
        }
        server.tool("clear_cookies", "清除指定 URL 所属域的 Runtime/WebView Cookie", schema(mapOf("url" to "URL"), listOf("url")), toolAnnotations = write) { req ->
            app.cookieStore.clear(req.arguments.str("url") ?: return@tool err("url 不能为空")); ok("Cookie 已清除")
        }
        server.tool("check_source", "按提供的搜索/详情/目录/正文入口批量运行真实校验", checkSourceSchema(), toolAnnotations = openWrite) { req ->
            val source = req.arguments.str("source") ?: return@tool err("source 不能为空")
            val checks = linkedMapOf<String, String>()
            req.arguments.str("searchKey")?.takeIf { it.isNotBlank() }?.let { checks["搜索"] = it }
            req.arguments.str("detailUrl")?.takeIf { it.isNotBlank() }?.let { checks["详情"] = it }
            req.arguments.str("tocUrl")?.takeIf { it.isNotBlank() }?.let { checks["目录"] = "++$it" }
            req.arguments.str("contentUrl")?.takeIf { it.isNotBlank() }?.let { checks["正文"] = "--$it" }
            val results = linkedMapOf<String, Any>()
            results["validation"] = app.runtime.validate(source)
            checks.forEach { (name, entry) -> results[name] = runCatching { app.runtime.debug(source, entry) }.fold({ it }, { mapOf("error" to it.message.orEmpty()) }) }
            ok(app.gson.toJson(results))
        }
        server.tool("search_knowledge", "搜索内置 Legado 书源知识库并返回命中片段", schema(mapOf("query" to "搜索词"), listOf("query")), toolAnnotations = readOnly) { req ->
            ok(app.gson.toJson(app.knowledge.search(req.arguments.str("query").orEmpty(), 50)))
        }
        server.tool("list_skills", "列出内置和自定义 Skills（含 enabled；禁用的不要加载）", ToolSchema(properties = buildJsonObject {}, required = emptyList()), toolAnnotations = readOnly) {
            ok(app.gson.toJson(app.skills.list()))
        }
        server.tool("get_skill", "读取已启用 Skill 的 Markdown；禁用技能会报错", schema(mapOf("id" to "Skill ID"), listOf("id")), toolAnnotations = readOnly) { req ->
            runCatching {
                val id = req.arguments.str("id") ?: error("id 不能为空")
                require(app.skills.isEnabled(id)) { "Skill 已禁用" }
                ok(app.skills.read(id))
            }.getOrElse { err(it.message.orEmpty()) }
        }
        server.tool("save_skill", "保存自定义 Skill；内置 Skill 不可覆盖", schema(mapOf("id" to "Skill ID", "markdown" to "SKILL.md 内容"), listOf("id", "markdown")), toolAnnotations = write) { req ->
            runCatching { app.skills.save(req.arguments.str("id")!!, req.arguments.str("markdown")!!); StudioLog.add("Skill saved: ${req.arguments.str("id")}"); ok("Skill 已保存") }.getOrElse { err(it.message.orEmpty()) }
        }
        server.tool("delete_skill", "删除自定义 Skill；内置 Skill 不可删除", schema(mapOf("id" to "Skill ID"), listOf("id")), toolAnnotations = write) { req ->
            runCatching {
                val id = req.arguments.str("id") ?: error("id 不能为空")
                require(app.skills.delete(id)) { "Skill 不存在或不是自定义技能" }
                StudioLog.add("Skill deleted: $id")
                ok("Skill 已删除")
            }.getOrElse { err(it.message.orEmpty()) }
        }
        server.tool("export_diagnostic", "创建诊断快照（仅含版本、MCP 状态与前置条件，不含 HTTP/崩溃正文）", ToolSchema(properties = buildJsonObject {}, required = emptyList()), toolAnnotations = readOnly) {
            val snap = app.snapshots.create()
            ok(app.gson.toJson(mapOf("id" to snap.id, "title" to snap.title, "createdAt" to snap.createdAt)))
        }
        server.tool("get_logs", "读取持久化操作日志（结构化 JSON）。排查工具调用与运行时故障时优先使用，无需从 App 导出。", ToolSchema(properties = buildJsonObject {
            putJsonObject("limit") { put("type", "integer"); put("description", "默认 50，最大 200") }
            putJsonObject("category") { put("type", "string"); put("description", "可选，按 category 过滤，如 tool/mcp/service") }
            putJsonObject("level") { put("type", "string"); put("description", "可选，按级别过滤：I/W/E") }
            putJsonObject("query") { put("type", "string"); put("description", "可选，匹配 message 或 detail") }
        }, required = emptyList()), toolAnnotations = readOnly) { req ->
            val limit = (req.arguments.int("limit") ?: 50).coerceIn(1, 200)
            val category = req.arguments.str("category").orEmpty()
            val level = req.arguments.str("level").orEmpty()
            val query = req.arguments.str("query").orEmpty()
            val items = app.database.dao().latestOperationLogs(500)
                .filter { category.isBlank() || it.category.equals(category, ignoreCase = true) }
                .filter { level.isBlank() || it.level.equals(level, ignoreCase = true) }
                .filter { query.isBlank() || it.message.contains(query, ignoreCase = true) || it.detail.contains(query, ignoreCase = true) }
                .take(limit)
            ok(app.gson.toJson(mapOf("count" to items.size, "items" to items)))
        }
        server.tool("get_log", "按 ID 读取单条操作日志详情", ToolSchema(properties = buildJsonObject { putJsonObject("id") { put("type", "integer") } }, required = listOf("id")), toolAnnotations = readOnly) { req ->
            app.database.dao().operationLog((req.arguments.int("id") ?: return@tool err("id 不能为空")).toLong())?.let { ok(app.gson.toJson(it)) } ?: err("日志不存在")
        }
        server.tool("get_http_logs", "列出最近 HTTP 事务摘要。排查请求失败、重定向或状态码时使用。", ToolSchema(properties = buildJsonObject {
            putJsonObject("limit") { put("type", "integer"); put("description", "默认 50，最大 200") }
            putJsonObject("query") { put("type", "string"); put("description", "可选，匹配 URL") }
        }, required = emptyList()), toolAnnotations = readOnly) { req ->
            val query = req.arguments.str("query").orEmpty()
            val items = app.database.dao().observeHttpLogs((req.arguments.int("limit") ?: 50).coerceIn(1, 200)).first()
                .filter { query.isBlank() || it.url.contains(query, ignoreCase = true) || it.finalUrl.contains(query, ignoreCase = true) }
                .map { mapOf("id" to it.id, "method" to it.method, "url" to it.url, "finalUrl" to it.finalUrl, "statusCode" to it.statusCode, "durationMs" to it.durationMs, "error" to it.error) }
            ok(app.gson.toJson(mapOf("count" to items.size, "items" to items)))
        }
        server.tool("get_http_log", "按 ID 读取单条 HTTP 事务详情（含请求/响应头与正文）", ToolSchema(properties = buildJsonObject { putJsonObject("id") { put("type", "integer") } }, required = listOf("id")), toolAnnotations = readOnly) { req ->
            app.database.dao().httpLog((req.arguments.int("id") ?: return@tool err("id 不能为空")).toLong())?.let { ok(app.gson.toJson(it)) } ?: err("日志不存在")
        }
        server.tool("set_http_log_recording", "启用或停用 HTTP 事务记录", ToolSchema(properties = buildJsonObject { putJsonObject("enabled") { put("type", "boolean") } }, required = listOf("enabled")), toolAnnotations = write) { req ->
            val enabled = req.arguments?.get("enabled")?.jsonPrimitive?.booleanOrNull ?: return@tool err("enabled 必须为布尔值")
            app.httpLogs.enabled = enabled; ok("HTTP 事务记录已${if (enabled) "启用" else "停用"}")
        }
        server.tool("get_crash_logs", "列出本地崩溃记录摘要", ToolSchema(properties = buildJsonObject { putJsonObject("limit") { put("type", "integer"); put("description", "默认 10，最大 20") } }, required = emptyList()), toolAnnotations = readOnly) { req ->
            val items = app.crashLogs.list().take((req.arguments.int("limit") ?: 10).coerceIn(1, 20)).map {
                mapOf("name" to it.name, "createdAt" to it.createdAt, "size" to it.size)
            }
            ok(app.gson.toJson(mapOf("count" to items.size, "items" to items)))
        }
        server.tool("get_crash_log", "按文件名读取崩溃记录正文", schema(mapOf("name" to "崩溃文件名"), listOf("name")), toolAnnotations = readOnly) { req ->
            runCatching { ok(app.crashLogs.read(req.arguments.str("name") ?: error("name 不能为空"))) }.getOrElse { err(it.message.orEmpty()) }
        }
        server.tool("get_diagnostic_snapshots", "列出诊断快照摘要", ToolSchema(properties = buildJsonObject {}, required = emptyList()), toolAnnotations = readOnly) {
            val items = app.database.dao().observeDiagnosticSnapshots().first().map { mapOf("id" to it.id, "title" to it.title, "createdAt" to it.createdAt) }
            ok(app.gson.toJson(mapOf("count" to items.size, "items" to items)))
        }
        server.tool("get_diagnostic_snapshot", "按 ID 读取诊断快照内容", schema(mapOf("id" to "快照 ID"), listOf("id")), toolAnnotations = readOnly) { req ->
            val snap = app.database.dao().diagnosticSnapshot(req.arguments.str("id") ?: return@tool err("id 不能为空")) ?: return@tool err("快照不存在")
            ok(app.snapshots.read(snap).ifBlank { "（空）" })
        }
    }

    private suspend fun verificationAware(error: Throwable): CallToolResult {
        val verification = error as? com.mina.legadostudio.verification.VerificationRequiredException
            ?: return err(error.message.orEmpty())
        val domain = com.mina.legadostudio.verification.DomainKey.fromUrl(verification.verificationUrl)
        val latest = app.database.dao().latestVerification("mcp", domain)
        if (latest?.status == "COMPLETED" && !app.domainModes.requiresWebView(verification.verificationUrl)) {
            app.domainModes.requireWebView(verification.verificationUrl)
            return ok(app.gson.toJson(mapOf(
                "status" to "webview_mode_enabled",
                "domain" to domain,
                "message" to "HTTP 会话仍被验证拦截，已切换 App 内 WebView 请求模式，请重试原工具",
            )))
        }
        val session = app.verification.create("mcp", verification.verificationUrl, "MCP 请求需要网站验证")
        return ok(app.gson.toJson(mapOf(
            "status" to "verification_required",
            "sessionId" to session.id,
            "url" to session.url,
            "domain" to session.domain,
            "message" to VERIFY_MESSAGE,
        )))
    }

    private fun Server.tool(
        name: String,
        description: String,
        inputSchema: ToolSchema,
        toolAnnotations: ToolAnnotations,
        handler: suspend (CallToolRequest) -> CallToolResult,
    ) {
        addTool(name, description, inputSchema, toolAnnotations = toolAnnotations) { req ->
            logged(name, req.arguments) { handler(req) }
        }
    }

    private suspend fun logged(name: String, args: JsonObject?, block: suspend () -> CallToolResult): CallToolResult {
        val started = System.currentTimeMillis()
        val keys = args?.keys.orEmpty().filter { it.lowercase() !in HIDDEN_ARG_KEYS }.sorted().joinToString(",")
        return try {
            val result = block()
            val ok = result.isError != true
            StudioLog.add(
                "tool $name ${if (ok) "ok" else "err"} ${System.currentTimeMillis() - started}ms",
                if (ok) "I" else "W",
                "tool",
                if (keys.isBlank()) "" else "keys=$keys",
            )
            result
        } catch (error: Throwable) {
            StudioLog.add("tool $name err ${System.currentTimeMillis() - started}ms", "E", "tool", error.message.orEmpty())
            throw error
        }
    }

    private fun ok(text: String) = CallToolResult(listOf(TextContent(text)))
    private fun err(text: String) = CallToolResult(listOf(TextContent(text)), isError = true)
    private fun JsonObject?.str(key: String): String? = this?.get(key)?.jsonPrimitive?.contentOrNull
    private fun JsonObject?.int(key: String): Int? = this?.get(key)?.jsonPrimitive?.intOrNull
    private fun JsonObject?.bool(key: String): Boolean? = this?.get(key)?.jsonPrimitive?.booleanOrNull

    private fun stringProp(description: String) = buildJsonObject { put("type", "string"); put("description", description) }
    private fun schema(props: Map<String, String>, required: List<String>) = ToolSchema(properties = buildJsonObject { props.forEach { (k, v) -> put(k, stringProp(v)) } }, required = required)
    private fun arraySchema(name: String, description: String) = ToolSchema(properties = buildJsonObject { putJsonObject(name) { put("type", "array"); putJsonObject("items") { put("type", "string") }; put("description", description) } }, required = listOf(name))
    private fun fetchSchema() = ToolSchema(properties = buildJsonObject {
        put("url", stringProp("HTTP/HTTPS URL")); put("method", stringProp("GET/POST/HEAD")); put("body", stringProp("请求体")); put("charset", stringProp("可选编码"));
        putJsonObject("timeoutSec") { put("type", "integer"); put("description", "5..120 秒") }
    }, required = listOf("url"))
    private fun checkSourceSchema() = ToolSchema(properties = buildJsonObject {
        put("source", stringProp("BookSource JSON"))
        put("searchKey", stringProp("可选搜索关键词"))
        put("detailUrl", stringProp("可选详情 URL"))
        put("tocUrl", stringProp("可选目录 URL"))
        put("contentUrl", stringProp("可选正文 URL"))
    }, required = listOf("source"))

    companion object {
        private const val VERIFY_MESSAGE = "请通过系统通知或 MCP 页顶部横幅，在应用内完成站点验证"
        private val HIDDEN_ARG_KEYS = setOf("token", "cookie", "authorization", "html", "source", "js", "markdown", "body", "project", "header", "password")
    }
}