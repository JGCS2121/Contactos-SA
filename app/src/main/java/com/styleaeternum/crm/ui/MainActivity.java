package com.styleaeternum.crm.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.styleaeternum.crm.R;
import com.styleaeternum.crm.adapter.ContactsAdapter;
import com.styleaeternum.crm.data.CapturedContact;
import com.styleaeternum.crm.data.ContactRepository;
import com.styleaeternum.crm.sync.GoogleContactsSync;
import com.styleaeternum.crm.ui.AgendaActivity;
import com.styleaeternum.crm.util.CsvExporter;
import com.styleaeternum.crm.util.CsvImporter;
import com.styleaeternum.crm.viewmodel.ContactsViewModel;

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
    private ContactRepository repository;

    private final ActivityResultLauncher<String> csvPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    Toast.makeText(this, "Importando contactos...", Toast.LENGTH_SHORT).show();
                    CsvImporter.importCsv(this, uri, repository);
                }
            });

    private android.widget.TextView tvCounter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repository = new ContactRepository(this);

        // Toolbar
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle(R.string.app_name);

        tvCounter = findViewById(R.id.tv_counter);

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
            List<CapturedContact> data = contacts != null ? contacts : new ArrayList<>();
            adapter.updateData(data);
            updateCounters(data);
        });

        // FAB exportar CSV
        FloatingActionButton fabExport = findViewById(R.id.fab_export_csv);
        fabExport.setOnClickListener(v -> exportCsv());

        // Configurar Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope("https://www.googleapis.com/auth/contacts"))
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }
    
    private void updateCounters(List<CapturedContact> contacts) {
        int total = contacts.size();
        int thisMonth = 0;
        String currentMonthYear = new java.text.SimpleDateFormat("MMMyyyy", java.util.Locale.getDefault()).format(new java.util.Date()).toLowerCase();
        
        for (CapturedContact c : contacts) {
            if (c.groupMembership != null && c.groupMembership.toLowerCase().equals(currentMonthYear)) {
                thisMonth++;
            }
        }
        
        tvCounter.setText("Total: " + total + " contactos · Este mes: " + thisMonth);
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
            runOnUiThread(() -> CsvExporter.export(this, all));
        });
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
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        
        MenuItem searchItem = menu.findItem(R.id.action_search);
        androidx.appcompat.widget.SearchView searchView = (androidx.appcompat.widget.SearchView) searchItem.getActionView();
        
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText);
                return true;
            }
        });
        
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_import_csv) {
            csvPickerLauncher.launch("text/csv");
            return true;
        } else if (id == R.id.action_agenda) {
            startActivity(new Intent(this, AgendaActivity.class));
            return true;
        } else if (id == R.id.action_export_csv) {
            exportCsv();
            return true;
        } else if (id == R.id.action_sync_google) {
            signInGoogle();
            return true;
        } else if (id == R.id.action_about) {
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
            builder.setTitle("Acerca de");
            builder.setMessage("Style Aeternum CRM v2.0\nSistema inteligente de captura.");
            builder.setPositiveButton("OK", null);
            builder.show();
            return true;
        } else if (id == R.id.action_manage_labels) {
            Toast.makeText(this, "Gestor de etiquetas próximamente", Toast.LENGTH_SHORT).show();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }
}
