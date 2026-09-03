package gg.roxy.chatFullscreen.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import gg.roxy.chatFullscreen.businessLogic.ToolCallStatus
import gg.roxy.chatFullscreen.businessLogic.ToolCallType
import gg.roxy.chatFullscreen.businessLogic.ToolCallUiModel
import gg.roxy.shared.styles.RoxyMonoFontFamily
import gg.roxy.shared.styles.roxyColors

@Composable
fun ToolCallCard(
    toolCall: ToolCallUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.roxyColors
    val toolIcon = when (toolCall.type) {
        ToolCallType.File -> Icons.Rounded.Description
        ToolCallType.Terminal -> Icons.Rounded.Terminal
    }

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = colors.surface2,
        contentColor = colors.text,
        border = BorderStroke(1.dp, colors.edge),
        tonalElevation = 0.dp,
    ) {
        Column {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = if (toolCall.isExpanded) "Collapse tool call" else "Expand tool call",
                    modifier = Modifier
                        .size(17.dp)
                        .rotate(if (toolCall.isExpanded) 90f else 0f),
                    tint = colors.textSubtle,
                )
                Spacer(Modifier.width(9.dp))
                Surface(
                    modifier = Modifier.size(30.dp),
                    shape = MaterialTheme.shapes.small,
                    color = colors.elevated,
                    border = BorderStroke(1.dp, colors.edge),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = toolIcon,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = colors.textMuted,
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = toolCall.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.text,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = toolCall.title,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = RoxyMonoFontFamily),
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (toolCall.status == ToolCallStatus.Complete) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Complete",
                        modifier = Modifier.size(16.dp),
                        tint = colors.success,
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(7.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = colors.accent,
                        content = {},
                    )
                }
            }

            if (toolCall.isExpanded) {
                HorizontalDivider(color = colors.border)
                Text(
                    text = toolCall.detail,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = RoxyMonoFontFamily),
                    color = colors.textMuted,
                )
            }
        }
    }
}

@Composable
fun ToolCallStack(
    toolCalls: List<ToolCallUiModel>,
    onToolCallClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        toolCalls.forEach { toolCall ->
            ToolCallCard(
                toolCall = toolCall,
                onClick = { onToolCallClick(toolCall.id) },
            )
        }
    }
}
