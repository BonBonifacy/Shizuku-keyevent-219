package com.clean.key219

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import rikka.shizuku.Shizuku
import kotlin.concurrent.thread

/**
 * 零界面 (Zero-UI) Shizuku 触发器 Activity
 * 打开即通过 Shizuku (adb/shell UID 2000 权限) 执行: input keyevent 219
 * 不加载任何 setContentView 布局，执行后立即销毁且不留后台卡片。
 */
class TriggerActivity : Activity(), Shizuku.OnRequestPermissionResultListener {

    companion object {
        private const val REQUEST_CODE_SHIZUKU = 2190
        private const val SHELL_COMMAND = "input keyevent 219"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 禁用窗口进出场动画，确保视觉零闪烁
        disableWindowTransition()

        if (!Shizuku.pingBinder()) {
            notifyError("Shizuku 服务未运行，请先启动 Shizuku")
            closeCleanly()
            return
        }

        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            // 已授权：直接异步执行命令并瞬间退出 Activity
            executeKeyeventAndExit()
        } else {
            // 首次打开尚未授权：注册回调并拉起 Shizuku 原生授权弹窗
            Shizuku.addRequestPermissionResultListener(this)
            Shizuku.requestPermission(REQUEST_CODE_SHIZUKU)
        }
    }

    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
        if (requestCode == REQUEST_CODE_SHIZUKU) {
            Shizuku.removeRequestPermissionResultListener(this)
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                executeKeyeventAndExit()
            } else {
                notifyError("Shizuku 权限被拒绝")
                closeCleanly()
            }
        }
    }

    private fun executeKeyeventAndExit() {
        thread(start = true, name = "ShizukuKey219Runner") {
            try {
                // 通过 Shizuku 内部 newProcess 在 UID 2000 (shell) 下执行命令
                val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                    "newProcess",
                    Array<String>::class.java,
                    Array<String>::class.java,
                    String::class.java
                )
                newProcessMethod.isAccessible = true
                val process = newProcessMethod.invoke(
                    null,
                    arrayOf("sh", "-c", SHELL_COMMAND),
                    null,
                    null
                ) as Process
                process.waitFor()
            } catch (e: Throwable) {
                notifyError("执行失败: ${e.message}")
            }
        }
        closeCleanly()
    }

    private fun closeCleanly() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask()
        } else {
            finish()
        }
        disableWindowTransition()
    }

    @Suppress("DEPRECATION")
    private fun disableWindowTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            overridePendingTransition(0, 0)
        }
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(this)
        super.onDestroy()
    }

    private fun notifyError(msg: String) {
        runOnUiThread {
            Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
        }
    }
}
