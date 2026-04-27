package com.styleaeternum.crm.ui;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.styleaeternum.crm.R;
import com.styleaeternum.crm.adapter.EtiquetasAdapter;
import com.styleaeternum.crm.data.AppDatabase;
import com.styleaeternum.crm.data.ContactLabel;
import com.styleaeternum.crm.data.LabelDao;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GestorEtiquetasActivity extends AppCompatActivity {

    // Paleta de colores predefinidos para elegir
    private static final String[] COLORES = {
        "#F44336", "#E91E63", "#9C27B0", "#673AB7",
        "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4",
        "#009688", "#4CAF50", "#8BC34A", "#CDDC39",
        "#FFC107", "#FF9800", "#FF5722", "#795548",
        "#607D8B", "#9E9E9E", "#25D366", "#006d2f"
    };

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private EtiquetasAdapter adapter;
    private List<ContactLabel> labelList = new ArrayList<>();
    private LabelDao labelDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gestor_etiquetas);

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        labelDao = AppDatabase.getInstance(this).labelDao();

        // ── Prefijo del negocio ───────────────────────────────────────────
        android.widget.EditText etPrefijo = findViewById(R.id.et_prefijo);
        android.widget.CheckBox cbReiniciar = findViewById(R.id.cb_reiniciar_contador);
        
        // Mostrar el prefijo actual guardado
        etPrefijo.setText(com.styleaeternum.crm.util.PrefijosHelper.getPrefijo(this));
        
        findViewById(R.id.btn_guardar_prefijo).setOnClickListener(v -> {
            String nuevoPrefijo = etPrefijo.getText().toString().trim();
            com.styleaeternum.crm.util.PrefijosHelper.setPrefijo(this, nuevoPrefijo);
            if (cbReiniciar.isChecked()) {
                com.styleaeternum.crm.util.PrefijosHelper.reiniciarContador(this);
                cbReiniciar.setChecked(false);
                Toast.makeText(this, "Prefijo guardado y contador reiniciado a 001 ✓", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Prefijo '" + (nuevoPrefijo.isEmpty() ? "(sin prefijo)" : nuevoPrefijo) + "' guardado ✓", Toast.LENGTH_SHORT).show();
            }
        });
        // ─────────────────────────────────────────────────────────────────

        RecyclerView rv = findViewById(R.id.rv_etiquetas);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new EtiquetasAdapter(labelList, new EtiquetasAdapter.OnEtiquetaListener() {
            @Override
            public void onSave(ContactLabel label, String newName, String newColor) {
                label.name = newName;
                label.colorHex = newColor;
                executor.execute(() -> {
                    labelDao.update(label);
                    runOnUiThread(() -> Toast.makeText(GestorEtiquetasActivity.this,
                            "Etiqueta guardada ✓", Toast.LENGTH_SHORT).show());
                });
            }

            @Override
            public void onDelete(ContactLabel label) {
                new AlertDialog.Builder(GestorEtiquetasActivity.this)
                    .setTitle("Eliminar etiqueta")
                    .setMessage("¿Eliminar \"" + label.name + "\"? Los contactos con esta etiqueta la perderán.")
                    .setPositiveButton("Eliminar", (d, w) -> executor.execute(() -> {
                        labelDao.delete(label);
                        runOnUiThread(() -> {
                            labelList.remove(label);
                            adapter.notifyDataSetChanged();
                            Toast.makeText(GestorEtiquetasActivity.this,
                                    "Etiqueta eliminada", Toast.LENGTH_SHORT).show();
                        });
                    }))
                    .setNegativeButton("Cancelar", null)
                    .show();
            }

            @Override
            public void onColorPick(ContactLabel label, View colorView) {
                mostrarSelectorColor(label, colorView);
            }
        });

        rv.setAdapter(adapter);

        // FAB: agregar nueva etiqueta
        FloatingActionButton fab = findViewById(R.id.fab_add_etiqueta);
        fab.setOnClickListener(v -> agregarNuevaEtiqueta());

        // Cargar etiquetas
        labelDao.getAll().observe(this, labels -> {
            if (labels != null) {
                labelList.clear();
                labelList.addAll(labels);
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void mostrarSelectorColor(ContactLabel label, View colorView) {
        // Crear vista con grid de colores
        View grid = getLayoutInflater().inflate(android.R.layout.activity_list_item, null);

        // Usamos un diálogo con botones de color en líneas
        String[] nombresColor = {
            "🔴 Rojo", "💗 Rosa", "🟣 Violeta", "🔵 Índigo",
            "🔵 Azul", "🩵 Celeste", "🔵 Agua", "🩵 Cyan",
            "🟢 Teal", "🟢 Verde", "🟢 Lima", "🟡 Amarillo",
            "🟡 Ámbar", "🟠 Naranja", "🟠 Rojo oscuro", "🟤 Café",
            "⚫ Gris azul", "⚫ Gris", "🟢 WhatsApp", "🟢 Verde oscuro"
        };

        new AlertDialog.Builder(this)
            .setTitle("Elige un color")
            .setItems(nombresColor, (dialog, which) -> {
                String colorElegido = COLORES[which];
                label.colorHex = colorElegido;

                // Actualizar círculo en pantalla
                GradientDrawable circle = new GradientDrawable();
                circle.setShape(GradientDrawable.OVAL);
                circle.setColor(Color.parseColor(colorElegido));
                colorView.setBackground(circle);

                // Guardar en BD
                executor.execute(() -> labelDao.update(label));
                Toast.makeText(this, "Color actualizado", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }

    private void agregarNuevaEtiqueta() {
        // Diálogo para nombre de nueva etiqueta
        android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Nombre de la etiqueta");
        input.setPadding(48, 24, 48, 24);

        new AlertDialog.Builder(this)
            .setTitle("Nueva etiqueta")
            .setView(input)
            .setPositiveButton("Crear", (dialog, which) -> {
                String nombre = input.getText().toString().trim();
                if (nombre.isEmpty()) {
                    Toast.makeText(this, "Escribe un nombre", Toast.LENGTH_SHORT).show();
                    return;
                }
                ContactLabel nueva = new ContactLabel();
                nueva.name = nombre;
                nueva.colorHex = "#25D366"; // Verde por defecto
                nueva.prefix = "";
                executor.execute(() -> {
                    labelDao.insert(nueva);
                    runOnUiThread(() -> Toast.makeText(this,
                            "Etiqueta '" + nombre + "' creada", Toast.LENGTH_SHORT).show());
                });
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
