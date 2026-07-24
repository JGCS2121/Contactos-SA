package com.styleaeternum.crm.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.styleaeternum.crm.R;
import com.styleaeternum.crm.data.CapturedContact;
import com.styleaeternum.crm.ui.CampaignProgressActivity;
import com.styleaeternum.crm.util.SpintaxEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Foreground Service que gestiona la cola de envío masivo de mensajes.
 * Se comunica con MessageSenderAccessibilityService para el envío real.
 *
 * Intents de control:
 *   ACTION_START — iniciar campaña con lista de contactos, mensaje y configuración
 *   ACTION_STOP  — detener campaña en cualquier momento
 *
 * Broadcasts emitidos (para CampaignProgressActivity):
 *   ACTION_PROGRESS_UPDATE — progreso actualizado
 *   ACTION_CAMPAIGN_DONE   — campaña finalizada
 */
public class CampaignService extends Service {

    private static final String TAG = "CampaignService";
    private static final String CHANNEL_ID = "campaign_channel";
    private static final int NOTIF_ID = 9001;

    // Intents de control
    public static final String ACTION_START  = "com.styleaeternum.crm.CAMPAIGN_START";
    public static final String ACTION_STOP   = "com.styleaeternum.crm.CAMPAIGN_STOP";

    // Extras del intent de inicio
    public static final String EXTRA_PHONES    = "phones";       // ArrayList<String>
    public static final String EXTRA_NAMES     = "names";        // ArrayList<String>
    public static final String EXTRA_MESSAGE   = "message";      // String (con spintax)
    public static final String EXTRA_INTERVAL  = "interval_min"; // int (minutos)
    public static final String EXTRA_WA_PKG    = "wa_pkg";       // String (paquete de WA a usar)

    // Broadcasts de progreso
    public static final String ACTION_PROGRESS_UPDATE = "com.styleaeternum.crm.PROGRESS_UPDATE";
    public static final String ACTION_CAMPAIGN_DONE   = "com.styleaeternum.crm.CAMPAIGN_DONE";
    public static final String EXTRA_SENT    = "sent";
    public static final String EXTRA_TOTAL   = "total";
    public static final String EXTRA_CURRENT = "current_name";
    public static final String EXTRA_STATUS  = "status"; // "ok" | "fail" | "done"

    // Estado de la instancia
    private static CampaignService instance;
    private volatile boolean running = false;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Random random = new Random();

    // Datos de la campaña activa
    private List<String> phones;
    private List<String> names;
    private String messageTemplate;
    private int intervalMinutes;
    private String waPkg;
    private int sentCount = 0;

    // Receptor de resultado del AccessibilityService
    private final BroadcastReceiver resultReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (MessageSenderAccessibilityService.ACTION_MESSAGE_SENT.equals(action)) {
                synchronized (CampaignService.this) {
                    CampaignService.this.notifyAll(); // Despertar el hilo de envío
                }
            } else if (MessageSenderAccessibilityService.ACTION_MESSAGE_FAILED.equals(action)) {
                synchronized (CampaignService.this) {
                    CampaignService.this.notifyAll();
                }
            }
        }
    };

    public static boolean isRunning() {
        return instance != null && instance.running;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        crearCanal();

        // Registrar receptor de resultados
        IntentFilter filter = new IntentFilter();
        filter.addAction(MessageSenderAccessibilityService.ACTION_MESSAGE_SENT);
        filter.addAction(MessageSenderAccessibilityService.ACTION_MESSAGE_FAILED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(resultReceiver, filter, RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(resultReceiver, filter);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        String action = intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopCampaign();
            return START_NOT_STICKY;
        }

        if (ACTION_START.equals(action)) {
            phones = intent.getStringArrayListExtra(EXTRA_PHONES);
            names  = intent.getStringArrayListExtra(EXTRA_NAMES);
            messageTemplate = intent.getStringExtra(EXTRA_MESSAGE);
            intervalMinutes = intent.getIntExtra(EXTRA_INTERVAL, 2);
            waPkg = intent.getStringExtra(EXTRA_WA_PKG);

            if (phones == null || phones.isEmpty() || messageTemplate == null) {
                stopSelf();
                return START_NOT_STICKY;
            }

            sentCount = 0;
            running = true;

            startForeground(NOTIF_ID, buildNotification("Iniciando campaña...", 0, phones.size()));
            executor.execute(this::runCampaign);
        }

        return START_NOT_STICKY;
    }

    private void runCampaign() {
        int total = phones.size();
        Log.i(TAG, "Campaña iniciada: " + total + " contactos, intervalo " + intervalMinutes + " min");

        for (int i = 0; i < total && running; i++) {
            String phone   = phones.get(i);
            String name    = names.get(i);
            String message = SpintaxEngine.spin(messageTemplate);

            Log.d(TAG, "Enviando a " + name + " (" + phone + "): " + message);

            // Actualizar notificación y pantalla de progreso
            updateProgress(i + 1, total, name, "sending");

            // Abrir WhatsApp con el mensaje pre-cargado
            openWhatsApp(phone, message);

            // Pedir al AccessibilityService que pulse Enviar
            MessageSenderAccessibilityService.requestSend(message);

            // Esperar confirmación del AccessibilityService (máx 15 seg)
            boolean ok = waitForSendConfirmation(15_000);

            sentCount++;
            updateProgress(sentCount, total, name, ok ? "ok" : "fail");

            // Esperar el intervalo configurado entre mensajes (+ retraso aleatorio ±15 seg)
            if (running && i < total - 1) {
                long baseMs  = (long) intervalMinutes * 60 * 1000;
                long jitter  = (random.nextInt(31) - 15) * 1000L; // ±15 segundos
                long waitMs  = Math.max(baseMs + jitter, 30_000);  // mínimo 30 seg siempre
                Log.d(TAG, "Esperando " + (waitMs / 1000) + " segundos antes del siguiente envío...");
                safeSleep(waitMs);
            }
        }

        Log.i(TAG, "Campaña finalizada. Enviados: " + sentCount + "/" + total);
        broadcastDone(sentCount, total);
        stopSelf();
    }

    /** Abre WhatsApp con el número y el mensaje pre-cargado en el campo de texto */
    private void openWhatsApp(String phone, String message) {
        try {
            String cleanPhone = phone.replaceAll("[^0-9+]", "");
            // Eliminar el '+' inicial para wa.me
            String waPhone = cleanPhone.startsWith("+") ? cleanPhone.substring(1) : cleanPhone;
            Uri uri = Uri.parse("https://wa.me/" + waPhone + "?text=" + Uri.encode(message));

            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.setPackage(waPkg != null ? waPkg : "com.whatsapp.w4b");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error al abrir WhatsApp: " + e.getMessage());
        }
    }

    /** Espera que el AccessibilityService confirme el envío (con timeout) */
    private synchronized boolean waitForSendConfirmation(long timeoutMs) {
        try {
            wait(timeoutMs);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void safeSleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void stopCampaign() {
        running = false;
        synchronized (this) { notifyAll(); }
        Log.i(TAG, "Campaña detenida por el usuario. Enviados: " + sentCount);
        stopSelf();
    }

    // ── Notificación y broadcasts ────────────────────────────────────────────

    private void updateProgress(int sent, int total, String currentName, String status) {
        // Actualizar notificación persistente
        updateNotification("Enviando a " + currentName + " · " + sent + "/" + total, sent, total);

        // Broadcast para CampaignProgressActivity
        Intent intent = new Intent(ACTION_PROGRESS_UPDATE);
        intent.putExtra(EXTRA_SENT, sent);
        intent.putExtra(EXTRA_TOTAL, total);
        intent.putExtra(EXTRA_CURRENT, currentName);
        intent.putExtra(EXTRA_STATUS, status);
        sendBroadcast(intent);
    }

    private void broadcastDone(int sent, int total) {
        updateNotification("✅ Campaña completada · " + sent + "/" + total + " enviados", sent, total);
        Intent intent = new Intent(ACTION_CAMPAIGN_DONE);
        intent.putExtra(EXTRA_SENT, sent);
        intent.putExtra(EXTRA_TOTAL, total);
        intent.putExtra(EXTRA_STATUS, "done");
        sendBroadcast(intent);
    }

    private void updateNotification(String text, int sent, int total) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(NOTIF_ID, buildNotification(text, sent, total));
    }

    private Notification buildNotification(String text, int sent, int total) {
        Intent stopIntent = new Intent(this, CampaignService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent piStop = PendingIntent.getService(this, 0, stopIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Intent openIntent = new Intent(this, CampaignProgressActivity.class);
        openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent piOpen = PendingIntent.getActivity(this, 1, openIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_send)
                .setContentTitle("📤 Campaña en progreso")
                .setContentText(text)
                .setOngoing(true)
                .setContentIntent(piOpen)
                .addAction(android.R.drawable.ic_delete, "Detener", piStop);

        if (total > 0) {
            builder.setProgress(total, sent, false);
        }

        return builder.build();
    }

    private void crearCanal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Campaña Masiva", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Progreso del envío masivo de mensajes");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        running = false;
        try { unregisterReceiver(resultReceiver); } catch (Exception ignored) {}
        instance = null;
        executor.shutdownNow();
        super.onDestroy();
    }
}
