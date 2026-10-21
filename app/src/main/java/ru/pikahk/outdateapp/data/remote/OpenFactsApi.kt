package ru.pikahk.outdateapp.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.serialization.ContentConvertException
import io.ktor.serialization.kotlinx.json.json
import java.io.IOException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val TIMEOUT_MS = 10_000L
private const val FIELDS = "product_name,product_name_ru"

enum class OpenFactsSource(val baseUrl: String) {
    FOOD("https://world.openfoodfacts.org/"),
    BEAUTY("https://world.openbeautyfacts.org/")
}

@Serializable
private data class OpenFactsProduct(
    @SerialName("product_name") val name: String? = null,
    @SerialName("product_name_ru") val nameRu: String? = null
)

@Serializable
private data class OpenFactsResponse(val status: Int = 0, val product: OpenFactsProduct? = null)

class OpenFactsApi(engine: HttpClientEngine, userAgent: String) {

    private val client = HttpClient(engine) {
        expectSuccess = false
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(HttpTimeout) {
            connectTimeoutMillis = TIMEOUT_MS
            requestTimeoutMillis = TIMEOUT_MS
        }
        install(UserAgent) {
            agent = userAgent
        }
    }

    suspend fun findName(source: OpenFactsSource, barcode: String): String? {
        val response = try {
            client.get("${source.baseUrl}api/v2/product/$barcode") { parameter("fields", FIELDS) }
        } catch (e: IOException) {
            throw ApiException(ApiException.NETWORK, e)
        }
        if (response.status == HttpStatusCode.NotFound) return null
        if (!response.status.isSuccess()) throw ApiException(ApiException.SERVER)
        val body = try {
            response.body<OpenFactsResponse>()
        } catch (e: ContentConvertException) {
            throw ApiException(ApiException.SERVER, e)
        } catch (e: NoTransformationFoundException) {
            throw ApiException(ApiException.SERVER, e)
        }
        if (body.status != 1) return null
        return listOfNotNull(body.product?.nameRu, body.product?.name)
            .map(String::trim)
            .firstOrNull(String::isNotEmpty)
    }
}
