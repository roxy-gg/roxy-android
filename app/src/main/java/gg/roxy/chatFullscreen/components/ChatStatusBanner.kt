package gg.roxy.chatFullscreen.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import gg.roxy.chatFullscreen.businessLogic.ChatFullScreenUiState
import gg.roxy.shared.styles.roxyColors

@Composable
fun ChatStatusBanner(state: ChatFullScreenUiState, onReconnect: () -> Unit) {
    val message = when {
        state.isConnecting -> "Reconnecting to your PC..."
        state.errorMessage != null -> state.errorMessage
        !state.isConnected -> "Your PC is disconnected. Your draft is saved."
        state.queuedPromptCount > 0 -> "Your PC has ${state.queuedPromptCount} queued message(s). Wait for them to finish."
        state.isAwaitingResponse -> "Waiting for your PC to confirm the message..."
        !state.isSessionReady -> "Refreshing this session..."
        else -> return
    }
    val colors = MaterialTheme.roxyColors
    Surface(color = colors.surface2, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = colors.text,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            if (!state.isConnecting && (!state.isConnected || state.errorMessage != null)) {
                TextButton(onClick = onReconnect) { Text(if (state.isConnected) "Refresh session" else "Reconnect") }
            }
        }
    }
}
