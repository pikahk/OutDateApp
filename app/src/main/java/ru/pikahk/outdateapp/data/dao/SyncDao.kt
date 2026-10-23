package ru.pikahk.outdateapp.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ru.pikahk.outdateapp.data.model.Category
import ru.pikahk.outdateapp.data.model.Item
import ru.pikahk.outdateapp.data.model.Product
import ru.pikahk.outdateapp.data.model.Subscription

@Dao
interface SyncDao {

    @Query(
        "SELECT (SELECT COUNT(*) FROM categories WHERE isPendingSync = 1) + " +
            "(SELECT COUNT(*) FROM items WHERE isPendingSync = 1) + " +
            "(SELECT COUNT(*) FROM subscriptions WHERE isPendingSync = 1) + " +
            "(SELECT COUNT(*) FROM products WHERE isPendingSync = 1)"
    )
    fun observePendingCount(): Flow<Int>

    @Query("SELECT * FROM categories WHERE isPendingSync = 1 LIMIT :limit")
    suspend fun pendingCategories(limit: Int): List<Category>

    @Query("SELECT * FROM items WHERE isPendingSync = 1 AND familyId IS NULL LIMIT :limit")
    suspend fun pendingItems(limit: Int): List<Item>

    @Query("SELECT * FROM items WHERE isPendingSync = 1 AND familyId = :familyId LIMIT :limit")
    suspend fun pendingFamilyItems(familyId: String, limit: Int): List<Item>

    @Query("SELECT * FROM subscriptions WHERE isPendingSync = 1 LIMIT :limit")
    suspend fun pendingSubscriptions(limit: Int): List<Subscription>

    @Query("SELECT * FROM products WHERE isPendingSync = 1 LIMIT :limit")
    suspend fun pendingProducts(limit: Int): List<Product>

    @Upsert
    suspend fun upsertCategories(categories: List<Category>)

    @Upsert
    suspend fun upsertItems(items: List<Item>)

    @Upsert
    suspend fun upsertSubscriptions(subscriptions: List<Subscription>)

    @Upsert
    suspend fun upsertProducts(products: List<Product>)

    @Query("DELETE FROM categories WHERE isDefault = 0")
    suspend fun deleteCustomCategories()

    @Query("DELETE FROM items")
    suspend fun deleteItems()

    @Query("DELETE FROM subscriptions")
    suspend fun deleteSubscriptions()

    @Query("DELETE FROM products")
    suspend fun deleteProducts()

    @Query("DELETE FROM items WHERE familyId IS NOT NULL")
    suspend fun deleteFamilyItems()

    @Transaction
    suspend fun saveFromServer(
        categories: List<Category>,
        items: List<Item>,
        subscriptions: List<Subscription>,
        products: List<Product>
    ) {
        upsertCategories(categories.notOlderThan(pendingCategories(Int.MAX_VALUE), Category::id, Category::updatedAt))
        upsertItems(items.notOlderThan(pendingItems(Int.MAX_VALUE), Item::id, Item::updatedAt))
        upsertSubscriptions(
            subscriptions.notOlderThan(pendingSubscriptions(Int.MAX_VALUE), Subscription::id, Subscription::updatedAt)
        )
        upsertProducts(products.notOlderThan(pendingProducts(Int.MAX_VALUE), Product::barcode, Product::updatedAt))
    }

    @Transaction
    suspend fun saveFamilyFromServer(familyId: String, items: List<Item>) {
        upsertItems(items.notOlderThan(pendingFamilyItems(familyId, Int.MAX_VALUE), Item::id, Item::updatedAt))
    }

    @Transaction
    suspend fun clearAccountData() {
        deleteItems()
        deleteSubscriptions()
        deleteProducts()
        deleteCustomCategories()
    }
}

private fun <T> List<T>.notOlderThan(pending: List<T>, key: (T) -> String, updatedAt: (T) -> Long): List<T> {
    val local = pending.associate { key(it) to updatedAt(it) }
    return filter { remote -> local[key(remote)]?.let { updatedAt(remote) >= it } ?: true }
}
