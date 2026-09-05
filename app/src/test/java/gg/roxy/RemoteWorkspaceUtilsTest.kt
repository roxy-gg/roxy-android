package gg.roxy

import gg.roxy.remote.RemoteWorkspaceUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
    fun parsesDesktopQrWithFragmentKey() {
        val qrValue = "https://roxy.gg/remote#k=guest_token_abc123"
        val parsed = RemoteWorkspaceUtils.parseQrPairing(qrValue)
        assertNotNull(parsed)
        assertEquals("guest_token_abc123", parsed?.token)
        assertNull(parsed?.pin)
        assertEquals(qrValue, parsed?.rawUrl)
    }

    @Test
    fun parsesDesktopQrWithFragmentKeyAndPin() {
        val qrValue = "https://roxy.gg/remote#k=guest_token_abc123&pin=839201"
        val parsed = RemoteWorkspaceUtils.parseQrPairing(qrValue)
        assertNotNull(parsed)
        assertEquals("guest_token_abc123", parsed?.token)
        assertEquals("839201", parsed?.pin)
    }

    @Test
    fun parsesCustomSchemeQr() {
        val qrValue = "roxy://remote?token=scheme_tok_789&pin=123456"
        val parsed = RemoteWorkspaceUtils.parseQrPairing(qrValue)
        assertNotNull(parsed)
        assertEquals("scheme_tok_789", parsed?.token)
        assertEquals("123456", parsed?.pin)
    }

    @Test
    fun parsesJsonQrPayload() {
        val qrValue = """{"token":"json_token_val","pin":"998877"}"""
        val parsed = RemoteWorkspaceUtils.parseQrPairing(qrValue)
        assertNotNull(parsed)
        assertEquals("json_token_val", parsed?.token)
        assertEquals("998877", parsed?.pin)
    }

    @Test
    fun parsesJsonWithUrlAndPin() {
        val qrValue = """{"url":"https://roxy.gg/remote#k=nested_token_555","pin":"443322"}"""
        val parsed = RemoteWorkspaceUtils.parseQrPairing(qrValue)
        assertNotNull(parsed)
        assertEquals("nested_token_555", parsed?.token)
        assertEquals("443322", parsed?.pin)
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
