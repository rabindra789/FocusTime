package com.rabindra.focustime.ui.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.rabindra.focustime.BuildConfig
import com.rabindra.focustime.ui.components.Hairline
import com.rabindra.focustime.ui.components.QuietAction
import com.rabindra.focustime.ui.components.SettingsRow
import com.rabindra.focustime.ui.theme.FocusSpacing
import com.rabindra.focustime.ui.theme.FocusType
import com.rabindra.focustime.ui.theme.LocalFocusPalette

internal const val CREATOR_NAME = "Rabindra Kumar Meher"
internal const val PROJECT_URL = "https://github.com/rabindra789/FocusTime"
internal const val CONTACT_EMAIL = "rabindrameher116@gmail.com"

internal const val MIT_LICENSE = """MIT License

Copyright (c) 2026 Rabindra Kumar Meher

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE."""

internal const val FONT_LICENSE_NOTE = """Nunito Sans is used under the SIL Open Font License 1.1.

Copyright 2016 The Nunito Sans Project Authors
(https://github.com/Fonthausen/NunitoSans)

The full licence ships with the source tree in
licenses/NunitoSans-OFL.txt."""

/** Creator, version and licences. Nothing here is interactive except the links. */
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    versionName: String = BuildConfig.VERSION_NAME,
    versionCode: Int = BuildConfig.VERSION_CODE,
) {
    val palette = LocalFocusPalette.current
    val uriHandler = LocalUriHandler.current
    var license by remember { mutableStateOf<Pair<String, String>?>(null) }

    Column(
        modifier
            .fillMaxSize()
            .background(palette.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = FocusSpacing.lg, vertical = FocusSpacing.lg),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "About", style = FocusType.title, color = palette.textPrimary)
            QuietAction(
                label = "Done",
                onClick = onBack,
                color = palette.accent,
                horizontalPadding = 0.dp,
            )
        }

        Spacer(Modifier.height(FocusSpacing.lg))

        Text(text = "FocusTime", style = FocusType.title, color = palette.textPrimary)
        Text(
            text = "A quiet Pomodoro desk clock.",
            style = FocusType.body,
            color = palette.textSecondary,
        )
        Spacer(Modifier.height(FocusSpacing.lg))
        Hairline()
        Spacer(Modifier.height(FocusSpacing.lg))

        SectionLabel("Version")
        SettingsRow(label = "App version", value = "$versionName ($versionCode)")

        Spacer(Modifier.height(FocusSpacing.lg))
        SectionLabel("Creator")
        SettingsRow(label = CREATOR_NAME, value = "GitHub \u2197", onClick = {
            runCatching { uriHandler.openUri("https://github.com/rabindra789") }
        })
        SettingsRow(label = CONTACT_EMAIL, value = "Email \u2197", onClick = {
            runCatching { uriHandler.openUri("mailto:$CONTACT_EMAIL") }
        })
        SettingsRow(label = "Source code", value = "GitHub \u2197", onClick = {
            runCatching { uriHandler.openUri(PROJECT_URL) }
        })

        Spacer(Modifier.height(FocusSpacing.lg))
        SectionLabel("License")
        SettingsRow(
            label = "MIT License",
            value = "VIEW",
            onClick = { license = "MIT License" to MIT_LICENSE },
        )
        SettingsRow(
            label = "Nunito Sans",
            value = "OFL 1.1",
            onClick = { license = "SIL Open Font License 1.1" to FONT_LICENSE_NOTE },
        )

        Spacer(Modifier.height(FocusSpacing.huge))
        Hairline()
        Spacer(Modifier.height(FocusSpacing.lg))
        Text(
            text = "\u00a9 2026 $CREATOR_NAME",
            style = FocusType.caption,
            color = palette.textMuted,
        )
    }

    license?.let { (title, body) ->
        AlertDialog(
            onDismissRequest = { license = null },
            title = { Text(text = title, style = FocusType.body, color = palette.textPrimary) },
            text = {
                Text(
                    text = body,
                    style = FocusType.caption,
                    color = palette.textSecondary,
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                )
            },
            confirmButton = {
                TextButton(onClick = { license = null }) {
                    Text(text = "Close", color = palette.accent)
                }
            },
            containerColor = palette.surface,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = FocusType.caption,
        color = LocalFocusPalette.current.textMuted,
    )
}
