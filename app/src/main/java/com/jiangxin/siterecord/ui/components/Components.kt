package com.jiangxin.siterecord.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.jiangxin.siterecord.ui.theme.OverdueRed
import com.jiangxin.siterecord.ui.theme.WarningAmber

/**
 * 通用删除二次确认弹窗。
 * 删除是不可逆操作，且项目删除会级联删掉其下全部巡查与备案，因此必须显式告知后果。
 */
@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmText: String = "确认删除"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun MetricCard(
    value: Int,
    label: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(12.dp)
    val body: @Composable () -> Unit = {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineMedium,
                color = if (highlight) OverdueRed else MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(text = label, style = MaterialTheme.typography.labelSmall)
        }
    }
    if (onClick != null) {
        Surface(onClick = onClick, tonalElevation = 1.dp, shape = shape, modifier = modifier) { body() }
    } else {
        Surface(tonalElevation = 1.dp, shape = shape, modifier = modifier) { body() }
    }
}

@Composable
fun StatusBadge(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun PhotoThumb(path: String, modifier: Modifier = Modifier, size: Int = 56) {
    // 按实际显示尺寸采样解码。原先是全尺寸 decodeFile：一张 1200 万像素照片解码约 48MB，
    // 列表里三五张就会撑爆堆，且解码跑在主线程会卡界面。
    val bitmap = remember(path, size) { decodeSampledBitmap(path, size * 3) }
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFE5E7EB)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text("图", style = MaterialTheme.typography.labelSmall)
        }
    }
}

/**
 * 按目标像素尺寸采样解码，避免全尺寸位图撑爆内存。
 * 用 Throwable 而非 Exception 兜底——OutOfMemoryError 是 Error 不是 Exception，
 * 只 catch Exception 是抓不住大图 OOM 的。
 */
private fun decodeSampledBitmap(path: String, targetPx: Int): ImageBitmap? {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val minSide = minOf(bounds.outWidth, bounds.outHeight)
        var sample = 1
        while (minSide / (sample * 2) >= targetPx) sample *= 2
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        BitmapFactory.decodeFile(path, opts)?.asImageBitmap()
    } catch (t: Throwable) {
        null
    }
}

@Composable
fun AttachmentChip(text: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun OverdueLabel(deadline: Long?, modifier: Modifier = Modifier) {
    if (deadline != null && deadline < System.currentTimeMillis()) {
        StatusBadge("逾期", OverdueRed, modifier)
    } else if (deadline != null) {
        StatusBadge("待办", WarningAmber, modifier)
    }
}
