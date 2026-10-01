package com.example.dopamine_gate_mobile

object ProtectedApps {

    val packages: Set<String> =
        setOf(
            "com.google.android.youtube",
            "com.zhiliaoapp.musically",
            "com.facebook.katana",
            "org.telegram.messenger",
            "com.instagram.android"
        )

    fun getAppName(packageName: String): String {
        return when (packageName) {
            "com.google.android.youtube" -> "YouTube"
            "com.zhiliaoapp.musically" -> "TikTok"
            "com.facebook.katana" -> "Facebook"
            "org.telegram.messenger" -> "Telegram"
            "com.instagram.android" -> "Instagram"
            else -> "Unknown app"
        }
    }
}