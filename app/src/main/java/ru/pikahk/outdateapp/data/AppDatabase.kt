package ru.pikahk.outdateapp.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ru.pikahk.outdateapp.data.dao.CategoryDao
import ru.pikahk.outdateapp.data.dao.ItemDao
import ru.pikahk.outdateapp.data.dao.ProductDao
import ru.pikahk.outdateapp.data.dao.SubscriptionDao
import ru.pikahk.outdateapp.data.model.Category
import ru.pikahk.outdateapp.data.model.Item
import ru.pikahk.outdateapp.data.model.Product
import ru.pikahk.outdateapp.data.model.Subscription

@Database(
    entities = [
        Item::class,
        Category::class,
        Product::class,
        Subscription::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun subscriptionDao(): SubscriptionDao
}
