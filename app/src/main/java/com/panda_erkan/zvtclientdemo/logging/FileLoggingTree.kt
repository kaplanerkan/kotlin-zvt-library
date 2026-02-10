package com.panda_erkan.zvtclientdemo.logging

import android.content.Context
import android.os.Environment
import android.util.Log
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class FileLoggingTree(context: Context) : Timber.Tree() {

    private val logDir: File = File(
        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
        "logs"
    )
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val fileDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val executor = Executors.newSingleThreadExecutor()

    init {
        logDir.mkdirs()
        cleanOldLogs()
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        executor.execute {
            try {
                val today = fileDateFormat.format(Date())
                val file = File(logDir, "zvt_$today.log")

                val level = when (priority) {
                    Log.VERBOSE -> "V"
                    Log.DEBUG -> "D"
                    Log.INFO -> "I"
                    Log.WARN -> "W"
                    Log.ERROR -> "E"
                    Log.ASSERT -> "A"
                    else -> "?"
                }

                val timestamp = dateFormat.format(Date())
                val logTag = tag ?: "ZvtClient"

                val sb = StringBuilder()
                sb.append("$timestamp $level/$logTag: $message\n")

                if (t != null) {
                    val sw = StringWriter()
                    t.printStackTrace(PrintWriter(sw))
                    sb.append(sw.toString())
                    sb.append("\n")
                }

                FileOutputStream(file, true).use { fos ->
                    fos.write(sb.toString().toByteArray(Charsets.UTF_8))
                }
            } catch (_: Exception) {
                // Logging should never crash the app
            }
        }
    }

    private fun cleanOldLogs() {
        executor.execute {
            try {
                val cutoff = System.currentTimeMillis() - RETENTION_MS
                logDir.listFiles()?.forEach { file ->
                    if (file.name.startsWith("zvt_") && file.lastModified() < cutoff) {
                        file.delete()
                    }
                }
            } catch (_: Exception) {
                // Ignore cleanup errors
            }
        }
    }

    companion object {
        private const val RETENTION_MS = 30L * 24 * 60 * 60 * 1000 // 30 days
    }
}
