package com.sinetech.latte

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class YTEglencePlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(YTEglence())
    }
}