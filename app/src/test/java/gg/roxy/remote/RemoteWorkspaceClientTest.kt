package gg.roxy.remote

import gg.roxy.chatFullscreen.businessLogic.ToolCallStatus
import gg.roxy.chatFullscreen.businessLogic.ToolCallType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        // No empty text messages should be created
        assertTrue(event.messages.isEmpty())

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
}
