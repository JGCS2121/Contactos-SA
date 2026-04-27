package com.styleaeternum.crm.adapter;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.styleaeternum.crm.R;
import com.styleaeternum.crm.data.ContactLabel;

import java.util.List;

public class EtiquetasAdapter extends RecyclerView.Adapter<EtiquetasAdapter.EtiquetaVH> {

    public interface OnEtiquetaListener {
        void onSave(ContactLabel label, String newName, String newColor);
        void onDelete(ContactLabel label);
        void onColorPick(ContactLabel label, View colorView);
    }

    private final List<ContactLabel> labels;
    private final OnEtiquetaListener listener;

    public EtiquetasAdapter(List<ContactLabel> labels, OnEtiquetaListener listener) {
        this.labels = labels;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EtiquetaVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_etiqueta_edit, parent, false);
        return new EtiquetaVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull EtiquetaVH holder, int pos) {
        ContactLabel label = labels.get(pos);
        holder.etName.setText(label.name);

        // Poner el color en el círculo
        try {
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(Color.parseColor(label.colorHex != null ? label.colorHex : "#25D366"));
            holder.vColor.setBackground(circle);
        } catch (Exception ignored) {}

        // Toca el círculo → selector de color
        holder.vColor.setOnClickListener(v -> listener.onColorPick(label, holder.vColor));

        // Guardar nombre
        holder.btnSave.setOnClickListener(v -> {
            String newName = holder.etName.getText().toString().trim();
            if (newName.isEmpty()) {
                Toast.makeText(v.getContext(), "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show();
                return;
            }
            listener.onSave(label, newName, label.colorHex);
        });

        // Eliminar
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(label));
    }

    @Override
    public int getItemCount() { return labels.size(); }

    static class EtiquetaVH extends RecyclerView.ViewHolder {
        View vColor;
        EditText etName;
        ImageView btnSave, btnDelete;

        EtiquetaVH(View v) {
            super(v);
            vColor = v.findViewById(R.id.v_color);
            etName = v.findViewById(R.id.et_label_name);
            btnSave = v.findViewById(R.id.btn_save_label);
            btnDelete = v.findViewById(R.id.btn_delete_label);
        }
    }
}
