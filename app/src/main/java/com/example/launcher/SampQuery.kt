package com.example.launcher

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/** Запрос статуса SA-MP сервера по UDP (пакет 'i'): онлайн, максимум, название. */
object SampQuery {

    data class Info(val players: Int, val maxPlayers: Int, val hostname: String)

    fun query(host: String, port: Int, timeoutMs: Int = 2000): Info? {
        return try {
            val addr = InetAddress.getByName(host)
            val ip = addr.address
            if (ip.size != 4) return null

            val req = ByteArray(11)
            "SAMP".toByteArray(Charsets.US_ASCII).copyInto(req, 0)
            ip.copyInto(req, 4)
            req[8] = (port and 0xFF).toByte()
            req[9] = ((port shr 8) and 0xFF).toByte()
            req[10] = 'i'.code.toByte()

            DatagramSocket().use { s ->
                s.soTimeout = timeoutMs
                s.send(DatagramPacket(req, req.size, addr, port))
                val buf = ByteArray(1024)
                val p = DatagramPacket(buf, buf.size)
                s.receive(p)
                parse(buf, p.length)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun u16(b: ByteArray, o: Int) = (b[o].toInt() and 0xFF) or ((b[o + 1].toInt() and 0xFF) shl 8)
    private fun u32(b: ByteArray, o: Int) = u16(b, o) or (u16(b, o + 2) shl 16)

    private fun parse(b: ByteArray, len: Int): Info? {
        // 11 байт заголовка, затем: пароль(1) игроки(2) максимум(2) длина имени(4) имя
        var o = 11
        if (len < o + 9) return null
        o += 1
        val players = u16(b, o)
        val max = u16(b, o + 2)
        val nameLen = u32(b, o + 4)
        val start = o + 8
        if (nameLen < 0 || start + nameLen > len) return Info(players, max, "")
        val name = String(b, start, nameLen, charset("windows-1251"))
        return Info(players, max, name)
    }
}
