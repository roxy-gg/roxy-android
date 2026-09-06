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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import gg.roxy.chatFullscreen.businessLogic.ChatFullScreenUiState
import gg.roxy.chatFullscreen.businessLogic.ChatMessageUiModel
import gg.roxy.chatFullscreen.businessLogic.ChatPartUiModel
import gg.roxy.chatFullscreen.businessLogic.ToolCallStatus
import gg.roxy.chatFullscreen.businessLogic.ToolCallType
import gg.roxy.chatFullscreen.businessLogic.ToolCallUiModel
import gg.roxy.shared.styles.RoxyTheme
import gg.roxy.shared.styles.roxyColors
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

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

        val listState = rememberLazyListState()

        // Opening a session must always land at the bottom. Keyed on the session
        // rather than on the message list, because a snapshot that arrives after
        // the cached content is structurally equal and would not retrigger it.
        LaunchedEffect(uiState.sessionTitle, uiState.projectName) {
            // Content streams in over several layout passes (markdown and code
            // blocks resize once measured), so keep re-pinning until the total
            // extent stops moving instead of scrolling on the first pass only.
            snapshotFlow { listState.layoutInfo.totalItemsCount }
                .filter { it > 0 }
                .first()

            var previous: Pair<Int, Int>? = null
            repeat(MAX_SETTLE_FRAMES) {
                val info = listState.layoutInfo
                listState.scrollToItem(info.totalItemsCount - 1, scrollOffset = Int.MAX_VALUE)

                val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()
                val current = listState.layoutInfo.totalItemsCount to
                    ((last?.offset ?: 0) + (last?.size ?: 0))
                if (current == previous) return@LaunchedEffect
                previous = current
                withFrameNanos { }
            }
        }

        // While streaming, follow the tail only if the user has not scrolled away.
        LaunchedEffect(uiState.messages, uiState.toolCalls) {
            // Read before suspending: layoutInfo still describes the pre-update
            // layout, so this answers "was the user pinned to the bottom before
            // this change?". Reading it after the new content is laid out would
            // always report false and disable autoscroll entirely.
            if (listState.canScrollForward) return@LaunchedEffect

            val itemCount = snapshotFlow { listState.layoutInfo.totalItemsCount }
                .filter { it > 0 }
                .first()
            // Large offset lands at the bottom of the last item even when it is
            // taller than the viewport.
            listState.scrollToItem(itemCount - 1, scrollOffset = Int.MAX_VALUE)
        }

        LazyColumn(
            state = listState,
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
                    if (message.isUser) {
                        item(key = message.id) {
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
                        }
                    } else {
                        // Assistant message: walk parts in chronological order, exactly like desktop
                        if (message.parts.isNotEmpty()) {
                            message.parts.forEach { part ->
                                when (part) {
                                    is ChatPartUiModel.Text -> {
                                        item(key = part.id) {
                                            MarkdownText(
                                                markdown = part.text,
                                                modifier = Modifier
                                                    .widthIn(max = 720.dp)
                                                    .fillMaxWidth(),
                                            )
                                        }
                                    }
                                    is ChatPartUiModel.Reasoning -> {
                                        item(key = part.id) {
                                            ReasoningCard(
                                                reasoning = part,
                                                modifier = Modifier
                                                    .widthIn(max = 720.dp)
                                                    .fillMaxWidth(),
                                            )
                                        }
                                    }
                                    is ChatPartUiModel.Tool -> {
                                        item(key = part.id) {
                                            ToolCallCard(
                                                toolCall = part.tool,
                                                onClick = { onToolCallClick(part.tool.id) },
                                                modifier = Modifier
                                                    .widthIn(max = 720.dp)
                                                    .fillMaxWidth(),
                                            )
                                        }
                                    }
                                }
                            }
                        } else if (message.text.isNotBlank()) {
                            item(key = message.id) {
                                MarkdownText(
                                    markdown = message.text,
                                    modifier = Modifier
                                        .widthIn(max = 720.dp)
                                        .fillMaxWidth(),
                                )
                            }
                        }
                    }
                }

                // Fallback for tools not associated with an existing message part
                val inPartsToolIds = uiState.messages.flatMap { it.parts }.filterIsInstance<ChatPartUiModel.Tool>().map { it.tool.id }.toSet()
                val orphanTools = uiState.toolCalls.filterNot { it.id in inPartsToolIds }
                if (orphanTools.isNotEmpty()) {
                    item(key = "orphan-tool-calls") {
                        ToolCallStack(
                            toolCalls = orphanTools,
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

/** Upper bound on layout passes waited for when pinning a freshly opened chat. */
private const val MAX_SETTLE_FRAMES = 10

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
