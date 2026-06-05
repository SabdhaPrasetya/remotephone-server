package com.remotephone.server.network

import android.util.Base64
import android.util.Log
import com.remotephone.server.service.AccessibilityControlService
import com.remotephone.server.service.ScreenCaptureService
import kotlinx.coroutines.*
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import java.net.URI

class RelayConnection(
    private val relayUrl: String,
    private val roomId: String,
    private val onStatus: (String) -> Unit
) {
    private var ws: WebSocketClient? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var active = true

    fun connect() {
        if (!active) return
        try {
            ws = object : WebSocketClient(URI(relayUrl)) {
                override fun onOpen(h: ServerHandshake?) {
                    send(JSONObject().put("type","register_server").put("room", roomId).toString())
                }
                override fun onMessage(msg: String?) {
                    msg ?: return
                    try {
                        val j = JSONObject(msg)
                        when (j.getString("type")) {
                            "registered" -> { onStatus("Relay aktif ✓ | Room: $roomId"); startStreaming() }
                            "client_joined" -> onStatus("Client terhubung! ✓ | Room: $roomId")
                            "tap" -> AccessibilityControlService.performTouch(j.getDouble("x").toFloat(), j.getDouble("y").toFloat())
                            "swipe" -> AccessibilityControlService.performSwipe(j.getDouble("x1").toFloat(), j.getDouble("y1").toFloat(), j.getDouble("x2").toFloat(), j.getDouble("y2").toFloat())
                            "back" -> AccessibilityControlService.performBack()
                            "home" -> AccessibilityControlService.performHome()
                            "recents" -> AccessibilityControlService.performRecents()
                        }
                    } catch (e: Exception) { Log.e("Relay", e.message ?: "") }
                }
                override fun onClose(code: Int, reason: String?, remote: Boolean) {
                    onStatus("Terputus, reconnect...")
                    scope.launch { delay(3000); if (active) connect() }
                }
                override fun onError(ex: Exception?) { onStatus("Error: ${ex?.message}") }
            }
            ws?.connect()
        } catch (e: Exception) { onStatus("Gagal: ${e.message}") }
    }

    private fun startStreaming() {
        ScreenCaptureService.onFrameCapture = { bytes ->
            if (ws?.isOpen == true) {
                try {
                    ws?.send(JSONObject().put("type","frame").put("data", Base64.encodeToString(bytes, Base64.NO_WRAP)).toString())
                } catch (_: Exception) {}
            }
        }
    }

    fun disconnect() {
        active = false
        ScreenCaptureService.onFrameCapture = null
        scope.cancel()
        ws?.close()
    }
}
