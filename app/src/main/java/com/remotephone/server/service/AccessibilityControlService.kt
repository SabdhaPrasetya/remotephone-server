package com.remotephone.server.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import org.json.JSONObject

class AccessibilityControlService : AccessibilityService() {

    companion object {
        var instance: AccessibilityControlService? = null

        fun performTouch(x: Float, y: Float) {
            instance?.doTap(x, y)
        }

        fun performSwipe(x1: Float, y1: Float, x2: Float, y2: Float) {
            instance?.doSwipe(x1, y1, x2, y2)
        }

        fun performBack() {
            instance?.performGlobalAction(GLOBAL_ACTION_BACK)
        }

        fun performHome() {
            instance?.performGlobalAction(GLOBAL_ACTION_HOME)
        }

        fun performRecents() {
            instance?.performGlobalAction(GLOBAL_ACTION_RECENTS)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    private fun doTap(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 100)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    private fun doSwipe(x1: Float, y1: Float, x2: Float, y2: Float) {
        val path = Path().apply { moveTo(x1, y1); lineTo(x2, y2) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 300)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}
