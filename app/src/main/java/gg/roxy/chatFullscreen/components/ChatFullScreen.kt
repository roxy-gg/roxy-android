package gg.roxy.chatFullscreen.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import gg.roxy.chatFullscreen.businessLogic.ChatFullScreenUiState
import gg.roxy.chatFullscreen.businessLogic.ChatMessageUiModel
import gg.roxy.chatFullscreen.businessLogic.ToolCallStatus
import gg.roxy.chatFullscreen.businessLogic.ToolCallType
import gg.roxy.chatFullscreen.businessLogic.ToolCallUiModel
import gg.roxy.shared.styles.RoxyTheme
import gg.roxy.shared.styles.roxyColors

@Composable
fun ChatFullScreen(
    uiState: ChatFullScreenUiState,
    onBackClick: () -> Unit,
    onComposerChange: (String) -> Unit,
    onComposerSubmit: () -> Unit,
    onToolCallClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.roxyColors
    BackHandler(onBack = onBackClick)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .safeDrawingPadding()
            .imePadding(),
    ) {
        ChatHeader(
            sessionTitle = uiState.sessionTitle,
            projectName = uiState.projectName,
            isRunning = uiState.isRunning,
            isSyncing = uiState.isSyncing,
            onBackClick = onBackClick,
        )
        HorizontalDivider(color = colors.border)

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, top = 28.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (uiState.isSyncing && uiState.messages.isEmpty() && uiState.toolCalls.isEmpty()) {
                item(key = "syncing-indicator") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = colors.accent,
                                strokeWidth = 2.5.dp,
                            )
                            Text(
                                text = "Syncing with desktop...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textMuted,
                            )
                        }
                    }
                }
            } else if (uiState.messages.isEmpty() && uiState.toolCalls.isEmpty()) {
                item(key = "empty-session") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No messages yet. Send a prompt to get started.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textMuted,
                        )
                    }
                }
            } else {
                uiState.messages.forEach { message ->
                    item(key = message.id) {
                        if (message.isUser) {
                            Box(
                                modifier = Modifier
                                    .widthIn(max = 720.dp)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.large,
                                    color = colors.surface2,
                                    border = BorderStroke(1.dp, colors.edge),
                                ) {
                                    Text(
                                        text = message.text,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = colors.text,
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = message.text,
                                modifier = Modifier
                                    .widthIn(max = 720.dp)
                                    .fillMaxWidth(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.text,
                            )
                        }
                    }
                }

                if (uiState.toolCalls.isNotEmpty()) {
                    item(key = "tool-calls") {
                        ToolCallStack(
                            toolCalls = uiState.toolCalls,
                            onToolCallClick = onToolCallClick,
                            modifier = Modifier
                                .widthIn(max = 720.dp)
                                .fillMaxWidth(),
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bg)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            ChatComposer(
                text = uiState.composerText,
                onTextChange = onComposerChange,
                onSubmit = onComposerSubmit,
                modifier = Modifier.widthIn(max = 720.dp),
            )
        }
    }
}

@Composable
fun ChatHeader(
    sessionTitle: String,
    projectName: String,
    isRunning: Boolean = false,
    isSyncing: Boolean = false,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.roxyColors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 6.dp, top = 5.dp, end = 16.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back to sessions",
                tint = colors.textMuted,
            )
        }
        Spacer(Modifier.width(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = sessionTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = colors.textSubtle,
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = projectName,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (isRunning) {
            Surface(
                modifier = Modifier.size(8.dp),
                shape = CircleShape,
                color = colors.accent,
                content = {},
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Thinking...",
                style = MaterialTheme.typography.labelSmall,
                color = colors.accent,
            )
        } else if (isSyncing) {
            Surface(
                modifier = Modifier.size(8.dp),
                shape = CircleShape,
                color = colors.textSubtle,
                content = {},
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Syncing...",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSubtle,
            )
        } else {
            Text(
                text = "Roxy",
                style = MaterialTheme.typography.labelMedium,
                color = colors.textSubtle,
            )
        }
    }
}

private val ChatPreviewState = ChatFullScreenUiState(
    sessionTitle = "Remote Session",
    projectName = "roxy-android",
    messages = emptyList(),
    toolCalls = emptyList(),
)

@Preview(name = "Chat - Dark", showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun ChatFullScreenDarkPreview() {
    RoxyTheme(darkTheme = true) {
        ChatFullScreen(
            uiState = ChatPreviewState,
            onBackClick = { },
            onComposerChange = { },
            onComposerSubmit = { },
            onToolCallClick = { },
        )
    }
}

@Preview(name = "Chat - Light", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun ChatFullScreenLightPreview() {
    RoxyTheme(darkTheme = false) {
        ChatFullScreen(
            uiState = ChatPreviewState,
            onBackClick = { },
            onComposerChange = { },
            onComposerSubmit = { },
            onToolCallClick = { },
        )
    }
}
