package ru.pikahk.outdateapp.data.remote

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpenFactsApiTest {

    private val requests = mutableListOf<HttpRequestData>()

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private fun api(reply: MockRequestHandleScope.() -> HttpResponseData) = OpenFactsApi(
        MockEngine { request ->
            requests += request
            reply()
        },
        userAgent = "outDate/test"
    )

    private fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(body, status, jsonHeaders)

    @Test
    fun `берётся русское название, если оно есть`() = runBlocking {
        val api = api {
            json("""{"status":1,"product":{"product_name":"Kefir","product_name_ru":"Кефир 3,2%"}}""")
        }
        assertEquals("Кефир 3,2%", api.findName(OpenFactsSource.FOOD, "4607001771555"))
        val request = requests.single()
        assertEquals("/api/v2/product/4607001771555", request.url.encodedPath)
        assertEquals("world.openfoodfacts.org", request.url.host)
        assertEquals("product_name,product_name_ru", request.url.parameters["fields"])
        assertEquals("outDate/test", request.headers[HttpHeaders.UserAgent])
    }

    @Test
    fun `без русского названия берётся обычное`() = runBlocking {
        val api = api { json("""{"status":1,"product":{"product_name":"Nivea Creme","product_name_ru":" "}}""") }
        assertEquals("Nivea Creme", api.findName(OpenFactsSource.BEAUTY, "4005808158164"))
        assertEquals("world.openbeautyfacts.org", requests.single().url.host)
    }

    @Test
    fun `товар без названия считается ненайденным`() = runBlocking {
        val api = api { json("""{"status":1,"product":{}}""") }
        assertNull(api.findName(OpenFactsSource.FOOD, "4607001771555"))
    }

    @Test
    fun `ответ 404 значит товар не найден`() = runBlocking {
        val api = api {
            json("""{"status":0,"status_verbose":"product not found"}""", HttpStatusCode.NotFound)
        }
        assertNull(api.findName(OpenFactsSource.FOOD, "4607001771555"))
    }

    @Test
    fun `статус 0 тоже значит товар не найден`() = runBlocking {
        val api = api { json("""{"status":0}""") }
        assertNull(api.findName(OpenFactsSource.FOOD, "4607001771555"))
    }

    @Test
    fun `ошибка базы товаров отличается от ненайденного товара`() = runBlocking {
        val api = api { respond("", HttpStatusCode.ServiceUnavailable) }
        val error = runCatching { api.findName(OpenFactsSource.FOOD, "4607001771555") }.exceptionOrNull()
        assertEquals(ApiException.SERVER, (error as ApiException).code)
    }

    @Test
    fun `без сети ошибка сети`() = runBlocking {
        val api = api { throw IOException("offline") }
        val error = runCatching { api.findName(OpenFactsSource.FOOD, "4607001771555") }.exceptionOrNull()
        assertEquals(ApiException.NETWORK, (error as ApiException).code)
    }
}
