package com.example.launcher

import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Формат manifest.json:
 * {
 *   "files": [
 *     { "path": "data/models.bin", "url": "https://cdn.example.com/models.bin", "sha256": "<hex>" }
 *   ]
 * }
 */
object Updater {

    private data class Entry(val path: String, val url: String, val sha256: String)

    /** Скачивает недостающие или изменённые файлы. onProgress(готово, всего). */
    fun sync(dir: File, manifestUrl: String, onProgress: (Int, Int) -> Unit) {
        val json = JSONObject(readText(manifestUrl))
        val arr = json.getJSONArray("files")
        val entries = (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Entry(o.getString("path"), o.getString("url"), o.getString("sha256").lowercase())
        }

        val root = dir.canonicalFile
        for ((index, e) in entries.withIndex()) {
            onProgress(index, entries.size)
            require(e.url.startsWith("https://")) { "Разрешены только https-ссылки: ${e.path}" }

            val target = File(root, e.path).canonicalFile
            // защита от выхода за пределы папки игры (../)
            require(target.path.startsWith(root.path + File.separator)) { "Недопустимый путь: ${e.path}" }

            if (target.exists() && sha256(target) == e.sha256) continue

            target.parentFile?.mkdirs()
            val part = File(target.path + ".part")
            download(e.url, part)
            check(sha256(part) == e.sha256) { "Контрольная сумма не совпала: ${e.path}" }
            if (target.exists()) target.delete()
            check(part.renameTo(target)) { "Не удалось сохранить: ${e.path}" }
        }
        onProgress(entries.size, entries.size)
    }

    private fun open(url: String): HttpURLConnection {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15_000
        c.readTimeout = 30_000
        c.instanceFollowRedirects = true
        check(c.responseCode == 200) { "HTTP ${c.responseCode}" }
        return c
    }

    private fun readText(url: String): String {
        val c = open(url)
        return try { c.inputStream.bufferedReader().use { it.readText() } } finally { c.disconnect() }
    }

    private fun download(url: String, out: File) {
        val c = open(url)
        try {
            c.inputStream.use { input -> out.outputStream().use { input.copyTo(it) } }
        } finally {
            c.disconnect()
        }
    }

    private fun sha256(f: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        f.inputStream().use { s ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = s.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
