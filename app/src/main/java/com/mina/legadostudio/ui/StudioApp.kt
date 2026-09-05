package com.mina.legadostudio.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mina.legadostudio.ui.screens.LogsScreen
import com.mina.legadostudio.ui.screens.McpStatusScreen
import com.mina.legadostudio.ui.screens.SkillsScreen
import com.mina.legadostudio.ui.screens.SourcesScreen
import com.mina.legadostudio.ui.screens.VerificationCenterScreen
import com.mina.legadostudio.ui.theme.GlassTabBar
import com.mina.legadostudio.ui.theme.ProvideStudioHaze
import com.mina.legadostudio.ui.theme.StudioTab
import com.mina.legadostudio.ui.theme.StudioTheme

private val tabs = listOf(
    StudioTab("mcp", "MCP", Icons.Outlined.Link),
    StudioTab("sources", "书源", Icons.Outlined.MenuBook),
    StudioTab("skills", "技能", Icons.Outlined.Extension),
    StudioTab("logs", "日志", Icons.AutoMirrored.Outlined.ReceiptLong),
)

@Composable
fun StudioApp(initialJobId: String? = null, initialRoute: String? = null, deepLinkNonce: Int = 0, onExit: () -> Unit) {
    StudioTheme {
        ProvideStudioHaze {
            val nav = rememberNavController()
            val context = LocalContext.current
            var confirmExit by remember { mutableStateOf(false) }
            var stopMcpOnExit by remember { mutableStateOf(false) }
            LaunchedEffect(initialRoute, deepLinkNonce) {
                if (!initialRoute.isNullOrBlank()) nav.navigate(initialRoute)
            }
            val backEntry by nav.currentBackStackEntryAsState()
            val route = backEntry?.destination?.route.orEmpty()
            BackHandler(route == "mcp") { confirmExit = true }
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                NavHost(
                    nav,
                    startDestination = "mcp",
                    modifier = Modifier.fillMaxSize(),
                    enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 12 } },
                    exitTransition = { fadeOut(tween(220)) },
                    popEnterTransition = { fadeIn(tween(220)) },
                    popExitTransition = { fadeOut(tween(220)) + slideOutHorizontally(tween(220)) { it / 12 } },
                ) {
                    composable("mcp") { McpStatusScreen(onOpenVerification = { nav.navigate("verification") }) }
                    composable("sources") { SourcesScreen() }
                    composable("skills") { SkillsScreen() }
                    composable("logs") { LogsScreen() }
                    composable("verification") { VerificationCenterScreen(onBack = { nav.popBackStack() }) }
                }
                if (route in tabs.map { it.route }) {
                    GlassTabBar(
                        tabs = tabs,
                        current = route,
                        onSelect = { target ->
                            if (route != target) nav.navigate(target) {
                                popUpTo("mcp") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
            if (confirmExit) AlertDialog(
                onDismissRequest = { confirmExit = false },
                title = { Text("退出应用？") },
                text = {
                    Column {
                        Text("退出后默认保持 MCP 服务运行，外部 Agent 仍可调用。")
                        Row {
                            Checkbox(stopMcpOnExit, { stopMcpOnExit = it })
                            Text("同时停止 MCP 服务", Modifier.padding(top = 12.dp))
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (stopMcpOnExit) com.mina.legadostudio.service.McpService.stop(context)
                        onExit()
                    }) { Text("退出") }
                },
                dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("取消") } },
            )
        }
    }
}