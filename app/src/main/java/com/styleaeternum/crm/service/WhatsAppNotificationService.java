package com.styleaeternum.crm.service;

import android.app.Notification;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.styleaeternum.crm.data.CapturedContact;
import com.styleaeternum.crm.data.ContactRepository;
import com.styleaeternum.crm.util.ContactIdHelper;
import com.styleaeternum.crm.util.PhoneExtractor;
import com.styleaeternum.crm.util.PrefijosHelper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Servicio que escucha notificaciones de WhatsApp Business en tiempo real.
 * Cuando detecta un número no agendado, lo guarda automáticamente en la BD.
 */
public class WhatsAppNotificationService extends NotificationListenerService {

    private static final String TAG = "SAeternumCRM";
    // Paquetes de WhatsApp a escuchar
    private static final String WA_PKG        = "com.whatsapp";
    private static final String WA_BUSSINES_PKG = "com.whatsapp.w4b";

    private ContactRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate() {
        super.onCreate();
        repository = new ContactRepository(getApplicationContext());
        Log.i(TAG, "NotificationListenerService iniciado");
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String pkg = sbn.getPackageName();
        if (!WA_PKG.equals(pkg) && !WA_BUSSINES_PKG.equals(pkg)) return;

        Notification notification = sbn.getNotification();
        if (notification == null) return;

        Bundle extras = notification.extras;
        if (extras == null) return;

        // Extraer texto de la notificación
        String title   = extras.getString(Notification.EXTRA_TITLE, "");
        String text    = extras.getCharSequence(Notification.EXTRA_TEXT, "").toString();
        String subText = extras.getString(Notification.EXTRA_SUB_TEXT, "");

        // Intentar extraer número de teléfono del título
        String phone = PhoneExtractor.extract(title);
        if (phone == null || phone.isEmpty()) {
            phone = PhoneExtractor.extract(text);
        }
        if (phone == null || phone.isEmpty()) return;

        final String finalPhone = phone;
        executor.execute(() -> {
            // Comprobar si ya existe en la BD
            CapturedContact existing = repository.getByPhone(finalPhone);
            if (existing != null) {
                Log.d(TAG, "Número ya registrado: " + finalPhone);
                return;
            }

            // Generar ID automático: abril2026_001
            String group = ContactIdHelper.getCurrentGroup();
            int count    = repository.countByGroup(group);
            String id    = ContactIdHelper.buildId(group, count + 1);

            // Nombre: prefijo del negocio + contador global (ej: "Tienda 010")
            // Si no hay prefijo configurado, usa el ID clásico (ej: "abril2026_010")
            int numero   = PrefijosHelper.getSiguienteNumero(getApplicationContext());
            String nombre = PrefijosHelper.generarNombre(getApplicationContext(), id, numero);

            CapturedContact contact = new CapturedContact();
            contact.id              = id;
            contact.phone           = finalPhone;
            contact.name            = nombre;
            contact.groupMembership = group;
            contact.phoneType       = "WhatsApp Business";
            contact.notes           = "Capturado desde " + (WA_BUSSINES_PKG.equals(pkg) ? "WA Business" : "WhatsApp");
            contact.capturedAt      = System.currentTimeMillis();

            repository.insert(contact);
            Log.i(TAG, "Nuevo contacto guardado: " + id + " | " + nombre + " | " + finalPhone);
        });
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) { /* no-op */ }

    @Override
    public void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }
}
