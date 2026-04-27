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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private final OnContactClick listener;

    public ContactsAdapter(List<CapturedContact> contacts, OnContactClick listener) {
        this.listener = listener;
        updateData(contacts);
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
            ((ContactVH) holder).bind(c, listener);
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
        ContactVH(View v) {
            super(v);
            tvId    = v.findViewById(R.id.tv_contact_id_item);
            tvPhone = v.findViewById(R.id.tv_phone_item);
            tvBadge = v.findViewById(R.id.tv_badge);
        }
        void bind(CapturedContact c, OnContactClick l) {
            tvId.setText(c.id);
            tvPhone.setText(c.phone);
            tvBadge.setText(c.groupMembership);
            itemView.setOnClickListener(v -> l.onClick(c));
        }
    }
}
