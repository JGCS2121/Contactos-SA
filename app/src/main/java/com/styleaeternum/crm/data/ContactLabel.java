package com.styleaeternum.crm.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "labels")
public class ContactLabel {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public String colorHex;
    public String prefix; // ej: "Clienta_"
}
