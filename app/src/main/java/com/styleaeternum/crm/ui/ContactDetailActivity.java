package com.styleaeternum.crm.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import android.graphics.Color;
import androidx.appcompat.app.AlertDialog;
import android.content.res.ColorStateList;

import com.styleaeternum.crm.R;
import com.styleaeternum.crm.data.CapturedContact;
import com.styleaeternum.crm.data.ContactLabel;
import com.styleaeternum.crm.viewmodel.ContactsViewModel;
import java.util.List;

/**
 * Pantalla 3 — Detalle de un contacto capturado.
 * Permite editar nombre/notas, abrir WhatsApp y eliminar.
 */
public class ContactDetailActivity extends AppCompatActivity {

    public static final String EXTRA_CONTACT_ID = "contact_id";

    private ContactsViewModel viewModel;
    private CapturedContact   currentContact;

    private TextView tvId, tvPhone, tvGroup, tvType;
    private EditText etName, etNotes;
    private Button   btnSave, btnWhatsApp, btnDelete, btnAgendar;
    private android.widget.LinearLayout containerPedidos;
    private ChipGroup chipGroupStatus;
    private List<ContactLabel> availableLabels;
    private final java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_detail);

        // Toolbar con botón atrás
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Detalle del Contacto");
        }

        tvId      = findViewById(R.id.tv_contact_id);
        tvPhone   = findViewById(R.id.tv_phone);
        tvGroup   = findViewById(R.id.tv_group);
        tvType    = findViewById(R.id.tv_type);
        etName    = findViewById(R.id.et_name);
        etNotes   = findViewById(R.id.et_notes);
        btnSave   = findViewById(R.id.btn_save);
        btnWhatsApp = findViewById(R.id.btn_open_whatsapp);
        btnDelete = findViewById(R.id.btn_delete);
        btnAgendar = findViewById(R.id.btn_agendar);
        containerPedidos = findViewById(R.id.container_pedidos);
        chipGroupStatus = findViewById(R.id.chip_group_status);

        viewModel = new ViewModelProvider(this).get(ContactsViewModel.class);

        btnAgendar.setOnClickListener(v -> {
            if (currentContact != null) {
                Intent intent = new Intent(this, NuevoPedidoActivity.class);
                intent.putExtra(NuevoPedidoActivity.EXTRA_CONTACTO_ID, currentContact.id);
                startActivity(intent);
            }
        });

        viewModel.getAllLabels().observe(this, labels -> {
            this.availableLabels = labels;
            populateChips();
        });

        String contactId = getIntent().getStringExtra(EXTRA_CONTACT_ID);
        if (contactId == null) { finish(); return; }

        viewModel.getContactById(contactId).observe(this, contact -> {
            if (contact == null) { finish(); return; }
            currentContact = contact;
            bindData(contact);
        });

        btnSave.setOnClickListener(v -> saveContact());
        btnWhatsApp.setOnClickListener(v -> openWhatsApp());
        btnDelete.setOnClickListener(v -> deleteContact());
    }

    private void bindData(CapturedContact c) {
        tvId.setText(c.id);
        tvPhone.setText(c.phone);
        tvGroup.setText(c.groupMembership);
        tvType.setText(c.phoneType);
        etName.setText(c.name);
        etNotes.setText(c.notes);
        populateChips();
        cargarPedidosRecientes(c.id);
    }

    private void cargarPedidosRecientes(String contactoId) {
        if (containerPedidos == null) return;
        
        com.styleaeternum.crm.data.AppDatabase.getInstance(this).agendaDao().getByContactoId(contactoId).observe(this, agendas -> {
            containerPedidos.removeAllViews();
            if (agendas == null || agendas.isEmpty()) {
                TextView tvEmpty = new TextView(this);
                tvEmpty.setText("No hay pedidos para este contacto.");
                tvEmpty.setTextSize(14sp);
                containerPedidos.addView(tvEmpty);
                return;
            }

            int count = 0;
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            for (com.styleaeternum.crm.data.Agenda a : agendas) {
                if (count >= 3) break;
                
                View item = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, null);
                TextView text1 = item.findViewById(android.R.id.text1);
                TextView text2 = item.findViewById(android.R.id.text2);
                
                text1.setText(a.descripcion);
                text2.setText(sdf.format(a.fechaHora) + " - " + a.estado.toUpperCase());
                
                containerPedidos.addView(item);
                count++;
            }
        });
    }

    private void populateChips() {
        if (availableLabels == null || chipGroupStatus == null) return;
        chipGroupStatus.removeAllViews();
        for (ContactLabel label : availableLabels) {
            Chip chip = new Chip(this);
            chip.setText(label.name);
            chip.setCheckable(true);
            try {
                int color = Color.parseColor(label.colorHex);
                chip.setChipBackgroundColor(ColorStateList.valueOf(color));
                chip.setTextColor(Color.WHITE);
            } catch (Exception ignored) {}

            chip.setOnClickListener(v -> onLabelSelected(label));
            chipGroupStatus.addView(chip);

            if (currentContact != null && label.name.equals(currentContact.etiqueta)) {
                chip.setChecked(true);
            }
        }
    }

    private void onLabelSelected(ContactLabel label) {
        if (currentContact == null) return;
        
        String currentName = etName.getText().toString().trim();
        String expectedOldName = currentContact.id;
        
        if (currentContact.etiqueta != null && !currentContact.etiqueta.isEmpty()) {
            ContactLabel oldLabel = findLabelByName(currentContact.etiqueta);
            if (oldLabel != null) {
                expectedOldName = generateExpectedName(oldLabel.prefix, currentContact.id);
            }
        }
        
        String newExpectedName = generateExpectedName(label.prefix, currentContact.id);
        
        if (currentName.isEmpty() || currentName.equals(expectedOldName) || currentName.equals(currentContact.id)) {
            etName.setText(newExpectedName);
            currentContact.etiqueta = label.name;
        } else {
            new AlertDialog.Builder(this)
                .setTitle("Actualizar nombre")
                .setMessage("¿Actualizar el nombre automáticamente a '" + newExpectedName + "'?")
                .setPositiveButton("Sí, actualizar", (dialog, which) -> {
                    etName.setText(newExpectedName);
                    currentContact.etiqueta = label.name;
                })
                .setNegativeButton("No, mantener mi nombre", (dialog, which) -> {
                    currentContact.etiqueta = label.name;
                })
                .show();
        }
    }
    
    private String generateExpectedName(String prefix, String id) {
        if (id == null) return "";
        String[] parts = id.split("_");
        String number = parts.length > 1 ? parts[parts.length - 1] : id;
        return prefix + number;
    }
    
    private ContactLabel findLabelByName(String name) {
        if (availableLabels == null) return null;
        for (ContactLabel l : availableLabels) {
            if (l.name.equals(name)) return l;
        }
        return null;
    }

    private void saveContact() {
        if (currentContact == null) return;
        currentContact.name  = etName.getText().toString().trim();
        currentContact.notes = etNotes.getText().toString().trim();
        viewModel.update(currentContact);
        Toast.makeText(this, "Guardado", Toast.LENGTH_SHORT).show();
    }

    private void openWhatsApp() {
        if (currentContact == null) return;
        // Limpiar el número: quitar +, espacios, etc.
        String phone = currentContact.phone.replaceAll("[^0-9]", "");
        Uri uri = Uri.parse("https://wa.me/" + phone);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage("com.whatsapp.w4b");
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "WhatsApp Business no está instalado", Toast.LENGTH_LONG).show();
        }
    }

    private void deleteContact() {
        if (currentContact == null) return;
        viewModel.delete(currentContact);
        Toast.makeText(this, "Contacto eliminado", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
