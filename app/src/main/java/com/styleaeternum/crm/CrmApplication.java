package com.styleaeternum.crm;

import android.app.Application;
import androidx.work.Configuration;
import androidx.work.WorkManager;
import com.styleaeternum.crm.service.SyncWorker;

/**
 * Clase Application principal. Inicializa WorkManager manualmente
 * para garantizar que las notificaciones de agenda funcionen correctamente
 * incluso con el teléfono en modo ahorro de batería.
 */
public class CrmApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Configuration config = new Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.INFO)
                .build();

        WorkManager.initialize(this, config);

        // Reprogramar sincronización automática si estaba activa
        SyncWorker.schedule(this);
    }
}
