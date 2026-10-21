package ru.pikahk.outdateapp.data.repository

import ru.pikahk.outdateapp.data.dao.ProductDao
import ru.pikahk.outdateapp.data.model.DefaultCategory
import ru.pikahk.outdateapp.data.model.Product
import ru.pikahk.outdateapp.data.remote.ApiException
import ru.pikahk.outdateapp.data.remote.OpenFactsApi
import ru.pikahk.outdateapp.data.remote.OpenFactsSource

sealed interface ProductLookup {
    data class Remembered(val product: Product) : ProductLookup

    data class Found(val name: String, val source: OpenFactsSource) : ProductLookup {
        val categoryId: String
            get() = when (source) {
                OpenFactsSource.FOOD -> DefaultCategory.FOOD.id
                OpenFactsSource.BEAUTY -> DefaultCategory.COSMETICS.id
            }
    }

    data object NotFound : ProductLookup

    data object Offline : ProductLookup

    data object Unavailable : ProductLookup
}

class ProductRepository(private val dao: ProductDao, private val openFacts: OpenFactsApi) {

    suspend fun lookup(barcode: String): ProductLookup {
        dao.findByBarcode(barcode)?.let { return ProductLookup.Remembered(it) }
        var unavailable = false
        for (source in OpenFactsSource.entries) {
            try {
                val name = openFacts.findName(source, barcode)
                if (name != null) return ProductLookup.Found(name, source)
            } catch (e: ApiException) {
                if (e.code == ApiException.NETWORK) return ProductLookup.Offline
                unavailable = true
            }
        }
        return if (unavailable) ProductLookup.Unavailable else ProductLookup.NotFound
    }

    suspend fun remember(
        barcode: String,
        name: String,
        categoryId: String?,
        daysAfterOpening: Int?,
        notifyDaysBefore: Int
    ) {
        dao.upsert(
            Product(
                barcode = barcode,
                name = name,
                categoryId = categoryId,
                defaultDaysAfterOpening = daysAfterOpening,
                defaultNotifyDaysBefore = notifyDaysBefore,
                updatedAt = System.currentTimeMillis(),
                isPendingSync = true
            )
        )
    }
}
