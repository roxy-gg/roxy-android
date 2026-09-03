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

data class ParsedQrPairing(
    val token: String,
    val pin: String? = null,
    val rawUrl: String? = null,
)

object RemoteWorkspaceUtils {
    fun parseQrPairing(input: String): ParsedQrPairing? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        // Check if JSON payload (using pure Kotlin regex so it runs cleanly on JVM tests & Android)
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            val tokenFromField = extractJsonField(trimmed, "token")
                ?: extractJsonField(trimmed, "k")
                ?: extractJsonField(trimmed, "guestToken")
            val pin = extractJsonField(trimmed, "pin")
                ?: extractJsonField(trimmed, "p")
            val url = extractJsonField(trimmed, "url")

            val token = when {
                !tokenFromField.isNullOrBlank() -> tokenFromField
                !url.isNullOrBlank() -> extractGuestToken(url)
                else -> ""
            }

            if (token.isNotBlank()) {
                return ParsedQrPairing(
                    token = token,
                    pin = pin?.filter { it.isDigit() }?.take(6),
                    rawUrl = url ?: trimmed,
                )
            }
        }

        // Check if URL or URI scheme
        val isUrlLike = trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("roxy://", ignoreCase = true) ||
            trimmed.contains("#") ||
            trimmed.contains("?")

        var extractedToken: String? = null
        var extractedPin: String? = null

        if (isUrlLike) {
            val hashIndex = trimmed.indexOf('#')
            val qIndex = trimmed.indexOf('?')

            // 1. Extract from fragment (Desktop Roxy puts guestToken in #k=... or #token=...)
            if (hashIndex != -1) {
                val fragment = trimmed.substring(hashIndex + 1)
                val params = fragment.split('&')
                for (param in params) {
                    val parts = param.split('=', limit = 2)
                    if (parts.size == 2) {
                        val key = parts[0].trim().lowercase()
                        val value = parts[1].trim()
                        if (key == "k" || key == "token" || key == "guesttoken") {
                            extractedToken = value
                        } else if (key == "pin" || key == "p") {
                            extractedPin = value
                        }
                    } else if (params.size == 1 && !param.contains("=") && param.length >= 8) {
                        extractedToken = param.trim()
                    }
                }
            }

            // 2. Extract from query string (?token=...&pin=...)
            if (qIndex != -1) {
                val endOfQuery = if (hashIndex != -1 && hashIndex > qIndex) hashIndex else trimmed.length
                val query = trimmed.substring(qIndex + 1, endOfQuery)
                val params = query.split('&')
                for (param in params) {
                    val parts = param.split('=', limit = 2)
                    if (parts.size == 2) {
                        val key = parts[0].trim().lowercase()
                        val value = parts[1].trim()
                        if (extractedToken == null && (key == "k" || key == "token" || key == "guesttoken")) {
                            extractedToken = value
                        } else if (extractedPin == null && (key == "pin" || key == "p")) {
                            extractedPin = value
                        }
                    }
                }
            }

            // 3. Extract from path (e.g. roxy.gg/r/<token>)
            if (extractedToken.isNullOrBlank()) {
                val cleanUrl = trimmed.substringBefore('#').substringBefore('?')
                val match = Regex("""roxy\.gg/r/([^/?#]+)""").find(cleanUrl)
                if (match != null) {
                    extractedToken = match.groupValues[1]
                }
            }
        }

        val finalToken = extractedToken ?: trimmed
        val cleanPin = extractedPin?.filter { it.isDigit() }?.take(6)

        return ParsedQrPairing(
            token = finalToken,
            pin = cleanPin,
            rawUrl = if (isUrlLike) trimmed else null,
        )
    }

    private fun extractJsonField(json: String, field: String): String? {
        val regex = Regex(""""$field"\s*:\s*"([^"]+)"""")
        return regex.find(json)?.groupValues?.get(1)
    }

    fun extractGuestToken(input: String): String {
        val parsed = parseQrPairing(input)
        return parsed?.token ?: input.trim()
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
