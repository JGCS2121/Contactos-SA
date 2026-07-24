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

        // Aplicar filtro de fuente configurado por el usuario
        String filter = PrefijosHelper.getWaSourceFilter(this);
        if ("personal".equals(filter) && WA_BUSSINES_PKG.equals(pkg)) return; // ignorar Business
        if ("business".equals(filter) && WA_PKG.equals(pkg)) return;           // ignorar Personal

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
            // Comprobar si ya existe en la BD por teléfono
            CapturedContact existing = repository.getByPhone(finalPhone);
            if (existing != null) {
                Log.d(TAG, "Número ya registrado: " + finalPhone);
                return;
            }

            // Lógica de Grupos y IDs
            String monthGroup = ContactIdHelper.getCurrentGroup();
            String prefixVal  = PrefijosHelper.getPrefijo(getApplicationContext()).trim();
            
            // El grupo de membresía será el prefijo (si existe) o el mes actual
            String targetGroup = prefixVal.isEmpty() ? monthGroup : prefixVal;

            // Contar cuántos hay en el grupo destino para la secuencia del ID
            int count = repository.countByGroup(targetGroup);
            String id = ContactIdHelper.buildId(targetGroup, count + 1);

            // Garantizar que el ID sea único (por si el contador de Room falló o hay desfase)
            int safetyCounter = 1;
            while (repository.getByIdSync(id) != null) {
                id = ContactIdHelper.buildId(targetGroup, count + 1 + safetyCounter);
                safetyCounter++;
            }

            // Nombre: prefijo del negocio + contador global (ej: "Tienda 010")
            int numero   = PrefijosHelper.getSiguienteNumero(getApplicationContext());
            String nombre = PrefijosHelper.generarNombre(getApplicationContext(), id, numero);

            CapturedContact contact = new CapturedContact();
            contact.id              = id;
            contact.phone           = finalPhone;
            contact.name            = nombre;
            contact.groupMembership = targetGroup;
            contact.phoneType       = "WhatsApp Business";
            contact.notes           = "Capturado desde " + (WA_BUSSINES_PKG.equals(pkg) ? "WA Business" : "WhatsApp");
            contact.capturedAt      = System.currentTimeMillis();

            // Inserción síncrona dentro del executor del servicio para mantener el orden
            repository.insertSync(contact);
            Log.i(TAG, "Nuevo contacto guardado con éxito: " + id + " | " + nombre + " | " + finalPhone);
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
