package com.styleaeternum.crm.ui;

import android.content.Intent;
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

        btnContinue.setOnClickListener(v -> checkAndProceed());

        updateUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
        // Si ya tiene permiso, pasar directo a MainActivity
        if (NotificationPermissionHelper.isGranted(this)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
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
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            updateUI();
        }
    }
}
