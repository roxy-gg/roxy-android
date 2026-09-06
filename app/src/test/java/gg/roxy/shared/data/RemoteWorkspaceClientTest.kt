package gg.roxy.shared.data

import gg.roxy.chatFullscreen.businessLogic.ToolCallStatus
import gg.roxy.chatFullscreen.businessLogic.ToolCallType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryRemoteStorage : RemoteStorage {
    override var savedToken: String? = null
    override var savedPin: String? = null
    override fun clear() {
        savedToken = null
        savedPin = null
    }
}

class RemoteWorkspaceClientTest {

    @Test
    fun snapshotWithTextAndToolPartsParsesBothCorrectly() = runBlocking {
        val storage = MemoryRemoteStorage()
        val client = DefaultRemoteWorkspaceClient(storage)

        val json = """
            {
              "t": "snapshot",
              "sessionId": "session-abc",
              "messages": [
                {
                  "id": "msg-1",
                  "role": "user",
                  "content": "Inspect the files"
                },
                {
                  "id": "msg-2",
                  "role": "assistant",
                  "content": "I checked them.",
                  "parts": [
                    {
                      "type": "tool",
                      "tool": "read",
                      "title": "src/App.kt",
                      "state": "done",
                      "output": "class App",
                      "callId": "call-1"
                    },
                    {
                      "type": "text",
                      "text": "I checked them."
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val deferred = CompletableDeferred<RemoteEvent>()
        val job = launch(Dispatchers.IO) {
            client.events.collect { deferred.complete(it) }
        }
        delay(50)

        client.handleIncomingMessage(json)

        val event = withTimeout(2000) { deferred.await() } as RemoteEvent.SnapshotReceived
        job.cancel()

        assertEquals("session-abc", event.sessionId)
        assertEquals(2, event.messages.size)
        assertEquals("Inspect the files", event.messages[0].text)
        assertTrue(event.messages[0].isUser)
        assertEquals("I checked them.", event.messages[1].text)
        assertFalse(event.messages[1].isUser)

        assertEquals(1, event.tools.size)
        val tool = event.tools[0]
        assertEquals("call-1", tool.id)
        assertEquals("read", tool.name)
        assertEquals("src/App.kt", tool.title)
        assertEquals("class App", tool.detail)
        assertEquals(ToolCallStatus.Complete, tool.status)
        assertEquals(ToolCallType.File, tool.type)
    }

    @Test
    fun snapshotWithAssistantToolOnlyMessageExtractsToolWithoutEmptyText() = runBlocking {
        val storage = MemoryRemoteStorage()
        val client = DefaultRemoteWorkspaceClient(storage)

        val json = """
            {
              "t": "snapshot",
              "sessionId": "session-tools-only",
              "messages": [
                {
                  "id": "msg-tool-only",
                  "role": "assistant",
                  "content": "",
                  "parts": [
                    {
                      "type": "tool",
                      "tool": "bash",
                      "title": "npm test",
                      "state": "running",
                      "output": "PASS test/index.test.ts",
                      "callId": "call-bash-1"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val deferred = CompletableDeferred<RemoteEvent>()
        val job = launch(Dispatchers.IO) {
            client.events.collect { deferred.complete(it) }
        }
        delay(50)

        client.handleIncomingMessage(json)

        val event = withTimeout(2000) { deferred.await() } as RemoteEvent.SnapshotReceived
        job.cancel()

        assertEquals("session-tools-only", event.sessionId)
        assertEquals(1, event.messages.size)
        assertEquals(1, event.messages[0].parts.size)

        assertEquals(1, event.tools.size)
        val tool = event.tools[0]
        assertEquals("call-bash-1", tool.id)
        assertEquals("bash", tool.name)
        assertEquals("npm test", tool.title)
        assertEquals("PASS test/index.test.ts", tool.detail)
        assertEquals(ToolCallStatus.Running, tool.status)
        assertEquals(ToolCallType.Terminal, tool.type)
    }

    @Test
    fun snapshotWithReasoningPartIncludesItInMessageText() = runBlocking {
        val storage = MemoryRemoteStorage()
        val client = DefaultRemoteWorkspaceClient(storage)

        val json = """
            {
              "t": "snapshot",
              "sessionId": "session-reasoning",
              "messages": [
                {
                  "id": "msg-reasoning",
                  "role": "assistant",
                  "content": "",
                  "parts": [
                    {
                      "type": "reasoning",
                      "text": "Thinking through the approach..."
                    },
                    {
                      "type": "text",
                      "text": "Here is the final plan."
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val deferred = CompletableDeferred<RemoteEvent>()
        val job = launch(Dispatchers.IO) {
            client.events.collect { deferred.complete(it) }
        }
        delay(50)

        client.handleIncomingMessage(json)

        val event = withTimeout(2000) { deferred.await() } as RemoteEvent.SnapshotReceived
        job.cancel()

        assertEquals("session-reasoning", event.sessionId)
        assertEquals(1, event.messages.size)
        assertEquals("Thinking through the approach...\n\nHere is the final plan.", event.messages[0].text)
    }

    @Test
    fun turnFrameWithInFlightPartsEmitsLiveTurnState() = runBlocking {
        val storage = MemoryRemoteStorage()
        val client = DefaultRemoteWorkspaceClient(storage)

        val json = """
            {
              "t": "turn",
              "sessionId": "session-in-flight",
              "state": "running",
              "userText": "Run tests",
              "parts": [
                {
                  "type": "tool",
                  "tool": "bash",
                  "title": "gradle test",
                  "state": "running",
                  "callId": "call-live-1"
                },
                {
                  "type": "text",
                  "text": "Starting tests now"
                }
              ]
            }
        """.trimIndent()

        val deferred = CompletableDeferred<RemoteEvent>()
        val job = launch(Dispatchers.IO) {
            client.events.collect { deferred.complete(it) }
        }
        delay(50)

        client.handleIncomingMessage(json)

        val event = withTimeout(2000) { deferred.await() } as RemoteEvent.TurnChanged
        job.cancel()

        assertEquals("session-in-flight", event.sessionId)
        assertTrue(event.isRunning)
        assertEquals("Run tests", event.userText)
        assertEquals("Starting tests now", event.inFlightText)
        assertEquals(1, event.inFlightTools.size)
        assertEquals("call-live-1", event.inFlightTools[0].id)
        assertEquals(ToolCallStatus.Running, event.inFlightTools[0].status)
    }

    @Test
    fun rejectsBlankTokenWithoutLeavingStateConnecting() {
        val client = DefaultRemoteWorkspaceClient(MemoryRemoteStorage())

        client.connect("", "123456")

        val state = client.connectionState.value
        assertTrue(state is RemoteConnectionState.Error)
        assertEquals("Invalid token or link", (state as RemoteConnectionState.Error).message)
    }

    @Test
    fun rejectsShortPinWithoutLeavingStateConnecting() {
        val client = DefaultRemoteWorkspaceClient(MemoryRemoteStorage())

        client.connect("guest_token_abc", "123")

        val state = client.connectionState.value
        assertTrue(state is RemoteConnectionState.Error)
        assertEquals("PIN must be 6 digits", (state as RemoteConnectionState.Error).message)
    }

    @Test
    fun serverErrorBeforeHandshakeSurfacesAsErrorState() {
        val client = DefaultRemoteWorkspaceClient(MemoryRemoteStorage())

        client.handleIncomingMessage("""{"t":"error","message":"Invalid PIN"}""")

        // Must fail loudly: staying in Connecting is what used to hang the app
        // forever on a wrong PIN.
        val state = client.connectionState.value
        assertTrue(state is RemoteConnectionState.Error)
        assertEquals("Invalid PIN", (state as RemoteConnectionState.Error).message)
    }

    @Test
    fun serverErrorAfterHandshakeKeepsConnectionAndEmitsEvent() = runBlocking {
        val client = DefaultRemoteWorkspaceClient(MemoryRemoteStorage())

        client.handleIncomingMessage("""{"t":"hello-ok"}""", "tok", "123456")
        assertTrue(client.connectionState.value is RemoteConnectionState.Connected)

        val deferred = CompletableDeferred<RemoteEvent>()
        val job = launch(Dispatchers.IO) {
            client.events.collect { deferred.complete(it) }
        }
        delay(50)

        client.handleIncomingMessage("""{"t":"error","message":"Tool failed"}""")

        val event = withTimeout(2000) { deferred.await() } as RemoteEvent.ErrorReceived
        job.cancel()

        assertEquals("Tool failed", event.message)
        // An in-session error must not tear down an established connection.
        assertTrue(client.connectionState.value is RemoteConnectionState.Connected)
    }

    @Test
    fun helloOkPersistsCredentialsAndMarksConnected() {
        val storage = MemoryRemoteStorage()
        val client = DefaultRemoteWorkspaceClient(storage)

        client.handleIncomingMessage("""{"t":"hello-ok"}""", "guest_token_xyz", "654321")

        assertTrue(client.connectionState.value is RemoteConnectionState.Connected)
        assertEquals("guest_token_xyz", storage.savedToken)
        assertEquals("654321", storage.savedPin)
    }

    @Test
    fun failedPairingDoesNotPersistCredentials() {
        val storage = MemoryRemoteStorage()
        val client = DefaultRemoteWorkspaceClient(storage)

        client.handleIncomingMessage("""{"t":"error","message":"Invalid PIN"}""", "tok", "000000")

        assertNull(storage.savedToken)
        assertNull(storage.savedPin)
    }

    @Test
    fun retryAfterFailedPairingReachesConnectingAgain() {
        val client = DefaultRemoteWorkspaceClient(MemoryRemoteStorage())

        client.handleIncomingMessage("""{"t":"error","message":"Invalid PIN"}""")
        assertTrue(client.connectionState.value is RemoteConnectionState.Error)

        // Correcting the PIN and reconnecting must leave the error behind; a stale
        // listener used to knock this attempt straight back to Error.
        client.connect("guest_token_abc", "654321")

        assertTrue(client.connectionState.value is RemoteConnectionState.Connecting)

        client.disconnect()
    }

    @Test
    fun disconnectResetsStateToDisconnected() {
        val client = DefaultRemoteWorkspaceClient(MemoryRemoteStorage())

        client.handleIncomingMessage("""{"t":"hello-ok"}""", "tok", "123456")
        assertTrue(client.connectionState.value is RemoteConnectionState.Connected)

        client.disconnect()

        assertTrue(client.connectionState.value is RemoteConnectionState.Disconnected)
    }

    @Test
    fun snapshotEmittedBeforeSubscriptionIsStillDelivered() = runBlocking {
        val client = DefaultRemoteWorkspaceClient(MemoryRemoteStorage())

        // Snapshot lands before anyone collects: this is the race that left the
        // chat showing "No messages yet" despite having history.
        client.handleIncomingMessage(
            """{"t":"snapshot","sessionId":"s1","messages":[
               {"id":"m1","role":"user","content":"hola"}]}"""
        )

        val event = withTimeout(2000) {
            client.events.first { it is RemoteEvent.SnapshotReceived }
        } as RemoteEvent.SnapshotReceived

        assertEquals("s1", event.sessionId)
        assertEquals(1, event.messages.size)
        assertEquals("hola", event.messages[0].text)
    }

    @Test
    fun byeFrameMarksConnectionDisconnected() {
        val client = DefaultRemoteWorkspaceClient(MemoryRemoteStorage())

        client.handleIncomingMessage("""{"t":"hello-ok"}""", "tok", "123456")
        client.handleIncomingMessage("""{"t":"bye"}""")

        assertTrue(client.connectionState.value is RemoteConnectionState.Disconnected)
    }
}
