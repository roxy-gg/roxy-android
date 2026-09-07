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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import gg.roxy.chatFullscreen.businessLogic.ChatFullScreenUiState
import gg.roxy.chatFullscreen.businessLogic.ChatMessageUiModel
import gg.roxy.chatFullscreen.businessLogic.ChatPartUiModel
import gg.roxy.chatFullscreen.businessLogic.ToolCallUiModel
import gg.roxy.shared.styles.RoxyTheme
import gg.roxy.shared.styles.roxyColors

/**
 * One entry of the transcript. Messages are flattened into rows up front so the
 * list has a single, stable index space: the newest row is always index 0, which
 * is what pins the viewport to the bottom of the conversation.
 */
@Immutable
private sealed interface ChatRow {
    val key: String

    @Immutable
    data class UserMessage(override val key: String, val text: String) : ChatRow

    @Immutable
    data class Markdown(override val key: String, val text: String) : ChatRow

    @Immutable
    data class Reasoning(val part: ChatPartUiModel.Reasoning) : ChatRow {
        override val key: String get() = part.id
    }

    @Immutable
    data class Tool(val tool: ToolCallUiModel) : ChatRow {
        override val key: String get() = tool.id
    }

    @Immutable
    data class OrphanTools(val tools: List<ToolCallUiModel>) : ChatRow {
        override val key: String get() = "orphan-tool-calls"
    }
}

/** Flattens the transcript into newest-first order, ready for [LazyColumn]'s `reverseLayout`. */
private fun buildChatRows(
    messages: List<ChatMessageUiModel>,
    toolCalls: List<ToolCallUiModel>,
): List<ChatRow> {
    val rows = mutableListOf<ChatRow>()

    messages.forEach { message ->
        when {
            message.isUser -> rows += ChatRow.UserMessage(message.id, message.text)
            // Assistant turn: walk parts in chronological order, exactly like desktop.
            message.parts.isNotEmpty() -> message.parts.forEach { part ->
                rows += when (part) {
                    is ChatPartUiModel.Text -> ChatRow.Markdown(part.id, part.text)
                    is ChatPartUiModel.Reasoning -> ChatRow.Reasoning(part)
                    is ChatPartUiModel.Tool -> ChatRow.Tool(part.tool)
                }
            }
            message.text.isNotBlank() -> rows += ChatRow.Markdown(message.id, message.text)
        }
    }

    // Fallback for tools not associated with an existing message part.
    val renderedToolIds = rows.filterIsInstance<ChatRow.Tool>().mapTo(mutableSetOf()) { it.tool.id }
    val orphanTools = toolCalls.filterNot { it.id in renderedToolIds }
    if (orphanTools.isNotEmpty()) rows += ChatRow.OrphanTools(orphanTools)

    rows.reverse()
    return rows
}

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

    val rows = remember(uiState.messages, uiState.toolCalls) {
        buildChatRows(uiState.messages, uiState.toolCalls)
    }

    // Each session gets its own scroll position, so opening one starts on its
    // newest row instead of inheriting wherever the previous one was left.
    val listState = key(uiState.sessionId) { rememberLazyListState() }

    // The list is reversed, so the anchor item is the newest row: this reads as
    // "the viewport is resting against the bottom edge".
    val isAtNewestRow by remember(listState) {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }
    }

    // Growing the newest row needs no scrolling at all -- it is the anchor, so
    // streamed markdown and tool output expand upwards while the bottom edge
    // stays put. Only an insertion has to be handled: rows carry stable keys, so
    // the anchor would otherwise follow the previously newest row and leave the
    // incoming one laid out below the viewport.
    val newestRowKey = rows.firstOrNull()?.key
    LaunchedEffect(newestRowKey) {
        // Effects run before this frame's measure pass, so isAtNewestRow still
        // describes the layout as it was before the row arrived: a user who had
        // scrolled up into history is left alone.
        if (isAtNewestRow) listState.requestScrollToItem(0)
    }

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

        if (rows.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (uiState.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = colors.accent,
                            strokeWidth = 2.5.dp,
                        )
                    }
                    Text(
                        text = if (uiState.isSyncing) {
                            "Syncing with desktop..."
                        } else {
                            "No messages yet. Send a prompt to get started."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textMuted,
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                // Paints row 0 against the bottom edge and anchors scrolling
                // there, which is what keeps the newest content on screen.
                reverseLayout = true,
                contentPadding = PaddingValues(start = 20.dp, top = 28.dp, end = 20.dp, bottom = 24.dp),
                // Alignment.Bottom parks a transcript shorter than the viewport
                // on the composer rather than under the header.
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Bottom),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items(rows, key = { it.key }, contentType = { it::class }) { row ->
                    val rowModifier = Modifier
                        .widthIn(max = 720.dp)
                        .fillMaxWidth()
                    when (row) {
                        is ChatRow.UserMessage -> Box(
                            modifier = rowModifier,
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.large,
                                color = colors.surface2,
                                border = BorderStroke(1.dp, colors.edge),
                            ) {
                                Text(
                                    text = row.text,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = colors.text,
                                )
                            }
                        }

                        is ChatRow.Markdown -> MarkdownText(
                            markdown = row.text,
                            modifier = rowModifier,
                        )

                        is ChatRow.Reasoning -> ReasoningCard(
                            reasoning = row.part,
                            modifier = rowModifier,
                        )

                        is ChatRow.Tool -> ToolCallCard(
                            toolCall = row.tool,
                            onClick = { onToolCallClick(row.tool.id) },
                            modifier = rowModifier,
                        )

                        is ChatRow.OrphanTools -> ToolCallStack(
                            toolCalls = row.tools,
                            onToolCallClick = onToolCallClick,
                            modifier = rowModifier,
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
                onSubmit = {
                    onComposerSubmit()
                    // Sending always returns to the newest row, even from deep
                    // in the history. The request applies to the next measure,
                    // by which point the sent message is row 0.
                    listState.requestScrollToItem(0)
                },
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
