package com.mina.legadostudio.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mina.legadostudio.StudioApplication
import com.mina.legadostudio.data.db.ProjectEntity
import com.mina.legadostudio.domain.SourceCatalog
import com.mina.legadostudio.domain.SourceGroup
import com.mina.legadostudio.export.ReaderCatalog
import com.mina.legadostudio.ui.theme.GlassCard
import com.mina.legadostudio.ui.theme.GlassTopBar
import com.mina.legadostudio.ui.theme.studioBottomInset
import com.mina.legadostudio.ui.theme.studioTopInset
import com.mina.legadostudio.ui.theme.LocalStudioHaze
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
fun SourcesScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as StudioApplication
    val haze = LocalStudioHaze.current
    val projects by app.projects.observe().collectAsState(initial = emptyList())
    val groups = remember(projects) { SourceCatalog.groupByDomain(projects) }
    val scope = rememberCoroutineScope()
    var notice by remember { mutableStateOf("") }
    var chooser by remember { mutableStateOf<Pair<String, List<ReaderCatalog.App>>?>(null) }
    var pendingDelete by remember { mutableStateOf<ProjectEntity?>(null) }
    var expanded by remember { mutableStateOf(setOf<String>()) }

    fun show(message: String) {
        notice = message
        toastNotice(context, message)
    }

    fun importSource(sourceJson: String) {
        launchReaderImport(context, sourceJson, onNeedChooser = { json, apps -> chooser = json to apps }, onNotice = ::show)
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize().then(if (haze != null) Modifier.hazeSource(haze) else Modifier),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 72.dp + studioTopInset(), bottom = 108.dp + studioBottomInset()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                GlassCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("本地书源库", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (projects.isEmpty()) "暂无记录。外部 Agent 调用 save_source 后将按站点域名分组列出。"
                            else "按站点域名分组，组内按保存时间倒序。同一 URL 默认只保留一条成品；下一轮修复时 Agent 才会追加新版本。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (notice.isNotBlank()) {
                            Text(notice, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            items(groups, key = { it.domain }) { group ->
                DomainSourceGroup(
                    group = group,
                    expanded = group.domain in expanded,
                    onToggle = {
                        expanded = if (group.domain in expanded) expanded - group.domain else expanded + group.domain
                    },
                    onImport = { importSource(it.sourceJson) },
                    onDelete = { pendingDelete = it },
                )
            }
        }
        GlassTopBar("书源", modifier = Modifier.align(Alignment.TopCenter))
    }
    chooser?.let { (json, apps) ->
        ReaderChooserDialog(
            json = json,
            apps = apps,
            onDismiss = { chooser = null },
            onPick = { sourceJson, packageName ->
                chooser = null
                startReaderImport(context, sourceJson, packageName, ::show)
            },
        )
    }
    pendingDelete?.let { project ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除书源") },
            text = { Text("将从工坊本地库移除「${project.name.ifBlank { project.id }}」（${formatTime(project.updatedAt)}），不影响同域名其他版本及已导入阅读客户端的副本。此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    val target = project
                    pendingDelete = null
                    scope.launch {
                        runCatching { app.projects.delete(listOf(target.id)) }
                            .onSuccess { show("已删除 1 条书源") }
                            .onFailure { show(it.message.orEmpty()) }
                    }
                }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun DomainSourceGroup(
    group: SourceGroup,
    expanded: Boolean,
    onToggle: () -> Unit,
    onImport: (ProjectEntity) -> Unit,
    onDelete: (ProjectEntity) -> Unit,
) {
    GlassCard {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(group.domain, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${group.items.size} 条记录 · 最近 ${formatTime(group.latest.updatedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        group.latest.name.ifBlank { "未命名书源" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (expanded) "收起" else "展开",
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    HorizontalDivider()
                    group.items.forEach { project ->
                        SourceVersionRow(project, onImport, onDelete)
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceVersionRow(
    project: ProjectEntity,
    onImport: (ProjectEntity) -> Unit,
    onDelete: (ProjectEntity) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(project.name.ifBlank { "未命名书源" }, fontWeight = FontWeight.Medium)
        Text(project.siteUrl.ifBlank { project.id }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(formatTime(project.updatedAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { onImport(project) }) { Text("导入至阅读") }
            TextButton(onClick = { onDelete(project) }) {
                Text("删除", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun formatTime(value: Long): String = DateFormat.getDateTimeInstance().format(Date(value))
