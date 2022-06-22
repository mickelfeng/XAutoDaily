package me.teble.xposed.autodaily.hook

import android.app.Activity
import android.view.Display
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND
import android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND
import androidx.compose.ui.platform.ComposeView
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.teble.xposed.autodaily.config.QQClasses.Companion.SplashActivity
import me.teble.xposed.autodaily.hook.CoreServiceHook.Companion.AUTO_EXEC
import me.teble.xposed.autodaily.hook.CoreServiceHook.Companion.handler
import me.teble.xposed.autodaily.hook.annotation.MethodHook
import me.teble.xposed.autodaily.hook.base.BaseHook
import me.teble.xposed.autodaily.hook.base.ProcUtil
import me.teble.xposed.autodaily.task.util.ConfigUtil
import me.teble.xposed.autodaily.task.util.ConfigUtil.loadSaveConf
import me.teble.xposed.autodaily.task.util.formatDate
import me.teble.xposed.autodaily.ui.AppUpdateLayout
import me.teble.xposed.autodaily.ui.ConfUnit
import me.teble.xposed.autodaily.ui.ConfigUpdateLayout
import me.teble.xposed.autodaily.ui.CustomDialog
import java.util.*

class SplashActivityHook : BaseHook() {

    override val isCompatible: Boolean
        get() = ProcUtil.isMain

    override val enabled: Boolean
        get() = true

    private val scope = CoroutineScope(Dispatchers.Default)

    @MethodHook("SplashActivity Hook")
    private fun splashActivityHook() {
        findMethod(SplashActivity) { name == "doOnCreate" }.hookAfter {
            val context = it.thisObject as Activity
            scope.launch {
                withContext(Dispatchers.IO) {
                    loadSaveConf()
                    if (ConfigUtil.checkUpdate(false)) {
                        ConfUnit.needUpdate = true
                    }
                }
                withContext(Dispatchers.Main) {
                    if (ConfUnit.needUpdate) {
                        context.openAppUpdateDialog()
                    } else if (ConfUnit.needShowUpdateLog) {
                        context.openConfigUpdateLog()
                        ConfUnit.needShowUpdateLog = false
                    }
                    handler.sendEmptyMessageDelayed(AUTO_EXEC, 10_000)
                }
            }
        }

        findMethod(SplashActivity) { name == "doOnStart" }.hookAfter {
            val context = it.thisObject as Activity
            scope.launch {
                withContext(Dispatchers.Main) {
                    if (ConfUnit.needUpdate) {
                        context.openAppUpdateDialog()
                    } else if (ConfUnit.needShowUpdateLog) {
                        context.openConfigUpdateLog()
                        ConfUnit.needShowUpdateLog = false
                    }
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
fun Activity.openAppUpdateDialog() {
    this.runOnUiThread {
        val dialog = CustomDialog(this).apply {
            val dialog = this
            val windowManager: WindowManager = this@openAppUpdateDialog.windowManager
            val display: Display = windowManager.defaultDisplay
            val lp = window?.attributes
            lp?.width = (display.width * 0.8).toInt()
            lp?.height = (display.height * 0.6).toInt()
            lp?.dimAmount = 0.42f
            window?.addFlags(FLAG_BLUR_BEHIND)
            window?.addFlags(FLAG_DIM_BEHIND)
            val view = ComposeView(context).apply {
                setContent {
                    AppUpdateLayout(dialog)
                }
            }
            setContentView(view)
        }
        val skipShow = ConfUnit.blockUpdateOneDay == Date().formatDate()
        if (!dialog.isShowing && !isFinishing && !skipShow) {
            dialog.show()
        }
    }
}

@Suppress("DEPRECATION")
fun Activity.openConfigUpdateLog() {
    this.runOnUiThread {
        val dialog = CustomDialog(this).apply {
            val dialog = this
            val windowManager: WindowManager = this@openConfigUpdateLog.windowManager
            val display: Display = windowManager.defaultDisplay
            val lp = window?.attributes
            lp?.width = (display.width * 0.8).toInt()
            lp?.height = (display.height * 0.6).toInt()
            lp?.dimAmount = 0.42f
            window?.addFlags(FLAG_BLUR_BEHIND)
            window?.addFlags(FLAG_DIM_BEHIND)
            val view = ComposeView(context).apply {
                setContent {
                    ConfigUpdateLayout(dialog)
                }
            }
            setContentView(view)
        }
        if (!dialog.isShowing && !isFinishing) {
            dialog.show()
        }
    }
}