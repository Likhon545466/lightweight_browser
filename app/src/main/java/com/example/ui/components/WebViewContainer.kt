package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.example.browser.ActiveTabState
import com.example.browser.BrowserViewModel
import com.example.browser.UrlUtils
import com.example.browser.WebViewAction
import com.example.privacy.ContentBlocker
import kotlinx.coroutines.flow.SharedFlow

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewContainer(
    tabId: String,
    initialUrl: String,
    isDesktopMode: Boolean,
    isAdBlockEnabled: Boolean,
    whitelistedDomains: Set<String>,
    blockThirdPartyCookies: Boolean,
    enableWebDarkMode: Boolean,
    viewModel: BrowserViewModel,
    actions: SharedFlow<WebViewAction>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var defaultUserAgent by remember { mutableStateOf<String?>(null) }
    var customVideoView by remember { mutableStateOf<View?>(null) }
    var customVideoCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    val activeTabState by viewModel.activeTabState.collectAsStateWithLifecycle()
    val isTabLoading = activeTabState?.isLoading == true && (activeTabState?.progress ?: 0) < 75

    // Handle incoming actions from ViewModel
    LaunchedEffect(tabId) {
        actions.collect { action ->
            webViewRef?.let { webView ->
                when (action) {
                    is WebViewAction.LoadUrl -> {
                        webView.loadUrl(action.url)
                    }
                    is WebViewAction.Reload -> {
                        webView.reload()
                    }
                    is WebViewAction.StopLoading -> {
                        webView.stopLoading()
                    }
                    is WebViewAction.GoBack -> {
                        if (webView.canGoBack()) webView.goBack()
                    }
                    is WebViewAction.GoForward -> {
                        if (webView.canGoForward()) webView.goForward()
                    }
                    is WebViewAction.SetDesktopMode -> {
                        val ua = if (action.enabled) DESKTOP_USER_AGENT else defaultUserAgent
                        webView.settings.userAgentString = ua
                        webView.reload()
                    }
                    is WebViewAction.FindAllAsync -> {
                        if (action.query.isNotBlank()) {
                            webView.findAllAsync(action.query)
                        } else {
                            webView.clearMatches()
                        }
                    }
                    is WebViewAction.FindNext -> {
                        webView.findNext(action.forward)
                    }
                    is WebViewAction.ClearFindMatches -> {
                        webView.clearMatches()
                    }
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    // Eliminate black flashing on init
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)

                    // Default user agent capture
                    if (defaultUserAgent == null) {
                        defaultUserAgent = settings.userAgentString
                    }

                    // Configure Settings
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                        allowFileAccess = false
                        allowContentAccess = false
                        setSupportMultipleWindows(true)
                        mediaPlaybackRequiresUserGesture = false

                        if (isDesktopMode) {
                            userAgentString = DESKTOP_USER_AGENT
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            safeBrowsingEnabled = true
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        }
                    }

                    // Cookie Policy
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        cookieManager.setAcceptThirdPartyCookies(this, !blockThirdPartyCookies)
                    }

                    // Dark Mode for Web Content (if supported by AndroidX WebKit)
                    if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                        val forceDark = if (enableWebDarkMode) WebSettingsCompat.FORCE_DARK_ON else WebSettingsCompat.FORCE_DARK_OFF
                        WebSettingsCompat.setForceDark(settings, forceDark)
                    }

                    // Find in page listener
                    setFindListener { activeMatchOrdinal, numberOfMatches, isDoneCounting ->
                        if (isDoneCounting) {
                            viewModel.onFindMatchResult(activeMatchOrdinal, numberOfMatches)
                        }
                    }

                    // Download Listener
                    setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                        viewModel.handleDownloadRequest(url, userAgent, contentDisposition, mimetype, contentLength)
                    }

                    // Custom WebViewClient
                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            val uri = request?.url ?: return null
                            val host = uri.host ?: ""
                            val isWhitelisted = whitelistedDomains.contains(host) || whitelistedDomains.contains(host.removePrefix("www."))

                            if (ContentBlocker.shouldBlock(uri, isAdBlockEnabled, isWhitelisted)) {
                                ContentBlocker.recordBlockForTab(tabId)
                                viewModel.onBlockerHit(tabId)
                                return ContentBlocker.createEmptyResponse()
                            }
                            return super.shouldInterceptRequest(view, request)
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            url?.let {
                                viewModel.onUrlChanged(it)
                                viewModel.onNavigationStateChanged(view?.canGoBack() == true, view?.canGoForward() == true)
                            }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            url?.let { viewModel.onUrlChanged(it) }
                            view?.title?.let { viewModel.onTitleChanged(it) }
                            viewModel.onNavigationStateChanged(view?.canGoBack() == true, view?.canGoForward() == true)
                            viewModel.onProgressChanged(100)
                        }

                        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                            super.doUpdateVisitedHistory(view, url, isReload)
                            url?.let { viewModel.onUrlChanged(it) }
                            view?.title?.let { viewModel.onTitleChanged(it) }
                        }

                        override fun onReceivedSslError(
                            view: WebView?,
                            handler: SslErrorHandler?,
                            error: SslError?
                        ) {
                            super.onReceivedSslError(view, handler, error)
                        }

                        override fun onRenderProcessGone(
                            view: WebView?,
                            detail: RenderProcessGoneDetail?
                        ): Boolean {
                            view?.let {
                                val deadUrl = it.url ?: initialUrl
                                it.loadUrl(deadUrl)
                            }
                            return true
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val url = request?.url?.toString() ?: return false
                            if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("about:") || url.startsWith("data:")) {
                                return false
                            }
                            return try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                                true
                            } catch (e: Exception) {
                                true
                            }
                        }
                    }

                    // Custom WebChromeClient
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            super.onProgressChanged(view, newProgress)
                            viewModel.onProgressChanged(newProgress)
                            viewModel.onNavigationStateChanged(view?.canGoBack() == true, view?.canGoForward() == true)
                        }

                        override fun onReceivedTitle(view: WebView?, title: String?) {
                            super.onReceivedTitle(view, title)
                            title?.let { viewModel.onTitleChanged(it) }
                        }

                        override fun onPermissionRequest(request: PermissionRequest?) {
                            request?.grant(request.resources)
                        }

                        override fun onGeolocationPermissionsShowPrompt(
                            origin: String?,
                            callback: GeolocationPermissions.Callback?
                        ) {
                            callback?.invoke(origin, true, false)
                        }

                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                            customVideoView = view
                            customVideoCallback = callback
                        }

                        override fun onHideCustomView() {
                            customVideoView = null
                            customVideoCallback?.onCustomViewHidden()
                            customVideoCallback = null
                        }

                        override fun onCreateWindow(
                            view: WebView?,
                            isDialog: Boolean,
                            isUserGesture: Boolean,
                            resultMsg: Message?
                        ): Boolean {
                            val href = view?.hitTestResult?.extra
                            if (!href.isNullOrBlank()) {
                                viewModel.createNewTab(url = href)
                                return true
                            }
                            return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
                        }
                    }

                    if (initialUrl.isNotBlank() && initialUrl != "about:blank") {
                        loadUrl(initialUrl)
                    }
                    webViewRef = this
                }
            },
            update = { webView ->
                webViewRef = webView
                val targetUa = if (isDesktopMode) DESKTOP_USER_AGENT else defaultUserAgent
                if (webView.settings.userAgentString != targetUa && targetUa != null) {
                    webView.settings.userAgentString = targetUa
                }
            },
            onRelease = { webView ->
                webView.stopLoading()
                webView.clearHistory()
                webView.destroy()
            },
            modifier = Modifier.fillMaxSize()
        )

        // Video Fullscreen Custom View Overlay
        if (customVideoView != null) {
            AndroidView(
                factory = { customVideoView!! },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Fluid & Beautiful Loading Screen (Replaces black flash)
        AnimatedVisibility(
            visible = isTabLoading,
            enter = fadeIn(animationSpec = tween(120, easing = FastOutSlowInEasing)),
            exit = fadeOut(animationSpec = tween(160, easing = FastOutSlowInEasing))
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 0.95f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(650, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulseScale"
            )

            val displayDomain = remember(activeTabState?.url) {
                val u = activeTabState?.url ?: initialUrl
                if (u.isNotBlank()) UrlUtils.extractDomain(u) else "Connecting"
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    // Pulsing Glow Badge
                    Box(
                        modifier = Modifier
                            .scale(pulseScale)
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Loading",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Domain Pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = displayDomain,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Progress Bar
                    val currentProgress = (activeTabState?.progress ?: 15) / 100f
                    val animatedProgress by animateFloatAsState(
                        targetValue = currentProgress.coerceIn(0.1f, 1f),
                        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
                        label = "progress"
                    )

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .width(160.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}
