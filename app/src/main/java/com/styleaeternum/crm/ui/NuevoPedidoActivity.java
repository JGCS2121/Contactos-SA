package com.styleaeternum.crm.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.styleaeternum.crm.R;
import com.styleaeternum.crm.data.Agenda;
import com.styleaeternum.crm.data.AppDatabase;
import com.styleaeternum.crm.data.CapturedContact;
import com.styleaeternum.crm.service.AgendaWorker;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NuevoPedidoActivity extends AppCompatActivity {

    public static final String EXTRA_CONTACTO_ID = "contacto_id";
    public static final String EXTRA_AGENDA_ID = "agenda_id";

    private AutoCompleteTextView actvCliente;
    private EditText etTelefono, etDescripcion, etNotas;
    private Button btnFecha, btnHora, btnGuardar, btnCancelar, btnEliminar;
    private CheckBox cbRec1, cbRec2;

    private Calendar calendar = Calendar.getInstance();
    private List<CapturedContact> allContacts = new ArrayList<>();
    private Agenda currentAgenda;
    private boolean isEditMode = false;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nuevo_pedido);

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        actvCliente = findViewById(R.id.actv_cliente);
        etTelefono = findViewById(R.id.et_telefono);
        etDescripcion = findViewById(R.id.et_descripcion);
        etNotas = findViewById(R.id.et_notas);
        btnFecha = findViewById(R.id.btn_fecha);
        btnHora = findViewById(R.id.btn_hora);
        btnGuardar = findViewById(R.id.btn_guardar_pedido);
        btnCancelar = findViewById(R.id.btn_cancelar_pedido);
        btnEliminar = findViewById(R.id.btn_eliminar_pedido);
        cbRec1 = findViewById(R.id.cb_recordatorio_1);
        cbRec2 = findViewById(R.id.cb_recordatorio_2);

        btnFecha.setOnClickListener(v -> mostrarDatePicker());
        btnHora.setOnClickListener(v -> mostrarTimePicker());
        btnCancelar.setOnClickListener(v -> finish());
        btnGuardar.setOnClickListener(v -> guardarPedido());
        btnEliminar.setOnClickListener(v -> confirmarEliminar());

        // Al seleccionar un cliente de las sugerencias, autocompletar teléfono
        actvCliente.setOnItemClickListener((parent, view, position, id) -> {
            String selectedName = (String) parent.getItemAtPosition(position);
            for (CapturedContact c : allContacts) {
                if (c.name.equals(selectedName)) {
                    etTelefono.setText(c.phone);
                    break;
                }
            }
        });

        cargarContactos();
        verificarModoEdicion();
        actualizarTextosFechaHora();
    }

    private void verificarModoEdicion() {
        int agendaId = getIntent().getIntExtra(EXTRA_AGENDA_ID, -1);
        if (agendaId != -1) {
            isEditMode = true;
            btnGuardar.setText("💾 Guardar cambios");
            btnEliminar.setVisibility(View.VISIBLE);
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("Editar Pedido");
            
            executor.execute(() -> {
                currentAgenda = AppDatabase.getInstance(this).agendaDao().getById(agendaId);
                if (currentAgenda != null) {
                    runOnUiThread(() -> {
                        actvCliente.setText(currentAgenda.nombreCliente);
                        etTelefono.setText(currentAgenda.telefono);
                        etDescripcion.setText(currentAgenda.descripcion);
                        etNotas.setText(currentAgenda.notas);
                        calendar.setTimeInMillis(currentAgenda.fechaHora);
                        cbRec1.setChecked(currentAgenda.recordatorio1 > 0);
                        cbRec2.setChecked(currentAgenda.recordatorio2 > 0);
                        actualizarTextosFechaHora();
                    });
                }
            });
        }
    }

    private void cargarContactos() {
        executor.execute(() -> {
            allContacts = AppDatabase.getInstance(this).contactDao().getAllContactsSync();
            List<String> nombres = new ArrayList<>();
            for (CapturedContact c : allContacts) {
                nombres.add(c.name);
            }
            runOnUiThread(() -> {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, nombres);
                actvCliente.setAdapter(adapter);
                
                // Pre-cargar si viene de detalles del contacto
                String cId = getIntent().getStringExtra(EXTRA_CONTACTO_ID);
                if (cId != null && !isEditMode) {
                    for (CapturedContact c : allContacts) {
                        if (c.id.equals(cId)) {
                            actvCliente.setText(c.name);
                            etTelefono.setText(c.phone);
                            break;
                        }
                    }
                }
            });
        });
    }

    private void mostrarDatePicker() {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            actualizarTextosFechaHora();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void mostrarTimePicker() {
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            actualizarTextosFechaHora();
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show();
    }

    private void actualizarTextosFechaHora() {
        SimpleDateFormat sdfFecha = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat sdfHora = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        btnFecha.setText(sdfFecha.format(calendar.getTime()));
        btnHora.setText(sdfHora.format(calendar.getTime()));
    }

    private void guardarPedido() {
        String nombre = actvCliente.getText().toString().trim();
        String desc = etDescripcion.getText().toString().trim();
        String telf = etTelefono.getText().toString().trim();
        
        if (nombre.isEmpty() || desc.isEmpty()) {
            Toast.makeText(this, "Nombre y descripción son obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentAgenda == null) currentAgenda = new Agenda();
        
        currentAgenda.nombreCliente = nombre;
        currentAgenda.telefono = telf;
        currentAgenda.descripcion = desc;
        currentAgenda.fechaHora = calendar.getTimeInMillis();
        currentAgenda.notas = etNotas.getText().toString().trim();
        currentAgenda.fechaCreacion = isEditMode ? currentAgenda.fechaCreacion : System.currentTimeMillis();
        currentAgenda.estado = isEditMode ? currentAgenda.estado : "pendiente";

        currentAgenda.recordatorio1 = cbRec1.isChecked() ? currentAgenda.fechaHora - 86400000L : 0;
        currentAgenda.recordatorio2 = cbRec2.isChecked() ? currentAgenda.fechaHora - 7200000L : 0;

        // Buscar si el cliente ingresado existe en CRM para vincular contactoId
        currentAgenda.contactoId = null;
        for (CapturedContact c : allContacts) {
            if (c.name.equalsIgnoreCase(nombre)) {
                currentAgenda.contactoId = c.id;
                if (telf.isEmpty()) currentAgenda.telefono = c.phone;
                break;
            }
        }

        executor.execute(() -> {
            if (isEditMode) {
                AppDatabase.getInstance(this).agendaDao().update(currentAgenda);
            } else {
                long id = AppDatabase.getInstance(this).agendaDao().insert(currentAgenda);
                currentAgenda.id = (int) id;
            }
            
            // Reprogramar notificaciones (cancelar anteriores y poner nuevas)
            WorkManager.getInstance(this).cancelAllWorkByTag("agenda_" + currentAgenda.id);
            programarNotificaciones(currentAgenda);
            
            runOnUiThread(() -> {
                Toast.makeText(this, isEditMode ? "Pedido actualizado" : "Pedido agendado", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }

    private void confirmarEliminar() {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Pedido")
                .setMessage("¿Estás seguro de que deseas eliminar este pedido de la agenda?")
                .setPositiveButton("Eliminar", (dialog, which) -> eliminarPedido())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarPedido() {
        if (currentAgenda == null) return;
        executor.execute(() -> {
            WorkManager.getInstance(this).cancelAllWorkByTag("agenda_" + currentAgenda.id);
            AppDatabase.getInstance(this).agendaDao().delete(currentAgenda);
            runOnUiThread(() -> {
                Toast.makeText(this, "Pedido eliminado", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }

    private void programarNotificaciones(Agenda a) {
        WorkManager wm = WorkManager.getInstance(this);
        long now = System.currentTimeMillis();

        if (a.recordatorio1 > now) scheduleWork(wm, a, 1, a.recordatorio1 - now);
        if (a.recordatorio2 > now) scheduleWork(wm, a, 2, a.recordatorio2 - now);
        if (a.fechaHora > now) scheduleWork(wm, a, 3, a.fechaHora - now);
    }

    private void scheduleWork(WorkManager wm, Agenda a, int tipo, long delayMs) {
        Data data = new Data.Builder()
                .putInt(AgendaWorker.KEY_TIPO, tipo)
                .putInt(AgendaWorker.KEY_ID, a.id)
                .putString(AgendaWorker.KEY_NOMBRE, a.nombreCliente)
                .putString(AgendaWorker.KEY_DESC, a.descripcion)
                .putString(AgendaWorker.KEY_PHONE, a.telefono)
                .putString(AgendaWorker.KEY_HORA, new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(a.fechaHora))
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(AgendaWorker.class)
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag("agenda_" + a.id)
                .build();

        wm.enqueue(request);
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
