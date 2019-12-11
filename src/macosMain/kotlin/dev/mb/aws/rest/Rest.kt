package dev.mb.aws.rest

import dev.mb.aws.HttpRequest
import dev.mb.aws.Method
import io.ktor.client.HttpClient
import io.ktor.client.features.ClientRequestException
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.url
import io.ktor.client.response.readText
import io.ktor.http.HttpMethod

class RestClient {

    private val client = HttpClient()

    suspend fun execute(request: HttpRequest): String {
        try {
            return client.request {
                method = when (request.method) {
                    Method.GET -> HttpMethod.Get
                    Method.POST -> HttpMethod.Post
                }

                url(request.url)

                request.headers.forEach { header(it.name, it.value) }

                body = request.payload
            }
        } catch (e: ClientRequestException) {
            val text = e.response.readText() // TODO exception
            println(text)
            e.printStackTrace()
            throw e
        }
    }

}

