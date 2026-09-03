package gg.roxy.chatFullscreen.businessLogic

import androidx.compose.runtime.Immutable

@Immutable
data class ChatMessageUiModel(
    val id: String,
    val text: String,
)

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
data class ChatFullScreenUiState(
    val sessionTitle: String,
    val projectName: String,
    val messages: List<ChatMessageUiModel>,
    val toolCalls: List<ToolCallUiModel>,
    val composerText: String = "",
)
