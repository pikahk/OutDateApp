package ru.pikahk.outdateapp.ui

import androidx.compose.ui.graphics.Color
import ru.pikahk.outdateapp.domain.Urgency

fun Urgency.color(): Color = when (this) {
    Urgency.EXPIRED, Urgency.CRITICAL -> Color(0xFFC0392B)
    Urgency.SOON -> Color(0xFF9A6A0C)
    Urgency.OK -> Color(0xFF24874A)
}
