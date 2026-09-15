package com.airship.ctvlab.ui.lab

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.airship.ctvlab.airship.AirshipLab
import com.airship.ctvlab.ui.theme.Netflix
import com.urbanairship.messagecenter.compose.ui.MessageCenterListScreen
import com.urbanairship.preferencecenter.compose.ui.PreferenceCenterScreen

private enum class LabOverlay { None, MessageCenter, PreferenceCenter }

@Composable
fun LabScreen(onBack: () -> Unit) {
    var overlay by remember { mutableStateOf(LabOverlay.None) }

    when (overlay) {
        LabOverlay.MessageCenter -> {
            MessageCenterListScreen(
                onNavigateUp = { overlay = LabOverlay.None },
                onMessageSelected = { },
            )
        }
        LabOverlay.PreferenceCenter -> {
            PreferenceCenterScreen(
                identifier = "default",
                onNavigateUp = { overlay = LabOverlay.None },
            )
        }
        LabOverlay.None -> LabContent(
            onBack = onBack,
            onOpenMessageCenter = { overlay = LabOverlay.MessageCenter },
            onOpenPreferenceCenter = { overlay = LabOverlay.PreferenceCenter },
        )
    }
}

@Composable
private fun LabContent(
    onBack: () -> Unit,
    onOpenMessageCenter: () -> Unit,
    onOpenPreferenceCenter: () -> Unit,
) {
    val channelId by AirshipLab.channelId.collectAsStateWithLifecycle()
    val namedUser by AirshipLab.namedUser.collectAsStateWithLifecycle()
    val status by AirshipLab.status.collectAsStateWithLifecycle()
    var namedUserInput by remember { mutableStateOf(namedUser.orEmpty()) }

    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Airship Lab", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = Netflix.Red)
        Text("Status: $status", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Channel ID: ${channelId ?: "—"}", style = MaterialTheme.typography.titleMedium)
        Text(
            "Named user: ${namedUser ?: "not identified"}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(8.dp))
        Text("Identify a named user")
        BasicTextField(
            value = namedUserInput,
            onValueChange = { namedUserInput = it },
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
                .padding(16.dp),
            decorationBox = { inner ->
                if (namedUserInput.isEmpty()) {
                    Text("ex. mediaset-qa-01", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                inner()
            },
        )

        Row {
            Button(onClick = { AirshipLab.identify(namedUserInput) }) {
                Text("Identify")
            }
            Spacer(Modifier.width(16.dp))
            Button(onClick = { AirshipLab.identify("") }) {
                Text("Reset")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = onOpenPreferenceCenter) { Text("Preference Center") }
            Button(onClick = onOpenMessageCenter) { Text("Message Center") }
            Button(onClick = onBack) { Text("Back to catalog") }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "Google TV does not show visible push. To test: publish a Scene / IAA targeting " +
                "the ctv_lab tag, then return to the catalog or start playback.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
