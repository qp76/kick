package com.kicklite.presentation.home

import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.collectAsState
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
private val kickReservedRoutes = setOf(
    "categories", "category", "videos", "clips", "search", "following",
    "settings", "login", "signup", "subscriptions", "dashboard", "terms",
    "privacy", "api", "auth", "browse", "directory"
)

private class KickWebBridge(
    private val onPath: (String) -> Unit,
    private val onChatFocus: (Boolean) -> Unit
) {
    @JavascriptInterface fun route(path: String) = onPath(path)
    @JavascriptInterface fun chatFocus(focused: Boolean) = onChatFocus(focused)
}

/**
 * KICK owns discovery, account UI and chat. The native shell owns navigation,
 * settings and native playback routing.
 */
@Composable
fun KickOfficialHomeScreen(
    onStreamClick: (LiveStream) -> Unit,
    onChannelClick: (String) -> Unit,
    onChatFocusChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val latestStreams by rememberUpdatedState(state.streams)
    val latestStreamClick by rememberUpdatedState(onStreamClick)
    val latestChannelClick by rememberUpdatedState(onChannelClick)
    val latestChatFocus by rememberUpdatedState(onChatFocusChanged)
    var progress by remember { mutableIntStateOf(0) }
    var pageLoaded by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }

    fun routeKickPath(path: String) {
        val uri = android.net.Uri.parse(path)
        val segments = uri.pathSegments
        val slug = segments.firstOrNull()?.takeIf {
            it.isNotBlank() && it.lowercase() !in kickReservedRoutes
        } ?: return
        val live = latestStreams.firstOrNull { it.channel.slug.equals(slug, ignoreCase = true) }
        if (live != null) latestStreamClick(live) else latestChannelClick(slug)
    }

    BackHandler(enabled = canGoBack) { webView?.goBack() }

    Box(modifier.fillMaxSize().background(KickWebBackground)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    webView = this
                    setBackgroundColor(android.graphics.Color.rgb(9, 13, 10))
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    settings.loadsImagesAutomatically = true
                    settings.mediaPlaybackRequiresUserGesture = true
                    settings.javaScriptCanOpenWindowsAutomatically = false
                    settings.setSupportMultipleWindows(false)
                    settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                    settings.setSupportZoom(false)
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    addJavascriptInterface(
                        KickWebBridge(
                            onPath = { path -> post { routeKickPath(path) } },
                            onChatFocus = { focused -> post { latestChatFocus(focused) } }
                        ),
                        "KickNative"
                    )
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
                            view?.evaluateJavascript(KICK_NAVIGATION_BRIDGE, null)
                            view?.evaluateJavascript(KICK_NATIVE_VIDEO_GUARD, null)
                        }
                        override fun onReceivedError(
                            view: WebView?, request: WebResourceRequest?,
                            error: android.webkit.WebResourceError?
                        ) {
                            if (request?.isForMainFrame == true) pageLoaded = true
                        }
                        override fun shouldOverrideUrlLoading(
                            view: WebView?, request: WebResourceRequest?
                        ): Boolean {
                            val uri = request?.url ?: return false
                            val host = uri.host.orEmpty().lowercase()
                            if (uri.scheme != "https" || (host != "kick.com" && !host.endsWith(".kick.com"))) return true
                            if (request.isForMainFrame) {
                                val path = uri.path.orEmpty()
                                val slug = uri.pathSegments.firstOrNull()
                                if (slug != null && slug.lowercase() !in kickReservedRoutes) {
                                    routeKickPath(path)
                                    return true
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
                modifier = Modifier.align(Alignment.Center),
                color = KickWebAccent,
                strokeWidth = 2.dp
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            latestChatFocus(false)
            webView?.apply {
                stopLoading()
                removeJavascriptInterface("KickNative")
                (parent as? android.view.ViewGroup)?.removeView(this)
                destroy()
            }
            webView = null
        }
    }
}

private val KICK_NAVIGATION_BRIDGE = """
(function() {
  if (window.__kickNativeBridgeInstalled) return;
  window.__kickNativeBridgeInstalled = true;
  const notify = () => {
    try { window.KickNative && window.KickNative.route(location.pathname + location.search); } catch (_) {}
  };
  const wrap = name => {
    const original = history[name];
    history[name] = function() {
      const result = original.apply(this, arguments);
      setTimeout(notify, 0);
      return result;
    };
  };
  wrap('pushState'); wrap('replaceState');
  addEventListener('popstate', notify, true);
  document.addEventListener('click', event => {
    const link = event.target && event.target.closest ? event.target.closest('a[href]') : null;
    if (!link) return;
    try {
      const url = new URL(link.href, location.href);
      if (url.hostname === 'kick.com' || url.hostname.endsWith('.kick.com')) {
        setTimeout(notify, 40);
      }
    } catch (_) {}
  }, true);
  document.addEventListener('focusin', event => {
    const el = event.target;
    if (el && (el.matches('textarea,input,[contenteditable="true"]') ||
      (el.closest && el.closest('[contenteditable="true"]')))) {
      window.KickNative && window.KickNative.chatFocus(true);
    }
  }, true);
  document.addEventListener('focusout', event => {
    const el = event.target;
    if (el && (el.matches('textarea,input,[contenteditable="true"]') ||
      (el.closest && el.closest('[contenteditable="true"]')))) {
      setTimeout(() => {
        const active = document.activeElement;
        const focused = active && (active.matches('textarea,input,[contenteditable="true"]') ||
          (active.closest && active.closest('[contenteditable="true"]')));
        window.KickNative && window.KickNative.chatFocus(!!focused);
      }, 100);
    }
  }, true);
  notify();
})();
""".trimIndent()

private val KICK_NATIVE_VIDEO_GUARD = """
(function() {
  if (window.__kickNativeVideoGuard) return;
  window.__kickNativeVideoGuard = true;
  const block = function() {
    document.querySelectorAll('video').forEach(video => {
      try { video.pause(); video.removeAttribute('autoplay'); } catch (_) {}
    });
  };
  block();
  new MutationObserver(block).observe(document.documentElement, {childList:true,subtree:true});
})();
""".trimIndent()
