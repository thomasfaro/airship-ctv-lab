package com.airship.ctvlab.airship

import android.app.Application
import android.util.Log
import com.airship.ctvlab.BuildConfig
import com.urbanairship.Airship
import com.urbanairship.AirshipConfigOptions
import com.urbanairship.PrivacyManager
import com.urbanairship.analytics.CustomEvent
import com.urbanairship.messagecenter.MessageCenter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AirshipLab {
    private const val TAG = "CtvLab"
    private const val DEFAULT_NAMED_USER = "thomasfctv"

    private val _channelId = MutableStateFlow<String?>(null)
    val channelId: StateFlow<String?> = _channelId.asStateFlow()

    private val _namedUser = MutableStateFlow<String?>(null)
    val namedUser: StateFlow<String?> = _namedUser.asStateFlow()

    private val _status = MutableStateFlow("not configured")
    val status: StateFlow<String> = _status.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val isConfigured: Boolean
        get() {
            val key = BuildConfig.AIRSHIP_APP_KEY
            return key.isNotBlank() && key != "YOUR_APP_KEY"
        }

    fun takeOff(app: Application) {
        if (!isConfigured) {
            _status.value = "fill config/airship.local.properties"
            Log.w(TAG, "Airship skipped: missing app key/secret")
            return
        }

        val site = if (BuildConfig.AIRSHIP_SITE.equals("eu", ignoreCase = true)) {
            AirshipConfigOptions.Site.SITE_EU
        } else {
            AirshipConfigOptions.Site.SITE_US
        }

        val options = AirshipConfigOptions.newBuilder()
            .setAppKey(BuildConfig.AIRSHIP_APP_KEY)
            .setAppSecret(BuildConfig.AIRSHIP_APP_SECRET)
            .setSite(site)
            .setInProduction(false)
            .setDevelopmentLogLevel(AirshipConfigOptions.LogLevel.VERBOSE)
            .build()

        Airship.takeOff(app, options)
        Airship.onReady {
            Airship.privacyManager.enable(PrivacyManager.Feature.ALL)
            Airship.channel.editTags {
                addTag("ctv_lab")
                addTag("google_tv")
            }
            identify(DEFAULT_NAMED_USER)
            _channelId.value = Airship.channel.id
            _status.value = if (Airship.channel.id.isNullOrBlank()) {
                "SDK ready, creating channel…"
            } else {
                "ready"
            }
            Log.i(TAG, "Airship ready. channel=${Airship.channel.id}")
            observeInbox()
        }
    }

    fun refreshInbox() {
        if (!Airship.isFlying) return
        scope.launch {
            runCatching { MessageCenter.shared().inbox.fetchMessages() }
        }
    }

    private fun observeInbox() {
        val inbox = MessageCenter.shared().inbox
        scope.launch {
            runCatching { inbox.fetchMessages() }
            inbox.getUnreadMessagesFlow().collect { messages ->
                _unreadCount.value = messages.size
            }
        }
    }

    fun identify(namedUserId: String) {
        if (!Airship.isFlying) return
        val trimmed = namedUserId.trim()
        if (trimmed.isEmpty()) {
            Airship.contact.reset()
            _namedUser.value = null
        } else {
            Airship.contact.identify(trimmed)
            _namedUser.value = trimmed
        }
    }

    fun trackPlay(title: String) {
        if (!Airship.isFlying) return
        CustomEvent.newBuilder("content_play")
            .setEventValue(1.0)
            .addProperty("title", title)
            .addProperty("platform", "google_tv")
            .build()
            .track()
    }
}
