package ru.pikahk.outdateapp.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import ru.pikahk.outdateapp.data.dao.SyncDao
import ru.pikahk.outdateapp.data.model.Category
import ru.pikahk.outdateapp.data.model.Item
import ru.pikahk.outdateapp.data.model.Product
import ru.pikahk.outdateapp.data.model.Subscription

internal class FakeSyncDao : SyncDao {
    val categories = MutableStateFlow<Map<String, Category>>(emptyMap())
    val items = MutableStateFlow<Map<String, Item>>(emptyMap())
    val subscriptions = MutableStateFlow<Map<String, Subscription>>(emptyMap())
    val products = MutableStateFlow<Map<String, Product>>(emptyMap())

    override fun observePendingCount(): Flow<Int> = combine(categories, items, subscriptions, products) { c, i, s, p ->
        c.values.count { it.isPendingSync } +
            i.values.count { it.isPendingSync } +
            s.values.count { it.isPendingSync } +
            p.values.count { it.isPendingSync }
    }

    override suspend fun pendingCategories(limit: Int) = categories.value.values.filter { it.isPendingSync }.take(limit)

    override suspend fun pendingItems(limit: Int) = items.value.values.filter { it.isPendingSync }.take(limit)

    override suspend fun pendingSubscriptions(limit: Int) =
        subscriptions.value.values.filter { it.isPendingSync }.take(limit)

    override suspend fun pendingProducts(limit: Int) = products.value.values.filter { it.isPendingSync }.take(limit)

    override suspend fun upsertCategories(categories: List<Category>) {
        this.categories.update { it + categories.associateBy(Category::id) }
    }

    override suspend fun upsertItems(items: List<Item>) {
        this.items.update { it + items.associateBy(Item::id) }
    }

    override suspend fun upsertSubscriptions(subscriptions: List<Subscription>) {
        this.subscriptions.update { it + subscriptions.associateBy(Subscription::id) }
    }

    override suspend fun upsertProducts(products: List<Product>) {
        this.products.update { it + products.associateBy(Product::barcode) }
    }

    override suspend fun deleteCustomCategories() {
        categories.update { map -> map.filterValues { it.isDefault } }
    }

    override suspend fun deleteItems() {
        items.value = emptyMap()
    }

    override suspend fun deleteSubscriptions() {
        subscriptions.value = emptyMap()
    }

    override suspend fun deleteProducts() {
        products.value = emptyMap()
    }
}
