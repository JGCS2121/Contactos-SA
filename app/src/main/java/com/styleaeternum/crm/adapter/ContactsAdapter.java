package com.styleaeternum.crm.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.styleaeternum.crm.R;
import com.styleaeternum.crm.data.CapturedContact;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.styleaeternum.crm.data.ContactLabel;

/**
 * RecyclerView adapter que muestra contactos agrupados por mes (groupMembership).
 * Tipos de vista: HEADER (0) y ITEM (1).
 */
public class ContactsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM   = 1;

    public interface OnContactClick { void onClick(CapturedContact contact); }

    private List<Object> items = new ArrayList<>(); // String (header) | CapturedContact
    private List<CapturedContact> fullContacts = new ArrayList<>();
    private Map<String, String> labelColors = new HashMap<>();
    private final OnContactClick listener;

    public ContactsAdapter(List<CapturedContact> contacts, OnContactClick listener) {
        this.listener = listener;
        updateData(contacts);
    }

    public void setLabels(List<ContactLabel> labels) {
        labelColors.clear();
        if (labels != null) {
            for (ContactLabel l : labels) {
                labelColors.put(l.name, l.colorHex);
            }
        }
        notifyDataSetChanged();
    }

    public void updateData(List<CapturedContact> contacts) {
        this.fullContacts = new ArrayList<>(contacts);
        applyFilter("");
    }
    
    public void filter(String query) {
        applyFilter(query);
    }

    private void applyFilter(String query) {
        items.clear();
        String lowerQuery = query == null ? "" : query.toLowerCase();
        
        Map<String, List<CapturedContact>> grouped = new LinkedHashMap<>();
        for (CapturedContact c : fullContacts) {
            if (c.name.toLowerCase().contains(lowerQuery) || c.phone.toLowerCase().contains(lowerQuery)) {
                grouped.computeIfAbsent(c.groupMembership, k -> new ArrayList<>()).add(c);
            }
        }
        for (Map.Entry<String, List<CapturedContact>> entry : grouped.entrySet()) {
            items.add(entry.getKey());               // header
            items.addAll(entry.getValue());          // items
        }
        notifyDataSetChanged();
    }

    @Override public int getItemViewType(int pos) {
        return items.get(pos) instanceof String ? TYPE_HEADER : TYPE_ITEM;
    }

    @Override public int getItemCount() { return items.size(); }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            View v = inf.inflate(R.layout.item_group_header, parent, false);
            return new HeaderVH(v);
        } else {
            View v = inf.inflate(R.layout.item_contact, parent, false);
            return new ContactVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int pos) {
        if (holder instanceof HeaderVH) {
            ((HeaderVH) holder).bind((String) items.get(pos));
        } else {
            CapturedContact c = (CapturedContact) items.get(pos);
            ((ContactVH) holder).bind(c, listener, labelColors);
        }
    }

    // ── ViewHolders ──────────────────────────────────────────────────────────

    static class HeaderVH extends RecyclerView.ViewHolder {
        TextView tvHeader;
        HeaderVH(View v) { super(v); tvHeader = v.findViewById(R.id.tv_group_header); }
        void bind(String group) { tvHeader.setText(group.toUpperCase()); }
    }

    static class ContactVH extends RecyclerView.ViewHolder {
        TextView tvId, tvPhone, tvBadge;
        android.widget.ImageView ivLabelTag;

        ContactVH(View v) {
            super(v);
            tvId    = v.findViewById(R.id.tv_contact_id_item);
            tvPhone = v.findViewById(R.id.tv_phone_item);
            tvBadge = v.findViewById(R.id.tv_badge);
            ivLabelTag = v.findViewById(R.id.iv_label_tag);
        }
        void bind(CapturedContact c, OnContactClick l, Map<String, String> labelColors) {
            tvId.setText(c.name);
            tvPhone.setText(c.phone);
            tvBadge.setText(c.groupMembership);

            // Mostrar bolita de color si tiene etiqueta
            if (c.etiqueta != null && !c.etiqueta.isEmpty() && labelColors.containsKey(c.etiqueta)) {
                ivLabelTag.setVisibility(View.VISIBLE);
                try {
                    android.graphics.drawable.GradientDrawable circle = new android.graphics.drawable.GradientDrawable();
                    circle.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                    circle.setColor(android.graphics.Color.parseColor(labelColors.get(c.etiqueta)));
                    ivLabelTag.setBackground(circle);
                } catch (Exception ignored) {}
            } else {
                ivLabelTag.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> l.onClick(c));
        }
    }
}
