package com.chukta

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*

val ChuktaJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }
class ApiError(val status: Int, message: String): Exception(message)
class Api(val platform: Platform) {
    var token: String? = platform.read("session")
    val client = HttpClient {
        install(ContentNegotiation) { json(ChuktaJson) }
        install(HttpTimeout) { requestTimeoutMillis = 12000; connectTimeoutMillis = 6000 }
    }
    suspend inline fun <reified T> get(path: String): T = check(client.get(platform.apiUrl.trimEnd('/') + path) { token?.let { bearerAuth(it) } }).body()
    suspend inline fun <reified B, reified T> send(path: String, body: B, method: HttpMethod = HttpMethod.Post): T =
        check(client.request(platform.apiUrl.trimEnd('/') + path) {
            this.method = method
            contentType(ContentType.Application.Json)
            token?.let { bearerAuth(it) }
            setBody(body)
        }).body()
    suspend fun check(response: HttpResponse): HttpResponse {
        if (!response.status.isSuccess()) {
            val message = runCatching { ChuktaJson.parseToJsonElement(response.bodyAsText()).jsonObject["error"]?.jsonPrimitive?.content }.getOrNull()
            throw ApiError(response.status.value, message ?: "Request failed. Please retry.")
        }
        return response
    }
}
