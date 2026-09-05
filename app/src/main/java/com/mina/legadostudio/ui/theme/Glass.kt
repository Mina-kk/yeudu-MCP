package com.mina.legadostudio.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.CupertinoMaterials
import dev.chrisbanes.haze.materials.HazeMaterials

/**
 * iOS 简约白玻璃组件库。
 * 真实高斯模糊由 haze 提供（CupertinoMaterials.thin），API < 31 时 haze 自动退化为雾面遮罩。
 * 不使用全屏 blur，只模糊顶栏窄条与悬浮底栏，GPU 开销可控。
 */

/** 全局 Haze 状态：各屏幕滚动列表注册为 hazeSource，顶栏/底栏施加 hazeEffect。 */
val LocalStudioHaze = compositionLocalOf<HazeState?> { null }

/** 提供全局 Haze 状态。 */
@Composable
fun ProvideStudioHaze(content: @Composable () -> Unit) {
    val state = remember { HazeState() }
    CompositionLocalProvider(LocalStudioHaze provides state, content = content)
}

/** 顶栏/底栏模糊材质。 */
@Composable
private fun glassHazeStyle(): HazeStyle =
    if (isSystemInDarkTheme()) HazeMaterials.ultraThin() else CupertinoMaterials.thin()

/** 对顶栏/底栏施加模糊；无 HazeState 时退化为雾面纯色。 */
@Composable
fun Modifier.glassBar(): Modifier {
    val haze = LocalStudioHaze.current
    return if (haze != null) {
        hazeEffect(state = haze, style = glassHazeStyle())
    } else {
        background(MaterialTheme.colorScheme.surface.copy(alpha = if (isSystemInDarkTheme()) 0.86f else 0.92f))
    }
}

/** 发丝分割线颜色。 */
@Composable
fun hairlineColor(): Color =
    if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.14f)
    else Color.Black.copy(alpha = 0.08f)

/** 文本框统一白底 + 发丝描边。 */
@Composable
fun studioFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
)

/** 玻璃面填充色：亮色近白，暗色极薄白雾。 */
@Composable
fun glassFill(): Color =
    if (isSystemInDarkTheme()) Color(0xFF1C1C1E).copy(alpha = 0.78f)
    else Color.White.copy(alpha = 0.86f)

/** 玻璃卡片：半透明白 + 0.5dp 发丝描边 + 柔和投影。 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    fill: Color = glassFill(),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val stroke = BorderStroke(0.5.dp, hairlineColor())
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = fill, border = stroke, shadowElevation = 6.dp) {
            Column { content() }
        }
    } else {
        Surface(modifier = modifier, shape = shape, color = fill, border = stroke, shadowElevation = 6.dp) {
            Column { content() }
        }
    }
}

/**
 * iOS 风格顶栏：标题居中，左侧可选 ‹ 返回，底部发丝分割线。
 * 覆盖式使用：内容从其下方滚过，经 haze 实时模糊。
 */
@Composable
fun GlassTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth().glassBar()) {
        Box(Modifier.fillMaxWidth().height(56.dp)) {
            if (onBack != null) {
                Text(
                    "‹ 返回",
                    Modifier.align(Alignment.CenterStart)
                        .clickable(onClick = onBack)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(
                title,
                Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            if (actions != null) {
                Row(
                    Modifier.align(Alignment.CenterEnd).padding(end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(hairlineColor()))
    }
}

/** 底栏项。 */
data class StudioTab(val route: String, val label: String, val icon: ImageVector)

/** 悬浮胶囊底栏（毛玻璃）。选中色 180ms 动画。 */
@Composable
fun GlassTabBar(
    tabs: List<StudioTab>,
    current: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.padding(horizontal = 18.dp, vertical = 12.dp).fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = glassFill(),
        border = BorderStroke(0.5.dp, hairlineColor()),
        shadowElevation = 10.dp,
    ) {
        val haze = LocalStudioHaze.current
        Row(
            Modifier.fillMaxWidth()
                .then(if (haze != null) Modifier.hazeEffect(state = haze, style = glassHazeStyle()) else Modifier)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            tabs.forEach { tab ->
                val selected = tab.route == current
                val tint by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(180),
                    label = "tabTint",
                )
                Column(
                    Modifier.clickable { onSelect(tab.route) }.padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(tab.icon, contentDescription = tab.label, tint = tint, modifier = Modifier.size(22.dp))
                    Text(tab.label, color = tint, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

/** 圆角色块图标座，用于列表与卡片头部。 */
@Composable
fun TonalIconBox(
    icon: ImageVector,
    container: Color,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Int = 46,
) {
    Box(
        modifier.size(size.dp).background(container, MaterialTheme.shapes.small),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint)
    }
}