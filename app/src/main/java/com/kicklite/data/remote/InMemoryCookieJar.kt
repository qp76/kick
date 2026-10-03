package com.kicklite.data.remote

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.CopyOnWriteArrayList

class InMemoryCookieJar : CookieJar {
    private val cookieStore = CopyOnWriteArrayList<Cookie>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return
        val now = System.currentTimeMillis()
        cookies.forEach { newCookie ->
            if (newCookie.expiresAt < now) return@forEach
            cookieStore.removeAll { existing ->
                existing.name == newCookie.name &&
                    existing.domain == newCookie.domain &&
                    existing.path == newCookie.path
            }
            cookieStore.add(newCookie)
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        if (cookieStore.isEmpty()) return emptyList()
        val now = System.currentTimeMillis()
        val validCookies = mutableListOf<Cookie>()

        cookieStore.removeAll { it.expiresAt < now }

        cookieStore.forEach { cookie ->
            if (cookie.matches(url)) {
                validCookies.add(cookie)
            }
        }

        return validCookies
    }
}
