package gg.roxy.remote

sealed interface RemoteConnectionState {
    data object Disconnected : RemoteConnectionState
    data object Connecting : RemoteConnectionState
    data class Connected(val hostInfo: String = "Desktop Host") : RemoteConnectionState
    data class Error(val message: String) : RemoteConnectionState
}

data class RemoteSessionInfo(
    val id: String,
    val title: String,
    val project: String,
    val cwd: String? = null,
    val updatedAt: Long = 0L,
    val messageCount: Int = 0,
)

object RemoteWorkspaceUtils {
    fun extractGuestToken(input: String): String {
        val trimmed = input.trim()
        val hashIndex = trimmed.indexOf('#')
        if (hashIndex != -1) {
            val fragment = trimmed.substring(hashIndex + 1)
            val params = fragment.split('&')
            for (param in params) {
                val parts = param.split('=')
                if (parts.size == 2 && parts[0] == "k") {
                    return parts[1].trim()
                }
            }
        }
        val qIndex = trimmed.indexOf('?')
        if (qIndex != -1) {
            val query = trimmed.substring(qIndex + 1)
            val params = query.split('&')
            for (param in params) {
                val parts = param.split('=')
                if (parts.size == 2 && (parts[0] == "token" || parts[0] == "k")) {
                    return parts[1].trim()
                }
            }
        }
        return trimmed
    }

    fun formatRelativeTime(updatedAtMs: Long): String {
        if (updatedAtMs <= 0L) return "Recent"
        val now = System.currentTimeMillis()
        val diffMs = (now - updatedAtMs).coerceAtLeast(0L)
        val diffSec = diffMs / 1000
        val diffMin = diffSec / 60
        val diffHour = diffMin / 60
        val diffDays = diffHour / 24

        return when {
            diffMin < 1 -> "Now"
            diffMin < 60 -> "${diffMin}m"
            diffHour < 24 -> "${diffHour}h"
            diffDays < 7 -> "${diffDays}d"
            else -> {
                val sdf = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
                sdf.format(java.util.Date(updatedAtMs))
            }
        }
    }
}
