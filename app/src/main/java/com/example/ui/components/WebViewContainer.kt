package com.example.ui.components

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Message
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.example.browser.BrowserViewModel
import com.example.browser.WebViewAction
import com.example.privacy.ContentBlocker
import kotlinx.coroutines.flow.SharedFlow

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun WebViewContainer(
    tabId: String,
    initialUrl: String,
    isDesktopMode: Boolean,
    isAdBlockEnabled: Boolean,
    whitelistedDomains: Set<String>,
    blockThirdPartyCookies: Boolean,
    enableWebDarkMode: Boolean,
    isDarkTheme: Boolean,
    viewModel: BrowserViewModel,
    actions: SharedFlow<WebViewAction>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var defaultUserAgent by remember { mutableStateOf<String?>(null) }
    var customVideoView by remember { mutableStateOf<View?>(null) }
    var customVideoCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    val effectiveDark = enableWebDarkMode || isDarkTheme

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
                val targetUiMode = if (effectiveDark) {
                    Configuration.UI_MODE_NIGHT_YES
                } else {
                    Configuration.UI_MODE_NIGHT_NO
                }
                val overrideConfig = Configuration(ctx.resources.configuration).apply {
                    uiMode = targetUiMode or (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv())
                }
                val themedContext = ctx.createConfigurationContext(overrideConfig)

                WebView(themedContext).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    // Eliminate black flashing on init
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)

                    // Touch listener to gain focus away from address bar on tap
                    setOnTouchListener { v, event ->
                        if (event.action == MotionEvent.ACTION_DOWN) {
                            v.requestFocus()
                        }
                        false
                    }

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

                    // Dark Mode for Web Content (aligned with active browser theme & force-dark setting)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        settings.isAlgorithmicDarkeningAllowed = effectiveDark
                    } else if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                        WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, effectiveDark)
                    } else if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                        WebSettingsCompat.setForceDark(
                            settings,
                            if (effectiveDark) WebSettingsCompat.FORCE_DARK_ON else WebSettingsCompat.FORCE_DARK_OFF
                        )
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
                                return ContentBlocker.createEmptyResponse()
                            }
                            return super.shouldInterceptRequest(view, request)
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            url?.let {
                                viewModel.onUrlChanged(it)
                            }
                            viewModel.onNavigationStateChanged(
                                canGoBack = view?.canGoBack() ?: false,
                                canGoForward = view?.canGoForward() ?: false
                            )
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            url?.let {
                                viewModel.onUrlChanged(it)
                            }
                            view?.title?.let {
                                viewModel.onTitleChanged(it)
                            }
                            viewModel.onNavigationStateChanged(
                                canGoBack = view?.canGoBack() ?: false,
                                canGoForward = view?.canGoForward() ?: false
                            )
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val url = request?.url?.toString() ?: return false
                            if (url.startsWith("http://") || url.startsWith("https://")) {
                                return false
                            }
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                                return true
                            } catch (e: Exception) {
                                return true
                            }
                        }
                    }

                    // Custom WebChromeClient
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            super.onProgressChanged(view, newProgress)
                            viewModel.onProgressChanged(newProgress)
                        }

                        override fun onReceivedTitle(view: WebView?, title: String?) {
                            super.onReceivedTitle(view, title)
                            title?.let { viewModel.onTitleChanged(it) }
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

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    webView.settings.isAlgorithmicDarkeningAllowed = effectiveDark
                } else if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                    WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.settings, effectiveDark)
                } else if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                    WebSettingsCompat.setForceDark(
                        webView.settings,
                        if (effectiveDark) WebSettingsCompat.FORCE_DARK_ON else WebSettingsCompat.FORCE_DARK_OFF
                    )
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
    }
}
