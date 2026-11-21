package com.rabindra.focustime.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rabindra.focustime.ui.theme.FocusSpacing
import com.rabindra.focustime.ui.theme.FocusType
import com.rabindra.focustime.ui.theme.LocalFocusPalette

@Composable
fun Hairline(modifier: Modifier = Modifier, color: Color = LocalFocusPalette.current.divider) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color),
    )
}

@Composable
fun VerticalHairline(modifier: Modifier = Modifier, height: Dp = 20.dp) {
    Box(
        modifier
            .width(1.dp)
            .height(height)
            .background(LocalFocusPalette.current.divider),
    )
}

/** A short line used as a quiet affordance for the controls sheet. */
@Composable
fun Handle(modifier: Modifier = Modifier) {
    val palette = LocalFocusPalette.current
    Box(
        modifier
            .width(36.dp)
            .height(2.dp)
            .background(palette.divider),
    )
}

/** Thin progress bar track + filled portion. */
@Composable
fun ThinProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    thickness: Dp = 2.dp,
    color: Color = LocalFocusPalette.current.accent,
) {
    val palette = LocalFocusPalette.current
    Box(
        modifier
            .height(thickness)
            .background(palette.track),
    ) {
        Box(
            Modifier
                .height(thickness)
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .background(color),
        )
    }
}

/** Text-only action with a comfortable hit target and no chrome. */
@Composable
fun QuietAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = LocalFocusPalette.current.textPrimary,
    enabled: Boolean = true,
    horizontalPadding: Dp = FocusSpacing.md,
) {
    val palette = LocalFocusPalette.current
    Box(
        modifier
            .heightIn(min = 48.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = horizontalPadding, vertical = FocusSpacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = FocusType.action,
            color = if (enabled) color else palette.textMuted,
            textAlign = TextAlign.Center,
        )
    }
}

/** A minimal row with label + optional detail; used in settings. */
@Composable
fun SettingsRow(
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    value: String? = null,
    color: Color = LocalFocusPalette.current.textPrimary,
) {
    val palette = LocalFocusPalette.current
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = FocusSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = FocusType.body, color = color)
        if (value != null) {
            Text(text = value, style = FocusType.label, color = palette.textSecondary)
        }
    }
}

/** Stepper row with label, value, and +/- controls. */
@Composable
fun StepperRow(
    label: String,
    value: String,
    decreaseLabel: String,
    increaseLabel: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
    canDecrease: Boolean = true,
    canIncrease: Boolean = true,
) {
    val palette = LocalFocusPalette.current
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(vertical = FocusSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = FocusType.body,
            color = palette.textPrimary,
            modifier = Modifier.weight(1f),
        )
        StepperControl(
            symbol = "\u2212",
            description = decreaseLabel,
            enabled = canDecrease,
            onClick = onDecrease,
        )
        Text(
            text = value,
            style = FocusType.numerals,
            color = palette.textPrimary,
            modifier = Modifier.width(62.dp),
            textAlign = TextAlign.Center,
        )
        StepperControl(
            symbol = "+",
            description = increaseLabel,
            enabled = canIncrease,
            onClick = onIncrease,
        )
    }
}

@Composable
private fun StepperControl(
    symbol: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalFocusPalette.current
    Box(
        Modifier
            .size(44.dp)
            .border(1.dp, palette.divider, CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = symbol,
            style = FocusType.body,
            color = if (enabled) palette.textSecondary else palette.textMuted,
        )
    }
}

/** Toggle row with label, description, and a Switch. */
@Composable
fun ToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalFocusPalette.current
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = FocusSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = label, style = FocusType.body, color = palette.textPrimary)
            Text(text = description, style = FocusType.caption, color = palette.textMuted)
        }
        Spacer(Modifier.width(FocusSpacing.md))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = palette.background,
                checkedTrackColor = palette.accent,
                checkedBorderColor = palette.accent,
                uncheckedThumbColor = palette.textSecondary,
                uncheckedTrackColor = palette.surface,
                uncheckedBorderColor = palette.divider,
            ),
        )
    }
}