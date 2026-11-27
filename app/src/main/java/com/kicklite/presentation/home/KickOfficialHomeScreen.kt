package com.kicklite.presentation.home

import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.kicklite.domain.model.LiveStream

private val KickWebBackground = Color(0xFF090D0A)
private val KickWebAccent = Color(0xFF53FC18)

/**
 * Official KICK is the discovery surface, hosted inside the app's own navigation shell.
 * Known live channels are handed to the native Media3 player instead of playing in WebView.
 */
@Composable
fun KickOfficialHomeScreen(
    onStreamClick: (LiveStream) -> Unit,
    onChannelClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var progress by remember { mutableIntStateOf(0) }
    var pageLoaded by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }

    BackHandler(enabled = canGoBack) {
        webView?.goBack()
    }

    Box(modifier.fillMaxSize().background(KickWebBackground)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    webView = this
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(android.graphics.Color.rgb(9, 13, 10))
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    settings.loadsImagesAutomatically = true
                    settings.mediaPlaybackRequiresUserGesture = true
                    settings.javaScriptCanOpenWindowsAutomatically = false
                    settings.setSupportMultipleWindows(false)
                    settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            progress = newProgress
                            if (newProgress >= 100) pageLoaded = true
                        }
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            pageLoaded = false
                            canGoBack = view?.canGoBack() == true
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            pageLoaded = true
                            canGoBack = view?.canGoBack() == true
                            CookieManager.getInstance().flush()
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: android.webkit.WebResourceError?
                        ) {
                            if (request?.isForMainFrame == true) pageLoaded = false
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val uri = request?.url ?: return false
                            val host = uri.host.orEmpty().lowercase()
                            if (uri.scheme != "https" || (host != "kick.com" && !host.endsWith(".kick.com"))) {
                                return true
                            }
                            if (request.isForMainFrame) {
                                val slug = uri.pathSegments.firstOrNull()?.takeIf {
                                    it.isNotBlank() && it !in setOf(
                                        "categories", "category", "videos", "clips", "search",
                                        "following", "settings", "login", "signup", "subscriptions",
                                        "dashboard", "terms", "privacy", "api"
                                    )
                                }
                                if (slug != null) {
                                    val matchingLive = state.streams.firstOrNull {
                                        it.channel.slug.equals(slug, ignoreCase = true)
                                    }
                                    if (matchingLive != null) {
                                        onStreamClick(matchingLive)
                                        return true
                                    }
                                }
                            }
                            return false
                        }
                    }
                    loadUrl("https://kick.com/")
                }
            },
            update = { webView = it }
        )

        if (!pageLoaded || progress in 1..99) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center).size(28.dp),
                color = KickWebAccent,
                strokeWidth = 2.dp
            )
        }

        FloatingActionButton(
            onClick = onSettingsClick,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 14.dp, end = 14.dp).size(44.dp),
            containerColor = Color(0xDD1A211C),
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Settings")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // Keep cookies/session, but release the native view cleanly when Home leaves composition.
            webView?.apply {
                stopLoading()
                (parent as? android.view.ViewGroup)?.removeView(this)
                destroy()
            }
            webView = null
        }
    }
}
