package com.styleaeternum.crm.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.styleaeternum.crm.R;
import com.styleaeternum.crm.data.Agenda;
import com.styleaeternum.crm.data.AppDatabase;
import com.styleaeternum.crm.data.CapturedContact;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AgendaActivity extends AppCompatActivity {

    private RecyclerView rvAgenda;
    private TextView tvCounter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private List<Agenda> allAgendas = new ArrayList<>();
    // Aquí usamos un Adapter muy simple que se implementará
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agenda);
        
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        tvCounter = findViewById(R.id.tv_agenda_counter);
        rvAgenda = findViewById(R.id.rv_agenda);
        rvAgenda.setLayoutManager(new LinearLayoutManager(this));

        FloatingActionButton fabAdd = findViewById(R.id.fab_add_pedido);
        fabAdd.setOnClickListener(v -> {
            startActivity(new Intent(this, NuevoPedidoActivity.class));
        });

        // Cargar datos
        AppDatabase.getInstance(this).agendaDao().getAll().observe(this, agendas -> {
            if (agendas != null) {
                this.allAgendas = agendas;
                updateUI();
            }
        });
    }

    private void updateUI() {
        int pendientes = 0;
        int hoy = 0;
        long now = System.currentTimeMillis();
        // Definir inicio y fin de hoy
        long dayStart = now - (now % 86400000); // Aproximado UTC, pero sirve para la cuenta simple
        long dayEnd = dayStart + 86400000;

        for (Agenda a : allAgendas) {
            if ("pendiente".equals(a.estado)) {
                pendientes++;
                if (a.fechaHora >= dayStart && a.fechaHora <= dayEnd) {
                    hoy++;
                }
            }
        }
        
        tvCounter.setText(pendientes + " pendientes · " + hoy + " para hoy");
        
        AgendaAdapter adapter = new AgendaAdapter(allAgendas);
        rvAgenda.setAdapter(adapter);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
