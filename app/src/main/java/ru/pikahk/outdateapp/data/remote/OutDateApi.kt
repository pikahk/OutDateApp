package ru.pikahk.outdateapp.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.ContentConvertException
import io.ktor.serialization.kotlinx.json.json
import java.io.IOException
import kotlinx.serialization.json.Json

private const val CONNECT_TIMEOUT_MS = 10_000L
private const val REQUEST_TIMEOUT_MS = 20_000L

class OutDateApi(baseUrl: String, engine: HttpClientEngine) {

    private val client = HttpClient(engine) {
        expectSuccess = false
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                }
            )
        }
        install(HttpTimeout) {
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
        }
        defaultRequest { url(baseUrl) }
    }

    suspend fun register(email: String, password: String): AuthResponse =
        post("auth/register") { setJson(CredentialsRequest(email, password)) }.read()

    suspend fun login(email: String, password: String): AuthResponse =
        post("auth/login") { setJson(CredentialsRequest(email, password)) }.read()

    suspend fun logout(token: String) {
        post("auth/logout") { bearerAuth(token) }
    }

    suspend fun me(token: String): MeResponse = send(HttpMethod.Get, "me") { bearerAuth(token) }.read()

    suspend fun verifyEmail(token: String, code: String) {
        post("auth/email/verify") {
            bearerAuth(token)
            setJson(CodeRequest(code))
        }
    }

    suspend fun resendCode(token: String) {
        post("auth/email/resend") { bearerAuth(token) }
    }

    suspend fun forgotPassword(email: String) {
        post("auth/password/forgot") { setJson(EmailRequest(email)) }
    }

    suspend fun resetPassword(email: String, code: String, password: String): AuthResponse =
        post("auth/password/reset") { setJson(ResetPasswordRequest(email, code, password)) }.read()

    private suspend fun post(path: String, block: HttpRequestBuilder.() -> Unit): HttpResponse =
        send(HttpMethod.Post, path, block)

    private suspend fun send(method: HttpMethod, path: String, block: HttpRequestBuilder.() -> Unit): HttpResponse {
        val response = try {
            client.request {
                this.method = method
                url(path)
                block()
            }
        } catch (e: IOException) {
            throw ApiException(ApiException.NETWORK, e)
        }
        if (!response.status.isSuccess()) throw ApiException(errorCode(response))
        return response
    }

    private suspend fun errorCode(response: HttpResponse): String = try {
        response.body<ErrorResponse>().error
    } catch (e: ContentConvertException) {
        ApiException.SERVER
    } catch (e: NoTransformationFoundException) {
        ApiException.SERVER
    }

    private suspend inline fun <reified T> HttpResponse.read(): T = try {
        body()
    } catch (e: ContentConvertException) {
        throw ApiException(ApiException.SERVER, e)
    } catch (e: NoTransformationFoundException) {
        throw ApiException(ApiException.SERVER, e)
    }

    private inline fun <reified T> HttpRequestBuilder.setJson(value: T) {
        contentType(ContentType.Application.Json)
        setBody(value)
    }
}
