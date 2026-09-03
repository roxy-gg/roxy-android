package gg.roxy

import gg.roxy.chatFullscreen.businessLogic.ChatMessageUiModel
import gg.roxy.chatFullscreen.businessLogic.ToolCallStatus
import gg.roxy.chatFullscreen.businessLogic.ToolCallType
import gg.roxy.chatFullscreen.businessLogic.ToolCallUiModel
import gg.roxy.remote.RemoteConnectionState
import gg.roxy.remote.RemoteEvent
import gg.roxy.remote.RemoteSessionInfo
import gg.roxy.remote.RemoteStorage
import gg.roxy.remote.RemoteWorkspaceClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeRemoteWorkspaceClient : RemoteWorkspaceClient {
    private val _connectionState = MutableStateFlow<RemoteConnectionState>(RemoteConnectionState.Disconnected)
    override val connectionState: StateFlow<RemoteConnectionState> = _connectionState.asStateFlow()

    val fakeEvents = MutableSharedFlow<RemoteEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<RemoteEvent> = fakeEvents.asSharedFlow()

    var lastPromptSent: String? = null
    var lastSwitchedSession: String? = null

    override fun connect(rawTokenOrUrl: String, pin: String) {
        _connectionState.value = RemoteConnectionState.Connected("Test PC")
    }

    override fun sendPrompt(text: String) {
        lastPromptSent = text
    }

    override fun switchSession(sessionId: String) {
        lastSwitchedSession = sessionId
    }

    override fun refreshSessions() {}
    override fun abort() {}
    override fun disconnect() {
        _connectionState.value = RemoteConnectionState.Disconnected
    }
}

class FakeRemoteStorage : RemoteStorage {
    override var savedToken: String? = null
    override var savedPin: String? = null
    override fun clear() {
        savedToken = null
        savedPin = null
    }
}

class RoxyAppViewModelTest {
    private fun createViewModel(
        client: RemoteWorkspaceClient = FakeRemoteWorkspaceClient(),
        storage: RemoteStorage = FakeRemoteStorage(),
    ) = RoxyAppViewModel(client, storage, CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun sessionSelectionAndBackNavigationUpdateTheRootState() {
        val client = FakeRemoteWorkspaceClient()
        val viewModel = createViewModel(client = client)

        viewModel.openSession("project-2-session-1")

        assertEquals("project-2-session-1", client.lastSwitchedSession)
        assertEquals(RoxyDestination.Chat, viewModel.uiState.value.destination)
        assertEquals("Project #2", viewModel.uiState.value.chat.projectName)
        assertEquals("Session #1", viewModel.uiState.value.chat.sessionTitle)
        assertTrue(
            viewModel.uiState.value.main.projects
                .flatMap { it.sessions }
                .first { it.id == "project-2-session-1" }
                .isActive,
        )
        assertFalse(
            viewModel.uiState.value.main.projects
                .flatMap { it.sessions }
                .first { it.id == "project-1-session-1" }
                .isActive,
        )

        viewModel.showMainScreen()

        assertEquals(RoxyDestination.Main, viewModel.uiState.value.destination)
    }

    @Test
    fun composerAndToolCallsAreControlledByTheViewModel() {
        val client = FakeRemoteWorkspaceClient()
        val viewModel = createViewModel(client = client)
        val toolCall = viewModel.uiState.value.chat.toolCalls.first()

        viewModel.updateComposer("Port the theme")
        assertEquals("Port the theme", viewModel.uiState.value.chat.composerText)

        viewModel.submitComposer()
        assertEquals("", viewModel.uiState.value.chat.composerText)
        assertEquals("Port the theme", client.lastPromptSent)

        assertFalse(toolCall.isExpanded)
        viewModel.toggleToolCall(toolCall.id)
        assertTrue(viewModel.toolCall(toolCall.id).isExpanded)
    }

    @Test
    fun computerSelectionClosesTheControlledMenu() {
        val viewModel = createViewModel()
        val computerId = viewModel.uiState.value.main.selectedComputer.id

        viewModel.setComputerMenuExpanded(true)
        assertTrue(viewModel.uiState.value.main.isComputerMenuExpanded)

        viewModel.selectComputer(computerId)
        assertFalse(viewModel.uiState.value.main.isComputerMenuExpanded)
    }

    @Test
    fun connectDialogOpensAndCloses() {
        val viewModel = createViewModel()

        assertFalse(viewModel.uiState.value.main.isConnectingDialogVisible)
        viewModel.showConnectDialog()
        assertTrue(viewModel.uiState.value.main.isConnectingDialogVisible)
        viewModel.dismissConnectDialog()
        assertFalse(viewModel.uiState.value.main.isConnectingDialogVisible)
    }

    @Test
    fun remoteEventStreamingUpdatesUiState() {
        val client = FakeRemoteWorkspaceClient()
        val viewModel = createViewModel(client = client)

        // 1. Receive sessions
        client.fakeEvents.tryEmit(
            RemoteEvent.SessionsReceived(
                sessions = listOf(
                    RemoteSessionInfo("sess-remote-1", "Remote Session", "Backend API", updatedAt = 1000L, messageCount = 2)
                ),
                currentId = "sess-remote-1"
            )
        )

        val project = viewModel.uiState.value.main.projects.first()
        assertEquals("Backend API", project.name)
        assertEquals("Remote Session", project.sessions.first().title)

        // 2. Open that session
        viewModel.openSession("sess-remote-1")

        // 3. Receive snapshot
        client.fakeEvents.tryEmit(
            RemoteEvent.SnapshotReceived(
                sessionId = "sess-remote-1",
                messages = listOf(
                    ChatMessageUiModel(id = "msg-1", text = "Can you check logs?", isUser = true)
                ),
                tools = listOf(
                    ToolCallUiModel(
                        id = "tool-1",
                        type = ToolCallType.Terminal,
                        name = "bash",
                        title = "cat logs",
                        detail = "Log output",
                        status = ToolCallStatus.Complete
                    )
                )
            )
        )

        assertEquals(1, viewModel.uiState.value.chat.messages.size)
        assertEquals("Can you check logs?", viewModel.uiState.value.chat.messages.first().text)
        assertTrue(viewModel.uiState.value.chat.messages.first().isUser)
        assertEquals(1, viewModel.uiState.value.chat.toolCalls.size)

        // 4. Stream delta
        client.fakeEvents.tryEmit(RemoteEvent.TextDelta("sess-remote-1", "Checking logs now..."))
        assertEquals(2, viewModel.uiState.value.chat.messages.size)
        assertEquals("Checking logs now...", viewModel.uiState.value.chat.messages[1].text)
        assertFalse(viewModel.uiState.value.chat.messages[1].isUser)

        client.fakeEvents.tryEmit(RemoteEvent.TextDelta("sess-remote-1", " All clear!"))
        assertEquals(2, viewModel.uiState.value.chat.messages.size)
        assertEquals("Checking logs now... All clear!", viewModel.uiState.value.chat.messages[1].text)
    }

    private fun RoxyAppViewModel.toolCall(id: String): ToolCallUiModel =
        uiState.value.chat.toolCalls.first { it.id == id }
}
