package com.styleaeternum.crm.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.styleaeternum.crm.R;
import com.styleaeternum.crm.adapter.ContactsAdapter;
import com.styleaeternum.crm.data.CapturedContact;
import com.styleaeternum.crm.sync.GoogleContactsSync;
import com.styleaeternum.crm.util.CsvExporter;
import com.styleaeternum.crm.viewmodel.ContactsViewModel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Pantalla 2 — Lista principal de contactos capturados agrupados por mes.
 */
public class MainActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 101;

    private ContactsViewModel viewModel;
    private ContactsAdapter   adapter;
    private GoogleSignInClient googleSignInClient;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Toolbar
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle(R.string.app_name);

        // RecyclerView
        RecyclerView rv = findViewById(R.id.rv_contacts);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ContactsAdapter(new ArrayList<>(), contact -> {
            Intent intent = new Intent(this, ContactDetailActivity.class);
            intent.putExtra(ContactDetailActivity.EXTRA_CONTACT_ID, contact.id);
            startActivity(intent);
        });
        rv.setAdapter(adapter);

        // ViewModel
        viewModel = new ViewModelProvider(this).get(ContactsViewModel.class);
        viewModel.getAllContacts().observe(this, contacts -> {
            adapter.updateData(contacts != null ? contacts : new ArrayList<>());
        });

        // FAB exportar CSV
        FloatingActionButton fabExport = findViewById(R.id.fab_export_csv);
        fabExport.setOnClickListener(v -> exportCsv());

        // Botón sincronizar Google Contacts
        ExtendedFloatingActionButton btnSync = findViewById(R.id.btn_sync_google);
        btnSync.setOnClickListener(v -> signInGoogle());

        // Configurar Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope("https://www.googleapis.com/auth/contacts"))
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    // ─── Exportar CSV ───────────────────────────────────────────────────────
    private void exportCsv() {
        executor.execute(() -> {
            List<CapturedContact> all = viewModel.getAllContactsSync();
            if (all == null || all.isEmpty()) {
                runOnUiThread(() -> Toast.makeText(this,
                        "No hay contactos para exportar", Toast.LENGTH_SHORT).show());
                return;
            }
            File csv = CsvExporter.export(this, all);
            runOnUiThread(() -> {
                if (csv != null) {
                    shareCsv(csv);
                } else {
                    Toast.makeText(this, "Error al exportar", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void shareCsv(File csv) {
        Uri uri = FileProvider.getUriForFile(this,
                getPackageName() + ".fileprovider", csv);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/csv");
        share.putExtra(Intent.EXTRA_STREAM, uri);
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(share, "Compartir CSV"));
    }

    // ─── Google Sign-In ──────────────────────────────────────────────────────
    private void signInGoogle() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            syncToGoogle(account);
        } else {
            startActivityForResult(googleSignInClient.getSignInIntent(), RC_SIGN_IN);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            try {
                GoogleSignInAccount account = GoogleSignIn
                        .getSignedInAccountFromIntent(data)
                        .getResult(ApiException.class);
                syncToGoogle(account);
            } catch (ApiException e) {
                Toast.makeText(this, "Error Google Sign-In: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void syncToGoogle(GoogleSignInAccount account) {
        Toast.makeText(this, "Sincronizando con Google Contacts…", Toast.LENGTH_SHORT).show();
        executor.execute(() -> {
            List<CapturedContact> all = viewModel.getAllContactsSync();
            int synced = GoogleContactsSync.syncAll(this, account, all);
            runOnUiThread(() -> Toast.makeText(this,
                    synced + " contactos sincronizados con Google Contacts",
                    Toast.LENGTH_LONG).show());
        });
    }

    @Override
    protected void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }
}
