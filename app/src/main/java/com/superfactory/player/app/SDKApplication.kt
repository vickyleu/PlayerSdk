package com.superfactory.player.app

import android.app.Application
import java.io.File

/**
 * Created by vicky on 2017.11.08.
 */

class SDKApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 检查/mnt/sdcard/TAOBAOPLAYER 是否存在,不存在创建
        val rootPath = File("/mnt/sdcard/aliyun")
        if (!rootPath.exists()) {
            rootPath.mkdir()
        }
    }
}
