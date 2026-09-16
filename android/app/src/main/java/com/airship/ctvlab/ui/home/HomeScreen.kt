package com.airship.ctvlab.ui.home

import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.airship.ctvlab.airship.AirshipIds
import com.airship.ctvlab.airship.AirshipLab
import com.airship.ctvlab.airship.correctForDpad
import com.airship.ctvlab.airship.keepFocusThroughPageChanges
import com.airship.ctvlab.data.Catalog
import com.airship.ctvlab.data.Show
import com.airship.ctvlab.ui.theme.Netflix
import com.urbanairship.automation.compose.AirshipEmbeddedView
import com.urbanairship.automation.compose.AirshipEmbeddedViewState
import com.urbanairship.automation.compose.rememberAirshipEmbeddedViewState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onPlay: (Show) -> Unit,
    onOpenLab: () -> Unit,
    onOpenInbox: () -> Unit,
    captureFocus: Boolean = true,
) {
    val channelId by AirshipLab.channelId.collectAsStateWithLifecycle()
    val unreadCount by AirshipLab.unreadCount.collectAsStateWithLifecycle()
    val bannerState = rememberAirshipEmbeddedViewState(embeddedId = AirshipIds.HOME_BANNER)
    val scrollState = rememberScrollState()
    val playFocus = remember { FocusRequester() }
    val bellFocus = remember { FocusRequester() }
    var playFocused by remember { mutableStateOf(false) }

    LaunchedEffect(captureFocus) {
        if (captureFocus) {
            AirshipLab.refreshInbox()
            runCatching { playFocus.requestFocus() }
        }
    }

    // The Scene is an Android view, so attaching it asks the scroll container to bring it into
    // view and the page opens already scrolled past the top bar. Undo that, but only while the
    // hero still holds focus: past that point the scroll position is the user's.
    LaunchedEffect(bannerState.isAvailable) {
        if (!bannerState.isAvailable) return@LaunchedEffect
        repeat(6) {
            if (!playFocused) return@LaunchedEffect
            if (scrollState.value != 0) scrollState.scrollTo(0)
            delay(50)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Netflix.Background)
            .verticalScroll(scrollState),
    ) {
        TopBar(
            channelId = channelId,
            unreadCount = unreadCount,
            bellFocus = bellFocus,
            onOpenInbox = onOpenInbox,
        )
        FeaturedHero(
            show = Catalog.featured,
            playFocus = playFocus,
            bellFocus = bellFocus,
            onPlayFocusChanged = { playFocused = it },
            onPlay = onPlay,
        )
        HomeBanner(state = bannerState)
        Catalog.rows.forEach { (title, shows) ->
            CatalogRow(
                title = title,
                shows = shows,
                onSelect = { show ->
                    if (show.isLab) onOpenLab() else onPlay(show)
                },
            )
        }
        Spacer(Modifier.height(64.dp))
    }
}

@Composable
private fun TopBar(
    channelId: String?,
    unreadCount: Int,
    bellFocus: FocusRequester,
    onOpenInbox: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "CTVLAB",
            color = Netflix.Red,
            fontWeight = FontWeight.Black,
            fontSize = 28.sp,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.width(28.dp))
        Text("Home", color = Color.White, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(20.dp))
        Text(
            channelId?.let { "Live  ·  ${it.take(8)}" } ?: "Live",
            color = Netflix.Mute,
        )
        Spacer(Modifier.weight(1f))
        NotificationsBell(
            onClick = onOpenInbox,
            unreadCount = unreadCount,
            modifier = Modifier.focusRequester(bellFocus),
        )
    }
}

@Composable
private fun FeaturedHero(
    show: Show,
    playFocus: FocusRequester,
    bellFocus: FocusRequester,
    onPlayFocusChanged: (Boolean) -> Unit,
    onPlay: (Show) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp),
    ) {
        AsyncImage(
            model = show.imageUrl,
            contentDescription = show.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to Color.Black.copy(alpha = 0.82f),
                        0.45f to Color.Black.copy(alpha = 0.35f),
                        0.75f to Color.Transparent,
                    ),
                ),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.55f),
                        0.28f to Color.Transparent,
                        0.68f to Color.Black.copy(alpha = 0.55f),
                        1f to Netflix.Background,
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.52f)
                .padding(start = 48.dp, end = 24.dp, bottom = 28.dp),
        ) {
            Text(
                "N  SERIES",
                color = Netflix.Red,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                show.title,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = Color.White,
            )
            Spacer(Modifier.height(8.dp))
            Text(show.metaLine, color = Netflix.Mute, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(12.dp))
            Text(
                show.synopsis,
                color = Color.White.copy(alpha = 0.9f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(20.dp))
            var playButtonFocused by remember { mutableStateOf(false) }
            Button(
                onClick = { onPlay(show) },
                modifier = Modifier
                    .focusRequester(playFocus)
                    .focusProperties { up = bellFocus }
                    .onFocusChanged {
                        playButtonFocused = it.isFocused
                        onPlayFocusChanged(it.isFocused)
                    },
                colors = ButtonDefaults.colors(
                    containerColor = Color.White,
                    focusedContainerColor = Netflix.Red,
                    contentColor = Color.Black,
                    focusedContentColor = Color.White,
                ),
            ) {
                Image(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(if (playButtonFocused) Color.White else Color.Black),
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("Play")
            }
        }
    }
}

@Composable
private fun NotificationsBell(
    onClick: () -> Unit,
    unreadCount: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(56.dp),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .pointerInput(Unit) {
                    detectTapGestures { onClick() }
                },
            scale = IconButtonDefaults.scale(focusedScale = 1.2f),
            colors = IconButtonDefaults.colors(
                containerColor = Color.White.copy(alpha = 0.18f),
                contentColor = Color.White,
                focusedContainerColor = Color.White,
                focusedContentColor = Color.Black,
                pressedContainerColor = Netflix.Red,
                pressedContentColor = Color.White,
            ),
            shape = IconButtonDefaults.shape(CircleShape),
            border = IconButtonDefaults.border(
                border = Border.None,
                focusedBorder = Border(
                    border = BorderStroke(3.dp, Color.White),
                    shape = CircleShape,
                ),
                pressedBorder = Border(
                    border = BorderStroke(3.dp, Netflix.Red),
                    shape = CircleShape,
                ),
            ),
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = if (unreadCount > 0) {
                    "Notifications, $unreadCount unread"
                } else {
                    "Notifications"
                },
                modifier = Modifier.size(28.dp),
            )
        }
        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .zIndex(2f)
                    .size(20.dp)
                    .background(Netflix.Red, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/** Aspect ratio of the banner artwork the Scenes are authored against (1800 × 560). */
private const val BannerAspectRatio = 1800f / 560f

/**
 * The slot takes the full content width and reports a height derived from the banner ratio.
 *
 * Both dimensions have to be reported for a placement sized in percentages to resolve: with an
 * unbounded height it falls back to `MATCH_PARENT` and a chain of percentages around a `media`
 * collapses to the tallest view carrying an intrinsic size (48 dp for a dismiss button). A
 * placement sized `auto` still sizes itself to its content within that height.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeBanner(state: AirshipEmbeddedViewState) {
    val host = LocalView.current
    val scope = rememberCoroutineScope()
    val bringIntoView = remember { BringIntoViewRequester() }
    var focused by remember { mutableStateOf(false) }

    LaunchedEffect(state.isAvailable) {
        if (!state.isAvailable) return@LaunchedEffect
        repeat(20) {
            val scene = host.findThomasEmbeddedView()
            if (scene != null) {
                scene.correctForDpad()
                scene.keepFocusThroughPageChanges()
                // A Scene sizes itself as its media loads, and the first pass reports a button of
                // zero height, so the wiring has to follow every layout rather than run once.
                scene.addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
                    view.correctForDpad()
                }
                return@LaunchedEffect
            }
            delay(50)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 8.dp)
            // A Scene draws no focus state of its own, so the slot carries the app's focus ring.
            .border(
                width = 3.dp,
                color = if (focused) Color.White else Color.Transparent,
                shape = RoundedCornerShape(8.dp),
            )
            .clip(RoundedCornerShape(8.dp))
            .bringIntoViewRequester(bringIntoView)
            // The scroll container doesn't follow focus into an Android view, so the Scene stays
            // pinned to the screen edge once its own button takes focus unless we ask for it.
            .onFocusChanged { focus ->
                focused = focus.hasFocus
                if (focus.hasFocus) scope.launch { bringIntoView.bringIntoView() }
            },
    ) {
        val parentWidth = constraints.maxWidth
        val parentHeight = (parentWidth / BannerAspectRatio).toInt()
        AirshipEmbeddedView(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .height(with(LocalDensity.current) { parentHeight.toDp() }),
            parentWidthProvider = { parentWidth },
            parentHeightProvider = { parentHeight },
            placeholder = { HomeBannerPlaceholder() },
        )
    }
}

private fun View.findThomasEmbeddedView(): View? {
    if (javaClass.simpleName == "ThomasEmbeddedView") return this
    if (this !is ViewGroup) return null
    for (index in 0 until childCount) {
        getChildAt(index).findThomasEmbeddedView()?.let { return it }
    }
    return null
}


@Composable
private fun HomeBannerPlaceholder() {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(BannerAspectRatio),
    ) {
        AsyncImage(
            model = Catalog.continueWatching.first().imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f)),
        )
        Column(
            Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = 28.dp),
        ) {
            Text("AIRSHIP SCENE", color = Netflix.Red, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 12.sp)
            Text(AirshipIds.HOME_BANNER, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
            Text("Publish an Embedded Content Scene with this ID", color = Netflix.Mute)
        }
    }
}

@Composable
private fun CatalogRow(
    title: String,
    shows: List<Show>,
    onSelect: (Show) -> Unit,
) {
    Column(Modifier.padding(top = 22.dp)) {
        Text(
            title,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            modifier = Modifier.padding(start = 48.dp, bottom = 12.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(shows, key = { it.id }) { show ->
                ShowCard(show = show, onClick = { onSelect(show) })
            }
        }
    }
}

@Composable
private fun ShowCard(show: Show, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.width(304.dp),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(4.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Netflix.Card,
            focusedContainerColor = Netflix.Card,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.12f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(3.dp, Color.White),
                shape = RoundedCornerShape(4.dp),
            ),
        ),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(171.dp),
        ) {
            if (show.imageUrl != null) {
                AsyncImage(
                    model = show.imageUrl,
                    contentDescription = show.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(listOf(Netflix.Red, Color(0xFF3B0008))),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("CTVLAB", color = Color.White, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                }
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.45f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.78f),
                        ),
                    ),
            )
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
            ) {
                Text(show.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(show.subtitle, color = Netflix.Mute, fontSize = 12.sp, maxLines = 1)
            }
            show.progress?.let { progress ->
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color.White.copy(alpha = 0.28f)),
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .background(Netflix.Red),
                    )
                }
            }
        }
    }
}
