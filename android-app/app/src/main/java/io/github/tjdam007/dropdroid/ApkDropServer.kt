package io.github.tjdam007.dropdroid

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.File
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlin.math.max

data class ReceiverState(
    val running: Boolean = false,
    val ipAddress: String = "Not connected",
    val port: Int = ApkDropServer.PORT,
    val autoOpenApkInstaller: Boolean = false,
    val lastFileName: String = "",
    val lastMessage: String = "Waiting for files",
)

object ApkDropServer {
    const val PORT = 47881
    private const val BEACON_PORT = 47882
    private const val BUFFER_SIZE = 64 * 1024

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableState = MutableStateFlow(ReceiverState())
    val state: StateFlow<ReceiverState> = mutableState

    private var serverSocket: ServerSocket? = null
    private var started = false

    fun start(context: Context) {
        if (started) return
        started = true

        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences("apk_drop", Context.MODE_PRIVATE)
        mutableState.update {
            it.copy(
                running = true,
                ipAddress = localIpAddress(),
                autoOpenApkInstaller = prefs.getBoolean("auto_open_apk_installer", false),
                lastMessage = "Ready on Wi-Fi",
            )
        }

        scope.launch { runServer(appContext) }
        scope.launch { runBeacon(appContext) }
    }

    fun setAutoOpenApkInstaller(context: Context, enabled: Boolean) {
        context.applicationContext
            .getSharedPreferences("apk_drop", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("auto_open_apk_installer", enabled)
            .apply()
        mutableState.update { it.copy(autoOpenApkInstaller = enabled) }
    }

    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    }

    private fun runServer(context: Context) {
        try {
            ServerSocket(PORT).use { socket ->
                serverSocket = socket
                socket.reuseAddress = true
                while (scope.isActive) {
                    val client = socket.accept()
                    scope.launch { handleClient(context, client) }
                }
            }
        } catch (throwable: Throwable) {
            mutableState.update {
                it.copy(running = false, lastMessage = "Receiver stopped: ${throwable.message ?: "unknown error"}")
            }
        }
    }

    private fun handleClient(context: Context, socket: Socket) {
        socket.use { client ->
            val input = BufferedInputStream(client.getInputStream())
            val output = client.getOutputStream()
            val requestLine = input.readLineAscii()
            if (!requestLine.startsWith("PUT /upload")) {
                output.writeHttp(404, "Not found")
                return
            }

            val headers = mutableMapOf<String, String>()
            while (true) {
                val line = input.readLineAscii()
                if (line.isEmpty()) break
                val splitAt = line.indexOf(':')
                if (splitAt > 0) {
                    headers[line.substring(0, splitAt).lowercase(Locale.US)] = line.substring(splitAt + 1).trim()
                }
            }

            val contentLength = headers["content-length"]?.toLongOrNull()
            if (contentLength == null || contentLength <= 0L) {
                output.writeHttp(411, "Missing content length")
                return
            }

            val filename = requestLine.substringAfter("filename=", "shared-file")
                .substringBefore(' ')
                .decodeUrl()
                .safeFileName()
            val target = incomingDirectory(context).resolve(filename)
            copyBody(input, target, contentLength)

            mutableState.update {
                it.copy(
                    ipAddress = localIpAddress(),
                    lastFileName = filename,
                    lastMessage = "Received ${target.name} (${contentLength.toReadableSize()})",
                )
            }
            output.writeHttp(200, "Saved")

            if (filename.endsWith(".apk", ignoreCase = true) && mutableState.value.autoOpenApkInstaller) {
                openApkInstaller(context, target)
            }
        }
    }

    private fun runBeacon(context: Context) {
        DatagramSocket().use { socket ->
            socket.broadcast = true
            while (scope.isActive) {
                val ip = localIpAddress()
                mutableState.update { it.copy(ipAddress = ip) }
                val message = """{"name":"${Build.MODEL}","ip":"$ip","port":$PORT,"app":"DropDroid"}"""
                val data = message.toByteArray(StandardCharsets.UTF_8)
                val packet = DatagramPacket(data, data.size, InetAddress.getByName("255.255.255.255"), BEACON_PORT)
                runCatching { socket.send(packet) }
                Thread.sleep(1500)
            }
        }
    }

    private fun openApkInstaller(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(intent) }
            .onFailure { throwable ->
                mutableState.update { it.copy(lastMessage = "APK saved. Installer did not open: ${throwable.message}") }
            }
    }

    private fun incomingDirectory(context: Context): File {
        return File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "APKDrop").also { it.mkdirs() }
    }

    private fun copyBody(input: BufferedInputStream, target: File, contentLength: Long) {
        target.outputStream().use { output ->
            val buffer = ByteArray(BUFFER_SIZE)
            var remaining = contentLength
            while (remaining > 0) {
                val read = input.read(buffer, 0, max(1, minOf(buffer.size.toLong(), remaining).toInt()))
                if (read == -1) break
                output.write(buffer, 0, read)
                remaining -= read
            }
        }
    }

    private fun localIpAddress(): String {
        return NetworkInterface.getNetworkInterfaces().asSequence()
            .flatMap { it.inetAddresses.asSequence() }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { !it.isLoopbackAddress && it.hostAddress?.startsWith("169.254.") == false }
            ?.hostAddress ?: "Connect to Wi-Fi"
    }

    private fun BufferedInputStream.readLineAscii(): String {
        val bytes = mutableListOf<Byte>()
        while (true) {
            val next = read()
            if (next == -1) break
            if (next == '\n'.code) break
            if (next != '\r'.code) bytes.add(next.toByte())
        }
        return bytes.toByteArray().toString(StandardCharsets.US_ASCII)
    }

    private fun java.io.OutputStream.writeHttp(status: Int, body: String) {
        val reason = when (status) {
            200 -> "OK"
            404 -> "Not Found"
            411 -> "Length Required"
            else -> "Error"
        }
        val data = body.toByteArray(StandardCharsets.UTF_8)
        write("HTTP/1.1 $status $reason\r\nContent-Length: ${data.size}\r\nConnection: close\r\n\r\n".toByteArray())
        write(data)
        flush()
    }

    private fun String.decodeUrl(): String = URLDecoder.decode(this, StandardCharsets.UTF_8.name())

    private fun String.safeFileName(): String {
        return replace(Regex("""[\\/:*?"<>|]"""), "_").ifBlank { "shared-file" }
    }

    private fun Long.toReadableSize(): String {
        if (this < 1024) return "$this B"
        val units = listOf("KB", "MB", "GB")
        var value = this.toDouble()
        var unit = "B"
        for (next in units) {
            value /= 1024.0
            unit = next
            if (value < 1024.0) break
        }
        return "%.1f %s".format(Locale.US, value, unit)
    }
}
