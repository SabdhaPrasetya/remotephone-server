package com.remotephone.server.service

import android.app.*
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream

class ScreenCaptureService : Service() {
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val CHANNEL_ID = "ScreenCapture"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_DATA = "data"
        var isRunning = false
        var onFrameCapture: ((ByteArray) -> Unit)? = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        startForeground(1, buildNotification())

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, -1) ?: return START_NOT_STICKY
        val data = intent.getParcelableExtra<Intent>(EXTRA_DATA) ?: return START_NOT_STICKY

        val pm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = pm.getMediaProjection(resultCode, data)
        setupVirtualDisplay()
        isRunning = true
        startCapturing()
        return START_STICKY
    }

    private fun setupVirtualDisplay() {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay.getMetrics(metrics)
        val w = metrics.widthPixels; val h = metrics.heightPixels
        imageReader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "RemoteCapture", w, h, metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface, null, null
        )
    }

    private fun startCapturing() {
        scope.launch {
            while (isRunning) {
                try {
                    val image = imageReader?.acquireLatestImage() ?: run { delay(50); return@run null }
                    image?.let {
                        val buf = it.planes[0].buffer
                        val ps = it.planes[0].pixelStride
                        val rs = it.planes[0].rowStride
                        val bmp = Bitmap.createBitmap(it.width + (rs - ps * it.width) / ps, it.height, Bitmap.Config.ARGB_8888)
                        bmp.copyPixelsFromBuffer(buf)
                        val scaled = Bitmap.createScaledBitmap(bmp, 720, 1280, false)
                        val out = ByteArrayOutputStream()
                        scaled.compress(Bitmap.CompressFormat.JPEG, 55, out)
                        onFrameCapture?.invoke(out.toByteArray())
                        bmp.recycle(); scaled.recycle(); it.close()
                    }
                    delay(50)
                } catch (e: Exception) { delay(100) }
            }
        }
    }

    private fun createNotificationChannel() {
        val ch = NotificationChannel(CHANNEL_ID, "Screen Share", NotificationManager.IMPORTANCE_LOW)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
    }
    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("RemotePhone Aktif").setContentText("Layar sedang dibagikan")
        .setSmallIcon(android.R.drawable.ic_menu_camera).build()

    override fun onDestroy() { super.onDestroy(); isRunning = false; scope.cancel(); virtualDisplay?.release(); mediaProjection?.stop(); imageReader?.close() }
    override fun onBind(intent: Intent?): IBinder? = null
}
