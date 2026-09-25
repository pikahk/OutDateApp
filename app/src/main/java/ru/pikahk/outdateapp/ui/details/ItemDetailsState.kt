package ru.pikahk.outdateapp.ui.details

import java.time.LocalDate
import ru.pikahk.outdateapp.domain.Urgency

data class ItemDetailsUi(
    val name: String,
    val expiresAt: LocalDate,
    val openedAt: LocalDate?,
    val daysAfterOpening: Int?,
    val effectiveExpiresAt: LocalDate,
    val daysLeft: Long,
    val urgency: Urgency
)

sealed interface ItemDetailsState {
    data object Loading : ItemDetailsState

    data class Loaded(val item: ItemDetailsUi) : ItemDetailsState

    data object Gone : ItemDetailsState
}
