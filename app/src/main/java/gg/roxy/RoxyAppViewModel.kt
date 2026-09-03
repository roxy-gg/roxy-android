package gg.roxy

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import gg.roxy.chatFullscreen.businessLogic.ChatFullScreenUiState
import gg.roxy.chatFullscreen.businessLogic.ChatMessageUiModel
import gg.roxy.chatFullscreen.businessLogic.ToolCallStatus
import gg.roxy.chatFullscreen.businessLogic.ToolCallType
import gg.roxy.chatFullscreen.businessLogic.ToolCallUiModel
import gg.roxy.mainFullScreen.businessLogic.ComputerUiModel
import gg.roxy.mainFullScreen.businessLogic.MainFullScreenUiState
import gg.roxy.mainFullScreen.businessLogic.ProjectUiModel
import gg.roxy.mainFullScreen.businessLogic.SessionUiModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class RoxyDestination {
    Main,
    Chat,
}

@Immutable
data class RoxyAppUiState(
    val destination: RoxyDestination,
    val main: MainFullScreenUiState,
    val chat: ChatFullScreenUiState,
)

@HiltViewModel
class RoxyAppViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(initialUiState())
    val uiState: StateFlow<RoxyAppUiState> = _uiState.asStateFlow()

    fun setComputerMenuExpanded(expanded: Boolean) {
        _uiState.update { state ->
            state.copy(main = state.main.copy(isComputerMenuExpanded = expanded))
        }
    }

    fun selectComputer(computerId: String) {
        _uiState.update { state ->
            val computer = state.main.computers.firstOrNull { it.id == computerId }
                ?: return@update state
            state.copy(
                main = state.main.copy(
                    selectedComputer = computer,
                    isComputerMenuExpanded = false,
                ),
            )
        }
    }

    fun openSession(sessionId: String) {
        _uiState.update { state ->
            val project = state.main.projects.firstOrNull { project ->
                project.sessions.any { it.id == sessionId }
            } ?: return@update state
            val session = project.sessions.first { it.id == sessionId }
            val projects = state.main.projects.map { candidate ->
                candidate.copy(
                    sessions = candidate.sessions.map { item ->
                        item.copy(isActive = item.id == sessionId)
                    },
                )
            }
            state.copy(
                destination = RoxyDestination.Chat,
                main = state.main.copy(
                    projects = projects,
                    isComputerMenuExpanded = false,
                ),
                chat = state.chat.copy(
                    sessionTitle = session.title,
                    projectName = project.name,
                    composerText = "",
                ),
            )
        }
    }

    fun showMainScreen() {
        _uiState.update { it.copy(destination = RoxyDestination.Main) }
    }

    fun updateComposer(text: String) {
        _uiState.update { state ->
            state.copy(chat = state.chat.copy(composerText = text))
        }
    }

    fun submitComposer() {
        _uiState.update { state ->
            if (state.chat.composerText.isBlank()) state
            else state.copy(chat = state.chat.copy(composerText = ""))
        }
    }

    fun toggleToolCall(toolCallId: String) {
        _uiState.update { state ->
            state.copy(
                chat = state.chat.copy(
                    toolCalls = state.chat.toolCalls.map { toolCall ->
                        if (toolCall.id == toolCallId) {
                            toolCall.copy(isExpanded = !toolCall.isExpanded)
                        } else {
                            toolCall
                        }
                    },
                ),
            )
        }
    }
}

private fun initialUiState(): RoxyAppUiState {
    val computer = ComputerUiModel(
        id = "computer-1",
        name = "Computer #1",
        status = "Connected",
        isConnected = true,
    )

    return RoxyAppUiState(
        destination = RoxyDestination.Main,
        main = MainFullScreenUiState(
            selectedComputer = computer,
            computers = listOf(computer),
            projects = listOf(
                ProjectUiModel(
                    id = "project-1",
                    name = "Project #1",
                    sessions = listOf(
                        SessionUiModel(
                            id = "project-1-session-1",
                            title = "Session #1",
                            summary = "Building the Android client",
                            updatedAt = "Now",
                            isActive = true,
                        ),
                        SessionUiModel(
                            id = "project-1-session-2",
                            title = "Session #2",
                            summary = "Desktop theme parity",
                            updatedAt = "18m",
                        ),
                        SessionUiModel(
                            id = "project-1-session-3",
                            title = "Session #3",
                            summary = "Compose architecture",
                            updatedAt = "Yesterday",
                        ),
                    ),
                ),
                ProjectUiModel(
                    id = "project-2",
                    name = "Project #2",
                    sessions = listOf(
                        SessionUiModel(
                            id = "project-2-session-1",
                            title = "Session #1",
                            summary = "Remote workspace",
                            updatedAt = "Mon",
                        ),
                    ),
                ),
                ProjectUiModel(
                    id = "project-3",
                    name = "Project #3",
                    sessions = listOf(
                        SessionUiModel(
                            id = "project-3-session-1",
                            title = "Session #1",
                            summary = "Initial setup",
                            updatedAt = "Aug 28",
                        ),
                    ),
                ),
            ),
        ),
        chat = ChatFullScreenUiState(
            sessionTitle = "Session #1",
            projectName = "Project #1",
            messages = listOf(ChatMessageUiModel(id = "message-1", text = "Hi dummy text")),
            toolCalls = listOf(
                ToolCallUiModel(
                    id = "tool-call-1",
                    type = ToolCallType.File,
                    name = "read",
                    title = "shared/theme.ts",
                    detail = "Read the desktop theme token definitions.",
                    status = ToolCallStatus.Complete,
                ),
                ToolCallUiModel(
                    id = "tool-call-2",
                    type = ToolCallType.Terminal,
                    name = "bash",
                    title = "./gradlew assembleDebug",
                    detail = "Build completed successfully.",
                    status = ToolCallStatus.Complete,
                ),
            ),
        ),
    )
}
