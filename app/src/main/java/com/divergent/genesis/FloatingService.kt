package com.divergent.genesis

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest

class FloatingService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: TextView

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
        setupFloatingWindow()
        observeStatus()
    }

    private fun setupFloatingWindow() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        floatingView = TextView(this).apply {
            text = "Genesis: Idle"
            setBackgroundColor(android.graphics.Color.parseColor("#CC000000"))
            setTextColor(android.graphics.Color.CYAN)
            textSize = 14f
            setPadding(30, 30, 30, 30)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 150
        }

        windowManager.addView(floatingView, params)
    }

    private fun observeStatus() {
        CoroutineScope(Dispatchers.Main).launch {
            AgentState.currentTask.collectLatest { task ->
                floatingView.text = if (task.isNullOrEmpty()) "Genesis: Idle" else "Genesis: $task"
            }
        }
    }

    private fun startForegroundNotification() {
        val notification = NotificationCompat.Builder(this, "genesis_channel")
            .setContentTitle("Divergent Genesis Active")
            .setContentText("Agent is running in the background")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "genesis_channel", "Genesis Service",
                android.app.NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager)
                .createNotificationChannel(channel)
        }
        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingView.isInitialized) {
            windowManager.removeView(floatingView)
        }
    }
}
