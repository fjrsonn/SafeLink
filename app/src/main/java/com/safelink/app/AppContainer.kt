package com.safelink.app

import android.content.Context
import com.safelink.app.data.HistoryRepository
import com.safelink.app.security.UrlAnalyzer

class AppContainer(context: Context) {
    val analyzer = UrlAnalyzer()
    val historyRepository = HistoryRepository(context.applicationContext)
}

val Context.safeLinkContainer: AppContainer
    get() = (applicationContext as SafeLinkApplication).container
