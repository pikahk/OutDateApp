package ru.pikahk.outdateapp.ui.add

import java.time.LocalDate

data class ItemDraft(val name: String, val expiresAt: LocalDate, val daysAfterOpening: Int?)
