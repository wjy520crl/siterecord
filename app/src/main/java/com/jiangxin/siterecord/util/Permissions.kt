package com.jiangxin.siterecord.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat

/**
 * 应用正常运行所必需的运行时权限。
 *
 * 说明：这些权限必须动态申请，仅在 Manifest 声明无效——
 * - 定位：不授权则水印上的地点永远显示兜底文案（LocationHelper 直接返回「定位未授权」）；
 * - 通知（Android 13+）：不授权则系统静默丢弃通知，「备案录不忘事」这条核心价值直接失效。
 */
fun requiredPermissions(): List<String> = buildList {
    add(Manifest.permission.ACCESS_FINE_LOCATION)
    add(Manifest.permission.ACCESS_COARSE_LOCATION)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.POST_NOTIFICATIONS)
    }
}

fun hasPermission(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

fun hasLocationPermission(context: Context): Boolean =
    hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
        hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)

/**
 * 进入界面时申请全部必需权限。
 * 已授权的权限系统不会重复弹窗，直接以 granted 返回，因此每次启动调用是安全的。
 */
@Composable
fun RequireRequiredPermissions(onResult: (Map<String, Boolean>) -> Unit = {}) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result -> onResult(result) }
    LaunchedEffect(Unit) {
        launcher.launch(requiredPermissions().toTypedArray())
    }
}
