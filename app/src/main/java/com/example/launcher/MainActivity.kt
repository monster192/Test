package com.example.launcher

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import java.io.File

class MainActivity : Activity() {

    // ---- палитра (собственная, тёмная с сине-фиолетовым акцентом)
    private val cBg1 = 0xFF0B0D13.toInt()
    private val cBg2 = 0xFF171B27.toInt()
    private val cCard = 0xFF151822.toInt()
    private val cField = 0xFF0F1219.toInt()
    private val cStroke = 0xFF2A2F42.toInt()
    private val cMuted = 0xFF7D8499.toInt()
    private val cAccent = 0xFF6C8CFF.toInt()
    private val cAccent2 = 0xFF8A5CFF.toInt()
    private val cOnline = 0xFF3DDC84.toInt()
    private val cOffline = 0xFFE5484D.toInt()

    private val ui = Handler(Looper.getMainLooper())
    private var servers: List<ServerInfo> = emptyList()
    private var current = 0
    private var busy = false
    private var destroyed = false

    private lateinit var nickInput: EditText
    private lateinit var statusDot: View
    private lateinit var serverName: TextView
    private lateinit var onlineText: TextView
    private lateinit var eyebrow: TextView
    private lateinit var title: TextView
    private lateinit var description: TextView
    private lateinit var socialBox: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var playButton: TextView
    private lateinit var repairButton: TextView

    private val nickRegex = Regex("^[A-Za-z0-9_]{3,20}$")

    private val poll = object : Runnable {
        override fun run() {
            refreshStatus()
            ui.postDelayed(this, 10_000)
        }
    }

    // ------------------------------------------------------------ жизненный цикл
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
        nickInput.setText(prefs().getString("nick", ""))
        loadServers()
    }

    override fun onResume() {
        super.onResume()
        ui.removeCallbacks(poll)
        ui.post(poll)
    }

    override fun onPause() {
        super.onPause()
        ui.removeCallbacks(poll)
    }

    override fun onDestroy() {
        destroyed = true
        super.onDestroy()
    }

    // ------------------------------------------------------------ помощники
    private fun prefs() = getSharedPreferences("launcher", Context.MODE_PRIVATE)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun lp(w: Int, h: Int, weight: Float = 0f) = LinearLayout.LayoutParams(w, h, weight)
    private val MATCH = LinearLayout.LayoutParams.MATCH_PARENT
    private val WRAP = LinearLayout.LayoutParams.WRAP_CONTENT

    private fun rounded(fill: Int, radiusDp: Int, stroke: Int = 0) = GradientDrawable().apply {
        setColor(fill)
        cornerRadius = dp(radiusDp).toFloat()
        if (stroke != 0) setStroke(dp(1), stroke)
    }

    private fun gradientBtn(radiusDp: Int) = GradientDrawable(
        GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(cAccent, cAccent2)
    ).apply { cornerRadius = dp(radiusDp).toFloat() }

    private fun setStatus(text: String) { statusText.text = text }

    private fun gameDir(): File = File(getExternalFilesDir(null) ?: filesDir, "game")

    // ------------------------------------------------------------ интерфейс
    private fun buildUi(): View {
        val root = FrameLayout(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(cBg1, cBg2))
        }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(28), dp(18), dp(28), dp(18))
        }
        root.addView(col, FrameLayout.LayoutParams(MATCH, MATCH))

        // --- верхняя строка: ник слева, выбор сервера справа
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val nickCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(cCard, 18, cStroke)
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }
        nickInput = EditText(this).apply {
            hint = "Nick_Name"
            setHintTextColor(cMuted)
            setTextColor(Color.WHITE)
            textSize = 16f
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            background = rounded(cField, 14, cStroke)
            setPadding(dp(14), dp(8), dp(14), dp(8))
        }
        val infoBtn = TextView(this).apply {
            text = "i"
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(cAccent)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
            background = rounded(cField, 14, cStroke)
            setOnClickListener {
                AlertDialog.Builder(this@MainActivity)
                    .setMessage("Ник: от 3 до 20 символов, только латиница, цифры и _.")
                    .setPositiveButton("OK", null).show()
            }
        }
        nickCard.addView(nickInput, lp(dp(230), dp(44)))
        nickCard.addView(infoBtn, lp(dp(44), dp(44)).apply { marginStart = dp(8) })
        top.addView(nickCard, lp(WRAP, WRAP))
        top.addView(View(this), lp(0, 1, 1f))

        val serverCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(cCard, 18, cStroke)
            setPadding(dp(18), dp(12), dp(18), dp(12))
            setOnClickListener { pickServer() }
        }
        statusDot = View(this).apply { background = rounded(cMuted, 6) }
        serverName = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
        }
        onlineText = TextView(this).apply {
            setTextColor(cAccent)
            textSize = 15f
        }
        serverCard.addView(statusDot, lp(dp(12), dp(12)))
        serverCard.addView(serverName, lp(WRAP, WRAP).apply { marginStart = dp(12) })
        serverCard.addView(onlineText, lp(WRAP, WRAP).apply { marginStart = dp(14) })
        top.addView(serverCard, lp(WRAP, WRAP))
        col.addView(top, lp(MATCH, WRAP))

        // --- середина: карточка проекта и соцсети
        val mid = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val mainCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(cCard, 22, cStroke)
            setPadding(dp(26), dp(22), dp(26), dp(22))
        }
        eyebrow = TextView(this).apply {
            setTextColor(cAccent)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.12f
        }
        title = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
        }
        description = TextView(this).apply {
            setTextColor(cMuted)
            textSize = 15f
        }
        mainCard.addView(eyebrow, lp(WRAP, WRAP))
        mainCard.addView(title, lp(WRAP, WRAP).apply { topMargin = dp(6) })
        mainCard.addView(description, lp(WRAP, WRAP).apply { topMargin = dp(12) })
        mid.addView(mainCard, lp(0, MATCH, 1f))

        socialBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        mid.addView(socialBox, lp(WRAP, WRAP).apply { marginStart = dp(16) })
        col.addView(mid, lp(MATCH, 0, 1f).apply { topMargin = dp(14); bottomMargin = dp(14) })

        // --- нижняя строка: «Починить игру», статус, «ИГРАТЬ»
        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        repairButton = TextView(this).apply {
            text = "Починить игру"
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = rounded(cCard, 18, cStroke)
            setPadding(dp(22), 0, dp(22), 0)
            setOnClickListener { confirmRepair() }
        }
        bottom.addView(repairButton, lp(WRAP, dp(52)))

        val mid2 = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(16), 0, dp(16), 0)
        }
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            visibility = View.INVISIBLE
        }
        statusText = TextView(this).apply {
            text = "Готов к запуску"
            setTextColor(cMuted)
            textSize = 13f
            gravity = Gravity.CENTER
        }
        mid2.addView(progressBar, lp(MATCH, WRAP))
        mid2.addView(statusText, lp(MATCH, WRAP))
        bottom.addView(mid2, lp(0, WRAP, 1f))

        playButton = TextView(this).apply {
            text = "▶   ИГРАТЬ"
            setTextColor(Color.WHITE)
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = gradientBtn(26)
            setPadding(dp(48), 0, dp(48), 0)
            setOnClickListener { startFlow(repair = false) }
        }
        bottom.addView(playButton, lp(WRAP, dp(56)))
        col.addView(bottom, lp(MATCH, WRAP))

        return root
    }

    // ------------------------------------------------------------ серверы
    private fun loadServers() {
        Thread {
            val list = try { Servers.load(this) } catch (e: Exception) { emptyList() }
            runOnUiThread {
                if (destroyed) return@runOnUiThread
                servers = list
                if (list.isEmpty()) {
                    serverName.text = "—"
                    setStatus("Не удалось прочитать servers.json")
                } else {
                    select(prefs().getInt("server", 0).coerceIn(0, list.size - 1))
                }
            }
        }.start()
    }

    private fun select(index: Int) {
        current = index
        prefs().edit().putInt("server", index).apply()
        val s = servers[index]
        serverName.text = s.name
        onlineText.text = "…"
        statusDot.background = rounded(cMuted, 6)
        eyebrow.text = s.subtitle
        title.text = s.title.ifBlank { s.name }
        description.text = s.description

        socialBox.removeAllViews()
        listOf("TG" to s.telegram, "VK" to s.vk, "DS" to s.discord).forEach { (label, url) ->
            if (url.isBlank()) return@forEach
            val b = TextView(this).apply {
                text = label
                setTextColor(cAccent)
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                background = rounded(cCard, 18, cStroke)
                setOnClickListener {
                    try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                    catch (_: Exception) { setStatus("Не удалось открыть ссылку") }
                }
            }
            socialBox.addView(b, lp(dp(60), dp(60)).apply { bottomMargin = dp(10) })
        }
        refreshStatus()
    }

    private fun pickServer() {
        if (servers.size < 2) return
        AlertDialog.Builder(this)
            .setItems(servers.map { it.name }.toTypedArray()) { _, which -> select(which) }
            .show()
    }

    private fun refreshStatus() {
        val s = servers.getOrNull(current) ?: return
        Thread {
            val info = SampQuery.query(s.ip, s.port)
            runOnUiThread {
                if (destroyed || servers.getOrNull(current) !== s) return@runOnUiThread
                if (info != null) {
                    statusDot.background = rounded(cOnline, 6)
                    onlineText.text = "${info.players} / ${info.maxPlayers}"
                } else {
                    statusDot.background = rounded(cOffline, 6)
                    onlineText.text = "оффлайн"
                }
            }
        }.start()
    }

    // ------------------------------------------------------------ запуск и ремонт
    private fun confirmRepair() {
        if (busy) return
        AlertDialog.Builder(this)
            .setMessage("Удалить загруженные файлы игры и скачать заново?")
            .setPositiveButton("Да") { _, _ -> startFlow(repair = true) }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun setBusy(value: Boolean) {
        busy = value
        playButton.isEnabled = !value
        repairButton.isEnabled = !value
        progressBar.visibility = if (value) View.VISIBLE else View.INVISIBLE
    }

    private fun startFlow(repair: Boolean) {
        if (busy) return
        val server = servers.getOrNull(current)
        if (server == null) { setStatus("Нет доступного сервера"); return }

        val nick = nickInput.text.toString().trim()
        if (!repair) {
            if (!nickRegex.matches(nick)) {
                setStatus("Ник: 3–20 символов, латиница, цифры и _")
                return
            }
            prefs().edit().putString("nick", nick).apply()
        }

        if (BuildConfig.MANIFEST_URL.isBlank()) {
            if (repair) setStatus("Обновления не настроены (MANIFEST_URL пуст)")
            else launchGame(nick, server)
            return
        }

        setBusy(true)
        setStatus("Проверка файлов…")
        Thread {
            try {
                val dir = gameDir()
                if (repair) { dir.deleteRecursively() }
                dir.mkdirs()
                Updater.sync(dir, BuildConfig.MANIFEST_URL) { done, total ->
                    runOnUiThread {
                        progressBar.max = total.coerceAtLeast(1)
                        progressBar.progress = done
                        setStatus("Загрузка файлов: $done / $total")
                    }
                }
                runOnUiThread {
                    if (repair) setStatus("Файлы игры восстановлены") else launchGame(nick, server)
                }
            } catch (e: Exception) {
                runOnUiThread { setStatus("Ошибка: ${e.message}") }
            } finally {
                runOnUiThread { setBusy(false) }
            }
        }.start()
    }

    private fun launchGame(nick: String, server: ServerInfo) {
        if (BuildConfig.LAUNCH_MODE == "jni") launchViaJni(nick, server) else launchViaIntent(nick, server)
    }

    private fun launchViaJni(nick: String, server: ServerInfo) {
        val err = NativeBridge.load(BuildConfig.NATIVE_LIB)
        if (err != null) {
            setStatus("Не загружена lib${BuildConfig.NATIVE_LIB}.so: $err")
            return
        }
        try {
            val rc = NativeBridge.start(nick, server.ip, server.port)
            if (rc != 0) setStatus("Клиент вернул код ошибки: $rc")
        } catch (e: UnsatisfiedLinkError) {
            setStatus("В библиотеке нет функции start(): ${e.message}")
        }
    }

    private fun launchViaIntent(nick: String, server: ServerInfo) {
        val pkg = BuildConfig.CLIENT_PACKAGE
        if (pkg.isBlank()) {
            setStatus("Клиент не подключён: укажите clientPackage в app/build.gradle.kts")
            return
        }
        val intent = packageManager.getLaunchIntentForPackage(pkg)
        if (intent == null) {
            setStatus("Игровой клиент не установлен: $pkg")
            return
        }
        intent.putExtra("nickname", nick)
        intent.putExtra("ip", server.ip)
        intent.putExtra("port", server.port)
        startActivity(intent)
    }
}
