package com.mlacker.samples.cloud.netflix.ribbon.http

import com.netflix.client.http.CaseInsensitiveMultiMap
import com.netflix.client.http.HttpHeaders
import java.io.InputStream
import java.lang.reflect.Type
import java.net.URI
import java.net.http.HttpResponse

class HttpRibbonResponse(private val response: HttpResponse<InputStream>, private val uri: URI) :
    com.netflix.client.http.HttpResponse {

    override fun getPayload(): Any? {
        return response.body()
    }

    override fun hasPayload(): Boolean {
        return response.body() != null
    }

    override fun isSuccess(): Boolean {
        return response.statusCode().div(100) == 2
    }

    override fun getRequestedURI(): URI {
        return this.uri
    }

    override fun getStatus(): Int {
        return response.statusCode()
    }

    override fun getStatusLine(): String {
        return response.statusCode().toString()
    }

    override fun getHeaders(): MutableMap<String, MutableCollection<String>> {
        return HashMap(response.headers().map())
    }

    override fun getHttpHeaders(): HttpHeaders {
        val headers = CaseInsensitiveMultiMap()
        response.headers().map().forEach { (key, values) ->
            values.forEach {
                headers.addHeader(key, it)
            }
        }
        return headers
    }

    override fun close() {
        response.body()?.close()
    }


    override fun getInputStream(): InputStream {
        return response.body()
    }

    override fun hasEntity(): Boolean {
        return hasPayload()
    }

    override fun <T : Any?> getEntity(type: Class<T>?): T? {
        return null
    }

    override fun <T : Any?> getEntity(type: Type?): T? {
        return null
    }

    override fun <T : Any?> getEntity(type: com.google.common.reflect.TypeToken<T>?): T? {
        return null
    }
}