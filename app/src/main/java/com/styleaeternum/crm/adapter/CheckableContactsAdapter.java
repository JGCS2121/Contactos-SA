package com.styleaeternum.crm.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.styleaeternum.crm.R;
import com.styleaeternum.crm.data.CapturedContact;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CheckableContactsAdapter extends RecyclerView.Adapter<CheckableContactsAdapter.ViewHolder> {

    private List<CapturedContact> contacts = new ArrayList<>();
    private Set<CapturedContact> selectedContacts = new HashSet<>();
    private OnSelectionChangedListener listener;

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selectedCount, int totalCount);
    }

    public CheckableContactsAdapter(List<CapturedContact> contacts, OnSelectionChangedListener listener) {
        this.contacts = new ArrayList<>(contacts);
        this.selectedContacts = new HashSet<>(contacts); // Por defecto todos seleccionados
        this.listener = listener;
    }

    public void selectAll(boolean select) {
        if (select) {
            selectedContacts.addAll(contacts);
        } else {
            selectedContacts.clear();
        }
        notifyDataSetChanged();
        if (listener != null) listener.onSelectionChanged(selectedContacts.size(), contacts.size());
    }

    public List<CapturedContact> getSelectedContacts() {
        // Preservar el orden original
        List<CapturedContact> result = new ArrayList<>();
        for (CapturedContact c : contacts) {
            if (selectedContacts.contains(c)) {
                result.add(c);
            }
        }
        return result;
    }
    
    public int getSelectedCount() {
        return selectedContacts.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact_checkable, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CapturedContact contact = contacts.get(position);
        holder.tvName.setText(contact.name);
        holder.tvPhone.setText(contact.phone);
        
        if (contact.etiqueta != null && !contact.etiqueta.isEmpty()) {
            holder.tvLabel.setVisibility(View.VISIBLE);
            holder.tvLabel.setText(contact.etiqueta.toUpperCase());
        } else {
            holder.tvLabel.setVisibility(View.GONE);
        }

        // Remover el listener temporalmente para no desencadenarlo programáticamente
        holder.cbContact.setOnCheckedChangeListener(null);
        holder.cbContact.setChecked(selectedContacts.contains(contact));
        
        holder.cbContact.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedContacts.add(contact);
            } else {
                selectedContacts.remove(contact);
            }
            if (listener != null) listener.onSelectionChanged(selectedContacts.size(), contacts.size());
        });

        // Permitir click en toda la fila
        holder.itemView.setOnClickListener(v -> holder.cbContact.setChecked(!holder.cbContact.isChecked()));
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbContact;
        TextView tvName, tvPhone, tvLabel;

        ViewHolder(View v) {
            super(v);
            cbContact = v.findViewById(R.id.cb_contact);
            tvName = v.findViewById(R.id.tv_contact_name);
            tvPhone = v.findViewById(R.id.tv_contact_phone);
            tvLabel = v.findViewById(R.id.tv_label_tag);
        }
    }
}
