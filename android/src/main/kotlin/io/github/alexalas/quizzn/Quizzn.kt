package io.github.alexalas.quizzn

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.widget.FrameLayout
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.window.OnBackInvokedDispatcher

// Quizz'n para Android: carga la misma web que la versión de Windows. La web habla con la app por
// window.QuizznHost.postMessage(): "cfg:minutos:esc" (ajustes), "now" (pausa ya) y "done" (pausa terminada).
// La pausa es la misma web con ?overlay, abierta en BreakActivity encima del juego.
const val SITE = "https://alex-alas.github.io/quizzn/"

fun Context.prefs() = getSharedPreferences("quizzn", Context.MODE_PRIVATE)

// "cfg:20:1" → guarda los minutos y reprograma solo si cambiaron (abrir la app no reinicia la cuenta)
fun Context.onConfig(m: String) {
    val min = m.split(":").getOrNull(1)?.toIntOrNull()?.coerceIn(1, 240) ?: return
    if (prefs().getInt("min", 20) == min) return
    prefs().edit().putInt("min", min).apply()
    BreakService.schedule(this)
}

@SuppressLint("SetJavaScriptEnabled")
fun Activity.webView(url: String, client: WebViewClient = WebViewClient(), onMessage: (String) -> Unit) {
    val web = WebView(this).apply {
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) WebView.setWebContentsDebuggingEnabled(true)
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true // la biblioteca de quizzes vive en localStorage
        webViewClient = client
        addJavascriptInterface(object {
            @JavascriptInterface fun postMessage(m: String) { post { onMessage(m) } } // llega en otro hilo
        }, "QuizznHost")
        loadUrl(url)
    }
    // Android 15+ dibuja detrás de la barra de estado, la de navegación y el teclado: el marco deja ese espacio libre
    setContentView(FrameLayout(this).apply {
        addView(web)
        if (Build.VERSION.SDK_INT >= 30) setOnApplyWindowInsetsListener { v, insets ->
            insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.ime()).run { v.setPadding(left, top, right, bottom) }
            WindowInsets.CONSUMED
        }
    })
}

class MainActivity : Activity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        webView(SITE) { m ->
            if (m.startsWith("cfg:")) onConfig(m)
            else if (m == "now") startActivity(Intent(this, BreakActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        if (!Settings.canDrawOverlays(this)) {
            AlertDialog.Builder(this)
                .setTitle("Permiso para las pausas")
                .setMessage("Quizz'n necesita «Mostrar sobre otras apps» para poner la pausa de estudio encima del juego.")
                .setPositiveButton("Abrir ajustes") { _, _ ->
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                }
                .setNegativeButton("Ahora no", null)
                .show()
            return
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        startForegroundService(Intent(this, BreakService::class.java))
    }
}

class BreakActivity : Activity() {
    private var ready = false // la web cargó y la pausa corre: Atrás ya no la cierra
    private var ended = false

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        webView("$SITE?overlay", object : WebViewClient() {
            // si la web no cargó o es una versión vieja sin pausa, no dejamos al jugador atrapado
            override fun onPageFinished(v: WebView, url: String) =
                v.evaluateJavascript("typeof overlayBreak") { if (it == "\"function\"") ready = true else end() }
            override fun onReceivedError(v: WebView, r: WebResourceRequest, e: WebResourceError) { if (r.isForMainFrame) end() }
        }) { m -> if (m == "done") end() else if (m.startsWith("cfg:")) onConfig(m) }
        if (Build.VERSION.SDK_INT >= 33)
            onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT) { if (!ready) finish() }
    }

    @Deprecated("Solo Android < 13")
    override fun onBackPressed() { if (!ready) super.onBackPressed() }

    // salir con Inicio o Recientes sin terminar: la pausa vuelve en 1 minuto
    override fun onStop() { super.onStop(); end(60_000) }

    private fun end(delay: Long? = null) {
        if (ended) return
        ended = true
        BreakService.schedule(this, delay)
        finish()
    }
}

class BreakService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val tick = Runnable { launchBreak() }
    private var armed = false

    override fun onBind(i: Intent?) = null
    override fun onCreate() { super.onCreate(); running = true }
    override fun onDestroy() { running = false; handler.removeCallbacks(tick); super.onDestroy() }

    override fun onStartCommand(i: Intent?, flags: Int, startId: Int): Int {
        if (i?.action == "stop") { stopSelf(); return START_NOT_STICKY }
        showNotification()
        if (i?.action == "schedule" || !armed) {
            val delay = i?.getLongExtra("delay", -1L)?.takeIf { it >= 0 } ?: (prefs().getInt("min", 20) * 60_000L)
            arm(delay)
        }
        return START_STICKY
    }

    private fun arm(delay: Long) {
        handler.removeCallbacks(tick)
        handler.postDelayed(tick, delay)
        armed = true
    }

    private fun showNotification() {
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel("pausas", "Pausas de estudio", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1, Intent(this, BreakService::class.java).setAction("stop"), PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(this, "pausas")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Pausas de estudio activas")
            .setContentText("Cada ${prefs().getInt("min", 20)} min. «Detener» las apaga hasta que abras Quizz'n.")
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(null as Icon?, "Detener", stop).build()) // salida de emergencia
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE) else startForeground(1, n)
    }

    private fun launchBreak() {
        armed = false
        if (!Settings.canDrawOverlays(this)) return
        // con la pantalla apagada o bloqueada no tiene sentido: se reintenta en 1 minuto
        if (!getSystemService(PowerManager::class.java).isInteractive || getSystemService(KeyguardManager::class.java).isKeyguardLocked)
            return arm(60_000)
        // Android 15+ solo deja abrir una pantalla desde segundo plano si la app tiene una ventana superpuesta visible
        val wm = getSystemService(WindowManager::class.java)
        val dot = View(this)
        wm.addView(dot, WindowManager.LayoutParams(1, 1, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, PixelFormat.TRANSPARENT))
        startActivity(Intent(this, BreakActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        handler.postDelayed({ wm.removeView(dot) }, 2000)
    }

    companion object {
        @Volatile var running = false

        // reprograma la próxima pausa (delay en ms; null = los minutos de los ajustes). Si "Detener" apagó el servicio, no hace nada
        fun schedule(c: Context, delay: Long? = null) {
            if (running) runCatching { c.startService(Intent(c, BreakService::class.java).setAction("schedule").putExtra("delay", delay ?: -1L)) }
        }
    }
}
