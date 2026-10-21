package ru.pikahk.outdateapp.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.pikahk.outdateapp.data.dao.ProductDao
import ru.pikahk.outdateapp.data.model.DefaultCategory
import ru.pikahk.outdateapp.data.model.Product
import ru.pikahk.outdateapp.data.remote.OpenFactsApi
import ru.pikahk.outdateapp.data.remote.OpenFactsSource

private typealias Reply = MockRequestHandleScope.() -> HttpResponseData

class ProductRepositoryTest {

    private class FakeProductDao : ProductDao {
        val products = mutableMapOf<String, Product>()

        override suspend fun findByBarcode(barcode: String) = products[barcode]

        override suspend fun upsert(product: Product) {
            products[product.barcode] = product
        }

        override suspend fun upsertAll(products: List<Product>) {
            products.forEach { upsert(it) }
        }
    }

    private val barcode = "4607001771555"

    private val dao = FakeProductDao()

    private val hosts = mutableListOf<String>()

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private val found: (String) -> Reply = { name ->
        { respond("""{"status":1,"product":{"product_name":"$name"}}""", HttpStatusCode.OK, jsonHeaders) }
    }

    private val notFound: Reply = { respond("""{"status":0}""", HttpStatusCode.NotFound, jsonHeaders) }

    private val broken: Reply = { respond("", HttpStatusCode.ServiceUnavailable) }

    private fun repository(food: Reply, beauty: Reply) = ProductRepository(
        dao,
        OpenFactsApi(
            MockEngine { request ->
                hosts += request.url.host
                if (request.url.host == "world.openfoodfacts.org") food() else beauty()
            },
            userAgent = "outDate/test"
        )
    )

    @Test
    fun `своя база проверяется первой, без запросов в интернет`() = runBlocking {
        val milk = Product(barcode, "Молоко", DefaultCategory.FOOD.id, 3, 1)
        dao.upsert(milk)
        val result = repository(found("Milk"), found("Milk")).lookup(barcode)
        assertEquals(ProductLookup.Remembered(milk), result)
        assertTrue(hosts.isEmpty())
    }

    @Test
    fun `товар из Open Food Facts попадает в категорию Еда`() = runBlocking {
        val result = repository(found("Кефир"), notFound).lookup(barcode)
        assertEquals(ProductLookup.Found("Кефир", OpenFactsSource.FOOD), result)
        assertEquals(DefaultCategory.FOOD.id, (result as ProductLookup.Found).categoryId)
        assertEquals(listOf("world.openfoodfacts.org"), hosts)
    }

    @Test
    fun `если еды нет, ищется в Open Beauty Facts и попадает в Косметику`() = runBlocking {
        val result = repository(notFound, found("Крем для рук")).lookup(barcode)
        assertEquals(DefaultCategory.COSMETICS.id, (result as ProductLookup.Found).categoryId)
        assertEquals(listOf("world.openfoodfacts.org", "world.openbeautyfacts.org"), hosts)
    }

    @Test
    fun `нигде нет — товар не найден`() = runBlocking {
        assertEquals(ProductLookup.NotFound, repository(notFound, notFound).lookup(barcode))
    }

    @Test
    fun `без сети сразу сообщается об этом`() = runBlocking {
        val offline: Reply = { throw IOException("offline") }
        assertEquals(ProductLookup.Offline, repository(offline, offline).lookup(barcode))
        assertEquals(1, hosts.size)
    }

    @Test
    fun `сбой одной базы не мешает найти товар в другой`() = runBlocking {
        val result = repository(broken, found("Шампунь")).lookup(barcode)
        assertEquals(ProductLookup.Found("Шампунь", OpenFactsSource.BEAUTY), result)
    }

    @Test
    fun `если базы не ответили, это не то же самое, что не найдено`() = runBlocking {
        assertEquals(ProductLookup.Unavailable, repository(broken, notFound).lookup(barcode))
    }

    @Test
    fun `сохранённый товар запоминается и ждёт отправки на сервер`() = runBlocking {
        repository(notFound, notFound).remember(barcode, "Кефир", DefaultCategory.FOOD.id, 3, 1)
        val saved = dao.products.getValue(barcode)
        assertEquals("Кефир", saved.name)
        assertEquals(3, saved.defaultDaysAfterOpening)
        assertTrue(saved.isPendingSync)
    }
}
