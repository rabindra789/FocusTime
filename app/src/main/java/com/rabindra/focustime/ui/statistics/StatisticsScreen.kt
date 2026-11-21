package com.rabindra.focustime.ui.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rabindra.focustime.StatisticsUi
import com.rabindra.focustime.ui.components.Hairline
import com.rabindra.focustime.ui.components.QuietAction
import com.rabindra.focustime.ui.components.SettingsRow
import com.rabindra.focustime.ui.theme.FocusSpacing
import com.rabindra.focustime.ui.theme.FocusType
import com.rabindra.focustime.ui.theme.LocalFocusPalette
import com.rabindra.focustime.util.TimeFormatter

/** A handful of quiet facts. No charts, no streaks. */
@Composable
fun StatisticsScreen(
    statistics: StatisticsUi,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalFocusPalette.current

    Column(
        modifier
            .fillMaxSize()
            .background(palette.background)
            .padding(horizontal = FocusSpacing.lg, vertical = FocusSpacing.lg),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Statistics", style = FocusType.title, color = palette.textPrimary)
            QuietAction(
                label = "Done",
                onClick = onBack,
                color = palette.accent,
                horizontalPadding = 0.dp,
            )
        }

        Spacer(Modifier.height(FocusSpacing.lg))

        SettingsRow(
            label = "Sessions today",
            value = statistics.todaySessions.toString(),
        )
        SettingsRow(
            label = "Focus time today",
            value = TimeFormatter.duration(statistics.todayFocusMillis).label,
        )
        SettingsRow(
            label = "Focus this week",
            value = TimeFormatter.duration(statistics.weekFocusMillis).label,
        )

        Spacer(Modifier.height(FocusSpacing.lg))
        Hairline()
        Spacer(Modifier.height(FocusSpacing.lg))

        SettingsRow(
            label = "Sessions completed",
            value = statistics.totalSessions.toString(),
        )
        SettingsRow(
            label = "Lifetime focus",
            value = TimeFormatter.duration(statistics.totalFocusMillis).label,
        )

        Spacer(Modifier.height(FocusSpacing.huge))
    }
}
