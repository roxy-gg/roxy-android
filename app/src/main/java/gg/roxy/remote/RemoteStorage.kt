package gg.roxy.remote

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface RemoteStorage {
    var savedToken: String?
    var savedPin: String?
    fun clear()
}

@Singleton
class SharedPreferencesRemoteStorage @Inject constructor(
    @ApplicationContext context: Context,
) : RemoteStorage {
    private val prefs = context.getSharedPreferences("roxy_remote_prefs", Context.MODE_PRIVATE)

    override var savedToken: String?
        get() = prefs.getString("guest_token", null)
        set(value) = prefs.edit().putString("guest_token", value).apply()

    override var savedPin: String?
        get() = prefs.getString("pin", null)
        set(value) = prefs.edit().putString("pin", value).apply()

    override fun clear() {
        prefs.edit().clear().apply()
    }
}
