package com.example.urlshortener

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URISyntaxException
import java.net.URL

private val Ink = Color(0xFF17233A)
private val MutedInk = Color(0xFF65728A)
private val Accent = Color(0xFF5267E8)
private val Canvas = Color(0xFFF5F7FC)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LinkShortTheme {
                LinkShortScreen(
                    onShorten = { url -> shortenUrl(url) },
                )
            }
        }
    }

    private suspend fun shortenUrl(url: String): String = withContext(Dispatchers.IO) {
        val requestUrl = Uri.parse("https://is.gd/create.php")
            .buildUpon()
            .appendQueryParameter("format", "simple")
            .appendQueryParameter("url", url)
            .build()
            .toString()
        val connection = URL(requestUrl).openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000

            val statusCode = connection.responseCode
            val response = (if (statusCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            })?.bufferedReader()?.use { it.readText().trim() }.orEmpty()

            if (statusCode !in 200..299) {
                throw IOException("The shortening service returned HTTP $statusCode.")
            }
            if (response.startsWith("Error:", ignoreCase = true) || response.isBlank()) {
                throw IOException(response.ifBlank { "The shortening service returned an empty response." })
            }

            response
        } finally {
            connection.disconnect()
        }
    }
}

@Composable
private fun LinkShortTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = Accent,
            onPrimary = Color.White,
            background = Canvas,
            surface = Color.White,
            onSurface = Ink,
        ),
        content = content,
    )
}

@Composable
private fun LinkShortScreen(onShorten: suspend (String) -> String) {
    val context = LocalContext.current
    var originalUrl by rememberSaveable { mutableStateOf("") }
    var shortUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Canvas,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = "LINKSHORT",
                style = MaterialTheme.typography.labelLarge,
                color = Accent,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Short links.\nBig possibilities.",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Ink,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Turn a long URL into a link that's easy to share.",
                style = MaterialTheme.typography.bodyLarge,
                color = MutedInk,
            )
            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Your long URL",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = originalUrl,
                        onValueChange = {
                            originalUrl = it
                            errorMessage = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://example.com/my-long-link") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        enabled = !isLoading,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val cleanedUrl = originalUrl.trim()
                            if (!isValidHttpUrl(cleanedUrl)) {
                                errorMessage = "Enter a valid URL starting with http:// or https://."
                                shortUrl = null
                            } else {
                                isLoading = true
                                errorMessage = null
                                shortUrl = null
                                scope.launch {
                                    try {
                                        shortUrl = onShorten(cleanedUrl)
                                    } catch (exception: CancellationException) {
                                        throw exception
                                    } catch (exception: IOException) {
                                        errorMessage = exception.message
                                            ?: "Couldn't connect to the shortening service."
                                    } catch (exception: Exception) {
                                        errorMessage = exception.message
                                            ?: "Something went wrong while shortening the URL."
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                            Text(
                                text = "  Shortening…",
                                fontWeight = FontWeight.SemiBold,
                            )
                        } else {
                            Text("Shorten URL", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            shortUrl?.let { result ->
                Spacer(modifier = Modifier.height(20.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Your short link is ready",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Ink,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = result,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Accent,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TextButton(
                                onClick = { copyToClipboard(context, result) },
                            ) {
                                Text("Copy")
                            }
                            TextButton(
                                onClick = {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(result)),
                                    )
                                },
                            ) {
                                Text("Open")
                            }
                            TextButton(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, result)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(shareIntent, "Share short link"),
                                    )
                                },
                            ) {
                                Text("Share")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "Free link shortening by is.gd",
                style = MaterialTheme.typography.bodySmall,
                color = MutedInk,
            )
        }
    }
}

private fun isValidHttpUrl(value: String): Boolean {
    if (value.isBlank() || value.any(Char::isWhitespace)) return false

    return try {
        val uri = URI(value)
        (uri.scheme.equals("http", ignoreCase = true) ||
            uri.scheme.equals("https", ignoreCase = true)) &&
            !uri.host.isNullOrBlank()
    } catch (_: URISyntaxException) {
        false
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Short URL", text))
}
