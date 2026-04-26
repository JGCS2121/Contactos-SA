package com.styleaeternum.crm.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

/**
 * Entidad Room que representa un contacto capturado de WhatsApp Business.
 * Columnas compatibles con Google Contacts CSV.
 */
@Entity(tableName = "contacts")
public class CapturedContact {

    @PrimaryKey
    @NonNull
    public String id = "";           // ej: abril2026_001

    public String name = "";         // Name (editable por el usuario)
    public String phone = "";        // Phone 1 - Value
    public String phoneType = "WhatsApp Business";  // Phone 1 - Type
    public String groupMembership = "";  // ej: abril2026
    public String notes = "";        // Notes
    public long capturedAt = 0L;     // timestamp milisegundos
}
