package com.jiangxin.siterecord

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.jiangxin.siterecord.ui.nav.AppNavHost
import com.jiangxin.siterecord.ui.theme.SiteRecordTheme
import com.jiangxin.siterecord.util.RequireRequiredPermissions

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SiteRecordTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 进入即申请定位与通知权限，否则水印地点与备案提醒形同虚设
                    RequireRequiredPermissions()
                    AppNavHost()
                }
            }
        }
    }
}
