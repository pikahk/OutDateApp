package ru.pikahk.outdateapp.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import ru.pikahk.outdateapp.domain.Urgency
import ru.pikahk.outdateapp.ui.theme.AmberContainerDark
import ru.pikahk.outdateapp.ui.theme.AmberContainerLight
import ru.pikahk.outdateapp.ui.theme.AmberDark
import ru.pikahk.outdateapp.ui.theme.AmberLight
import ru.pikahk.outdateapp.ui.theme.GreenContainerDark
import ru.pikahk.outdateapp.ui.theme.GreenContainerLight
import ru.pikahk.outdateapp.ui.theme.GreenDark
import ru.pikahk.outdateapp.ui.theme.GreenLight
import ru.pikahk.outdateapp.ui.theme.RedContainerDark
import ru.pikahk.outdateapp.ui.theme.RedContainerLight
import ru.pikahk.outdateapp.ui.theme.RedDark
import ru.pikahk.outdateapp.ui.theme.RedLight

@Composable
fun Urgency.color(): Color {
    val dark = isSystemInDarkTheme()
    return when (this) {
        Urgency.EXPIRED, Urgency.CRITICAL -> if (dark) RedDark else RedLight
        Urgency.SOON -> if (dark) AmberDark else AmberLight
        Urgency.OK -> if (dark) GreenDark else GreenLight
    }
}

@Composable
fun Urgency.containerColor(): Color {
    val dark = isSystemInDarkTheme()
    return when (this) {
        Urgency.EXPIRED, Urgency.CRITICAL -> if (dark) RedContainerDark else RedContainerLight
        Urgency.SOON -> if (dark) AmberContainerDark else AmberContainerLight
        Urgency.OK -> if (dark) GreenContainerDark else GreenContainerLight
    }
}
