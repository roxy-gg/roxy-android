package gg.roxy.shared.businessLogic

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gg.roxy.chatFullscreen.businessLogic.ChatFullScreenUiState
import gg.roxy.chatFullscreen.businessLogic.ChatMessageUiModel
import gg.roxy.chatFullscreen.businessLogic.ChatPartUiModel
import gg.roxy.chatFullscreen.businessLogic.ToolCallStatus
import gg.roxy.chatFullscreen.businessLogic.ToolCallType
import gg.roxy.chatFullscreen.businessLogic.ToolCallUiModel
import gg.roxy.mainFullscreen.businessLogic.ComputerUiModel
import gg.roxy.mainFullscreen.businessLogic.MainFullScreenUiState
import gg.roxy.mainFullscreen.businessLogic.ProjectUiModel
import gg.roxy.mainFullscreen.businessLogic.SessionUiModel
import gg.roxy.shared.data.RemoteConnectionState
import gg.roxy.shared.data.RemoteEvent
import gg.roxy.shared.data.RemoteSessionInfo
import gg.roxy.shared.data.RemoteStorage
import gg.roxy.shared.data.RemoteWorkspaceClient
import gg.roxy.shared.data.RemoteWorkspaceUtils
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

    private data class SessionChatCache(
        val messages: List<ChatMessageUiModel> = emptyList(),
        val toolCalls: List<ToolCallUiModel> = emptyList(),
        val isRunning: Boolean = false,
    )

    private val sessionCache = mutableMapOf<String, SessionChatCache>()

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
                                    qrFeedbackMessage = null,
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
                            sessionCache.clear()
                            activeSessionId = null
                            val emptyPc = ComputerUiModel(
                                id = "none",
                                name = "No computer connected",
                                status = "Disconnected",
                                isConnected = false,
                            )
                            state.copy(
                                destination = RoxyDestination.Main,
                                main = state.main.copy(
                                    isConnecting = false,
                                    selectedComputer = emptyPc,
                                    computers = emptyList(),
                                    projects = emptyList(),
                                ),
                                chat = state.chat.copy(
                                    sessionTitle = "",
                                    projectName = "",
                                    messages = emptyList(),
                                    toolCalls = emptyList(),
                                    isSyncing = false,
                                ),
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
                val current = sessionCache[event.sessionId] ?: SessionChatCache()
                val allTools = if (event.tools.isNotEmpty()) {
                    event.tools
                } else {
                    event.messages.flatMap { m -> m.parts.filterIsInstance<ChatPartUiModel.Tool>().map { it.tool } }
                }
                sessionCache[event.sessionId] = current.copy(
                    messages = event.messages,
                    toolCalls = allTools,
                )

                if (activeSessionId == null || activeSessionId == event.sessionId) {
                    if (activeSessionId == null) {
                        activeSessionId = event.sessionId
                    }
                    _uiState.update { state ->
                        state.copy(
                            chat = state.chat.copy(
                                messages = event.messages,
                                toolCalls = allTools,
                                isSyncing = false,
                            )
                        )
                    }
                }
            }
            is RemoteEvent.TextDelta -> {
                val current = sessionCache[event.sessionId] ?: SessionChatCache()
                val cachedMessages = current.messages.toMutableList()
                if (cachedMessages.isEmpty() || cachedMessages.last().isUser) {
                    cachedMessages.add(
                        ChatMessageUiModel(
                            id = UUID.randomUUID().toString(),
                            text = event.chunk,
                            isUser = false,
                            parts = listOf(ChatPartUiModel.Text(id = UUID.randomUUID().toString(), text = event.chunk)),
                        )
                    )
                } else {
                    val last = cachedMessages.last()
                    val parts = last.parts.toMutableList()
                    val lastPart = parts.lastOrNull()
                    if (lastPart is ChatPartUiModel.Text) {
                        parts[parts.lastIndex] = lastPart.copy(text = lastPart.text + event.chunk)
                    } else {
                        parts.add(ChatPartUiModel.Text(id = UUID.randomUUID().toString(), text = event.chunk))
                    }
                    cachedMessages[cachedMessages.lastIndex] = last.copy(
                        text = last.text + event.chunk,
                        parts = parts,
                    )
                }
                sessionCache[event.sessionId] = current.copy(messages = cachedMessages)

                if (activeSessionId == null || activeSessionId == event.sessionId) {
                    _uiState.update { state ->
                        state.copy(chat = state.chat.copy(messages = cachedMessages))
                    }
                }
            }
            is RemoteEvent.ToolStarted -> {
                val type = if (event.tool.lowercase() in listOf("read", "write", "edit", "glob", "grep", "file", "read_file", "write_file", "list", "list_dir")) {
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
                val current = sessionCache[event.sessionId] ?: SessionChatCache()
                val cachedMessages = current.messages.toMutableList()
                if (cachedMessages.isEmpty() || cachedMessages.last().isUser) {
                    cachedMessages.add(
                        ChatMessageUiModel(
                            id = UUID.randomUUID().toString(),
                            isUser = false,
                            parts = listOf(ChatPartUiModel.Tool(tool)),
                        )
                    )
                } else {
                    val last = cachedMessages.last()
                    cachedMessages[cachedMessages.lastIndex] = last.copy(
                        parts = last.parts + ChatPartUiModel.Tool(tool)
                    )
                }
                val updatedTools = current.toolCalls + tool
                sessionCache[event.sessionId] = current.copy(
                    messages = cachedMessages,
                    toolCalls = updatedTools,
                )

                if (activeSessionId == null || activeSessionId == event.sessionId) {
                    _uiState.update { state ->
                        state.copy(
                            chat = state.chat.copy(
                                messages = cachedMessages,
                                toolCalls = updatedTools,
                            )
                        )
                    }
                }
            }
            is RemoteEvent.ToolDelta -> {
                val current = sessionCache[event.sessionId] ?: SessionChatCache()
                val updatedTools = current.toolCalls.map { tool ->
                    if (tool.id == event.callId) {
                        tool.copy(detail = tool.detail + event.chunk)
                    } else {
                        tool
                    }
                }
                val cachedMessages = current.messages.map { msg ->
                    if (msg.isUser) msg
                    else {
                        val updatedParts = msg.parts.map { part ->
                            if (part is ChatPartUiModel.Tool && part.tool.id == event.callId) {
                                part.copy(tool = part.tool.copy(detail = part.tool.detail + event.chunk))
                            } else part
                        }
                        msg.copy(parts = updatedParts)
                    }
                }
                sessionCache[event.sessionId] = current.copy(
                    messages = cachedMessages,
                    toolCalls = updatedTools,
                )

                if (activeSessionId == null || activeSessionId == event.sessionId) {
                    _uiState.update { state ->
                        state.copy(
                            chat = state.chat.copy(
                                messages = cachedMessages,
                                toolCalls = updatedTools,
                            )
                        )
                    }
                }
            }
            is RemoteEvent.ToolEnded -> {
                val current = sessionCache[event.sessionId] ?: SessionChatCache()
                val updatedTools = current.toolCalls.map { tool ->
                    if (tool.id == event.callId) {
                        tool.copy(
                            status = ToolCallStatus.Complete,
                            detail = if (event.output.isNotBlank()) event.output else tool.detail,
                        )
                    } else {
                        tool
                    }
                }
                val cachedMessages = current.messages.map { msg ->
                    if (msg.isUser) msg
                    else {
                        val updatedParts = msg.parts.map { part ->
                            if (part is ChatPartUiModel.Tool && part.tool.id == event.callId) {
                                part.copy(
                                    tool = part.tool.copy(
                                        status = ToolCallStatus.Complete,
                                        detail = if (event.output.isNotBlank()) event.output else part.tool.detail,
                                    )
                                )
                            } else part
                        }
                        msg.copy(parts = updatedParts)
                    }
                }
                sessionCache[event.sessionId] = current.copy(
                    messages = cachedMessages,
                    toolCalls = updatedTools,
                )

                if (activeSessionId == null || activeSessionId == event.sessionId) {
                    _uiState.update { state ->
                        state.copy(
                            chat = state.chat.copy(
                                messages = cachedMessages,
                                toolCalls = updatedTools,
                            )
                        )
                    }
                }
            }
            is RemoteEvent.TurnChanged -> {
                val current = sessionCache[event.sessionId] ?: SessionChatCache()
                val currentMessages = current.messages.toMutableList()
                if (event.userText != null && (currentMessages.isEmpty() || currentMessages.last().text != event.userText)) {
                    currentMessages.add(
                        ChatMessageUiModel(
                            id = UUID.randomUUID().toString(),
                            text = event.userText,
                            isUser = true,
                        )
                    )
                }

                if (event.inFlightTools.isNotEmpty() || event.inFlightText != null) {
                    val inFlightParts = mutableListOf<ChatPartUiModel>()
                    event.inFlightTools.forEach { tool ->
                        inFlightParts.add(ChatPartUiModel.Tool(tool))
                    }
                    if (event.inFlightText != null) {
                        inFlightParts.add(
                            ChatPartUiModel.Text(
                                id = UUID.randomUUID().toString(),
                                text = event.inFlightText,
                            )
                        )
                    }

                    if (currentMessages.isEmpty() || currentMessages.last().isUser) {
                        currentMessages.add(
                            ChatMessageUiModel(
                                id = UUID.randomUUID().toString(),
                                isUser = false,
                                parts = inFlightParts,
                            )
                        )
                    } else {
                        val last = currentMessages.last()
                        currentMessages[currentMessages.lastIndex] = last.copy(parts = inFlightParts)
                    }
                }

                val currentTools = if (event.inFlightTools.isNotEmpty()) {
                    val existingIds = current.toolCalls.map { it.id }.toSet()
                    current.toolCalls + event.inFlightTools.filterNot { it.id in existingIds }
                } else {
                    current.toolCalls
                }

                sessionCache[event.sessionId] = current.copy(
                    isRunning = event.isRunning,
                    messages = currentMessages,
                    toolCalls = currentTools,
                )

                if (activeSessionId == null || activeSessionId == event.sessionId) {
                    _uiState.update { state ->
                        state.copy(
                            chat = state.chat.copy(
                                isRunning = event.isRunning,
                                messages = currentMessages,
                                toolCalls = currentTools,
                            )
                        )
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

    fun showConnectDialog(prefilledToken: String? = null, prefilledPin: String? = null) {
        val token = prefilledToken ?: storage.savedToken ?: ""
        val pin = prefilledPin ?: storage.savedPin ?: ""
        _uiState.update { state ->
            state.copy(
                main = state.main.copy(
                    isConnectingDialogVisible = true,
                    isComputerMenuExpanded = false,
                    connectionError = null,
                    prefilledToken = token,
                    prefilledPin = pin,
                    qrFeedbackMessage = null,
                )
            )
        }
    }

    fun dismissConnectDialog() {
        // Aborts the in-flight attempt; the connectionState collector owns
        // isConnecting and clears it when Disconnected arrives.
        if (_uiState.value.main.isConnecting) {
            remoteClient.disconnect()
        }
        _uiState.update { state ->
            state.copy(
                main = state.main.copy(
                    isConnectingDialogVisible = false,
                    connectionError = null,
                    qrFeedbackMessage = null,
                )
            )
        }
    }

    fun onQrCodeScanned(scannedText: String) {
        val parsed = RemoteWorkspaceUtils.parseQrPairing(scannedText)
        if (parsed == null || parsed.token.isBlank()) {
            _uiState.update { state ->
                state.copy(
                    main = state.main.copy(
                        isConnectingDialogVisible = true,
                        connectionError = "Invalid QR code: no Roxy connection token found.",
                        qrFeedbackMessage = null,
                    )
                )
            }
            return
        }

        if (parsed.pin?.length == 6) {
            _uiState.update { state ->
                state.copy(
                    main = state.main.copy(
                        isConnectingDialogVisible = true,
                        prefilledToken = parsed.token,
                        prefilledPin = parsed.pin,
                        qrFeedbackMessage = "QR paired! Connecting to desktop...",
                        connectionError = null,
                    )
                )
            }
            connectRemote(parsed.token, parsed.pin)
        } else {
            _uiState.update { state ->
                state.copy(
                    main = state.main.copy(
                        isConnectingDialogVisible = true,
                        prefilledToken = parsed.token,
                        prefilledPin = "",
                        qrFeedbackMessage = "QR code scanned! Enter the 6-digit PIN shown on your PC.",
                        connectionError = null,
                    )
                )
            }
        }
    }

    fun onScanError(errorMessage: String) {
        _uiState.update { state ->
            state.copy(
                main = state.main.copy(
                    connectionError = "QR Scanner error: $errorMessage"
                )
            )
        }
    }

    fun connectRemote(tokenOrUrl: String, pin: String) {
        remoteClient.connect(tokenOrUrl, pin)
    }

    fun disconnectRemote() {
        sessionCache.clear()
        activeSessionId = null
        storage.clear()
        remoteClient.disconnect()
        _uiState.update { state ->
            val emptyPc = ComputerUiModel(
                id = "none",
                name = "No computer connected",
                status = "Disconnected",
                isConnected = false,
            )
            state.copy(
                destination = RoxyDestination.Main,
                main = state.main.copy(
                    selectedComputer = emptyPc,
                    computers = emptyList(),
                    projects = emptyList(),
                    isConnecting = false,
                    connectionError = null,
                ),
                chat = state.chat.copy(
                    sessionTitle = "",
                    projectName = "",
                    messages = emptyList(),
                    toolCalls = emptyList(),
                    isSyncing = false,
                ),
            )
        }
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

        val cached = sessionCache[sessionId]
        val hasCache = cached != null && (cached.messages.isNotEmpty() || cached.toolCalls.isNotEmpty())

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
                    messages = cached?.messages ?: emptyList(),
                    toolCalls = cached?.toolCalls ?: emptyList(),
                    isRunning = cached?.isRunning ?: false,
                    isSyncing = !hasCache,
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

        val activeId = activeSessionId

        _uiState.update { state ->
            state.copy(
                chat = state.chat.copy(
                    composerText = "",
                    messages = state.chat.messages + userMessage,
                    isRunning = true,
                )
            )
        }

        if (activeId != null) {
            val cached = sessionCache[activeId] ?: SessionChatCache()
            sessionCache[activeId] = cached.copy(
                messages = cached.messages + userMessage,
                isRunning = true,
            )
        }

        remoteClient.sendPrompt(currentText)
    }

    fun toggleToolCall(toolCallId: String) {
        val activeId = activeSessionId
        if (activeId != null) {
            val cached = sessionCache[activeId]
            if (cached != null) {
                val updatedTools = cached.toolCalls.map { tool ->
                    if (tool.id == toolCallId) tool.copy(isExpanded = !tool.isExpanded) else tool
                }
                val updatedMessages = cached.messages.map { msg ->
                    if (msg.isUser) msg
                    else {
                        val updatedParts = msg.parts.map { part ->
                            if (part is ChatPartUiModel.Tool && part.tool.id == toolCallId) {
                                part.copy(tool = part.tool.copy(isExpanded = !part.tool.isExpanded))
                            } else part
                        }
                        msg.copy(parts = updatedParts)
                    }
                }
                sessionCache[activeId] = cached.copy(
                    messages = updatedMessages,
                    toolCalls = updatedTools,
                )
            }
        }

        _uiState.update { state ->
            val updatedMessages = state.chat.messages.map { msg ->
                if (msg.isUser) msg
                else {
                    val updatedParts = msg.parts.map { part ->
                        if (part is ChatPartUiModel.Tool && part.tool.id == toolCallId) {
                            part.copy(tool = part.tool.copy(isExpanded = !part.tool.isExpanded))
                        } else part
                    }
                    msg.copy(parts = updatedParts)
                }
            }
            state.copy(
                chat = state.chat.copy(
                    messages = updatedMessages,
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
        id = "none",
        name = "No computer connected",
        status = "Disconnected",
        isConnected = false,
    )

    return RoxyAppUiState(
        destination = RoxyDestination.Main,
        main = MainFullScreenUiState(
            selectedComputer = computer,
            computers = emptyList(),
            projects = emptyList(),
        ),
        chat = ChatFullScreenUiState(
            sessionTitle = "",
            projectName = "",
            messages = emptyList(),
            toolCalls = emptyList(),
            isSyncing = false,
        ),
    )
}
