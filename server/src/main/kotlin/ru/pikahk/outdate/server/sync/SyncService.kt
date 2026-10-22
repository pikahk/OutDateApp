package ru.pikahk.outdate.server.sync

import io.ktor.http.HttpStatusCode
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import ru.pikahk.outdate.server.auth.UserPrincipal
import ru.pikahk.outdate.server.db.CategoriesTable
import ru.pikahk.outdate.server.db.ItemsTable
import ru.pikahk.outdate.server.db.ProductsTable
import ru.pikahk.outdate.server.db.SubscriptionsTable
import ru.pikahk.outdate.server.db.UsersTable
import ru.pikahk.outdate.server.plugins.ApiException

class SyncService(private val database: Database) {

    suspend fun sync(user: UserPrincipal, request: SyncRequest): SyncResponse {
        if (!user.emailVerified) throw ApiException(HttpStatusCode.Forbidden, "email_not_verified")
        validate(request)
        return withContext(Dispatchers.IO) {
            transaction(database) {
                val current = lockVersion(user.id)
                val since = if (request.since in 0..current) request.since else 0
                val next = current + 1
                val received = CategoriesTable.save(user.id, next, request.categories) { fill(it) } +
                    ItemsTable.save(user.id, next, request.items) { fill(ItemsTable, it) } +
                    SubscriptionsTable.save(user.id, next, request.subscriptions) { fill(it) } +
                    ProductsTable.save(user.id, next, request.products) { fill(it) }
                val version = if (received > 0) next else current
                if (received > 0) UsersTable.update({ UsersTable.id eq user.id }) { it[syncVersion] = version }
                SyncResponse(
                    version = version,
                    categories = CategoriesTable.changedSince(user.id, since) { it.toCategory() },
                    items = ItemsTable.changedSince(user.id, since) { it.toItem(ItemsTable) },
                    subscriptions = SubscriptionsTable.changedSince(user.id, since) { it.toSubscription() },
                    products = ProductsTable.changedSince(user.id, since) { it.toProduct() }
                )
            }
        }
    }

    private fun validate(request: SyncRequest) {
        checkRecords(request.categories, request.items, request.subscriptions, request.products)
    }

    private fun lockVersion(owner: UUID): Long = UsersTable
        .select(UsersTable.syncVersion)
        .where { UsersTable.id eq owner }
        .forUpdate()
        .single()[UsersTable.syncVersion]
}
