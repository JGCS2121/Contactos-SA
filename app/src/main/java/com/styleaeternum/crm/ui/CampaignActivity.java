package com.styleaeternum.crm.ui;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.tabs.TabLayout;
import com.styleaeternum.crm.R;
import com.styleaeternum.crm.data.AppDatabase;
import com.styleaeternum.crm.data.CapturedContact;
import com.styleaeternum.crm.data.ContactLabel;
import com.styleaeternum.crm.service.CampaignService;
import com.styleaeternum.crm.service.MessageSenderAccessibilityService;
import com.styleaeternum.crm.util.SpintaxEngine;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Pantalla de configuración de una campaña de mensajes masivos.
 * Permite: escribir el mensaje con spintax, programar la hora, configurar el intervalo,
 * seleccionar los contactos destinatarios y elegir con qué WhatsApp enviar.
 */
public class CampaignActivity extends AppCompatActivity {

    private static final int RC_IMPORT_FILE = 801;

    // ── Views ────────────────────────────────────────────────────────────────
    private EditText etMessage;
    private TextView tvPreview, tvCombinations, tvContactsSummary, tvIntervalValue,
                     tvDurationEstimate, tvScheduledTime;
    private Button btnPreview, btnPickTime, btnSelectContacts, btnImportNumbers, btnStart;
    private RadioGroup rgStartMode, rgWaApp;
    private RadioButton rbStartNow, rbStartScheduled;
    private SeekBar sbInterval;
    private TabLayout tabContactMode;
    private Spinner spinnerFilter;

    // ── Estado ───────────────────────────────────────────────────────────────
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private List<CapturedContact> allContacts = new ArrayList<>();
    private List<CapturedContact> selectedContacts = new ArrayList<>();
    private List<ContactLabel> allLabels = new ArrayList<>();
    private int scheduledHour = -1, scheduledMinute = -1;
    private int intervalMinutes = 2;

    // Modes: 0=Todos, 1=Etiqueta, 2=Grupo, 3=Manual, 4=Archivo
    private int contactMode = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campaign);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        bindViews();
        setupSpintaxPreview();
        setupIntervalSlider();
        setupStartMode();
        setupContactTabs();
        setupWaAppSelector();
        loadData();

        btnStart.setOnClickListener(v -> validateAndStart());
    }

    private void bindViews() {
        etMessage        = findViewById(R.id.et_message);
        tvPreview        = findViewById(R.id.tv_preview);
        tvCombinations   = findViewById(R.id.tv_combinations);
        tvContactsSummary = findViewById(R.id.tv_contacts_summary);
        tvIntervalValue  = findViewById(R.id.tv_interval_value);
        tvDurationEstimate = findViewById(R.id.tv_duration_estimate);
        tvScheduledTime  = findViewById(R.id.tv_scheduled_time);
        btnPreview       = findViewById(R.id.btn_preview);
        btnPickTime      = findViewById(R.id.btn_pick_time);
        btnSelectContacts = findViewById(R.id.btn_select_contacts);
        btnImportNumbers = findViewById(R.id.btn_import_numbers);
        btnStart         = findViewById(R.id.btn_start_campaign);
        rgStartMode      = findViewById(R.id.rg_start_mode);
        rgWaApp          = findViewById(R.id.rg_wa_app);
        rbStartNow       = findViewById(R.id.rb_start_now);
        rbStartScheduled = findViewById(R.id.rb_start_scheduled);
        sbInterval       = findViewById(R.id.sb_interval);
        tabContactMode   = findViewById(R.id.tab_contact_mode);
        spinnerFilter    = findViewById(R.id.spinner_filter);
    }

    // ── Spintax Preview ──────────────────────────────────────────────────────

    private void setupSpintaxPreview() {
        etMessage.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                updatePreview();
                updateDurationEstimate();
            }
        });

        btnPreview.setOnClickListener(v -> updatePreview());
    }

    private void updatePreview() {
        String template = etMessage.getText().toString().trim();
        if (template.isEmpty()) {
            tvPreview.setText("(Escribe el mensaje para ver la vista previa)");
            tvCombinations.setText("");
            return;
        }
        tvPreview.setText(SpintaxEngine.spin(template));
        long combos = SpintaxEngine.countCombinations(template);
        tvCombinations.setText(combos > 1
                ? "✨ " + combos + " variaciones posibles"
                : "Sin variaciones — todos recibirán el mismo mensaje");
    }

    // ── Interval Slider ──────────────────────────────────────────────────────

    private void setupIntervalSlider() {
        sbInterval.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                intervalMinutes = progress + 1;
                tvIntervalValue.setText(intervalMinutes + " min");
                updateDurationEstimate();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        intervalMinutes = 2;
        tvIntervalValue.setText("2 min");
    }

    private void updateDurationEstimate() {
        int total = selectedContacts.size();
        if (total == 0) {
            tvDurationEstimate.setText("Selecciona contactos para ver la duración estimada");
            return;
        }
        long totalMin = (long) intervalMinutes * total;
        String estimate = totalMin < 60
                ? "~" + totalMin + " minutos para " + total + " contactos"
                : "~" + (totalMin / 60) + "h " + (totalMin % 60) + "min para " + total + " contactos";
        tvDurationEstimate.setText(estimate);
    }

    // ── Hora de inicio ───────────────────────────────────────────────────────

    private void setupStartMode() {
        rgStartMode.setOnCheckedChangeListener((group, id) -> {
            boolean scheduled = (id == R.id.rb_start_scheduled);
            btnPickTime.setVisibility(scheduled ? android.view.View.VISIBLE : android.view.View.GONE);
            tvScheduledTime.setVisibility(scheduled ? android.view.View.VISIBLE : android.view.View.GONE);
        });

        btnPickTime.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            new TimePickerDialog(this, (view, h, min) -> {
                scheduledHour = h;
                scheduledMinute = min;
                tvScheduledTime.setText(String.format("⏰ Inicio programado: %02d:%02d", h, min));
            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show();
        });
    }

    // ── Tabs de selección de contactos ───────────────────────────────────────

    private void setupContactTabs() {
        String[] tabs = {"Todos", "Etiqueta", "Grupo", "Manual", "Archivo"};
        for (String t : tabs) tabContactMode.addTab(tabContactMode.newTab().setText(t));

        tabContactMode.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                contactMode = tab.getPosition();
                updateContactSelector();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        updateContactSelector();
    }

    private void updateContactSelector() {
        spinnerFilter.setVisibility(android.view.View.GONE);
        btnSelectContacts.setVisibility(android.view.View.GONE);
        btnImportNumbers.setVisibility(android.view.View.GONE);

        switch (contactMode) {
            case 0: // Todos
                selectedContacts = new ArrayList<>(allContacts);
                updateContactsSummary();
                break;
            case 1: // Por etiqueta
                spinnerFilter.setVisibility(android.view.View.VISIBLE);
                setupLabelSpinner();
                break;
            case 2: // Por grupo
                spinnerFilter.setVisibility(android.view.View.VISIBLE);
                setupGroupSpinner();
                break;
            case 3: // Manual
                selectedContacts = new ArrayList<>();
                updateContactsSummary();
                btnSelectContacts.setVisibility(android.view.View.VISIBLE);
                btnSelectContacts.setText("Pegar números");
                btnSelectContacts.setOnClickListener(v -> showManualInputDialog());
                break;
            case 4: // Archivo
                btnImportNumbers.setVisibility(android.view.View.VISIBLE);
                btnImportNumbers.setOnClickListener(v -> pickFile());
                break;
        }
    }

    private void setupLabelSpinner() {
        if (allLabels.isEmpty()) {
            Toast.makeText(this, "No hay etiquetas creadas", Toast.LENGTH_SHORT).show();
            return;
        }
        List<String> labelNames = new ArrayList<>();
        for (ContactLabel l : allLabels) labelNames.add(l.name);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labelNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(adapter);
        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int pos, long id) {
                String label = labelNames.get(pos);
                selectedContacts = new ArrayList<>();
                for (CapturedContact c : allContacts) {
                    if (label.equals(c.etiqueta)) selectedContacts.add(c);
                }
                updateContactsSummary();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupGroupSpinner() {
        List<String> groups = new ArrayList<>();
        for (CapturedContact c : allContacts) {
            if (c.groupMembership != null && !c.groupMembership.isEmpty()
                    && !groups.contains(c.groupMembership)) {
                groups.add(c.groupMembership);
            }
        }
        if (groups.isEmpty()) {
            Toast.makeText(this, "No hay grupos disponibles", Toast.LENGTH_SHORT).show();
            return;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, groups);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(adapter);
        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int pos, long id) {
                String group = groups.get(pos);
                selectedContacts = new ArrayList<>();
                for (CapturedContact c : allContacts) {
                    if (group.equals(c.groupMembership)) selectedContacts.add(c);
                }
                updateContactsSummary();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void showManualInputDialog() {
        EditText input = new EditText(this);
        input.setHint("Pega los números aquí (ej: +57300...)\\nPuedes separarlos por saltos de línea o comas");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setMinLines(5);
        input.setGravity(android.view.Gravity.TOP);
        
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(this)
            .setTitle("Pegar números manualmente")
            .setView(input)
            .setPositiveButton("Agregar a la campaña", (dialog, which) -> {
                String text = input.getText().toString();
                String[] lines = text.split("[,\\n\\r]+");
                selectedContacts = new ArrayList<>();
                for (String line : lines) {
                    String phone = line.trim().replaceAll("[^+0-9]", "");
                    if (!phone.isEmpty()) {
                        CapturedContact c = new CapturedContact();
                        c.phone = phone;
                        c.name = phone; // Nombre genérico
                        selectedContacts.add(c);
                    }
                }
                updateContactsSummary();
                Toast.makeText(this, selectedContacts.size() + " números agregados", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }

    private void pickFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, RC_IMPORT_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_IMPORT_FILE && resultCode == RESULT_OK && data != null) {
            importNumbersFromFile(data.getData());
        }
    }

    private void importNumbersFromFile(Uri uri) {
        executor.execute(() -> {
            try {
                java.io.InputStream is = getContentResolver().openInputStream(uri);
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is));
                List<CapturedContact> imported = new ArrayList<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    String phone = line.trim().replaceAll("[^+0-9]", "");
                    if (phone.startsWith("+") && phone.length() >= 8) {
                        CapturedContact c = new CapturedContact();
                        c.phone = phone;
                        c.name  = phone;
                        imported.add(c);
                    }
                }
                reader.close();
                selectedContacts = imported;
                runOnUiThread(() -> {
                    updateContactsSummary();
                    Toast.makeText(this, imported.size() + " números importados", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error al leer el archivo", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void updateContactsSummary() {
        int n = selectedContacts.size();
        tvContactsSummary.setText(n + " contacto" + (n == 1 ? "" : "s") + " seleccionado" + (n == 1 ? "" : "s"));
        updateDurationEstimate();
    }

    // ── WhatsApp a usar ──────────────────────────────────────────────────────

    private void setupWaAppSelector() { /* configuración en XML, solo leemos en el inicio */ }

    private String getSelectedWaPkg() {
        RadioButton rbPersonal = findViewById(R.id.rb_wa_personal_send);
        return rbPersonal.isChecked() ? "com.whatsapp" : "com.whatsapp.w4b";
    }

    // ── Cargar datos ─────────────────────────────────────────────────────────

    private void loadData() {
        AppDatabase db = AppDatabase.getInstance(this);
        db.contactDao().getAllContacts().observe(this, contacts -> {
            allContacts = contacts != null ? contacts : new ArrayList<>();
            if (contactMode == 0) {
                selectedContacts = new ArrayList<>(allContacts);
                updateContactsSummary();
            }
        });
        db.labelDao().getAllLabels().observe(this, labels -> {
            allLabels = labels != null ? labels : new ArrayList<>();
        });
    }

    // ── Validación e inicio ──────────────────────────────────────────────────

    private void validateAndStart() {
        String message = etMessage.getText().toString().trim();
        if (message.isEmpty()) {
            Toast.makeText(this, "Escribe el mensaje de la campaña", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedContacts.isEmpty()) {
            Toast.makeText(this, "Selecciona al menos un contacto", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verificar que el Servicio de Accesibilidad esté activo
        if (!MessageSenderAccessibilityService.isRunning()) {
            new AlertDialog.Builder(this)
                .setTitle("⚠️ Servicio de Accesibilidad no activo")
                .setMessage("Para enviar mensajes automáticamente, activa el Servicio de Accesibilidad de la app en los ajustes de Android.\n\n¿Ir a activarlo ahora?")
                .setPositiveButton("Ir a Ajustes", (d, w) -> {
                    startActivity(new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS));
                })
                .setNegativeButton("Cancelar", null)
                .show();
            return;
        }

        // Advertencia anti-ban
        int total = selectedContacts.size();
        long totalMin = (long) intervalMinutes * total;
        new AlertDialog.Builder(this)
            .setTitle("🚀 Confirmar campaña")
            .setMessage("Se enviarán " + total + " mensajes con un intervalo de " + intervalMinutes +
                        " minutos entre cada uno.\n\nDuración estimada: ~" +
                        (totalMin < 60 ? totalMin + " min" : (totalMin/60) + "h " + (totalMin%60) + "min") +
                        "\n\n⚠️ Mantén el teléfono activo durante el envío.\n\n¿Iniciar ahora?")
            .setPositiveButton("Iniciar campaña", (d, w) -> startCampaign(message))
            .setNegativeButton("Cancelar", null)
            .show();
    }

    private void startCampaign(String message) {
        ArrayList<String> phones = new ArrayList<>();
        ArrayList<String> names  = new ArrayList<>();
        for (CapturedContact c : selectedContacts) {
            phones.add(c.phone);
            names.add(c.name);
        }

        Intent intent = new Intent(this, CampaignService.class);
        intent.setAction(CampaignService.ACTION_START);
        intent.putStringArrayListExtra(CampaignService.EXTRA_PHONES, phones);
        intent.putStringArrayListExtra(CampaignService.EXTRA_NAMES, names);
        intent.putExtra(CampaignService.EXTRA_MESSAGE, message);
        intent.putExtra(CampaignService.EXTRA_INTERVAL, intervalMinutes);
        intent.putExtra(CampaignService.EXTRA_WA_PKG, getSelectedWaPkg());
        startForegroundService(intent);

        // Abrir pantalla de progreso
        startActivity(new Intent(this, CampaignProgressActivity.class));
    }

    @Override
    public boolean onSupportNavigateUp() { onBackPressed(); return true; }

    @Override
    protected void onDestroy() { executor.shutdown(); super.onDestroy(); }
}
