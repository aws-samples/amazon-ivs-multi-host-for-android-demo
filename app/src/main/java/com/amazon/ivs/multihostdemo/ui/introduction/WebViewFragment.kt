package com.amazon.ivs.multihostdemo.ui.introduction

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import com.amazon.ivs.multihostdemo.R
import com.amazon.ivs.multihostdemo.ui.BackHandler
import com.amazon.ivs.multihostdemo.common.SOURCE_CODE_URL
import com.amazon.ivs.multihostdemo.common.viewBinding
import com.amazon.ivs.multihostdemo.databinding.FragmentWebViewBinding

class WebViewFragment : Fragment(R.layout.fragment_web_view), BackHandler {
    private val binding by viewBinding(FragmentWebViewBinding::bind)

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            webView.settings.javaScriptEnabled = true
            webView.settings.databaseEnabled = true
            webView.settings.domStorageEnabled = true
            webView.settings.cacheMode = WebSettings.LOAD_DEFAULT
            webView.settings.mediaPlaybackRequiresUserGesture = false
            webView.settings.useWideViewPort = true
            webView.settings.builtInZoomControls = true
            webView.settings.displayZoomControls = false
            webView.settings.loadWithOverviewMode = true
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            webView.webViewClient = WebViewClient()
            webView.loadUrl(SOURCE_CODE_URL)
        }
    }

    override fun canGoBack() = when {
        binding.webView.canGoBack() -> {
            binding.webView.goBack()
            false
        }
        else -> true
    }
}
