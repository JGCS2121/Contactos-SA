package com.styleaeternum.crm.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "agenda",
        foreignKeys = @ForeignKey(
                entity = CapturedContact.class,
                parentColumns = "id",
                childColumns = "contactoId",
                onDelete = ForeignKey.SET_NULL
        ))
public class Agenda {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String contactoId; // Referencia a contacts.id (String ej: abril2026_001)
    public String nombreCliente;
    public String telefono;
    public String descripcion;
    public long fechaHora;
    public long recordatorio1; // Día anterior
    public long recordatorio2; // 2 horas antes
    public String estado = "pendiente"; // 'pendiente', 'entregado', 'vencido', 'cancelado'
    public String notas;
    public long fechaCreacion;
}
