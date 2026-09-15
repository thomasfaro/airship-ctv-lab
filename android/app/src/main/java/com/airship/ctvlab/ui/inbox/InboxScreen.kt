package com.airship.ctvlab.ui.inbox

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.airship.ctvlab.ui.theme.Netflix
import com.urbanairship.Airship
import com.urbanairship.messagecenter.Message
import com.urbanairship.messagecenter.MessageCenter
import com.urbanairship.messagecenter.compose.ui.MessageCenterMessage
import com.urbanairship.messagecenter.compose.ui.rememberMessageCenterMessageState
import com.urbanairship.messagecenter.compose.ui.theme.MessageCenterColors
import com.urbanairship.messagecenter.compose.ui.theme.MessageCenterTheme
import java.text.DateFormat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow

private val brandColors = MessageCenterColors.darkDefaults(
    background = Netflix.Background,
    surface = Netflix.Elevated,
    accent = Netflix.Red,
    textPrimary = Color.White,
    textSecondary = Netflix.Mute,
    divider = Color.White.copy(alpha = 0.12f),
).copy(
    messageEmptyBackground = Netflix.Background,
    messageLoadingBackground = Netflix.Background,
    messageErrorBackground = Netflix.Background,
)

@Composable
fun InboxScreen(onBack: () -> Unit) {
    val inbox = remember { if (Airship.isFlying) MessageCenter.shared().inbox else null }
    val messages by remember(inbox) { inbox?.getMessagesFlow() ?: emptyFlow() }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    var selectedId by remember { mutableStateOf<String?>(null) }
    val deleteFocus = remember { FocusRequester() }
    val listFocus = remember { FocusRequester() }
    // Bumped to pull focus back into the list once the detail pane goes away.
    var refocusList by remember { mutableStateOf(0) }

    // The message body is a WebView that keeps focus once entered, so Back is the way out of it.
    BackHandler { if (selectedId != null) selectedId = null else onBack() }

    LaunchedEffect(inbox) {
        inbox?.let { runCatching { it.fetchMessages() } }
    }

    // Drop the selection if the message is gone (deleted or expired).
    LaunchedEffect(messages, selectedId) {
        val id = selectedId
        if (id != null && messages.none { it.id == id }) {
            selectedId = null
        }
    }

    MessageCenterTheme(colors = brandColors) {
        Row(
            Modifier
                .fillMaxSize()
                .background(Netflix.Background),
        ) {
            MessageList(
                messages = messages,
                selectedId = selectedId,
                onSelect = { selectedId = it.id },
                onBack = onBack,
                deleteFocus = deleteFocus,
                listFocus = listFocus,
                refocusList = refocusList,
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight(),
            )
            Box(
                Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Color.White.copy(alpha = 0.10f)),
            )
            MessageDetail(
                messageId = selectedId,
                deleteFocus = deleteFocus,
                listFocus = listFocus,
                onDelete = { id ->
                    inbox?.deleteMessages(id)
                    selectedId = null
                    refocusList++
                },
                onClose = {
                    selectedId = null
                    refocusList++
                },
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun MessageList(
    messages: List<Message>,
    selectedId: String?,
    onSelect: (Message) -> Unit,
    onBack: () -> Unit,
    deleteFocus: FocusRequester,
    listFocus: FocusRequester,
    refocusList: Int,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(messages.isNotEmpty(), refocusList) {
        // The requester is only attached once the row has been composed, which can
        // lag a frame or two behind a delete.
        repeat(6) {
            if (messages.isEmpty()) return@LaunchedEffect
            if (runCatching { listFocus.requestFocus() }.isSuccess) return@LaunchedEffect
            delay(50)
        }
    }

    Column(modifier.background(Netflix.Background)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 40.dp, end = 32.dp, top = 32.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "NOTIFICATIONS",
                    color = Netflix.Red,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    fontSize = 12.sp,
                )
                Text(
                    "Message Center",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 30.sp,
                )
            }
            Button(
                onClick = onBack,
                colors = ButtonDefaults.colors(
                    containerColor = Color.White.copy(alpha = 0.14f),
                    contentColor = Color.White,
                    focusedContainerColor = Color.White,
                    focusedContentColor = Color.Black,
                ),
            ) {
                Text("Close")
            }
        }

        if (messages.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No messages yet", color = Netflix.Mute)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 40.dp, end = 32.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(messages, key = { it.id }) { message ->
                    val isSelected = message.id == selectedId
                    // The row's focus properties have to sit on a parent node: the TV
                    // Surface declares its own focus target after the caller modifier.
                    Box(
                        Modifier
                            .focusProperties {
                                right = if (isSelected) deleteFocus else FocusRequester.Cancel
                            }
                            .then(
                                if (message.id == messages.first().id) {
                                    Modifier.focusRequester(listFocus)
                                } else {
                                    Modifier
                                },
                            ),
                    ) {
                        MessageRow(
                            message = message,
                            isSelected = isSelected,
                            onClick = { onSelect(message) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageRow(
    message: Message,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) Netflix.Red.copy(alpha = 0.22f) else Netflix.Elevated,
            contentColor = Color.White,
            focusedContainerColor = if (isSelected) Netflix.Red.copy(alpha = 0.34f) else Netflix.Card,
            focusedContentColor = Color.White,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
        border = ClickableSurfaceDefaults.border(
            border = if (isSelected) {
                Border(BorderStroke(2.dp, Netflix.Red), shape = RoundedCornerShape(6.dp))
            } else {
                Border.None
            },
            focusedBorder = Border(
                border = BorderStroke(3.dp, Color.White),
                shape = RoundedCornerShape(6.dp),
            ),
        ),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(112.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(if (isSelected) Netflix.Red else Color.Transparent),
            )
            Spacer(Modifier.width(15.dp))
            Box(
                Modifier
                    .size(92.dp, 62.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.10f)),
            ) {
                message.listIconUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    message.title,
                    color = Color.White,
                    fontWeight = if (message.isRead) FontWeight.Medium else FontWeight.Bold,
                    fontSize = 17.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                message.subtitle?.let {
                    Text(it, color = Netflix.Mute, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    DateFormat.getDateInstance(DateFormat.MEDIUM).format(message.sentDate),
                    color = Netflix.Mute,
                    fontSize = 12.sp,
                )
            }
            if (!message.isRead) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Netflix.Red),
                )
            }
            Spacer(Modifier.width(20.dp))
        }
    }
}

@Composable
private fun MessageDetail(
    messageId: String?,
    deleteFocus: FocusRequester,
    listFocus: FocusRequester,
    onDelete: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.background(Netflix.Background)) {
        if (messageId == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Select a message to read it", color = Netflix.Mute)
            }
        } else {
            key(messageId) {
                // The SDK message view resolves its message from a ViewModel created with the id.
                // A dedicated store per message forces a fresh load on every selection.
                val storeOwner = remember { MessageViewModelStoreOwner() }
                DisposableEffect(Unit) {
                    onDispose { storeOwner.viewModelStore.clear() }
                }

                Column(Modifier.fillMaxSize()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 40.dp, vertical = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = { onDelete(messageId) },
                            modifier = Modifier
                                .focusRequester(deleteFocus)
                                .focusProperties { left = listFocus },
                            colors = ButtonDefaults.colors(
                                containerColor = Color.White.copy(alpha = 0.14f),
                                contentColor = Color.White,
                                focusedContainerColor = Netflix.Red,
                                focusedContentColor = Color.White,
                            ),
                        ) {
                            Text("Delete")
                        }
                    }
                    CompositionLocalProvider(LocalViewModelStoreOwner provides storeOwner) {
                        val state = rememberMessageCenterMessageState(messageId = messageId)
                        MessageCenterMessage(
                            state = state,
                            onClose = onClose,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 40.dp, end = 40.dp, bottom = 40.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        )
                    }
                }
            }
        }
    }
}

private class MessageViewModelStoreOwner : ViewModelStoreOwner {
    override val viewModelStore: ViewModelStore = ViewModelStore()
}
