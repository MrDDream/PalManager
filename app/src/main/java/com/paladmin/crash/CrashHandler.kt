package com.paladmin.crash

import android.content.Context
import android.os.Process
import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

/**
 * Sans adb, un crash au démarrage ne laisse aucune trace exploitable pour l'utilisateur — ce
 * handler écrit la stack trace dans des SharedPreferences (survit au kill du process) et
 * MainActivity l'affiche en plein écran au relancement suivant, avec un bouton copier.
 */
object CrashHandler {
    private const val PREFS_NAME = "crash_log"
    private const val KEY_TRACE = "last_trace"

    fun install(context: Context) {
        val appContext = context.applicationContext
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val writer = StringWriter()
                throwable.printStackTrace(PrintWriter(writer))
                appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_TRACE, writer.toString())
                    .commit()
            }
            Log.e("PalAdminCrash", "Uncaught exception", throwable)
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable)
            } else {
                Process.killProcess(Process.myPid())
                exitProcess(10)
            }
        }
    }

    fun consumeLastCrash(context: Context): String? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val trace = prefs.getString(KEY_TRACE, null)
        if (trace != null) {
            prefs.edit().remove(KEY_TRACE).commit()
        }
        return trace
    }
}
