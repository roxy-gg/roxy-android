package gg.roxy.chatFullscreen.businessLogic

import androidx.compose.runtime.Immutable

enum class ToolCallType {
    File,
    Terminal,
}

enum class ToolCallStatus {
    Complete,
    Running,
}

@Immutable
data class ToolCallUiModel(
    val id: String,
    val type: ToolCallType,
    val name: String,
    val title: String,
    val detail: String,
    val status: ToolCallStatus,
    val isExpanded: Boolean = false,
)

@Immutable
sealed interface ChatPartUiModel {
    val id: String

    @Immutable
    data class Text(
        override val id: String,
        val text: String,
    ) : ChatPartUiModel

    @Immutable
    data class Reasoning(
        override val id: String,
        val text: String,
        val isExpanded: Boolean = false,
    ) : ChatPartUiModel

    @Immutable
    data class Tool(
        val tool: ToolCallUiModel,
    ) : ChatPartUiModel {
        override val id: String get() = tool.id
    }
}

@Immutable
data class ChatMessageUiModel(
    val id: String,
    val text: String = "",
    val isUser: Boolean = false,
    val parts: List<ChatPartUiModel> = emptyList(),
)

@Immutable
data class ChatFullScreenUiState(
    val sessionId: String = "",
    val sessionTitle: String,
    val projectName: String,
    val messages: List<ChatMessageUiModel>,
    val toolCalls: List<ToolCallUiModel> = emptyList(),
    val composerText: String = "",
    val isRunning: Boolean = false,
    val isSyncing: Boolean = false,
)
