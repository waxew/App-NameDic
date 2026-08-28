package ir.asteam.namedic.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll as foundationHorizontalScroll
import androidx.compose.ui.Modifier

/**
 * این bridge کوچک باعث می‌شود فایل اصلی UI بدون وابستگی به import پراکندهٔ extension
 * بتواند horizontalScroll را در همان package فراخوانی کند.
 */
fun Modifier.horizontalScroll(state: ScrollState): Modifier =
    this.foundationHorizontalScroll(state)
