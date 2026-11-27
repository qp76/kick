package com.kicklite.presentation

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import com.kicklite.ui.theme.KickLiteTheme

private val BrowserAccent = Color(0xFF53FC18)
private val BrowserGlass = Color(0xE91A211C)

class KickWebActivity : ComponentActivity() {
    private var activeWebView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            KickLiteTheme(darkTheme = true, dynamicColor = false) {
                var progress by remember { mutableIntStateOf(0) }
                var pageTitle by remember { mutableStateOf("KICK") }
                var canGoBack by remember { mutableStateOf(false) }
                var pageFailed by remember { mutableStateOf(false) }

                BackHandler(enabled = canGoBack) { activeWebView?.goBack() }

                Column(
                    Modifier
                        .fillMaxSize()
                        .background(Color(0xFF090D0A))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xF12A332D), BrowserGlass)
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                            .padding(horizontal = 6.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (activeWebView?.canGoBack() == true) activeWebView?.goBack()
                                else finish()
                            }
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("KICK", color = BrowserAccent, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                            Text(
                                pageTitle,
                                color = Color.White.copy(alpha = 0.72f),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        }
                        IconButton(onClick = { pageFailed = false; activeWebView?.reload() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reload KICK", tint = Color.White)
                        }
                    }

                    if (progress in 1..99) {
                        LinearProgressIndicator(
                            progress = progress / 100f,
                            modifier = Modifier.fillMaxWidth().height(2.dp),
                            color = BrowserAccent,
                            trackColor = Color.Transparent
                        )
                    }

                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { context ->
                                WebView(context).apply {
                                    activeWebView = this
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    setBackgroundColor(android.graphics.Color.rgb(9, 13, 10))
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.databaseEnabled = true
                                    settings.loadsImagesAutomatically = true
                                    settings.mediaPlaybackRequiresUserGesture = false
                                    settings.javaScriptCanOpenWindowsAutomatically = false
                                    settings.setSupportMultipleWindows(false)
                                    CookieManager.getInstance().setAcceptCookie(true)
                                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                                    webChromeClient = object : WebChromeClient() {
                                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                            progress = newProgress
                                        }

                                        override fun onReceivedTitle(view: WebView?, title: String?) {
                                            pageTitle = title?.take(48)?.ifBlank { "KICK" } ?: "KICK"
                                        }
                                    }

                                    webViewClient = object : WebViewClient() {
                                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                            pageFailed = false
                                            canGoBack = view?.canGoBack() == true
                                        }

                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            canGoBack = view?.canGoBack() == true
                                            CookieManager.getInstance().flush()
                                        }

                                        override fun onReceivedError(
                                            view: WebView?,
                                            request: WebResourceRequest?,
                                            error: android.webkit.WebResourceError?
                                        ) {
                                            if (request?.isForMainFrame == true) pageFailed = true
                                        }

                                        override fun shouldOverrideUrlLoading(
                                            view: WebView?,
                                            request: WebResourceRequest?
                                        ): Boolean {
                                            val uri = request?.url ?: return false
                                            val host = uri.host.orEmpty().lowercase()
                                            if (uri.scheme == "https" && (host == "kick.com" || host.endsWith(".kick.com"))) {
                                                return false
                                            }
                                            if (uri.scheme == "https" || uri.scheme == "http") {
                                                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                                            }
                                            return true
                                        }
                                    }
                                    loadUrl("https://kick.com/")
                                }
                            },
                            update = { activeWebView = it }
                        )

                        if (progress == 0) {
                            CircularProgressIndicator(
                                Modifier.align(Alignment.Center).size(30.dp),
                                color = BrowserAccent,
                                strokeWidth = 2.dp
                            )
                        }

                        if (pageFailed) {
                            Column(
                                Modifier.align(Alignment.Center).padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("KICK couldn't load", color = Color.White, fontWeight = FontWeight.Bold)
                                Text("Check your connection and try again.", color = Color.White.copy(alpha = 0.65f))
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 18.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            Modifier
                                .clip(CircleShape)
                                .background(BrowserGlass)
                                .border(1.dp, Color.White.copy(alpha = 0.09f), CircleShape)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(BrowserAccent))
                            Text("  Official KICK website  ·  kick.com", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                DisposableEffect(Unit) {
                    onDispose { activeWebView = null }
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onDestroy() {
        activeWebView?.apply {
            stopLoading()
            loadUrl("about:blank")
            clearHistory()
            removeAllViews()
            destroy()
        }
        activeWebView = null
        super.onDestroy()
    }
}
