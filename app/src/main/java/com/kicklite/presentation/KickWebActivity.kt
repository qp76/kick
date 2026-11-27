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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import com.kicklite.ui.theme.KickLiteTheme

class KickWebActivity : ComponentActivity() {
    private var activeWebView: WebView? = null

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContent {
            KickLiteTheme(darkTheme = true, dynamicColor = false) {
                var progress by remember { mutableIntStateOf(0) }
                var pageTitle by remember { mutableStateOf("KICK") }
                var canGoBack by remember { mutableStateOf(false) }
                BackHandler(enabled = canGoBack) { activeWebView?.goBack() }
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(pageTitle, maxLines = 1) },
                            navigationIcon = {
                                IconButton(onClick = {
                                    if (activeWebView?.canGoBack() == true) activeWebView?.goBack() else finish()
                                }) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                            },
                            actions = {
                                IconButton(onClick = { activeWebView?.reload() }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Reload KICK")
                                }
                            }
                        )
                    }
                ) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding)) {
                        if (progress in 1..99) {
                            LinearProgressIndicator(progress = progress / 100f, modifier = Modifier.fillMaxWidth().height(2.dp))
                        }
                        Box(Modifier.fillMaxSize()) {
                            AndroidView(
                                modifier = Modifier.fillMaxSize(),
                                factory = { context ->
                                    WebView(context).apply {
                                        activeWebView = this
                                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
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
                                            override fun onProgressChanged(view: WebView?, newProgress: Int) { progress = newProgress }
                                            override fun onReceivedTitle(view: WebView?, title: String?) { pageTitle = title?.take(48)?.ifBlank { "KICK" } ?: "KICK" }
                                        }
                                        webViewClient = object : WebViewClient() {
                                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) { canGoBack = view?.canGoBack() == true }
                                            override fun onPageFinished(view: WebView?, url: String?) {
                                                canGoBack = view?.canGoBack() == true
                                                CookieManager.getInstance().flush()
                                            }
                                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                                val uri = request?.url ?: return false
                                                val host = uri.host.orEmpty().lowercase()
                                                if (uri.scheme == "https" && (host == "kick.com" || host.endsWith(".kick.com"))) return false
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
                            if (progress == 0) CircularProgressIndicator(Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                DisposableEffect(Unit) { onDispose { activeWebView = null } }
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
