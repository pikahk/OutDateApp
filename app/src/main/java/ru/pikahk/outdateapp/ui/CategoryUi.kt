package ru.pikahk.outdateapp.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.pikahk.outdateapp.R
import ru.pikahk.outdateapp.data.Category
import ru.pikahk.outdateapp.data.DefaultCategory

data class CategoryUi(val id: String, val name: String, val builtIn: DefaultCategory?)

fun Category.toUi(): CategoryUi = CategoryUi(id = id, name = name, builtIn = DefaultCategory.of(id))

@Composable
fun CategoryUi.label(): String = if (builtIn != null) stringResource(builtIn.titleRes()) else name

fun DefaultCategory.titleRes(): Int = when (this) {
    DefaultCategory.FOOD -> R.string.category_food
    DefaultCategory.MEDICINE -> R.string.category_medicine
    DefaultCategory.COSMETICS -> R.string.category_cosmetics
}

fun DefaultCategory.iconRes(): Int = when (this) {
    DefaultCategory.FOOD -> R.drawable.ic_category_food
    DefaultCategory.MEDICINE -> R.drawable.ic_category_medicine
    DefaultCategory.COSMETICS -> R.drawable.ic_category_cosmetics
}
