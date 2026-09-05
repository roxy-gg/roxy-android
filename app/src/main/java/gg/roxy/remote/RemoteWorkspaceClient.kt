package gg.roxy.remote

import gg.roxy.chatFullscreen.businessLogic.ChatMessageUiModel
import gg.roxy.chatFullscreen.businessLogic.ChatPartUiModel
import gg.roxy.chatFullscreen.businessLogic.ToolCallStatus
import gg.roxy.chatFullscreen.businessLogic.ToolCallType
import gg.roxy.chatFullscreen.businessLogic.ToolCallUiModel
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject

sealed interface RemoteEvent {
    data class SessionsReceived(val sessions: List<RemoteSessionInfo>, val currentId: String) : RemoteEvent
    data class SnapshotReceived(
        val sessionId: String,
        val messages: List<ChatMessageUiModel>,
        val tools: List<ToolCallUiModel>,
    ) : RemoteEvent
    data class TextDelta(val sessionId: String, val chunk: String) : RemoteEvent
    data class ToolStarted(val sessionId: String, val callId: String, val tool: String, val title: String) : RemoteEvent
    data class ToolDelta(val sessionId: String, val callId: String, val chunk: String) : RemoteEvent
    data class ToolEnded(val sessionId: String, val callId: String, val output: String, val ok: Boolean) : RemoteEvent
    data class TurnChanged(
        val sessionId: String,
        val isRunning: Boolean,
        val userText: String? = null,
        val inFlightText: String? = null,
        val inFlightTools: List<ToolCallUiModel> = emptyList(),
    ) : RemoteEvent
    data class ErrorReceived(val message: String) : RemoteEvent
}

interface RemoteWorkspaceClient {
    val connectionState: StateFlow<RemoteConnectionState>
    val events: SharedFlow<RemoteEvent>
    fun connect(rawTokenOrUrl: String, pin: String)
    fun sendPrompt(text: String)
    fun switchSession(sessionId: String)
    fun refreshSessions()
    fun abort()
    fun disconnect()
}

@Singleton
class DefaultRemoteWorkspaceClient @Inject constructor(
    private val storage: RemoteStorage,
) : RemoteWorkspaceClient {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val client: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var activeWebSocket: WebSocket? = null
    private var pendingPin: String = ""
    private var handshakeTimeoutJob: Job? = null
    private var isHandshakeComplete = false

    private val _connectionState = MutableStateFlow<RemoteConnectionState>(RemoteConnectionState.Disconnected)
    override val connectionState: StateFlow<RemoteConnectionState> = _connectionState.asStateFlow()

    private val _events = MutableSharedFlow<RemoteEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<RemoteEvent> = _events.asSharedFlow()

    override fun connect(rawTokenOrUrl: String, pin: String) {
        val token = RemoteWorkspaceUtils.extractGuestToken(rawTokenOrUrl)
        if (token.isBlank()) {
            _connectionState.value = RemoteConnectionState.Error("Invalid token or link")
            return
        }

        val cleanedPin = pin.trim()
        if (cleanedPin.length != 6) {
            _connectionState.value = RemoteConnectionState.Error("PIN must be 6 digits")
            return
        }

        disconnect()
        pendingPin = cleanedPin
        isHandshakeComplete = false
        _connectionState.value = RemoteConnectionState.Connecting

        handshakeTimeoutJob = scope.launch {
            delay(HANDSHAKE_TIMEOUT_MS)
            if (!isHandshakeComplete) {
                failConnection("The PC did not respond. Check the PIN and that Roxy is running on your PC.")
            }
        }

        val wsUrl = "wss://roxy.gg/api/remote/ws?token=${token}"
        val request = Request.Builder().url(wsUrl).build()

        activeWebSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val helloPayload = JSONObject().apply {
                    put("t", "hello")
                    put("pin", pendingPin)
                }
                webSocket.send(helloPayload.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingMessage(text, token, cleanedPin)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                failConnection(t.localizedMessage ?: "Connection failure to PC relay")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (!isHandshakeComplete) {
                    failConnection(
                        reason.takeIf { it.isNotBlank() }
                            ?: "The PC rejected the connection. Verify the 6-digit PIN."
                    )
                } else {
                    handshakeTimeoutJob?.cancel()
                    _connectionState.value = RemoteConnectionState.Disconnected
                }
            }
        })
    }

    private fun failConnection(message: String) {
        handshakeTimeoutJob?.cancel()
        handshakeTimeoutJob = null
        isHandshakeComplete = false
        activeWebSocket = null
        _connectionState.value = RemoteConnectionState.Error(message)
    }

    internal fun handleIncomingMessage(text: String, token: String = "test-token", pin: String = "123456") {
        val json = try {
            JSONObject(text)
        } catch (_: Exception) {
            return
        }

        when (json.optString("t")) {
            "hello-ok" -> {
                handshakeTimeoutJob?.cancel()
                isHandshakeComplete = true
                storage.savedToken = token
                storage.savedPin = pin
                _connectionState.value = RemoteConnectionState.Connected()
            }
            "paired" -> {
                handshakeTimeoutJob?.cancel()
                isHandshakeComplete = true
                _connectionState.value = RemoteConnectionState.Connected()
            }
            "sessions" -> {
                val sessionsArray = json.optJSONArray("sessions") ?: JSONArray()
                val currentId = json.optString("currentId", "")
                val list = mutableListOf<RemoteSessionInfo>()
                for (i in 0 until sessionsArray.length()) {
                    val sObj = sessionsArray.optJSONObject(i) ?: continue
                    list.add(
                        RemoteSessionInfo(
                            id = sObj.optString("id", ""),
                            title = sObj.optString("title", "Untitled Session"),
                            project = sObj.optString("project", "Default Project"),
                            cwd = sObj.optString("cwd").takeIf { it.isNotBlank() },
                            updatedAt = sObj.optLong("updatedAt", 0L),
                            messageCount = sObj.optInt("messageCount", 0),
                        )
                    )
                }
                scope.launch {
                    _events.emit(RemoteEvent.SessionsReceived(list, currentId))
                }
            }
            "snapshot" -> {
                val sessionId = json.optString("sessionId", "")
                val messagesArray = json.optJSONArray("messages") ?: JSONArray()
                val messagesList = mutableListOf<ChatMessageUiModel>()
                val toolsList = mutableListOf<ToolCallUiModel>()

                for (i in 0 until messagesArray.length()) {
                    val mObj = messagesArray.optJSONObject(i) ?: continue
                    val id = mObj.optString("id", UUID.randomUUID().toString())
                    val role = mObj.optString("role", "assistant")
                    val isUser = role == "user"
                    val content = mObj.optString("content", "")
                    val partsArray = mObj.optJSONArray("parts")

                    if (isUser) {
                        if (content.isNotBlank()) {
                            messagesList.add(
                                ChatMessageUiModel(
                                    id = id,
                                    text = content,
                                    isUser = true,
                                    parts = emptyList(),
                                )
                            )
                        }
                    } else {
                        val partsList = mutableListOf<ChatPartUiModel>()
                        val textParts = mutableListOf<String>()

                        if (partsArray != null && partsArray.length() > 0) {
                            for (p in 0 until partsArray.length()) {
                                val partObj = partsArray.optJSONObject(p) ?: continue
                                when (partObj.optString("type")) {
                                    "text" -> {
                                        val partText = partObj.optString("text", "")
                                        if (partText.isNotBlank()) {
                                            partsList.add(ChatPartUiModel.Text(id = "$id-text-$p", text = partText))
                                            textParts.add(partText)
                                        }
                                    }
                                    "reasoning" -> {
                                        val reasoningText = partObj.optString("text", "")
                                        if (reasoningText.isNotBlank()) {
                                            partsList.add(ChatPartUiModel.Reasoning(id = "$id-reasoning-$p", text = reasoningText))
                                            textParts.add(reasoningText)
                                        }
                                    }
                                    "tool" -> {
                                        val toolName = partObj.optString("tool", "tool")
                                        val toolTitle = partObj.optString("title", toolName)
                                        val toolState = partObj.optString("state", "done")
                                        val toolOutput = partObj.optString("output", "")
                                        val callId = partObj.optString("callId", UUID.randomUUID().toString())
                                        val toolType = resolveToolType(toolName)

                                        val toolModel = ToolCallUiModel(
                                            id = callId,
                                            type = toolType,
                                            name = toolName,
                                            title = toolTitle,
                                            detail = toolOutput,
                                            status = if (toolState == "running") ToolCallStatus.Running else ToolCallStatus.Complete,
                                            isExpanded = false,
                                        )
                                        partsList.add(ChatPartUiModel.Tool(toolModel))
                                        toolsList.add(toolModel)
                                    }
                                }
                            }
                        }

                        val fullText = if (textParts.isNotEmpty()) textParts.joinToString("\n\n") else content
                        if (partsList.isEmpty() && fullText.isNotBlank()) {
                            partsList.add(ChatPartUiModel.Text(id = "$id-content", text = fullText))
                        }

                        if (partsList.isNotEmpty() || fullText.isNotBlank()) {
                            messagesList.add(
                                ChatMessageUiModel(
                                    id = id,
                                    text = fullText,
                                    isUser = false,
                                    parts = partsList,
                                )
                            )
                        }
                    }
                }

                scope.launch {
                    _events.emit(RemoteEvent.SnapshotReceived(sessionId, messagesList, toolsList))
                }
            }
            "delta" -> {
                val sessionId = json.optString("sessionId", "")
                val eventObj = json.optJSONObject("event") ?: return
                when (eventObj.optString("type")) {
                    "text" -> {
                        val delta = eventObj.optString("delta", "")
                        if (delta.isNotEmpty()) {
                            scope.launch {
                                _events.emit(RemoteEvent.TextDelta(sessionId, delta))
                            }
                        }
                    }
                    "tool-start" -> {
                        val callId = eventObj.optString("callId", UUID.randomUUID().toString())
                        val tool = eventObj.optString("tool", "tool")
                        val title = eventObj.optString("title", tool)
                        scope.launch {
                            _events.emit(RemoteEvent.ToolStarted(sessionId, callId, tool, title))
                        }
                    }
                    "tool-delta" -> {
                        val callId = eventObj.optString("callId", "")
                        val chunk = eventObj.optString("chunk", "")
                        scope.launch {
                            _events.emit(RemoteEvent.ToolDelta(sessionId, callId, chunk))
                        }
                    }
                    "tool-end" -> {
                        val callId = eventObj.optString("callId", "")
                        val output = eventObj.optString("output", "")
                        val ok = eventObj.optBoolean("ok", true)
                        scope.launch {
                            _events.emit(RemoteEvent.ToolEnded(sessionId, callId, output, ok))
                        }
                    }
                }
            }
            "turn" -> {
                val sessionId = json.optString("sessionId", "")
                val state = json.optString("state", "idle")
                val userText = json.optString("userText").takeIf { it.isNotBlank() }
                val isRunning = state == "running"

                val inFlightTools = mutableListOf<ToolCallUiModel>()
                var inFlightText: String? = null
                val partsArray = json.optJSONArray("parts")
                if (partsArray != null && partsArray.length() > 0) {
                    val textParts = mutableListOf<String>()
                    for (p in 0 until partsArray.length()) {
                        val partObj = partsArray.optJSONObject(p) ?: continue
                        when (partObj.optString("type")) {
                            "text" -> {
                                val t = partObj.optString("text", "")
                                if (t.isNotBlank()) textParts.add(t)
                            }
                            "reasoning" -> {
                                val r = partObj.optString("text", "")
                                if (r.isNotBlank()) textParts.add(r)
                            }
                            "tool" -> {
                                val toolName = partObj.optString("tool", "tool")
                                val toolTitle = partObj.optString("title", toolName)
                                val toolState = partObj.optString("state", "running")
                                val toolOutput = partObj.optString("output", "")
                                val callId = partObj.optString("callId", UUID.randomUUID().toString())
                                val toolType = resolveToolType(toolName)
                                inFlightTools.add(
                                    ToolCallUiModel(
                                        id = callId,
                                        type = toolType,
                                        name = toolName,
                                        title = toolTitle,
                                        detail = toolOutput,
                                        status = if (toolState == "done") ToolCallStatus.Complete else ToolCallStatus.Running,
                                        isExpanded = false,
                                    )
                                )
                            }
                        }
                    }
                    if (textParts.isNotEmpty()) {
                        inFlightText = textParts.joinToString("\n\n")
                    }
                }

                scope.launch {
                    _events.emit(RemoteEvent.TurnChanged(sessionId, isRunning, userText, inFlightText, inFlightTools))
                }
            }
            "error" -> {
                val msg = json.optString("message", "Unknown error from remote host")
                if (!isHandshakeComplete) {
                    try {
                        activeWebSocket?.close(1000, "Pairing rejected")
                    } catch (_: Exception) {}
                    failConnection(msg)
                } else {
                    scope.launch {
                        _events.emit(RemoteEvent.ErrorReceived(msg))
                    }
                }
            }
            "bye" -> {
                handshakeTimeoutJob?.cancel()
                isHandshakeComplete = false
                _connectionState.value = RemoteConnectionState.Disconnected
            }
        }
    }

    private fun resolveToolType(name: String): ToolCallType {
        val normalized = name.lowercase()
        return if (normalized in listOf("read", "write", "edit", "glob", "grep", "file", "read_file", "write_file", "list", "list_dir")) {
            ToolCallType.File
        } else {
            ToolCallType.Terminal
        }
    }

    override fun sendPrompt(text: String) {
        val ws = activeWebSocket ?: return
        val payload = JSONObject().apply {
            put("t", "prompt")
            put("text", text)
        }
        ws.send(payload.toString())
    }

    override fun switchSession(sessionId: String) {
        val ws = activeWebSocket ?: return
        val payload = JSONObject().apply {
            put("t", "switch")
            put("sessionId", sessionId)
        }
        ws.send(payload.toString())
    }

    override fun refreshSessions() {
        val ws = activeWebSocket ?: return
        val payload = JSONObject().apply {
            put("t", "list")
        }
        ws.send(payload.toString())
    }

    override fun abort() {
        val ws = activeWebSocket ?: return
        val payload = JSONObject().apply {
            put("t", "abort")
        }
        ws.send(payload.toString())
    }

    override fun disconnect() {
        handshakeTimeoutJob?.cancel()
        handshakeTimeoutJob = null
        isHandshakeComplete = false
        try {
            activeWebSocket?.close(1000, "User disconnected")
        } catch (_: Exception) {}
        activeWebSocket = null
        _connectionState.value = RemoteConnectionState.Disconnected
    }

    private companion object {
        const val HANDSHAKE_TIMEOUT_MS = 15_000L
    }
}
