package com.styleaeternum.crm.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

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

    private AutoCompleteTextView actvCliente;
    private EditText etDescripcion, etNotas;
    private Button btnFecha, btnHora, btnGuardar, btnCancelar;
    private CheckBox cbRec1, cbRec2;

    private Calendar calendar = Calendar.getInstance();
    private List<CapturedContact> allContacts = new ArrayList<>();
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
        etDescripcion = findViewById(R.id.et_descripcion);
        etNotas = findViewById(R.id.et_notas);
        btnFecha = findViewById(R.id.btn_fecha);
        btnHora = findViewById(R.id.btn_hora);
        btnGuardar = findViewById(R.id.btn_guardar_pedido);
        btnCancelar = findViewById(R.id.btn_cancelar_pedido);
        cbRec1 = findViewById(R.id.cb_recordatorio_1);
        cbRec2 = findViewById(R.id.cb_recordatorio_2);

        btnFecha.setOnClickListener(v -> mostrarDatePicker());
        btnHora.setOnClickListener(v -> mostrarTimePicker());
        btnCancelar.setOnClickListener(v -> finish());
        btnGuardar.setOnClickListener(v -> guardarPedido());

        cargarContactos();
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
                
                // Pre-cargar si viene de detalles
                String cId = getIntent().getStringExtra(EXTRA_CONTACTO_ID);
                if (cId != null) {
                    for (CapturedContact c : allContacts) {
                        if (c.id.equals(cId)) {
                            actvCliente.setText(c.name);
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
        
        if (nombre.isEmpty() || desc.isEmpty()) {
            Toast.makeText(this, "Nombre y descripción son obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        Agenda agenda = new Agenda();
        agenda.nombreCliente = nombre;
        agenda.descripcion = desc;
        agenda.fechaHora = calendar.getTimeInMillis();
        agenda.notas = etNotas.getText().toString().trim();
        agenda.fechaCreacion = System.currentTimeMillis();
        agenda.estado = "pendiente";

        if (cbRec1.isChecked()) agenda.recordatorio1 = agenda.fechaHora - 86400000L;
        if (cbRec2.isChecked()) agenda.recordatorio2 = agenda.fechaHora - 7200000L;

        // Buscar si es un contacto existente
        for (CapturedContact c : allContacts) {
            if (c.name.equals(nombre)) {
                agenda.contactoId = c.id;
                agenda.telefono = c.phone;
                break;
            }
        }

        executor.execute(() -> {
            long id = AppDatabase.getInstance(this).agendaDao().insert(agenda);
            agenda.id = (int) id;
            programarNotificaciones(agenda);
            
            runOnUiThread(() -> {
                Toast.makeText(this, "Pedido agendado", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }

    private void programarNotificaciones(Agenda a) {
        WorkManager wm = WorkManager.getInstance(this);
        long now = System.currentTimeMillis();

        // 1. Notificación Día Antes
        if (a.recordatorio1 > now) {
            scheduleWork(wm, a, 1, a.recordatorio1 - now);
        }

        // 2. Notificación 2 Horas Antes
        if (a.recordatorio2 > now) {
            scheduleWork(wm, a, 2, a.recordatorio2 - now);
        }

        // 3. Notificación Hora Exacta
        if (a.fechaHora > now) {
            scheduleWork(wm, a, 3, a.fechaHora - now);
        }
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
