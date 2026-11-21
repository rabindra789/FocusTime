package com.rabindra.focustime.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.rabindra.focustime.R

private val NunitoSans = FontFamily(
    Font(
        resId = R.font.nunito_sans,
        weight = FontWeight.Light,
    ),
    Font(
        resId = R.font.nunito_sans,
        weight = FontWeight.Normal,
    ),
    Font(
        resId = R.font.nunito_sans,
        weight = FontWeight.Medium,
    ),
)

object FocusType {

    private const val TABULAR = "tnum"

    val timer = TextStyle(
        fontFamily = NunitoSans,
        fontWeight = FontWeight.Light,
        fontSize = 92.sp,
        lineHeight = 98.sp,
        fontFeatureSettings = TABULAR,
    )

    val timerCompact = TextStyle(
        fontFamily = NunitoSans,
        fontWeight = FontWeight.Light,
        fontSize = 64.sp,
        lineHeight = 70.sp,
        fontFeatureSettings = TABULAR,
    )

    val clock = TextStyle(
        fontFamily = NunitoSans,
        fontWeight = FontWeight.Light,
        fontSize = 118.sp,
        lineHeight = 124.sp,
        fontFeatureSettings = TABULAR,
    )

    val clockCountdown = TextStyle(
        fontFamily = NunitoSans,
        fontWeight = FontWeight.Light,
        fontSize = 64.sp,
        lineHeight = 70.sp,
        fontFeatureSettings = TABULAR,
    )

    val display = TextStyle(
        fontFamily = NunitoSans,
        fontWeight = FontWeight.Normal,
        fontSize = 26.sp,
        lineHeight = 36.sp,
    )

    val title = TextStyle(
        fontFamily = NunitoSans,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 26.sp,
    )

    val body = TextStyle(
        fontFamily = NunitoSans,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 23.sp,
    )

    val label = TextStyle(
        fontFamily = NunitoSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )

    val caption = TextStyle(
        fontFamily = NunitoSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    )

    val action = TextStyle(
        fontFamily = NunitoSans,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    )

    val numerals = TextStyle(
        fontFamily = NunitoSans,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontFeatureSettings = TABULAR,
    )
}