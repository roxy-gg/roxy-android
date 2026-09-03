package gg.roxy

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import gg.roxy.remote.RemoteConnectionState
import gg.roxy.remote.RemoteEvent
import gg.roxy.remote.RemoteSessionInfo
import gg.roxy.remote.RemoteStorage
import gg.roxy.remote.RemoteWorkspaceClient
import gg.roxy.remote.RemoteWorkspaceUtils
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
class RoxyAppViewModel(
    private val remoteClient: RemoteWorkspaceClient,
    private val storage: RemoteStorage,
    private val externalScope: CoroutineScope? = null,
) : ViewModel() {

    @Inject
    constructor(
        remoteClient: RemoteWorkspaceClient,
        storage: RemoteStorage,
    ) : this(remoteClient, storage, null)

    private val scope: CoroutineScope
        get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow(initialUiState())
    val uiState: StateFlow<RoxyAppUiState> = _uiState.asStateFlow()

    private var activeSessionId: String? = null

    init {
        observeRemote()
        val savedToken = storage.savedToken
        val savedPin = storage.savedPin
        if (!savedToken.isNullOrBlank() && !savedPin.isNullOrBlank()) {
            remoteClient.connect(savedToken, savedPin)
        }
    }

    private fun observeRemote() {
        scope.launch {
            remoteClient.connectionState.collect { connectionState ->
                _uiState.update { state ->
                    when (connectionState) {
                        is RemoteConnectionState.Connecting -> {
                            state.copy(
                                main = state.main.copy(
                                    isConnecting = true,
                                    connectionError = null,
                                    selectedComputer = state.main.selectedComputer.copy(
                                        status = "Connecting...",
                                        isConnected = false,
                                    ),
                                )
                            )
                        }
                        is RemoteConnectionState.Connected -> {
                            val pc = ComputerUiModel(
                                id = "pc-remote",
                                name = connectionState.hostInfo,
                                status = "Connected",
                                isConnected = true,
                            )
                            state.copy(
                                main = state.main.copy(
                                    selectedComputer = pc,
                                    computers = listOf(pc),
                                    isConnecting = false,
                                    isConnectingDialogVisible = false,
                                    connectionError = null,
                                )
                            )
                        }
                        is RemoteConnectionState.Error -> {
                            state.copy(
                                main = state.main.copy(
                                    isConnecting = false,
                                    connectionError = connectionState.message,
                                    selectedComputer = state.main.selectedComputer.copy(
                                        status = "Connection error",
                                        isConnected = false,
                                    ),
                                )
                            )
                        }
                        is RemoteConnectionState.Disconnected -> {
                            state.copy(
                                main = state.main.copy(
                                    isConnecting = false,
                                    selectedComputer = state.main.selectedComputer.copy(
                                        status = "Disconnected",
                                        isConnected = false,
                                    ),
                                )
                            )
                        }
                    }
                }
            }
        }

        scope.launch {
            remoteClient.events.collect { event ->
                handleRemoteEvent(event)
            }
        }
    }

    private fun handleRemoteEvent(event: RemoteEvent) {
        when (event) {
            is RemoteEvent.SessionsReceived -> {
                val projects = mapSessionsToProjects(event.sessions, event.currentId)
                _uiState.update { state ->
                    state.copy(main = state.main.copy(projects = projects))
                }
            }
            is RemoteEvent.SnapshotReceived -> {
                if (activeSessionId == null || activeSessionId == event.sessionId) {
                    activeSessionId = event.sessionId
                    _uiState.update { state ->
                        state.copy(
                            chat = state.chat.copy(
                                messages = event.messages,
                                toolCalls = event.tools,
                            )
                        )
                    }
                }
            }
            is RemoteEvent.TextDelta -> {
                if (activeSessionId == null || activeSessionId == event.sessionId) {
                    _uiState.update { state ->
                        val messages = state.chat.messages.toMutableList()
                        if (messages.isEmpty() || messages.last().isUser) {
                            messages.add(
                                ChatMessageUiModel(
                                    id = UUID.randomUUID().toString(),
                                    text = event.chunk,
                                    isUser = false,
                                )
                            )
                        } else {
                            val last = messages.last()
                            messages[messages.lastIndex] = last.copy(text = last.text + event.chunk)
                        }
                        state.copy(chat = state.chat.copy(messages = messages))
                    }
                }
            }
            is RemoteEvent.ToolStarted -> {
                if (activeSessionId == null || activeSessionId == event.sessionId) {
                    _uiState.update { state ->
                        val type = if (event.tool.lowercase() in listOf("read", "write", "edit", "glob", "grep", "file")) {
                            ToolCallType.File
                        } else {
                            ToolCallType.Terminal
                        }
                        val tool = ToolCallUiModel(
                            id = event.callId,
                            type = type,
                            name = event.tool,
                            title = event.title,
                            detail = "",
                            status = ToolCallStatus.Running,
                            isExpanded = false,
                        )
                        state.copy(chat = state.chat.copy(toolCalls = state.chat.toolCalls + tool))
                    }
                }
            }
            is RemoteEvent.ToolDelta -> {
                if (activeSessionId == null || activeSessionId == event.sessionId) {
                    _uiState.update { state ->
                        val updatedTools = state.chat.toolCalls.map { tool ->
                            if (tool.id == event.callId) {
                                tool.copy(detail = tool.detail + event.chunk)
                            } else {
                                tool
                            }
                        }
                        state.copy(chat = state.chat.copy(toolCalls = updatedTools))
                    }
                }
            }
            is RemoteEvent.ToolEnded -> {
                if (activeSessionId == null || activeSessionId == event.sessionId) {
                    _uiState.update { state ->
                        val updatedTools = state.chat.toolCalls.map { tool ->
                            if (tool.id == event.callId) {
                                tool.copy(
                                    status = ToolCallStatus.Complete,
                                    detail = if (event.output.isNotBlank()) event.output else tool.detail,
                                )
                            } else {
                                tool
                            }
                        }
                        state.copy(chat = state.chat.copy(toolCalls = updatedTools))
                    }
                }
            }
            is RemoteEvent.TurnChanged -> {
                if (activeSessionId == null || activeSessionId == event.sessionId) {
                    _uiState.update { state ->
                        var currentMessages = state.chat.messages
                        if (event.userText != null && (currentMessages.isEmpty() || currentMessages.last().text != event.userText)) {
                            currentMessages = currentMessages + ChatMessageUiModel(
                                id = UUID.randomUUID().toString(),
                                text = event.userText,
                                isUser = true,
                            )
                        }
                        state.copy(chat = state.chat.copy(isRunning = event.isRunning, messages = currentMessages))
                    }
                }
            }
            is RemoteEvent.ErrorReceived -> {
                _uiState.update { state ->
                    state.copy(
                        main = state.main.copy(connectionError = event.message)
                    )
                }
            }
        }
    }

    private fun mapSessionsToProjects(
        sessions: List<RemoteSessionInfo>,
        activeSessionId: String,
    ): List<ProjectUiModel> {
        val grouped = sessions.groupBy { it.project.ifBlank { "Main Workspace" } }
        return grouped.map { (projectName, sessionList) ->
            ProjectUiModel(
                id = "project-$projectName",
                name = projectName,
                sessions = sessionList.map { session ->
                    SessionUiModel(
                        id = session.id,
                        title = session.title.ifBlank { "Untitled Session" },
                        summary = if (session.messageCount > 0) "${session.messageCount} messages" else (session.cwd ?: "Remote workspace"),
                        updatedAt = RemoteWorkspaceUtils.formatRelativeTime(session.updatedAt),
                        isActive = session.id == activeSessionId,
                    )
                }
            )
        }
    }

    fun showConnectDialog() {
        _uiState.update { state ->
            state.copy(
                main = state.main.copy(
                    isConnectingDialogVisible = true,
                    isComputerMenuExpanded = false,
                    connectionError = null,
                )
            )
        }
    }

    fun dismissConnectDialog() {
        _uiState.update { state ->
            state.copy(
                main = state.main.copy(
                    isConnectingDialogVisible = false,
                    connectionError = null,
                )
            )
        }
    }

    fun connectRemote(tokenOrUrl: String, pin: String) {
        remoteClient.connect(tokenOrUrl, pin)
    }

    fun disconnectRemote() {
        storage.clear()
        remoteClient.disconnect()
    }

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
        activeSessionId = sessionId
        remoteClient.switchSession(sessionId)

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
                    messages = emptyList(),
                    toolCalls = emptyList(),
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
        val currentText = _uiState.value.chat.composerText.trim()
        if (currentText.isBlank()) return

        val userMessage = ChatMessageUiModel(
            id = UUID.randomUUID().toString(),
            text = currentText,
            isUser = true,
        )

        _uiState.update { state ->
            state.copy(
                chat = state.chat.copy(
                    composerText = "",
                    messages = state.chat.messages + userMessage,
                    isRunning = true,
                )
            )
        }

        remoteClient.sendPrompt(currentText)
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

    fun getInitialToken(): String = storage.savedToken ?: ""
    fun getInitialPin(): String = storage.savedPin ?: ""
}

private fun initialUiState(): RoxyAppUiState {
    val computer = ComputerUiModel(
        id = "computer-1",
        name = "Computer #1",
        status = "Disconnected (Tap to connect)",
        isConnected = false,
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
                            summary = "Connect your PC to sync sessions",
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
