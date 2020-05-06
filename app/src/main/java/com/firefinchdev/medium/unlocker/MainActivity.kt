package com.firefinchdev.medium.unlocker

import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.webkit.*
import androidx.appcompat.app.AlertDialog
import androidx.core.util.PatternsCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.net.URLDecoder

class MainActivity : AppCompatActivity() {

    private var extraHeaders = mapOf<String, String>()
    private val FALLBACK_URL = "https://medium.com"
    private var url = FALLBACK_URL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        extraHeaders = mapOf(Pair("referer", "https://t.co/je0v3782u2"))

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                if (request?.url?.toString()?.startsWith("intent://") == true) {
                    val encodedFragment = request.url.encodedFragment
                    val fragMap = encodedFragment?.split(";")?.map { fragPart ->
                        fragPart.split("=", limit = 2).let {
                            return@map it[0] to it.getOrNull(1)
                        }
                    }?.toMap()
                    url = URLDecoder.decode(fragMap?.get("S.browser_fallback_url"), "utf-8")
                    view?.loadUrl(url, extraHeaders)
                    return true
                }
                if (request?.method == "GET") {
                    view?.loadUrl(request.url.toString(), extraHeaders)
                    return true
                }
                return super.shouldOverrideUrlLoading(view, request)
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
//                if (request?.url?.toString()?.startsWith("intent://") == true) {
//                    Log.d("asd", request?.url?.toString())
//                }
                return super.shouldInterceptRequest(view, request)
            }
        }

        webView.clearHistory()
        webView.clearCache(true)

        webView.settings.apply {
            userAgentString = "Mozilla/5.0 (Linux; Android 8.0.0;) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.132 Mobile Safari/537.36"
            javaScriptEnabled = true

            //Make sure no caching is done
//            cacheMode = WebSettings.LOAD_NO_CACHE
//            setAppCacheEnabled(false)
        }
    }

    override fun onResume() {
        super.onResume()
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                if ("text/plain" == intent.type) {
                    url = getUrlFromIntentSend(intent) ?: FALLBACK_URL
                }
            }
            Intent.ACTION_VIEW -> {
                url = getUrlFromIntentView(intent) ?: FALLBACK_URL
            }
            else -> url = FALLBACK_URL
        }
        webView.loadUrl(url, extraHeaders)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item?.itemId){
            R.id.main_menu_cookies -> {
                AlertDialog.Builder(this)
                    .setTitle("Clear Cookies")
                    .setMessage(getString(R.string.clear_cookies_msg))
                    .setPositiveButton("Clear Cookies") { dialogInterface: DialogInterface, i: Int ->
                        clearCookies()
                    }
                    .setNegativeButton("Cancel") { dialogInterface: DialogInterface, i: Int ->
                        dialogInterface.dismiss()
                    }
                    .show()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getUrlFromIntentSend(intent: Intent): String? {
        var returnUrl: String? = null
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let { text ->
            val matcher = PatternsCompat.WEB_URL.matcher(text)
            // TODO: For now, using the last url if the text contains multiple urls
            while (matcher.find()) {
                val urlString = matcher.group()
                if (true) {     // TODO: Verify supported domains
                    returnUrl = urlString
                }
            }
        }
        return returnUrl
    }

    private fun getUrlFromIntentView(intent: Intent): String? {
        return intent.data.toString()
    }

    private fun clearCookies() {
        CookieManager.getInstance().removeAllCookies {
            webView.reload()
        }
        CookieManager.getInstance().flush()
    }
}
