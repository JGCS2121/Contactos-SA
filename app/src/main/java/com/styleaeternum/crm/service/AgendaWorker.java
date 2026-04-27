package com.styleaeternum.crm.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.styleaeternum.crm.R;
import com.styleaeternum.crm.ui.AgendaActivity;

public class AgendaWorker extends Worker {

    public static final String KEY_TIPO = "tipo_recordatorio"; // 1=dia_antes, 2=dos_horas, 3=exacto
    public static final String KEY_ID = "agenda_id";
    public static final String KEY_NOMBRE = "nombre";
    public static final String KEY_DESC = "desc";
    public static final String KEY_PHONE = "phone";
    public static final String KEY_HORA = "hora_formateada";
    
    private static final String CHANNEL_ID = "style_aeternum_agenda";

    public AgendaWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        int tipo = getInputData().getInt(KEY_TIPO, 3);
        int agendaId = getInputData().getInt(KEY_ID, -1);
        String nombre = getInputData().getString(KEY_NOMBRE);
        String desc = getInputData().getString(KEY_DESC);
        String phone = getInputData().getString(KEY_PHONE);
        String horaFormateada = getInputData().getString(KEY_HORA);

        if (agendaId == -1 || nombre == null) return Result.failure();

        crearCanal();

        String titulo = "";
        String cuerpo = desc;
        
        Intent intentApp = new Intent(getApplicationContext(), AgendaActivity.class);
        PendingIntent piApp = PendingIntent.getActivity(getApplicationContext(), agendaId * 10, intentApp, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(piApp);

        if (tipo == 1) {
            titulo = "📦 Pedido mañana — " + nombre;
            cuerpo = desc + " · Entrega: " + horaFormateada;
            builder.setContentTitle(titulo).setContentText(cuerpo);
        } else if (tipo == 2) {
            titulo = "⏰ Pedido en 2 horas — " + nombre;
            builder.setContentTitle(titulo).setContentText(cuerpo);
            
            // Acción WhatsApp
            if (phone != null && !phone.isEmpty()) {
                Intent wApp = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/" + phone));
                wApp.setPackage("com.whatsapp.w4b");
                PendingIntent piWapp = PendingIntent.getActivity(getApplicationContext(), agendaId * 10 + 1, wApp, PendingIntent.FLAG_IMMUTABLE);
                builder.addAction(0, "📲 WhatsApp", piWapp);
            }
        } else if (tipo == 3) {
            titulo = "🔔 HOY entrega " + nombre;
            builder.setContentTitle(titulo).setContentText(cuerpo);
            
            // Acción Marcar Entregado
            Intent actionEntregado = new Intent(getApplicationContext(), AgendaReceiver.class);
            actionEntregado.setAction(AgendaReceiver.ACTION_ENTREGADO);
            actionEntregado.putExtra(AgendaReceiver.EXTRA_AGENDA_ID, agendaId);
            PendingIntent piEntregado = PendingIntent.getBroadcast(getApplicationContext(), agendaId * 10 + 2, actionEntregado, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(0, "✅ Marcar entregado", piEntregado);
            
            if (phone != null && !phone.isEmpty()) {
                Intent wApp = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/" + phone));
                wApp.setPackage("com.whatsapp.w4b");
                PendingIntent piWapp = PendingIntent.getActivity(getApplicationContext(), agendaId * 10 + 3, wApp, PendingIntent.FLAG_IMMUTABLE);
                builder.addAction(0, "📲 WhatsApp", piWapp);
            }
        }

        NotificationManager nm = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(agendaId * 10 + tipo, builder.build());

        return Result.success();
    }

    private void crearCanal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                    CHANNEL_ID,
                    "Agenda Style Aeternum",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager nm = getApplicationContext().getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(canal);
            }
        }
    }
}
