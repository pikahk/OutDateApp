package ru.pikahk.outdateapp.ui.add

import androidx.annotation.StringRes
import ru.pikahk.outdateapp.R
import ru.pikahk.outdateapp.data.remote.OpenFactsSource
import ru.pikahk.outdateapp.data.repository.ProductLookup

enum class ScanNote(@StringRes val title: Int, @StringRes val text: Int?, val success: Boolean) {
    REMEMBERED(R.string.scan_remembered, R.string.scan_enter_date, true),
    FOUND_FOOD(R.string.scan_found_food, R.string.scan_check_name, true),
    FOUND_BEAUTY(R.string.scan_found_beauty, R.string.scan_check_name, true),
    NOT_FOUND(R.string.scan_not_found, R.string.scan_fill_manually, false),
    OFFLINE(R.string.scan_offline, R.string.scan_fill_manually, false),
    UNAVAILABLE(R.string.scan_unavailable, R.string.scan_fill_manually, false),
    NO_CAMERA(R.string.scan_no_camera, null, false),
    PRODUCTION_DATE(R.string.date_production_found, R.string.date_enter_shelf_life, true)
}

fun ProductLookup.toNote(): ScanNote = when (this) {
    is ProductLookup.Remembered -> ScanNote.REMEMBERED
    is ProductLookup.Found -> if (source == OpenFactsSource.FOOD) ScanNote.FOUND_FOOD else ScanNote.FOUND_BEAUTY
    ProductLookup.NotFound -> ScanNote.NOT_FOUND
    ProductLookup.Offline -> ScanNote.OFFLINE
    ProductLookup.Unavailable -> ScanNote.UNAVAILABLE
}
