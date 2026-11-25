package com.example.urlshortener

import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class BitlyClient(private val accessToken: String) {
    fun shorten(longUrl: String): String {
        if (accessToken.isBlank()) {
            throw IOException(
                "Bitly is not configured. Add BITLY_ACCESS_TOKEN to local.properties.",
            )
        }

        val connection = URL("https://api-ssl.bitly.com/v4/shorten")
            .openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")

            val requestBody = JSONObject().put("long_url", longUrl).toString()
            connection.outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(requestBody)
            }

            val statusCode = connection.responseCode
            val response = (if (statusCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            })?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

            if (statusCode !in 200..299) {
                val message = try {
                    JSONObject(response).optString("message")
                } catch (_: JSONException) {
                    ""
                }
                throw IOException(
                    message.ifBlank { "Bitly returned HTTP $statusCode." },
                )
            }

            val shortUrl = try {
                JSONObject(response).getString("link")
            } catch (exception: JSONException) {
                throw IOException("Bitly returned an invalid response.", exception)
            }
            if (shortUrl.isBlank()) {
                throw IOException("Bitly returned an empty short link.")
            }

            return shortUrl
        } finally {
            connection.disconnect()
        }
    }
}
