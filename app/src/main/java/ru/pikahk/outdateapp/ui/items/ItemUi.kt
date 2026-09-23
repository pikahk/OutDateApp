package ru.pikahk.outdateapp.ui.items

import ru.pikahk.outdateapp.domain.Urgency

data class ItemUi(val id: String, val name: String, val daysLeft: Long, val urgency: Urgency)
