package com.styleaeternum.crm.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.styleaeternum.crm.R;
import com.styleaeternum.crm.service.CampaignService;

import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla de progreso en tiempo real de la campaña masiva.
 * Recibe broadcasts de CampaignService y actualiza la UI.
 */
public class CampaignProgressActivity extends AppCompatActivity {

    private TextView tvSentCount, tvTotalCount, tvCurrentContact, tvTimeRemaining;
    private ProgressBar progressBar;
    private Button btnStop, btnBackground;
    private RecyclerView rvLog;
    private LogAdapter logAdapter;

    private int intervalMinutes = 2;

    private final BroadcastReceiver progressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (CampaignService.ACTION_PROGRESS_UPDATE.equals(action)) {
                int sent  = intent.getIntExtra(CampaignService.EXTRA_SENT, 0);
                int total = intent.getIntExtra(CampaignService.EXTRA_TOTAL, 0);
                String name   = intent.getStringExtra(CampaignService.EXTRA_CURRENT);
                String status = intent.getStringExtra(CampaignService.EXTRA_STATUS);
                updateUI(sent, total, name, status);
            } else if (CampaignService.ACTION_CAMPAIGN_DONE.equals(action)) {
                int sent  = intent.getIntExtra(CampaignService.EXTRA_SENT, 0);
                int total = intent.getIntExtra(CampaignService.EXTRA_TOTAL, 0);
                onCampaignDone(sent, total);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campaign_progress);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        tvSentCount      = findViewById(R.id.tv_sent_count);
        tvTotalCount     = findViewById(R.id.tv_total_count);
        tvCurrentContact = findViewById(R.id.tv_current_contact);
        tvTimeRemaining  = findViewById(R.id.tv_time_remaining);
        progressBar      = findViewById(R.id.progress_bar);
        btnStop          = findViewById(R.id.btn_stop_campaign);
        btnBackground    = findViewById(R.id.btn_background);
        rvLog            = findViewById(R.id.rv_send_log);

        logAdapter = new LogAdapter();
        rvLog.setLayoutManager(new LinearLayoutManager(this));
        rvLog.setAdapter(logAdapter);

        btnStop.setOnClickListener(v -> stopCampaign());
        btnBackground.setOnClickListener(v -> finish()); // Cierra la pantalla sin detener

        registerProgressReceiver();
    }

    private void registerProgressReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(CampaignService.ACTION_PROGRESS_UPDATE);
        filter.addAction(CampaignService.ACTION_CAMPAIGN_DONE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(progressReceiver, filter, RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(progressReceiver, filter);
        }
    }

    private void updateUI(int sent, int total, String name, String status) {
        tvSentCount.setText(String.valueOf(sent));
        tvTotalCount.setText(String.valueOf(total));
        int progress = total > 0 ? (int) ((sent * 100f) / total) : 0;
        progressBar.setProgress(progress);

        boolean isOk = "ok".equals(status);
        String icon = isOk ? "✅" : "sending".equals(status) ? "📤" : "❌";
        tvCurrentContact.setText(icon + " " + (name != null ? name : ""));

        // Tiempo restante estimado
        int remaining = total - sent;
        if (remaining > 0) {
            long totalMin = (long) intervalMinutes * remaining;
            tvTimeRemaining.setText("~" + (totalMin < 60 ? totalMin + " min restantes"
                    : (totalMin/60) + "h " + (totalMin%60) + "min restantes"));
        }

        // Añadir al registro
        if (name != null && !"sending".equals(status)) {
            logAdapter.add((isOk ? "✅ " : "❌ ") + name);
        }
    }

    private void onCampaignDone(int sent, int total) {
        tvCurrentContact.setText("✅ Campaña completada");
        tvTimeRemaining.setText(sent + " de " + total + " mensajes enviados");
        progressBar.setProgress(100);
        tvSentCount.setText(String.valueOf(sent));
        tvTotalCount.setText(String.valueOf(total));
        btnStop.setEnabled(false);
        logAdapter.add("─── Campaña finalizada ───");
    }

    private void stopCampaign() {
        Intent intent = new Intent(this, CampaignService.class);
        intent.setAction(CampaignService.ACTION_STOP);
        startService(intent);
        tvCurrentContact.setText("⛔ Campaña detenida");
        btnStop.setEnabled(false);
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }

    @Override
    protected void onDestroy() {
        try { unregisterReceiver(progressReceiver); } catch (Exception ignored) {}
        super.onDestroy();
    }

    // ── Adapter del registro ─────────────────────────────────────────────────

    private static class LogAdapter extends RecyclerView.Adapter<LogAdapter.VH> {
        private final List<String> items = new ArrayList<>();

        void add(String entry) { items.add(0, entry); notifyItemInserted(0); }

        @Override
        public VH onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setPadding(8, 10, 8, 10);
            tv.setTextSize(13f);
            return new VH(tv);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            ((TextView) holder.itemView).setText(items.get(position));
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            VH(android.view.View v) { super(v); }
        }
    }
}
