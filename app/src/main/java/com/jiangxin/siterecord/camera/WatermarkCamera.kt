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
    /** 水印图最长边上限。1600px 下文字依然清晰，内存占用从几十 MB 降到个位数。 */
    private const val MAX_SIDE = 1600

    /**
     * 读取原始照片，在底部半透明条烧录 定位 / 时间 / 拍照人 三段水印，
     * 压缩为 JPEG 存入 App 私有 photos 目录，返回新文件。
     * 失败返回 null —— 调用方必须兜底保留原图，不能让老板拍完照发现照片没了。
     */
    fun addWatermark(context: Context, src: File, location: String, photographer: String): File? {
        return try {
            val bmp = decodeCapped(src.absolutePath) ?: return null

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
        } catch (t: Throwable) {
            // 用 Throwable 而非 Exception：OutOfMemoryError 是 Error 不是 Exception，
            // 只 catch Exception 根本抓不住大图 OOM，照样崩。
            null
        }
    }

    /**
     * 按最长边采样解码，并直接解成可变位图（inMutable）：
     * 省掉原先「全尺寸解码 + 再 copy 一份可变位图」的两张原图峰值内存。
     */
    private fun decodeCapped(path: String): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
        return BitmapFactory.decodeFile(
            path,
            BitmapFactory.Options().apply {
                inSampleSize = sample
                inMutable = true
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
        )
    }
}
