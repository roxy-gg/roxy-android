package gg.roxy.shared.businessLogic

import gg.roxy.chatFullscreen.businessLogic.ChatMessageUiModel
import gg.roxy.chatFullscreen.businessLogic.ChatPartUiModel
import gg.roxy.chatFullscreen.businessLogic.ToolCallStatus
import gg.roxy.chatFullscreen.businessLogic.ToolCallType
import gg.roxy.chatFullscreen.businessLogic.ToolCallUiModel
import gg.roxy.shared.PAIRING_PIN_LENGTH
import gg.roxy.shared.data.RemoteConnectionState
import gg.roxy.shared.data.RemoteEvent
import gg.roxy.shared.data.RemoteSessionInfo
import gg.roxy.shared.data.RemoteStorage
import gg.roxy.shared.data.RemoteWorkspaceClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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
    var acceptPrompts = true
    var promptCount = 0
    var abortCount = 0
    var acceptAbort = true

    fun setConnection(state: RemoteConnectionState) { _connectionState.value = state }

    override fun connect(rawTokenOrUrl: String, pin: String) {
        _connectionState.value = RemoteConnectionState.Connected("Test PC")
    }

    override fun sendPrompt(text: String): Boolean {
        if (!acceptPrompts) return false
        lastPromptSent = text
        promptCount++
        return true
    }

    override fun switchSession(sessionId: String) {
        lastSwitchedSession = sessionId
    }

    override fun refreshSessions() {}
    override fun abort(): Boolean {
        abortCount++
        return acceptAbort
    }
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
    fun initialStateHasNoDemoComputerOrSessions() {
        val viewModel = createViewModel()
        assertFalse(viewModel.uiState.value.main.selectedComputer.isConnected)
        assertEquals("No computer connected", viewModel.uiState.value.main.selectedComputer.name)
        assertTrue(viewModel.uiState.value.main.computers.isEmpty())
        assertTrue(viewModel.uiState.value.main.projects.isEmpty())
        assertTrue(viewModel.uiState.value.chat.messages.isEmpty())
        assertTrue(viewModel.uiState.value.chat.toolCalls.isEmpty())
    }

    @Test
    fun sessionSelectionAndBackNavigationUpdateTheRootState() {
        val client = FakeRemoteWorkspaceClient()
        val viewModel = createViewModel(client = client)

        client.fakeEvents.tryEmit(
            RemoteEvent.SessionsReceived(
                sessions = listOf(
                    RemoteSessionInfo("sess-1", "Session #1", "Project #1"),
                    RemoteSessionInfo("sess-2", "Session #2", "Project #2"),
                ),
                currentId = "sess-1"
            )
        )

        viewModel.openSession("sess-2")

        assertEquals("sess-2", client.lastSwitchedSession)
        assertEquals(RoxyDestination.Chat, viewModel.uiState.value.destination)
        assertEquals("Project #2", viewModel.uiState.value.chat.projectName)
        assertEquals("Session #2", viewModel.uiState.value.chat.sessionTitle)
        assertTrue(
            viewModel.uiState.value.main.projects
                .flatMap { it.sessions }
                .first { it.id == "sess-2" }
                .isActive,
        )
        assertFalse(
            viewModel.uiState.value.main.projects
                .flatMap { it.sessions }
                .first { it.id == "sess-1" }
                .isActive,
        )

        viewModel.showMainScreen()

        assertEquals(RoxyDestination.Main, viewModel.uiState.value.destination)
    }

    @Test
    fun composerAndToolCallsAreControlledByTheViewModel() {
        val client = FakeRemoteWorkspaceClient()
        val viewModel = createViewModel(client = client)
        client.connect("tok", "123456")
        client.fakeEvents.tryEmit(RemoteEvent.SnapshotReceived("sess-1", emptyList(), emptyList()))
        client.fakeEvents.tryEmit(RemoteEvent.TurnChanged("sess-1", false))

        client.fakeEvents.tryEmit(
            RemoteEvent.ToolStarted(
                sessionId = "sess-1",
                callId = "tool-1",
                tool = "read",
                title = "shared/theme.ts"
            )
        )

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
        val client = FakeRemoteWorkspaceClient()
        val viewModel = createViewModel(client = client)
        client.connect("tok", "123456")

        viewModel.setComputerMenuExpanded(true)
        assertTrue(viewModel.uiState.value.main.isComputerMenuExpanded)

        viewModel.selectComputer("pc-remote")
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


    @Test
    fun turnChangedKeepsStreamingPartIdsStableAndPreservesReasoning() {
        val client = FakeRemoteWorkspaceClient()
        val viewModel = createViewModel(client = client)

        client.fakeEvents.tryEmit(
            RemoteEvent.SnapshotReceived(
                sessionId = "sess-remote-1",
                messages = listOf(ChatMessageUiModel(id = "user-1", text = "Explain", isUser = true)),
                tools = emptyList(),
            )
        )

        client.fakeEvents.tryEmit(
            RemoteEvent.TurnChanged(
                sessionId = "sess-remote-1",
                isRunning = true,
                inFlightParts = listOf(
                    ChatPartUiModel.Reasoning(id = "turn-reasoning-0", text = "Thinking"),
                    ChatPartUiModel.Text(id = "turn-text-1", text = "Hel"),
                ),
            )
        )
        val firstAssistant = viewModel.uiState.value.chat.messages.last()
        val firstParts = firstAssistant.parts

        client.fakeEvents.tryEmit(
            RemoteEvent.TurnChanged(
                sessionId = "sess-remote-1",
                isRunning = true,
                inFlightParts = listOf(
                    ChatPartUiModel.Reasoning(id = "turn-reasoning-0", text = "Thinking more"),
                    ChatPartUiModel.Text(id = "turn-text-1", text = "Hello"),
                ),
            )
        )
        val secondAssistant = viewModel.uiState.value.chat.messages.last()
        val secondParts = secondAssistant.parts

        assertEquals(firstAssistant.id, secondAssistant.id)
        assertEquals(firstParts.map { it.id }, secondParts.map { it.id })
        assertTrue(secondParts[0] is ChatPartUiModel.Reasoning)
        assertEquals("Thinking more", (secondParts[0] as ChatPartUiModel.Reasoning).text)
        assertEquals("Hello", (secondParts[1] as ChatPartUiModel.Text).text)
    }
    @Test
    fun qrCodeScannedWithPinAutomaticallyConnects() {
        val client = FakeRemoteWorkspaceClient()
        val viewModel = createViewModel(client = client)

        val qrContent = "https://roxy.gg/remote#k=test_jwt_guest_token&pin=654321"
        viewModel.onQrCodeScanned(qrContent)

        assertEquals("test_jwt_guest_token", viewModel.uiState.value.main.prefilledToken)
        assertEquals("654321", viewModel.uiState.value.main.prefilledPin)
        assertFalse(viewModel.uiState.value.main.isConnectingDialogVisible)
        assertEquals("Connected", viewModel.uiState.value.main.selectedComputer.status)
        assertTrue(viewModel.uiState.value.main.selectedComputer.isConnected)
    }

    @Test
    fun qrCodeScannedWithoutPinPrefillsTokenAndAsksForPin() {
        val client = FakeRemoteWorkspaceClient()
        val viewModel = createViewModel(client = client)

        val qrContent = "https://roxy.gg/remote#k=token_without_pin"
        viewModel.onQrCodeScanned(qrContent)

        assertEquals("token_without_pin", viewModel.uiState.value.main.prefilledToken)
        assertEquals("", viewModel.uiState.value.main.prefilledPin)
        assertTrue(viewModel.uiState.value.main.isConnectingDialogVisible)
        assertEquals("QR code scanned! Enter the $PAIRING_PIN_LENGTH-digit PIN shown on your PC.", viewModel.uiState.value.main.qrFeedbackMessage)
    }

    @Test
    fun navigatingBackAndReopeningPreservesMessagesAndToolsFromRamCache() {
        val client = FakeRemoteWorkspaceClient()
        val viewModel = createViewModel(client = client)

        client.fakeEvents.tryEmit(
            RemoteEvent.SessionsReceived(
                sessions = listOf(
                    RemoteSessionInfo("sess-1", "Session #1", "Project #1"),
                ),
                currentId = "sess-1"
            )
        )

        viewModel.openSession("sess-1")

        // Populate with snapshot
        client.fakeEvents.tryEmit(
            RemoteEvent.SnapshotReceived(
                sessionId = "sess-1",
                messages = listOf(
                    ChatMessageUiModel(id = "msg-1", text = "Hello Roxy", isUser = true),
                    ChatMessageUiModel(id = "msg-2", text = "Hello! How can I help?", isUser = false),
                ),
                tools = listOf(
                    ToolCallUiModel(
                        id = "tool-1",
                        type = ToolCallType.File,
                        name = "read",
                        title = "README.md",
                        detail = "Content",
                        status = ToolCallStatus.Complete,
                    )
                )
            )
        )

        assertEquals(2, viewModel.uiState.value.chat.messages.size)
        assertEquals(1, viewModel.uiState.value.chat.toolCalls.size)
        assertFalse(viewModel.uiState.value.chat.isSyncing)

        // Navigate back to Main
        viewModel.showMainScreen()
        assertEquals(RoxyDestination.Main, viewModel.uiState.value.destination)

        // Re-open sess-1: Must NOT blank out messages or tools!
        viewModel.openSession("sess-1")
        assertEquals(RoxyDestination.Chat, viewModel.uiState.value.destination)
        assertEquals(2, viewModel.uiState.value.chat.messages.size)
        assertEquals("Hello Roxy", viewModel.uiState.value.chat.messages[0].text)
        assertEquals("Hello! How can I help?", viewModel.uiState.value.chat.messages[1].text)
        assertEquals(1, viewModel.uiState.value.chat.toolCalls.size)
        assertEquals("tool-1", viewModel.uiState.value.chat.toolCalls[0].id)
        assertFalse(viewModel.uiState.value.chat.isSyncing)
    }

    @Test
    fun snapshotReceivedWhileOnMainScreenIsCachedAndRenderedImmediatelyOnOpen() {
        val client = FakeRemoteWorkspaceClient()
        val viewModel = createViewModel(client = client)

        client.fakeEvents.tryEmit(
            RemoteEvent.SessionsReceived(
                sessions = listOf(
                    RemoteSessionInfo("sess-1", "Session #1", "Project #1"),
                    RemoteSessionInfo("sess-2", "Session #2", "Project #2"),
                ),
                currentId = "sess-1"
            )
        )

        // Phone receives a snapshot for sess-2 while the user is still looking at MainScreen
        client.fakeEvents.tryEmit(
            RemoteEvent.SnapshotReceived(
                sessionId = "sess-2",
                messages = listOf(
                    ChatMessageUiModel(id = "msg-from-main", text = "Cached message", isUser = false)
                ),
                tools = listOf(
                    ToolCallUiModel(
                        id = "tool-main",
                        type = ToolCallType.Terminal,
                        name = "bash",
                        title = "ls -la",
                        detail = "",
                        status = ToolCallStatus.Running
                    )
                )
            )
        )

        // User now taps sess-2
        viewModel.openSession("sess-2")

        assertEquals(RoxyDestination.Chat, viewModel.uiState.value.destination)
        assertEquals("Session #2", viewModel.uiState.value.chat.sessionTitle)
        assertEquals(1, viewModel.uiState.value.chat.messages.size)
        assertEquals("Cached message", viewModel.uiState.value.chat.messages.first().text)
        assertEquals(1, viewModel.uiState.value.chat.toolCalls.size)
        assertEquals("tool-main", viewModel.uiState.value.chat.toolCalls.first().id)
        assertFalse(viewModel.uiState.value.chat.isSyncing)
    }

    @Test
    fun disconnectRemoteClearsRamSessionCache() {
        val client = FakeRemoteWorkspaceClient()
        val viewModel = createViewModel(client = client)

        client.fakeEvents.tryEmit(
            RemoteEvent.SessionsReceived(
                sessions = listOf(
                    RemoteSessionInfo("sess-1", "Session #1", "Project #1"),
                ),
                currentId = "sess-1"
            )
        )

        viewModel.openSession("sess-1")
        client.fakeEvents.tryEmit(
            RemoteEvent.SnapshotReceived(
                sessionId = "sess-1",
                messages = listOf(
                    ChatMessageUiModel(id = "msg-1", text = "Sensitive data", isUser = true)
                ),
                tools = emptyList()
            )
        )

        assertEquals(1, viewModel.uiState.value.chat.messages.size)

        // Disconnect
        viewModel.disconnectRemote()

        assertEquals(RoxyDestination.Main, viewModel.uiState.value.destination)
        assertTrue(viewModel.uiState.value.chat.messages.isEmpty())

        // Reconnect fresh
        client.fakeEvents.tryEmit(
            RemoteEvent.SessionsReceived(
                sessions = listOf(
                    RemoteSessionInfo("sess-1", "Session #1", "Project #1"),
                ),
                currentId = "sess-1"
            )
        )

        viewModel.openSession("sess-1")
        assertTrue(viewModel.uiState.value.chat.messages.isEmpty())
        assertTrue(viewModel.uiState.value.chat.isSyncing)
    }

    @Test
    fun offlineSendPreservesDraftAndTranscript() {
        val client = FakeRemoteWorkspaceClient()
        val viewModel = connectedSession(client)
        viewModel.updateComposer("Keep this draft")
        client.setConnection(RemoteConnectionState.Error("Network lost"))

        viewModel.submitComposer()

        assertEquals("Keep this draft", viewModel.uiState.value.chat.composerText)
        assertTrue(viewModel.uiState.value.chat.messages.isEmpty())
        assertEquals(0, client.promptCount)
        assertFalse(viewModel.uiState.value.chat.canSubmit)
        assertEquals("Network lost", viewModel.uiState.value.chat.errorMessage)
    }

    @Test
    fun socketRejectionPreservesDraftWithoutAnOptimisticMessage() {
        val client = FakeRemoteWorkspaceClient()
        val viewModel = connectedSession(client)
        client.acceptPrompts = false
        viewModel.updateComposer("Do not lose this")

        viewModel.submitComposer()

        assertEquals("Do not lose this", viewModel.uiState.value.chat.composerText)
        assertTrue(viewModel.uiState.value.chat.messages.isEmpty())
        assertFalse(viewModel.uiState.value.chat.isRunning)
        assertTrue(viewModel.uiState.value.chat.errorMessage!!.contains("not sent"))
    }

    @Test
    fun reconnectPreservesDraftAndRequestsTheSameSessionWithoutResending() {
        val client = FakeRemoteWorkspaceClient()
        val storage = FakeRemoteStorage().apply { savedToken = "tok"; savedPin = "123456" }
        val viewModel = createViewModel(client, storage)
        client.fakeEvents.tryEmit(RemoteEvent.SnapshotReceived("sess-1", emptyList(), emptyList()))
        viewModel.updateComposer("A pending draft")
        client.setConnection(RemoteConnectionState.Disconnected)

        viewModel.reconnectRemote()

        assertEquals("sess-1", client.lastSwitchedSession)
        assertEquals("A pending draft", viewModel.uiState.value.chat.composerText)
        assertTrue(viewModel.uiState.value.chat.isSyncing)
        assertFalse(viewModel.uiState.value.chat.canSubmit)
        assertEquals(0, client.promptCount)
    }

    @Test
    fun sessionSwitchRestoresEachSessionsDraft() {
        val client = FakeRemoteWorkspaceClient()
        val viewModel = connectedSession(client)
        client.fakeEvents.tryEmit(RemoteEvent.SessionsReceived(listOf(
            RemoteSessionInfo("sess-1", "One", "Project"),
            RemoteSessionInfo("sess-2", "Two", "Project"),
        ), "sess-1"))
        viewModel.updateComposer("Draft one")
        viewModel.openSession("sess-2")
        viewModel.updateComposer("Draft two")
        viewModel.openSession("sess-1")
        assertEquals("Draft one", viewModel.uiState.value.chat.composerText)
        viewModel.openSession("sess-2")
        assertEquals("Draft two", viewModel.uiState.value.chat.composerText)
    }

    @Test
    fun onlyOnePromptCanBeOutstandingOrRunning() {
        val client = FakeRemoteWorkspaceClient()
        val viewModel = connectedSession(client)
        viewModel.updateComposer("First")
        viewModel.submitComposer()
        viewModel.updateComposer("Second")
        viewModel.submitComposer()
        assertEquals(1, client.promptCount)
        assertEquals("Second", viewModel.uiState.value.chat.composerText)
        assertTrue(viewModel.uiState.value.chat.isAwaitingResponse)

        client.fakeEvents.tryEmit(RemoteEvent.TurnChanged("sess-1", true))
        assertEquals("First", viewModel.uiState.value.chat.messages.single().text)
        viewModel.submitComposer()
        assertEquals(1, client.promptCount)
        client.fakeEvents.tryEmit(RemoteEvent.TurnChanged("sess-1", false))
        viewModel.submitComposer()
        assertEquals(2, client.promptCount)
    }

    @Test
    fun remoteErrorIsVisibleAndRefreshRequiresBothSnapshotAndTurn() {
        val client = FakeRemoteWorkspaceClient()
        val viewModel = connectedSession(client)
        viewModel.updateComposer("Draft")
        client.fakeEvents.tryEmit(RemoteEvent.ErrorReceived("Provider unavailable"))
        assertEquals("Provider unavailable", viewModel.uiState.value.chat.errorMessage)
        assertFalse(viewModel.uiState.value.chat.canSubmit)

        viewModel.reconnectRemote()
        client.fakeEvents.tryEmit(RemoteEvent.SnapshotReceived("sess-1", emptyList(), emptyList()))
        assertFalse(viewModel.uiState.value.chat.canSubmit)
        client.fakeEvents.tryEmit(RemoteEvent.TurnChanged("sess-1", false))
        assertTrue(viewModel.uiState.value.chat.canSubmit)
        assertEquals("Draft", viewModel.uiState.value.chat.composerText)
    }

    @Test
    fun previousSessionsUpdatesCannotEnableSendForNewSession() {
        val client = FakeRemoteWorkspaceClient()
        val viewModel = connectedSession(client)
        client.fakeEvents.tryEmit(RemoteEvent.SessionsReceived(listOf(
            RemoteSessionInfo("sess-1", "One", "Project"),
            RemoteSessionInfo("sess-2", "Two", "Project"),
        ), "sess-1"))
        viewModel.openSession("sess-2")
        viewModel.updateComposer("For session two")
        client.fakeEvents.tryEmit(RemoteEvent.SnapshotReceived("sess-1", emptyList(), emptyList()))
        client.fakeEvents.tryEmit(RemoteEvent.TurnChanged("sess-1", false))
        viewModel.submitComposer()
        assertEquals(0, client.promptCount)
        client.fakeEvents.tryEmit(RemoteEvent.TurnChanged("sess-2", false))
        assertFalse(viewModel.uiState.value.chat.canSubmit)
        client.fakeEvents.tryEmit(RemoteEvent.SnapshotReceived("sess-2", emptyList(), emptyList()))
        assertTrue(viewModel.uiState.value.chat.canSubmit)
    }

    @Test
    fun queuedMobilePromptDoesNotSplitTheCurrentAssistantReply() {
        val client = FakeRemoteWorkspaceClient()
        val viewModel = connectedSession(client)
        client.fakeEvents.tryEmit(RemoteEvent.SnapshotReceived("sess-1", listOf(
            ChatMessageUiModel("assistant", "Current reply"),
        ), emptyList()))
        viewModel.updateComposer("My queued prompt")
        viewModel.submitComposer()
        client.fakeEvents.tryEmit(RemoteEvent.QueueChanged("sess-1", 1))
        client.fakeEvents.tryEmit(RemoteEvent.TextDelta("sess-1", " finished"))
        assertEquals("Current reply finished", viewModel.uiState.value.chat.messages.single().text)
        client.fakeEvents.tryEmit(RemoteEvent.QueueChanged("sess-1", 0))
        client.fakeEvents.tryEmit(RemoteEvent.TurnChanged("sess-1", true, userText = "My queued prompt"))
        client.fakeEvents.tryEmit(RemoteEvent.TextDelta("sess-1", "New reply"))
        assertEquals(listOf("Current reply finished", "My queued prompt", "New reply"),
            viewModel.uiState.value.chat.messages.map { it.text })
        assertFalse(viewModel.uiState.value.chat.isAwaitingResponse)
    }

    @Test
    fun desktopQueueBlocksNewMobilePrompts() {
        val client = FakeRemoteWorkspaceClient()
        val viewModel = connectedSession(client)
        client.fakeEvents.tryEmit(RemoteEvent.QueueChanged("sess-1", 2))
        viewModel.updateComposer("Wait for the queue")
        viewModel.submitComposer()
        assertEquals(0, client.promptCount)
        client.fakeEvents.tryEmit(RemoteEvent.QueueChanged("sess-1", 0))
        assertTrue(viewModel.uiState.value.chat.canSubmit)
    }

    @Test
    fun stopWaitsForHostIdleAndDoesNotEraseTheNextDraft() {
        val client = FakeRemoteWorkspaceClient()
        val viewModel = runningMobileSession(client)
        viewModel.updateComposer("Next question")
        viewModel.stopTurn()
        viewModel.stopTurn()
        assertEquals(1, client.abortCount)
        assertTrue(viewModel.uiState.value.chat.isRunning)
        assertTrue(viewModel.uiState.value.chat.isStopping)
        assertFalse(viewModel.uiState.value.chat.canSubmit)
        client.fakeEvents.tryEmit(RemoteEvent.TurnChanged("other-session", false))
        assertTrue(viewModel.uiState.value.chat.isStopping)
        client.fakeEvents.tryEmit(RemoteEvent.TurnChanged("sess-1", false))
        assertFalse(viewModel.uiState.value.chat.isStopping)
        assertFalse(viewModel.uiState.value.chat.isRunning)
        assertEquals("Next question", viewModel.uiState.value.chat.composerText)
        assertTrue(viewModel.uiState.value.chat.canSubmit)
    }

    @Test
    fun desktopStartedTurnDoesNotOfferAnUnsupportedStop() {
        val client = FakeRemoteWorkspaceClient()
        val viewModel = connectedSession(client)
        client.fakeEvents.tryEmit(RemoteEvent.TurnChanged("sess-1", true, userText = "From desktop"))
        assertFalse(viewModel.uiState.value.chat.canStop)
        viewModel.stopTurn()
        assertEquals(0, client.abortCount)
    }

    @Test
    fun rejectedStopKeepsRunningStateAndShowsRecovery() {
        val client = FakeRemoteWorkspaceClient()
        val viewModel = runningMobileSession(client)
        client.acceptAbort = false
        viewModel.stopTurn()
        assertTrue(viewModel.uiState.value.chat.isRunning)
        assertFalse(viewModel.uiState.value.chat.isStopping)
        assertTrue(viewModel.uiState.value.chat.errorMessage!!.contains("Could not send Stop"))
        client.setConnection(RemoteConnectionState.Error("Offline"))
        viewModel.stopTurn()
        assertEquals(1, client.abortCount)
    }

    @Test
    fun stopTimeoutDoesNotPretendTheHostStopped() = runBlocking {
        val client = FakeRemoteWorkspaceClient()
        val viewModel = runningMobileSession(client, timeoutMs = 50)
        viewModel.stopTurn()
        val state = withTimeout(2000) { viewModel.uiState.first { it.chat.errorMessage != null } }
        assertTrue(state.chat.isRunning)
        assertFalse(state.chat.isStopping)
        assertFalse(state.chat.canSubmit)
        assertTrue(state.chat.errorMessage!!.contains("has not confirmed Stop"))
    }

    @Test
    fun promptTimeoutRequiresRefreshWithoutAutomaticReplay() = runBlocking {
        val client = FakeRemoteWorkspaceClient()
        val viewModel = connectedSession(client, timeoutMs = 50)
        viewModel.updateComposer("Run once")
        viewModel.submitComposer()
        val state = withTimeout(2000) { viewModel.uiState.first { it.chat.errorMessage != null } }
        assertTrue(state.chat.errorMessage!!.contains("has not confirmed this message"))
        assertEquals(1, client.promptCount)
        viewModel.submitComposer()
        assertEquals(1, client.promptCount)
    }

    @Test
    fun unrelatedDesktopTurnDoesNotAcknowledgePendingMobilePrompt() {
        val client = FakeRemoteWorkspaceClient()
        val viewModel = connectedSession(client)
        viewModel.updateComposer("Mobile prompt")
        viewModel.submitComposer()
        client.fakeEvents.tryEmit(RemoteEvent.TurnChanged("sess-1", true, userText = "Desktop prompt"))
        assertTrue(viewModel.uiState.value.chat.isAwaitingResponse)
        assertFalse(viewModel.uiState.value.chat.isMobileTurn)
        assertEquals("Desktop prompt", viewModel.uiState.value.chat.messages.single().text)
    }

    private fun runningMobileSession(client: FakeRemoteWorkspaceClient, timeoutMs: Long = 15_000): RoxyAppViewModel {
        val viewModel = connectedSession(client, timeoutMs)
        viewModel.updateComposer("Start work")
        viewModel.submitComposer()
        client.fakeEvents.tryEmit(RemoteEvent.TurnChanged("sess-1", true))
        assertTrue(viewModel.uiState.value.chat.canStop)
        return viewModel
    }

    private fun connectedSession(client: FakeRemoteWorkspaceClient, timeoutMs: Long = 15_000): RoxyAppViewModel {
        val viewModel = RoxyAppViewModel(client, FakeRemoteStorage(), CoroutineScope(Dispatchers.Unconfined), timeoutMs)
        client.connect("tok", "123456")
        client.fakeEvents.tryEmit(RemoteEvent.SnapshotReceived("sess-1", emptyList(), emptyList()))
        client.fakeEvents.tryEmit(RemoteEvent.TurnChanged("sess-1", false))
        return viewModel
    }

    private fun RoxyAppViewModel.toolCall(id: String): ToolCallUiModel =
        uiState.value.chat.toolCalls.first { it.id == id }
}
