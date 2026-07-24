package com.styleaeternum.crm.service;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.styleaeternum.crm.data.ContactRepository;
import com.styleaeternum.crm.sync.GoogleContactsSync;
import com.styleaeternum.crm.util.PrefijosHelper;

import java.util.concurrent.TimeUnit;

/**
 * Worker periódico que sincroniza automáticamente los contactos con Google Contacts.
 * Se programa cada 12 o 24 horas según la configuración del usuario.
 */
public class SyncWorker extends Worker {

    private static final String TAG = "SyncWorker";
    public static final String WORK_NAME = "auto_sync_google";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();

        // Comprobar que el usuario tiene sesión de Google activa
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(ctx);
        if (account == null) {
            Log.w(TAG, "Sin sesión Google — sincronización automática omitida.");
            return Result.success(); // No es un error, simplemente no hay sesión
        }

        ContactRepository repository = new ContactRepository(ctx);
        int result = GoogleContactsSync.syncAll(ctx, account, repository);

        if (result >= 0) {
            Log.i(TAG, "Sincronización automática completada: " + result + " contactos.");
            return Result.success();
        } else {
            Log.e(TAG, "Error en sincronización automática: " + GoogleContactsSync.lastErrorMessage);
            return Result.retry(); // Reintenta según la política de backoff de WorkManager
        }
    }

    // ── Helpers de programación ─────────────────────────────────────────────

    /**
     * Programa o reprograma la sincronización automática según la preferencia guardada.
     * Llama a este método al iniciar la app y al cambiar la configuración.
     */
    public static void schedule(Context ctx) {
        String interval = PrefijosHelper.getSyncInterval(ctx);
        WorkManager wm = WorkManager.getInstance(ctx);

        if ("off".equals(interval)) {
            wm.cancelUniqueWork(WORK_NAME);
            Log.i(TAG, "Sincronización automática desactivada.");
            return;
        }

        long hours = "12h".equals(interval) ? 12 : 24;

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                SyncWorker.class, hours, TimeUnit.HOURS)
                .build();

        wm.enqueueUniquePeriodicWork(
                WORK_NAME,
                androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
                request);

        Log.i(TAG, "Sincronización automática programada cada " + hours + " horas.");
    }
}
