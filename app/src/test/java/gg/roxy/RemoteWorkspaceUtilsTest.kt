package gg.roxy

import gg.roxy.remote.RemoteWorkspaceUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteWorkspaceUtilsTest {
    @Test
    fun extractsTokenFromFullUrlWithFragment() {
        val url = "https://roxy.gg/r/e747b46f-386f-425f-9532-6b3f5c748ea3#k=eyJiIjoiZTc0N2I0NmYtMzg2Zi00MjVmLTk1MzItNmIzZjVjNzQ4ZWEzIiwiciI6Imd1ZXN0In0.test_sig"
        val token = RemoteWorkspaceUtils.extractGuestToken(url)
        assertEquals("eyJiIjoiZTc0N2I0NmYtMzg2Zi00MjVmLTk1MzItNmIzZjVjNzQ4ZWEzIiwiciI6Imd1ZXN0In0.test_sig", token)
    }

    @Test
    fun extractsTokenFromQueryParam() {
        val url = "https://roxy.gg/api/remote/ws?token=my_jwt_token_123"
        val token = RemoteWorkspaceUtils.extractGuestToken(url)
        assertEquals("my_jwt_token_123", token)
    }

    @Test
    fun keepsBareToken() {
        val bareToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
        val token = RemoteWorkspaceUtils.extractGuestToken(bareToken)
        assertEquals("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9", token)
    }

    @Test
    fun formatsRelativeTimeCorrectly() {
        val now = System.currentTimeMillis()
        assertEquals("Now", RemoteWorkspaceUtils.formatRelativeTime(now - 1000 * 10))
        assertEquals("15m", RemoteWorkspaceUtils.formatRelativeTime(now - 1000 * 60 * 15))
        assertEquals("2h", RemoteWorkspaceUtils.formatRelativeTime(now - 1000 * 60 * 60 * 2))
        assertEquals("3d", RemoteWorkspaceUtils.formatRelativeTime(now - 1000 * 60 * 60 * 24 * 3))
    }
}
