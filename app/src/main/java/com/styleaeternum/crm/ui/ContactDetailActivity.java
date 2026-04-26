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

import com.styleaeternum.crm.R;
import com.styleaeternum.crm.data.CapturedContact;
import com.styleaeternum.crm.viewmodel.ContactsViewModel;

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
    private Button   btnSave, btnWhatsApp, btnDelete;

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

        viewModel = new ViewModelProvider(this).get(ContactsViewModel.class);

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
        intent.setPackage("com.whatsapp");
        try {
            startActivity(intent);
        } catch (Exception e) {
            // Si no está instalado WA, abrir en navegador
            intent.setPackage(null);
            startActivity(intent);
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
