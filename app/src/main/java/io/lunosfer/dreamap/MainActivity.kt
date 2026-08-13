package io.lunosfer.dreamap

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import io.lunosfer.dreamap.service.LunosferMessagingService
import io.lunosfer.dreamap.supabase.supabaseClient
import io.lunosfer.dreamap.ui.screens.MainScreen
import io.lunosfer.dreamap.ui.theme.MyApplicationTheme
import io.github.jan.supabase.auth.handleDeeplinks

class MainActivity : AppCompatActivity() {
    private val pendingRouteState = androidx.compose.runtime.mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        supabaseClient.handleDeeplinks(intent)
        
        intent.getStringExtra("target_route")?.let { route ->
            if (route.isNotBlank()) {
                pendingRouteState.value = route
            }
        }

        requestNotificationPermission()
        initFcmToken()

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen(
                    pendingRoute = pendingRouteState.value,
                    onRouteHandled = { pendingRouteState.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        supabaseClient.handleDeeplinks(intent)

        val route = intent.getStringExtra("target_route")
        if (!route.isNullOrBlank()) {
            pendingRouteState.value = route
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
    }

    private fun initFcmToken() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(applicationContext)
            }
            if (FirebaseApp.getApps(this).isNotEmpty()) {
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result
                        if (!token.isNull_or_blank()) {
                            Log.d("MainActivity", "FCM Token: $token")
                            LunosferMessagingService.sendTokenToServer(token)
                        }
                    } else {
                        Log.w("MainActivity", "Fetching FCM registration token failed", task.exception)
                    }
                }
            } else {
                Log.w("MainActivity", "FirebaseApp could not be initialized (google-services.json missing or incomplete)")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "FCM token initialization error", e)
        }
    }

    private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()
}

