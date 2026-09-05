package com.mina.legadostudio

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mina.legadostudio.ui.StudioApp

class MainActivity : ComponentActivity() {
    private var launchJobId by mutableStateOf<String?>(null)
    private var launchRoute by mutableStateOf<String?>(null)
    private var launchNonce by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        consume(intent)
        setContent { StudioApp(initialJobId = launchJobId, initialRoute = launchRoute, deepLinkNonce = launchNonce, onExit = { finish() }) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consume(intent)
    }

    private fun consume(intent: Intent?) {
        launchJobId = intent?.getStringExtra("jobId")
        launchRoute = intent?.getStringExtra("route")
        launchNonce++
    }
}
