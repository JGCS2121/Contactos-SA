package com.styleaeternum.crm.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import com.styleaeternum.crm.R;
import com.styleaeternum.crm.util.NotificationPermissionHelper;

/**
 * Pantalla de splash: redirige según si tiene permiso de notificaciones.
 */
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (NotificationPermissionHelper.isGranted(this)) {
                startActivity(new Intent(this, MainActivity.class));
            } else {
                startActivity(new Intent(this, PermissionSetupActivity.class));
            }
            finish();
        }, 1500);
    }
}
