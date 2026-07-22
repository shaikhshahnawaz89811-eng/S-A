package com.sacompanion.service.background

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.sacompanion.R
import com.sacompanion.SAApplication

class FloatingWindowService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var isMinimized = false

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createFloatingView()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_MOVE_LEFT -> moveToLeft()
            ACTION_MOVE_RIGHT -> moveToRight()
            ACTION_MINIMIZE -> minimize()
            ACTION_EXPAND -> expand()
            ACTION_DISMISS -> dismissWindow()
        }
        return START_STICKY
    }

    private fun createFloatingView() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }

        // Simple floating orb view
        val container = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            background = ContextCompat.getDrawable(context, R.drawable.floating_orb_bg)
            setPadding(24, 24, 24, 24)
        }

        val orbText = TextView(this).apply {
            text = "SA"
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, android.R.color.white))
            gravity = Gravity.CENTER
        }
        container.addView(orbText)

        floatingView = container

        // Touch listener for drag
        floatingView?.setOnTouchListener { _, event ->
            handleTouch(event, params)
            true
        }

        windowManager?.addView(floatingView, params)
    }

    private fun handleTouch(event: MotionEvent, params: WindowManager.LayoutParams) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = params.x
                initialY = params.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
            }
            MotionEvent.ACTION_MOVE -> {
                params.x = initialX + (event.rawX - initialTouchX).toInt()
                params.y = initialY + (event.rawY - initialTouchY).toInt()
                windowManager?.updateViewLayout(floatingView, params)
            }
            MotionEvent.ACTION_UP -> {
                val dx = event.rawX - initialTouchX
                val dy = event.rawY - initialTouchY
                if (dx < 5 && dy < 5) {
                    // It's a tap — open main app
                    val intent = Intent(this, com.sacompanion.MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(intent)
                }
            }
        }
    }

    private fun moveToLeft() {
        floatingView?.let { view ->
            val params = view.layoutParams as? WindowManager.LayoutParams ?: return
            params.x = 0
            windowManager?.updateViewLayout(view, params)
        }
    }

    private fun moveToRight() {
        floatingView?.let { view ->
            val params = view.layoutParams as? WindowManager.LayoutParams ?: return
            val displayMetrics = resources.displayMetrics
            params.x = displayMetrics.widthPixels - 200
            windowManager?.updateViewLayout(view, params)
        }
    }

    private fun minimize() {
        isMinimized = true
        floatingView?.apply {
            scaleX = 0.5f
            scaleY = 0.5f
            alpha = 0.7f
        }
    }

    private fun expand() {
        isMinimized = false
        floatingView?.apply {
            scaleX = 1.0f
            scaleY = 1.0f
            alpha = 1.0f
        }
    }

    private fun dismissWindow() {
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        floatingView?.let { windowManager?.removeView(it) }
        floatingView = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_MOVE_LEFT = "com.sacompanion.FLOAT_LEFT"
        const val ACTION_MOVE_RIGHT = "com.sacompanion.FLOAT_RIGHT"
        const val ACTION_MINIMIZE = "com.sacompanion.FLOAT_MINIMIZE"
        const val ACTION_EXPAND = "com.sacompanion.FLOAT_EXPAND"
        const val ACTION_DISMISS = "com.sacompanion.FLOAT_DISMISS"
    }
}
