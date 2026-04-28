package com.styleaeternum.crm.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.styleaeternum.crm.data.AppDatabase;
import com.styleaeternum.crm.data.CapturedContact;
import com.styleaeternum.crm.databinding.ActivityImportMassBinding;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImportMassActivity extends AppCompatActivity {

    private ActivityImportMassBinding binding;
    private ExecutorService executor;
    private List<String> validPhones = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityImportMassBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setTitle("Importación Masiva");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        executor = Executors.newSingleThreadExecutor();

        // Configurar el Spinner de Estado
        String[] statuses = new String[]{"Nuevo", "Compró", "Pidió no compró", "No Vender", "Interesada"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statuses);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spInitialStatus.setAdapter(spinnerAdapter);

        binding.btnPreview.setOnClickListener(v -> processPhones());
        binding.btnSaveAll.setOnClickListener(v -> saveAllContacts());
    }

    private void processPhones() {
        String rawText = binding.etMassPhones.getText() != null ? binding.etMassPhones.getText().toString() : "";
        if (TextUtils.isEmpty(rawText.trim())) {
            Toast.makeText(this, "La lista de teléfonos está vacía", Toast.LENGTH_SHORT).show();
            return;
        }

        // Dividir por saltos de línea o comas
        String[] tokens = rawText.split("[\\n,]");
        
        validPhones.clear();
        Set<String> uniquePhones = new HashSet<>();
        int invalidCount = 0;
        int duplicateCount = 0;

        for (String token : tokens) {
            String cleanPhone = token.replaceAll("[^0-9]", ""); // Quita espacios, -, (), etc.
            
            // Si tiene prefijo de Colombia +57 (que se limpió a 57), lo quitamos
            if (cleanPhone.startsWith("57") && cleanPhone.length() >= 12) {
                cleanPhone = cleanPhone.substring(2);
            }

            if (cleanPhone.length() >= 10) {
                if (uniquePhones.contains(cleanPhone)) {
                    duplicateCount++;
                } else {
                    uniquePhones.add(cleanPhone);
                    validPhones.add(cleanPhone);
                }
            } else {
                if (!cleanPhone.isEmpty()) {
                    invalidCount++;
                }
            }
        }

        binding.tvPreviewStats.setVisibility(View.VISIBLE);
        binding.tvPreviewStats.setText(String.format("Válidos a guardar: %d\nDuplicados omitidos: %d\nInválidos (<10 dígitos): %d", 
                validPhones.size(), duplicateCount, invalidCount));

        if (!validPhones.isEmpty()) {
            binding.btnSaveAll.setEnabled(true);
        } else {
            binding.btnSaveAll.setEnabled(false);
            Toast.makeText(this, "No se encontraron números válidos", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveAllContacts() {
        if (validPhones.isEmpty()) return;

        String baseName = binding.etBaseName.getText() != null ? binding.etBaseName.getText().toString().trim() : "Cliente";
        String group = binding.etGroup.getText() != null ? binding.etGroup.getText().toString().trim() : "";
        String notes = binding.etNotes.getText() != null ? binding.etNotes.getText().toString().trim() : "";
        String selectedStatus = binding.spInitialStatus.getSelectedItem().toString();

        if (TextUtils.isEmpty(baseName)) {
            baseName = "Cliente";
        }

        binding.btnSaveAll.setEnabled(false);
        binding.btnSaveAll.setText("Guardando...");

        final String finalBaseName = baseName;
        
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            List<CapturedContact> newContacts = new ArrayList<>();
            
            int counter = 1;
            for (String phone : validPhones) {
                // Verificar si ya existe en DB para no duplicarlo a nivel de BD
                CapturedContact existing = db.contactDao().getByPhone(phone);
                if (existing != null) {
                    continue; // Ya existe en la base de datos
                }

                CapturedContact contact = new CapturedContact();
                contact.id = java.util.UUID.randomUUID().toString(); // ID único para masivos
                contact.name = finalBaseName + " " + String.format("%03d", counter);
                contact.phone = phone;
                contact.groupMembership = group;
                contact.notes = notes;
                contact.etiqueta = selectedStatus;
                contact.origen = "Carga masiva";
                contact.capturedAt = System.currentTimeMillis() + counter; // Para mantener orden
                
                newContacts.add(contact);
                counter++;
            }

            for (CapturedContact contact : newContacts) {
                db.contactDao().insert(contact);
            }

            runOnUiThread(() -> {
                Toast.makeText(this, "Se guardaron " + newContacts.size() + " contactos correctamente", Toast.LENGTH_LONG).show();
                finish();
            });
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }
}
