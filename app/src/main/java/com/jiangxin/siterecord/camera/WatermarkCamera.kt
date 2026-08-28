package com.jiangxin.siterecord.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.RectF
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WatermarkCamera {
    /**
     * 读取原始照片，在底部半透明条烧录 定位 / 时间 / 拍照人 三段水印，
     * 压缩为 JPEG 存入 App 私有 photos 目录，返回新文件。
     */
    fun addWatermark(context: Context, src: File, location: String, photographer: String): File? {
        return try {
            val original = BitmapFactory.decodeFile(src.absolutePath) ?: return null
            val bmp = original.copy(Bitmap.Config.ARGB_8888, true)
            original.recycle()

            val canvas = Canvas(bmp)
            val density = context.resources.displayMetrics.density
            val minSide = if (bmp.width <= bmp.height) bmp.width else bmp.height
            val barH = (minSide * 0.12f).coerceAtLeast(60f * density)
            val barTop = bmp.height - barH

            val barPaint = Paint().apply { color = Color.argb(155, 0, 0, 0); isAntiAlias = true }
            canvas.drawRect(RectF(0f, barTop, bmp.width.toFloat(), bmp.height.toFloat()), barPaint)

            val textSize = (barH * 0.30f).coerceAtLeast(15f * density)
            val textPaint = Paint().apply {
                color = Color.WHITE
                this.textSize = textSize
                isAntiAlias = true
                typeface = Typeface.DEFAULT_BOLD
            }
            val time = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date())
            val line1 = "定位：$location   时间：$time"
            val line2 = "拍摄人：$photographer"
            val padX = textSize * 0.45f
            val lineH = barH * 0.5f
            canvas.drawText(line1, padX, barTop + lineH * 0.72f + textSize * 0.35f, textPaint)
            canvas.drawText(line2, padX, barTop + lineH + lineH * 0.72f + textSize * 0.35f, textPaint)

            val outDir = File(context.filesDir, "photos")
            if (!outDir.exists()) outDir.mkdirs()
            val out = File(outDir, "wm_${System.currentTimeMillis()}.jpg")
            out.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            bmp.recycle()
            out
        } catch (e: Exception) {
            null
        }
    }
}
