package com.styleaeternum.crm.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.styleaeternum.crm.sync.GoogleContactsSync;

import java.text.SimpleDateFormat;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import android.graphics.Color;
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

    private TextView tvId, tvPhone, tvType;
    private EditText etName, etNotes, etGroup;
    private Button   btnSave, btnWhatsApp, btnDelete, btnAgendar;
    private android.widget.LinearLayout containerPedidos;
    private ChipGroup chipGroupStatus;
    private List<ContactLabel> availableLabels;
    private final java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
    private boolean dataCargada = false; // Para no sobreescribir edits del usuario

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
        tvType    = findViewById(R.id.tv_type);
        etGroup   = findViewById(R.id.et_group);
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
            // Solo cargamos los datos la primera vez para no sobreescribir lo que el usuario edita
            if (!dataCargada) {
                currentContact = contact;
                bindData(contact);
                dataCargada = true;
            } else {
                // Solo actualizamos el objeto en memoria sin tocar los campos de UI
                currentContact = contact;
            }
        });

        btnSave.setOnClickListener(v -> saveContact());
        btnWhatsApp.setOnClickListener(v -> openWhatsApp());
        btnDelete.setOnClickListener(v -> deleteContact());
    }

    private String selectedLabel = "";

    private void bindData(CapturedContact c) {
        tvId.setText(c.id);
        tvPhone.setText(c.phone);
        etGroup.setText(c.groupMembership);
        tvType.setText(c.phoneType);
        etName.setText(c.name);
        etNotes.setText(c.notes);
        selectedLabel = c.etiqueta != null ? c.etiqueta : "";
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
                tvEmpty.setTextSize(14.0f);
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
        chipGroupStatus.setSingleSelection(true);
        chipGroupStatus.setSelectionRequired(false);

        for (ContactLabel label : availableLabels) {
            Chip chip = new Chip(this);
            chip.setText(label.name);
            chip.setCheckable(true);
            chip.setCheckedIconVisible(true);
            chip.setCheckedIcon(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.ic_check));
            
            boolean isThisSelected = label.name.equals(selectedLabel);
            updateChipStyle(chip, label, isThisSelected);

            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedLabel = label.name;
                } else if (selectedLabel.equals(label.name)) {
                    selectedLabel = "";
                }
                
                // Actualizar estilos de todos los chips para reflejar la selección única
                for (int i = 0; i < chipGroupStatus.getChildCount(); i++) {
                    Chip child = (Chip) chipGroupStatus.getChildAt(i);
                    ContactLabel cl = availableLabels.get(i);
                    updateChipStyle(child, cl, cl.name.equals(selectedLabel));
                }
            });

            chipGroupStatus.addView(chip);
            if (isThisSelected) {
                chip.setChecked(true);
            }
        }
    }

    private void updateChipStyle(Chip chip, ContactLabel label, boolean isSelected) {
        try {
            int color = Color.parseColor(label.colorHex);
            if (isSelected) {
                chip.setChipBackgroundColor(ColorStateList.valueOf(color));
                chip.setTextColor(Color.WHITE);
                chip.setChipStrokeWidth(0);
                chip.setCheckedIconTint(ColorStateList.valueOf(Color.WHITE));
            } else {
                chip.setChipBackgroundColor(ColorStateList.valueOf(Color.TRANSPARENT));
                chip.setTextColor(color);
                chip.setChipStrokeColor(ColorStateList.valueOf(color));
                chip.setChipStrokeWidth(3.0f);
            }
        } catch (Exception ignored) {}
    }

    private void saveContact() {
        if (currentContact == null) return;
        currentContact.name  = etName.getText().toString().trim();
        currentContact.notes = etNotes.getText().toString().trim();
        currentContact.groupMembership = etGroup.getText().toString().trim();
        currentContact.etiqueta = selectedLabel; // Se guarda la etiqueta seleccionada
        
        viewModel.update(currentContact);
        Toast.makeText(this, "Datos guardados correctamente", Toast.LENGTH_SHORT).show();
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

        // Comprobar si el contacto tiene ID de Google y hay sesión activa
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        boolean hasGoogleLink = currentContact.googleResourceName != null
                && !currentContact.googleResourceName.isEmpty();

        if (hasGoogleLink && account != null) {
            // Ofrecer borrar también de Google
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Eliminar contacto")
                .setMessage("Este contacto está sincronizado con Google Contacts.\n¿Quieres borrarlo también de Google?")
                .setPositiveButton("Borrar de Google también", (d, w) -> {
                    final String resourceName = currentContact.googleResourceName;
                    final GoogleSignInAccount finalAccount = account;
                    executor.execute(() -> {
                        boolean ok = GoogleContactsSync.deleteFromGoogle(this, finalAccount, resourceName);
                        runOnUiThread(() -> {
                            if (!ok) Toast.makeText(this,
                                "No se pudo borrar de Google: " + GoogleContactsSync.lastErrorMessage,
                                Toast.LENGTH_LONG).show();
                        });
                    });
                    viewModel.delete(currentContact);
                    Toast.makeText(this, "Contacto eliminado", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("Solo en la app", (d, w) -> {
                    viewModel.delete(currentContact);
                    Toast.makeText(this, "Contacto eliminado", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNeutralButton("Cancelar", null)
                .show();
        } else {
            // Sin enlace Google: borrar directamente
            viewModel.delete(currentContact);
            Toast.makeText(this, "Contacto eliminado", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        // Ocultar menú de 3 puntos temporalmente hasta que se definan funciones de configuración
        return false;
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
