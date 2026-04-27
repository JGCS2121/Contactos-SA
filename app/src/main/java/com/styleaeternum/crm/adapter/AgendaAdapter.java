package com.styleaeternum.crm.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.styleaeternum.crm.R;
import com.styleaeternum.crm.data.Agenda;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class AgendaAdapter extends RecyclerView.Adapter<AgendaAdapter.AgendaVH> {

    private final List<Agenda> agendaList;
    private final SimpleDateFormat sdf = new SimpleDateFormat("EEEE d 'de' MMMM · hh:mm a", new Locale("es", "CO"));

    public AgendaAdapter(List<Agenda> agendaList) {
        this.agendaList = agendaList;
    }

    @NonNull
    @Override
    public AgendaVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_agenda, parent, false);
        return new AgendaVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull AgendaVH holder, int pos) {
        Agenda a = agendaList.get(pos);
        holder.tvClient.setText(a.nombreCliente);
        holder.tvDesc.setText(a.descripcion);
        holder.tvDate.setText(sdf.format(a.fechaHora));

        // Estado
        String estado = a.estado != null ? a.estado : "pendiente";
        holder.tvStatus.setText(estado.toUpperCase());
        
        int color = Color.parseColor("#FF9800"); // Naranja pendiente
        if ("entregado".equals(estado)) color = Color.parseColor("#4CAF50");
        else if ("vencido".equals(estado)) color = Color.parseColor("#F44336");
        else if ("cancelado".equals(estado)) color = Color.parseColor("#757575");
        
        holder.tvStatus.getBackground().setTint(color);

        holder.btnWhatsapp.setOnClickListener(v -> {
            if (a.telefono == null || a.telefono.isEmpty()) {
                Toast.makeText(v.getContext(), "No hay teléfono para este cliente", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/" + a.telefono));
                intent.setPackage("com.whatsapp.w4b");
                v.getContext().startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(v.getContext(), "WhatsApp Business no instalado", Toast.LENGTH_SHORT).show();
            }
        });

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), com.styleaeternum.crm.ui.NuevoPedidoActivity.class);
            intent.putExtra(com.styleaeternum.crm.ui.NuevoPedidoActivity.EXTRA_AGENDA_ID, a.id);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return agendaList.size();
    }

    static class AgendaVH extends RecyclerView.ViewHolder {
        TextView tvClient, tvDesc, tvDate, tvStatus;
        ImageView btnWhatsapp;

        AgendaVH(View v) {
            super(v);
            tvClient = v.findViewById(R.id.tv_agenda_client);
            tvDesc = v.findViewById(R.id.tv_agenda_desc);
            tvDate = v.findViewById(R.id.tv_agenda_date);
            tvStatus = v.findViewById(R.id.tv_agenda_status);
            btnWhatsapp = v.findViewById(R.id.btn_agenda_whatsapp);
        }
    }
}
