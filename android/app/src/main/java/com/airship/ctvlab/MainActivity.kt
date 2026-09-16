package com.airship.ctvlab

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.airship.ctvlab.data.Catalog
import com.airship.ctvlab.data.Show
import com.airship.ctvlab.ui.home.HomeScreen
import com.airship.ctvlab.ui.inbox.InboxScreen
import com.airship.ctvlab.ui.lab.LabScreen
import com.airship.ctvlab.ui.player.PlayerScreen
import com.airship.ctvlab.ui.theme.CtvLabTheme
import com.airship.ctvlab.ui.theme.Netflix
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    /**
     * The Scene's own buttons reach the app as `ctvlab://` deep links: the SDK fires them as a
     * VIEW intent at our package, so a foreground app is handed them through `onNewIntent`.
     */
    private val deepLink = MutableStateFlow<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deepLink.value = intent?.data
        setContent {
            CtvLabTheme {
                CtvLabApp(deepLink)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLink.value = intent.data
    }
}

private sealed interface Route {
    data object Home : Route
    data object Lab : Route
    data object Inbox : Route
    data class Player(val show: Show) : Route
}

/** `ctvlab://play/<film-id>`, `ctvlab://inbox`, `ctvlab://lab`, `ctvlab://home`. */
private fun Uri.toRoute(): Route? = when {
    scheme != LabInfo.DEEP_LINK_SCHEME -> null
    host == LabInfo.PLAY_HOST -> Catalog.findFilm(pathSegments.firstOrNull())?.let(Route::Player)
    host == LabInfo.INBOX_HOST -> Route.Inbox
    host == LabInfo.LAB_HOST -> Route.Lab
    host == LabInfo.HOME_HOST -> Route.Home
    else -> null
}

@Composable
private fun CtvLabApp(deepLink: MutableStateFlow<Uri?>) {
    var route by remember { mutableStateOf<Route>(Route.Home) }

    LaunchedEffect(Unit) {
        deepLink.collect { uri ->
            val target = uri?.toRoute()
            if (uri != null) deepLink.value = null
            if (target != null) route = target
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (route !is Route.Player) {
            HomeScreen(
                onPlay = { route = Route.Player(it) },
                onOpenLab = { route = Route.Lab },
                onOpenInbox = { route = Route.Inbox },
                captureFocus = route is Route.Home,
            )
        }
        when (val current = route) {
            Route.Home -> Unit
            Route.Lab -> FullscreenOverlay(onDismiss = { route = Route.Home }) {
                LabScreen(onBack = { route = Route.Home })
            }
            Route.Inbox -> FullscreenOverlay(onDismiss = { route = Route.Home }) {
                InboxScreen(onBack = { route = Route.Home })
            }
            is Route.Player -> PlayerScreen(
                show = current.show,
                onBack = { route = Route.Home },
            )
        }
    }
}

@Composable
private fun FullscreenOverlay(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Netflix.Background),
        ) {
            content()
        }
    }
}
