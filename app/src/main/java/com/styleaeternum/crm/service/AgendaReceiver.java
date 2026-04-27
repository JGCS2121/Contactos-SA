package com.styleaeternum.crm.service;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.work.WorkManager;

import com.styleaeternum.crm.data.Agenda;
import com.styleaeternum.crm.data.AppDatabase;
import com.styleaeternum.crm.data.CapturedContact;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AgendaReceiver extends BroadcastReceiver {

    public static final String ACTION_ENTREGADO = "com.styleaeternum.crm.ENTREGADO";
    public static final String EXTRA_AGENDA_ID = "agenda_id";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_ENTREGADO.equals(intent.getAction())) {
            int agendaId = intent.getIntExtra(EXTRA_AGENDA_ID, -1);
            if (agendaId == -1) return;

            // Cancelar notificaciones de este ID
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.cancel(agendaId * 10 + 1);
            nm.cancel(agendaId * 10 + 2);
            nm.cancel(agendaId * 10 + 3);
            
            // Cancelar los workers pendientes
            WorkManager.getInstance(context).cancelAllWorkByTag("agenda_" + agendaId);

            executor.execute(() -> {
                AppDatabase db = AppDatabase.getInstance(context);
                Agenda agenda = db.agendaDao().getById(agendaId);
                if (agenda != null) {
                    agenda.estado = "entregado";
                    db.agendaDao().update(agenda);
                    
                    // Actualizar CRM si tiene contacto
                    if (agenda.contactoId != null) {
                        CapturedContact contact = db.contactDao().getByPhone(agenda.telefono);
                        if (contact != null) {
                            contact.etiqueta = "Compró"; // Etiqueta por defecto de compra
                            db.contactDao().update(contact);
                        }
                    }
                }
            });
            
            Toast.makeText(context, "¡Pedido marcado como entregado!", Toast.LENGTH_SHORT).show();
        }
    }
}
