package com.example.launcher

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ServerInfo(
    val name: String,
    val ip: String,
    val port: Int,
    val subtitle: String,
    val title: String,
    val description: String,
    val telegram: String,
    val vk: String,
    val discord: String
)

/**
 * Формат servers.json:
 * { "servers": [ { "name": "...", "ip": "1.2.3.4", "port": 7777, "subtitle": "...",
 *                  "title": "...", "description": "...", "telegram": "", "vk": "", "discord": "" } ] }
 */
object Servers {

    /** Сначала пробует SERVERS_URL (если задан), при ошибке берёт assets/servers.json. */
    fun load(context: Context): List<ServerInfo> {
        if (BuildConfig.SERVERS_URL.isNotBlank()) {
            try {
                return parse(download(BuildConfig.SERVERS_URL))
            } catch (_: Exception) {
                // нет сети или битый файл — берём встроенный список
            }
        }
        val text = context.assets.open("servers.json").bufferedReader().use { it.readText() }
        return parse(text)
    }

    private fun download(url: String): String {
        require(url.startsWith("https://")) { "Разрешены только https-ссылки" }
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 10_000
        c.readTimeout = 15_000
        check(c.responseCode == 200) { "HTTP ${c.responseCode}" }
        return try { c.inputStream.bufferedReader().use { it.readText() } } finally { c.disconnect() }
    }

    private fun parse(text: String): List<ServerInfo> {
        val arr = JSONObject(text).getJSONArray("servers")
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            ServerInfo(
                name = o.optString("name", "SERVER"),
                ip = o.getString("ip"),
                port = o.optInt("port", 7777),
                subtitle = o.optString("subtitle", ""),
                title = o.optString("title", ""),
                description = o.optString("description", ""),
                telegram = o.optString("telegram", ""),
                vk = o.optString("vk", ""),
                discord = o.optString("discord", "")
            )
        }
    }
}
