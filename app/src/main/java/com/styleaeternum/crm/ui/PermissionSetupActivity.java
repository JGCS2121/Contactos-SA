package com.styleaeternum.crm.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.styleaeternum.crm.R;
import com.styleaeternum.crm.util.NotificationPermissionHelper;
import com.styleaeternum.crm.util.PrefijosHelper;
import com.styleaeternum.crm.service.SyncWorker;

/**
 * Pantalla 1 — Bienvenida y solicitud de permiso de notificaciones.
 * La app no funciona hasta que el usuario active el permiso.
 */
public class PermissionSetupActivity extends AppCompatActivity {

    private TextView tvStatus;
    private Button   btnActivate;
    private Button   btnContinue;
    private RadioGroup rgWaSource;
    private RadioButton rbWaBoth, rbWaPersonal, rbWaBusiness;
    private RadioGroup rgSyncInterval;
    private RadioButton rbSyncOff, rbSync12h, rbSync24h;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission_setup);

        tvStatus    = findViewById(R.id.tv_permission_status);
        btnActivate = findViewById(R.id.btn_activate_permission);
        btnContinue = findViewById(R.id.btn_continue);
        rgWaSource  = findViewById(R.id.rg_wa_source);
        rbWaBoth     = findViewById(R.id.rb_wa_both);
        rbWaPersonal = findViewById(R.id.rb_wa_personal);
        rbWaBusiness = findViewById(R.id.rb_wa_business);

        // Cargar preferencia guardada en el RadioGroup
        String savedFilter = PrefijosHelper.getWaSourceFilter(this);
        if ("personal".equals(savedFilter))       rbWaPersonal.setChecked(true);
        else if ("business".equals(savedFilter))  rbWaBusiness.setChecked(true);
        else                                       rbWaBoth.setChecked(true);

        // Guardar preferencia cuando el usuario cambia la selección
        rgWaSource.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_wa_personal)       PrefijosHelper.setWaSourceFilter(this, "personal");
            else if (checkedId == R.id.rb_wa_business)  PrefijosHelper.setWaSourceFilter(this, "business");
            else                                         PrefijosHelper.setWaSourceFilter(this, "both");
        });

        // ── Sincronización automática ────────────────────────────────────────
        rgSyncInterval = findViewById(R.id.rg_sync_interval);
        rbSyncOff  = findViewById(R.id.rb_sync_off);
        rbSync12h  = findViewById(R.id.rb_sync_12h);
        rbSync24h  = findViewById(R.id.rb_sync_24h);

        // Cargar preferencia guardada
        String savedInterval = PrefijosHelper.getSyncInterval(this);
        if ("12h".equals(savedInterval))    rbSync12h.setChecked(true);
        else if ("24h".equals(savedInterval)) rbSync24h.setChecked(true);
        else                                   rbSyncOff.setChecked(true);

        // Guardar y reprogramar el worker al cambiar la selección
        rgSyncInterval.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_sync_12h)       PrefijosHelper.setSyncInterval(this, "12h");
            else if (checkedId == R.id.rb_sync_24h)  PrefijosHelper.setSyncInterval(this, "24h");
            else                                      PrefijosHelper.setSyncInterval(this, "off");
            SyncWorker.schedule(this); // Aplicar cambio inmediatamente
        });

        // Botón que lleva directamente a Ajustes → Acceso a Notificaciones
        btnActivate.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            startActivity(intent);
        });

        // Botón que lleva a Info de la App (para desbloquear ajustes restringidos)
        Button btnAppInfo = findViewById(R.id.btn_app_info);
        btnAppInfo.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        });

        btnContinue.setOnClickListener(v -> checkAndProceed());

        updateUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
        // Si ya tiene permiso Y NO vino desde el menú (es decir, es el inicio real), pasar directo.
        // Como no podemos saber fácil si vino del menú, si está iniciada por el launcher pasa.
        // Mejor: no forzamos finish() en onResume si la abrimos intencionalmente, pero
        // para no romper el flujo, dejemos que el usuario presione el botón de Continuar o Salir.
    }

    private void updateUI() {
        boolean granted = NotificationPermissionHelper.isGranted(this);
        if (granted) {
            tvStatus.setText(R.string.permission_granted);
            tvStatus.setTextColor(getColor(R.color.wa_green));
            btnContinue.setEnabled(true);
        } else {
            tvStatus.setText(R.string.permission_required_msg);
            tvStatus.setTextColor(getColor(R.color.error_color));
            btnContinue.setEnabled(false);
        }
    }

    private void checkAndProceed() {
        if (NotificationPermissionHelper.isGranted(this)) {
            // Si ya está en MainActivity (vino por el menú), solo cerramos esta.
            // Si vino del Splash, abrimos MainActivity.
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        } else {
            updateUI();
        }
    }
}
