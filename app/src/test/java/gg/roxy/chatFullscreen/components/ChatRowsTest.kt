package gg.roxy.chatFullscreen.components

import gg.roxy.chatFullscreen.businessLogic.ChatMessageUiModel
import gg.roxy.chatFullscreen.businessLogic.ChatPartUiModel
import gg.roxy.chatFullscreen.businessLogic.ToolCallStatus
import gg.roxy.chatFullscreen.businessLogic.ToolCallType
import gg.roxy.chatFullscreen.businessLogic.ToolCallUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatRowsTest {
    @Test
    fun buildChatRowsReturnsNewestRenderableRowFirst() {
        val rows = buildChatRows(
            messages = listOf(
                ChatMessageUiModel(id = "user-1", text = "Hello", isUser = true),
                ChatMessageUiModel(
                    id = "assistant-1",
                    parts = listOf(
                        ChatPartUiModel.Reasoning(id = "assistant-1-reasoning-0", text = "Thinking"),
                        ChatPartUiModel.Text(id = "assistant-1-text-1", text = "Answer"),
                    ),
                ),
            ),
            toolCalls = emptyList(),
        )

        assertEquals(
            listOf("assistant-1-text-1", "assistant-1-reasoning-0", "user-1"),
            rows.map { it.key },
        )
    }

    @Test
    fun orphanToolsRenderAtOldestEndOfTranscript() {
        val orphan = ToolCallUiModel(
            id = "orphan-tool",
            type = ToolCallType.Terminal,
            name = "bash",
            title = "Detached tool",
            detail = "Detached output",
            status = ToolCallStatus.Complete,
        )

        val rows = buildChatRows(
            messages = listOf(ChatMessageUiModel(id = "user-1", text = "Hello", isUser = true)),
            toolCalls = listOf(orphan),
        )

        assertEquals("user-1", rows.first().key)
        assertTrue(rows.last() is ChatRow.OrphanTools)
        assertEquals("orphan-tool-calls", rows.last().key)
    }

    @Test
    fun rowKeysStayStableAcrossEquivalentSnapshots() {
        val first = buildChatRows(
            messages = listOf(
                ChatMessageUiModel(
                    id = "assistant-1",
                    parts = listOf(ChatPartUiModel.Text(id = "assistant-1-text-0", text = "Hel")),
                ),
            ),
            toolCalls = emptyList(),
        )
        val second = buildChatRows(
            messages = listOf(
                ChatMessageUiModel(
                    id = "assistant-1",
                    parts = listOf(ChatPartUiModel.Text(id = "assistant-1-text-0", text = "Hello")),
                ),
            ),
            toolCalls = emptyList(),
        )

        assertEquals(first.map { it.key }, second.map { it.key })
    }
}
