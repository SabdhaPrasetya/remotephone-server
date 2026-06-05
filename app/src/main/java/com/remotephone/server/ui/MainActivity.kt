package com.remotephone.server.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.remotephone.server.network.RelayConnection
import com.remotephone.server.service.ScreenCaptureService
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private lateinit var tvStatus: TextView
    private lateinit var tvRoomId: TextView
    private lateinit var etRelayUrl: EditText
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnCopyRoom: Button

    private var relay: RelayConnection? = null
    private var roomId = UUID.randomUUID().toString().substring(0, 8).uppercase()
    private val CAPTURE_REQUEST = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_server)
        tvStatus = findViewById(R.id.tvStatus)
        tvRoomId = findViewById(R.id.tvRoomId)
        etRelayUrl = findViewById(R.id.etRelayUrl)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        btnCopyRoom = findViewById(R.id.btnCopyRoom)

        tvRoomId.text = "Room ID: $roomId"

        btnStart.setOnClickListener {
            val url = etRelayUrl.text.toString().trim()
            if (url.isEmpty()) { Toast.makeText(this, "Masukkan URL Relay Server!", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            val pm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            startActivityForResult(pm.createScreenCaptureIntent(), CAPTURE_REQUEST)
        }
        btnStop.setOnClickListener {
            relay?.disconnect(); stopService(Intent(this, ScreenCaptureService::class.java))
            tvStatus.text = "Status: Dihentikan"; btnStart.isEnabled = true; btnStop.isEnabled = false
        }
        btnCopyRoom.setOnClickListener {
            (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager)
                .setPrimaryClip(ClipData.newPlainText("room", roomId))
            Toast.makeText(this, "Room ID disalin: $roomId", Toast.LENGTH_SHORT).show()
        }
        Toast.makeText(this, "Aktifkan Accessibility Service di Settings!", Toast.LENGTH_LONG).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAPTURE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            startForegroundService(Intent(this, ScreenCaptureService::class.java).apply {
                putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, resultCode)
                putExtra(ScreenCaptureService.EXTRA_DATA, data)
            })
            var url = etRelayUrl.text.toString().trim()
            url = url.replace("https://", "wss://").replace("http://", "ws://")
            if (!url.startsWith("ws")) url = "wss://$url"

            relay = RelayConnection(url, roomId) { s -> runOnUiThread { tvStatus.text = "Status: $s" } }
            relay?.connect()
            tvStatus.text = "Status: Menghubungkan..."; btnStart.isEnabled = false; btnStop.isEnabled = true
        }
    }
}
