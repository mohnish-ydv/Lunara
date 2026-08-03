package com.mohnishraj.lunara.core

import com.mohnishraj.lunara.BuildConfig

object AppConfig {
    val supabaseUrl: String = BuildConfig.SUPABASE_URL.trim().removeSuffix("/")
    val supabaseAnonKey: String = BuildConfig.SUPABASE_ANON_KEY.trim()
    val isCloudReady: Boolean
        get() = supabaseUrl.startsWith("https://") && supabaseAnonKey.length > 20
}
