package com.styleaeternum.crm.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;

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
import com.styleaeternum.crm.data.AppDatabase;
import com.styleaeternum.crm.data.CapturedContact;
import com.styleaeternum.crm.data.ContactRepository;
import com.styleaeternum.crm.data.LabelDao;
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
    private DrawerLayout drawerLayout;

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

        // Drawer (menú hamburguesa)
        drawerLayout = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, findViewById(R.id.toolbar),
                R.string.app_name, R.string.app_name);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navView = findViewById(R.id.nav_view);
        navView.setNavigationItemSelectedListener(item -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            int id = item.getItemId();
            if (id == R.id.nav_agenda) {
                startActivity(new Intent(this, AgendaActivity.class));
            } else if (id == R.id.nav_export_csv) {
                exportCsv();
            } else if (id == R.id.nav_import_csv) {
                csvPickerLauncher.launch("*/*");
            } else if (id == R.id.nav_sync_google) {
                signInGoogle();
            } else if (id == R.id.nav_etiquetas) {
                startActivity(new Intent(this, GestorEtiquetasActivity.class));
            } else if (id == R.id.nav_permissions) {
                startActivity(new Intent(this, PermissionSetupActivity.class));
            } else if (id == R.id.nav_about) {
                new AlertDialog.Builder(this)
                    .setTitle("Style Aeternum CRM")
                    .setMessage("v2.0 — Sistema inteligente de captura de clientes.")
                    .setPositiveButton("OK", null).show();
            }
            return true;
        });

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

        // Observar Etiquetas para mostrar los colores en la lista principal
        LabelDao labelDao = AppDatabase.getInstance(this).labelDao();
        labelDao.getAll().observe(this, labels -> {
            adapter.setLabels(labels);
        });

        // FAB exportar CSV
        FloatingActionButton fabExport = findViewById(R.id.fab_export_csv);
        fabExport.setOnClickListener(v -> exportCsv());

        // Configurar Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestScopes(new Scope("https://www.googleapis.com/auth/contacts"))
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }
    
    private void updateCounters(List<CapturedContact> contacts) {
        int total = contacts.size();
        int thisMonth = 0;
        
        java.util.Calendar now = java.util.Calendar.getInstance();
        int currentMonth = now.get(java.util.Calendar.MONTH);
        int currentYear = now.get(java.util.Calendar.YEAR);
        
        for (CapturedContact c : contacts) {
            java.util.Calendar captured = java.util.Calendar.getInstance();
            captured.setTimeInMillis(c.capturedAt);
            if (captured.get(java.util.Calendar.MONTH) == currentMonth && 
                captured.get(java.util.Calendar.YEAR) == currentYear) {
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
            } catch (com.google.android.gms.common.api.ApiException e) {
                String debugSha = getAppSignatureSHA1();
                new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Error de Conexión (Código " + e.getStatusCode() + ")")
                    .setMessage("Para solucionar este error, registra este código SHA-1 en Google Cloud Console:\n\n" + debugSha)
                    .setPositiveButton("Cerrar", null)
                    .show();
            }
        }
    }

    private void syncToGoogle(GoogleSignInAccount account) {
        Toast.makeText(this, "Sincronizando con Google Contacts…", Toast.LENGTH_SHORT).show();
        executor.execute(() -> {
            // El nuevo método maneja la obtención de locales y remotos internamente
            int synced = GoogleContactsSync.syncAll(this, account, repository);
            
            runOnUiThread(() -> {
                if (synced >= 0) {
                    Toast.makeText(this, 
                        synced + " contactos sincronizados (subida y bajada)", 
                        Toast.LENGTH_LONG).show();
                } else {
                    String debugSha = getAppSignatureSHA1();
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Error de Sincronización (10)")
                        .setMessage("Copia este código SHA-1 y regístralo en Google Cloud Console:\n\n" + debugSha)
                        .setPositiveButton("Cerrar", null)
                        .show();
                }
            });
        });
    }

    private String getAppSignatureSHA1() {
        try {
            android.content.pm.PackageInfo info = getPackageManager().getPackageInfo(
                    getPackageName(), android.content.pm.PackageManager.GET_SIGNATURES);
            for (android.content.pm.Signature signature : info.signatures) {
                java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
                byte[] digest = md.digest(signature.toByteArray());
                StringBuilder hexString = new StringBuilder();
                for (int i = 0; i < digest.length; i++) {
                    String append = Integer.toHexString(0xFF & digest[i]);
                    if (append.length() == 1) hexString.append("0");
                    hexString.append(append.toUpperCase());
                    if (i < digest.length - 1) hexString.append(":");
                }
                return hexString.toString();
            }
        } catch (Exception e) {
            return "No se pudo obtener el SHA-1: " + e.getMessage();
        }
        return "No se encontraron firmas.";
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
            csvPickerLauncher.launch("*/*");
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
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }
}
