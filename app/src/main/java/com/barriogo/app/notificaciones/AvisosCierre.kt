package com.barriogo.app.notificaciones

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.barriogo.app.BarrioGoApp
import com.barriogo.app.MainActivity
import com.barriogo.app.R
import com.barriogo.app.data.caja.CajaRepository
import com.barriogo.app.domain.AvisoCierre
import com.barriogo.app.domain.ReglasCaja
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Avisos de cierre de caja, en el celular y sin internet:
 * 1. 30 minutos antes de la hora de cierre: "Administrador: la caja se cierra a las 8:00 p. m.".
 * 2. Pasada la hora, cada 30 minutos mientras la caja siga abierta.
 * Se programan con AlarmManager (alarma inexacta: Android puede correrla unos minutos).
 * Al abrir o cerrar la caja, cambiar la hora o reiniciar el celular, se vuelven a programar.
 */
object AvisosCierre {
    const val CANAL = "cierre_caja"
    private const val ID_NOTIFICACION = 4210
    private const val CODIGO_ALARMA = 4211
    /** Extra del Intent que abre la app: lleva directo a Caja. */
    const val EXTRA_ABRIR_CAJA = "abrir_caja"

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun crearCanal(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val canal = NotificationChannel(CANAL, "Cierre de caja", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Avisa cuando se acerca o ya pasó la hora de cerrar la caja"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    /** Vuelve a calcular cuándo avisar según la caja abierta y la hora de cierre. */
    fun reprogramar(context: Context) {
        val app = context.applicationContext as BarrioGoApp
        alcance.launch {
            val c = app.container
            val abierta = c.caja.cajaAbiertaUnaVez()  // Firestore (o su caché sin internet)
            val hora = ReglasCaja.hora(c.store.horaCierre.value)
            val cuando = ReglasCaja.proximoAviso(LocalDateTime.now(), hora, abierta?.let(CajaRepository::fechaDe))
            val alarmas = context.getSystemService(AlarmManager::class.java)
            val pendiente = intentAlarma(context)
            alarmas.cancel(pendiente)
            if (cuando == null) {
                NotificationManagerCompat.from(context).cancel(ID_NOTIFICACION)
                return@launch
            }
            val ms = cuando.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            alarmas.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, pendiente)
        }
    }

    /** Lo llama la alarma: muestra el aviso si la caja sigue abierta y programa el siguiente. */
    internal suspend fun avisarSiHaceFalta(context: Context) {
        val c = (context.applicationContext as BarrioGoApp).container
        val abierta = c.caja.cajaAbiertaUnaVez()  // Firestore (o su caché sin internet)
        val hora = ReglasCaja.hora(c.store.horaCierre.value)
        val aviso = ReglasCaja.aviso(LocalDateTime.now(), hora, abierta?.let(CajaRepository::fechaDe))
        val texto = ReglasCaja.textoAviso(aviso, hora)
        if (texto != null) mostrar(context, aviso, texto)
        reprogramar(context)
    }

    private fun mostrar(context: Context, aviso: AvisoCierre, texto: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val abrir = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).putExtra(EXTRA_ABRIR_CAJA, true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val titulo = if (aviso is AvisoCierre.Vencido) "Administrador: cierra la caja" else "Administrador: pronto se cierra la caja"
        val n = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(abrir)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(ID_NOTIFICACION, n)
    }

    private fun intentAlarma(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, CODIGO_ALARMA,
        Intent(context, AvisoCierreReceiver::class.java).setAction(AvisoCierreReceiver.ACCION_AVISO),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

/** Recibe la alarma del aviso y el reinicio del celular (para volver a programar). */
class AvisoCierreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendiente = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (intent.action == ACCION_AVISO) AvisosCierre.avisarSiHaceFalta(context) else AvisosCierre.reprogramar(context)
            } finally {
                pendiente.finish()
            }
        }
    }

    companion object {
        const val ACCION_AVISO = "com.barriogo.app.AVISO_CIERRE_CAJA"
    }
}
