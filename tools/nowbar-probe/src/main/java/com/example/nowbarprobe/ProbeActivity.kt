package com.example.nowbarprobe

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.example.reading.samsung.SamsungNowBarCompatibility

/** No network, accounts, library access, service, overlay or periodic updates. */
class ProbeActivity : Activity() {
    private lateinit var status: TextView
    private val manager get() = getSystemService(NotificationManager::class.java)
    private val notificationId = 4201
    private val channel = "reading_compatibility_test"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 96, 32, 32)
        }
        status = TextView(this).apply { textSize = 18f }
        container.addView(status)
        listOf("Teste Android padrão" to "native", "Teste Samsung Now Bar" to "samsung", "Teste Samsung simples" to "samsung_plain", "Encerrar teste" to "stop").forEach { (title, mode) ->
            container.addView(Button(this).apply { text = title; minHeight = (48 * resources.displayMetrics.density).toInt(); setOnClickListener { publish(mode) } })
        }
        setContentView(container)
        publish(intent.getStringExtra("mode") ?: "native")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        publish(intent.getStringExtra("mode") ?: "native")
    }

    @Suppress("MissingPermission")
    private fun publish(mode: String) {
        if (mode == "stop") { manager.cancel(notificationId); status.text = "Teste encerrado."; return }
        if (Build.VERSION.SDK_INT < 37) { status.text = "Este diagnóstico requer Android 17."; return }
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            status.text = "Permita notificações para executar o teste."; return
        }
        manager.createNotificationChannel(NotificationChannel(channel, "Teste de leitura", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, ProbeActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val stop = PendingIntent.getActivity(this, 1, Intent(this, ProbeActivity::class.java).putExtra("mode", "stop"), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val builder = Notification.Builder(this, channel)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle("Teste de leitura · Quadrinheiro")
            .setContentText("Pág. 24 de 120 · 20%")
            .setShortCriticalText("20%")
            .setOngoing(true).setOnlyAlertOnce(true).setShowWhen(false)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_PROGRESS).setContentIntent(open)
            .addAction(Notification.Action.Builder(null, "Encerrar teste", stop).build())
        if (mode != "samsung_plain") builder.setRequestPromotedOngoing(true).setColorized(false)
            .setStyle(Notification.ProgressStyle().setProgress(24)
                .setProgressSegments(listOf(Notification.ProgressStyle.Segment(120))))
        val notification = builder.build()
        val bridge = if (mode.startsWith("samsung")) SamsungNowBarCompatibility.applyTo(notification).name else "NATIVE"
        manager.notify(notificationId, notification)
        status.text = "Modo: $mode\nCompatibilidade: $bridge\nElegível: ${notification.hasPromotableCharacteristics()}\nPromoção permitida: ${manager.canPostPromotedNotifications()}"
    }
}
