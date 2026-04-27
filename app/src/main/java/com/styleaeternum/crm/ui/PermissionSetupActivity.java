package com.styleaeternum.crm.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.styleaeternum.crm.R;
import com.styleaeternum.crm.util.NotificationPermissionHelper;

/**
 * Pantalla 1 — Bienvenida y solicitud de permiso de notificaciones.
 * La app no funciona hasta que el usuario active el permiso.
 */
public class PermissionSetupActivity extends AppCompatActivity {

    private TextView tvStatus;
    private Button   btnActivate;
    private Button   btnContinue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission_setup);

        tvStatus    = findViewById(R.id.tv_permission_status);
        btnActivate = findViewById(R.id.btn_activate_permission);
        btnContinue = findViewById(R.id.btn_continue);

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
