package com.langrunner.app

import android.app.Application
import android.content.Intent
import android.os.Process
import android.util.Log
import kotlin.system.exitProcess

/**
 * Installs a global uncaught-exception handler that replaces the generic
 * "Lang Runner has stopped" system dialog with [CrashActivity], a screen
 * showing the actual stack trace (selectable/copyable). Without this, any
 * bug anywhere in the app just silently kills the process with no way to
 * see what actually went wrong.
 */
class LangRunnerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val trace = Log.getStackTraceString(throwable)
                val intent = Intent(this, CrashActivity::class.java).apply {
                    putExtra(CrashActivity.EXTRA_TRACE, trace)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(intent)
            } catch (_: Throwable) {
                previousHandler?.uncaughtException(thread, throwable)
            }
            Process.killProcess(Process.myPid())
            exitProcess(1)
        }
    }
}
