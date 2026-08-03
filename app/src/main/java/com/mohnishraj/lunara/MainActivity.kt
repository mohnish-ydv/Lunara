package com.mohnishraj.lunara

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mohnishraj.lunara.ui.AppViewModel
import com.mohnishraj.lunara.ui.LunaraApp
import com.mohnishraj.lunara.ui.theme.LunaraTheme

class MainActivity : ComponentActivity() {
    private var pendingProfile by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingProfile = profileFromIntent(intent)
        enableEdgeToEdge()
        setContent {
            LunaraTheme {
                val appViewModel: AppViewModel = viewModel()
                LaunchedEffect(pendingProfile) {
                    pendingProfile?.let(appViewModel::openSharedProfile)
                    pendingProfile = null
                }
                LunaraApp(appViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingProfile = profileFromIntent(intent)
    }

    private fun profileFromIntent(intent: Intent?): String? {
        val data = intent?.data ?: return null
        if (data.scheme != "lunara" || data.host != "profile") return null
        return data.lastPathSegment?.takeIf(String::isNotBlank)
    }
}
