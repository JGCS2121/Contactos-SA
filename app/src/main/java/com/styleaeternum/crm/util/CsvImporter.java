package com.styleaeternum.crm.util;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.styleaeternum.crm.data.CapturedContact;
import com.styleaeternum.crm.data.ContactRepository;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class CsvImporter {

    private static final String TAG = "CsvImporter";

    public static void importCsv(Context context, Uri uri, ContactRepository repository) {
        new Thread(() -> {
            int imported = 0;
            try (InputStream is = context.getContentResolver().openInputStream(uri);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

                String line;
                boolean firstLine = true;
                
                int nameIndex = -1;
                int phoneIndex = -1;
                
                while ((line = reader.readLine()) != null) {
                    // Manejo básico de comillas
                    String[] tokens = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                    
                    if (firstLine) {
                        for (int i = 0; i < tokens.length; i++) {
                            String header = tokens[i].replace("\"", "").trim();
                            if (header.equalsIgnoreCase("Name")) nameIndex = i;
                            if (header.equalsIgnoreCase("Phone 1 - Value")) phoneIndex = i;
                        }
                        firstLine = false;
                        if (nameIndex == -1 || phoneIndex == -1) {
                            showToast(context, "Formato incorrecto. Faltan columnas Name o Phone 1 - Value");
                            return;
                        }
                        continue;
                    }
                    
                    if (tokens.length <= Math.max(nameIndex, phoneIndex)) continue;
                    
                    String rawName = tokens[nameIndex].replace("\"", "").trim();
                    String rawPhone = tokens[phoneIndex].replace("\"", "").trim();
                    
                    String phone = rawPhone.replaceAll("[^0-9]", "");
                    if (phone.isEmpty()) continue;
                    
                    // Comprobar si existe
                    if (repository.getByPhone(phone) != null) continue;
                    
                    String group = detectGroupFromName(rawName);
                    
                    CapturedContact c = new CapturedContact();
                    c.id = rawName.replace(" ", "_").toLowerCase();
                    c.name = rawName;
                    c.phone = phone;
                    c.groupMembership = group;
                    c.capturedAt = System.currentTimeMillis();
                    
                    repository.insert(c);
                    imported++;
                }
                
                showToast(context, "Importados " + imported + " contactos nuevos del CSV");
                
            } catch (Exception e) {
                Log.e(TAG, "Error importando CSV", e);
                showToast(context, "Error al importar: " + e.getMessage());
            }
        }).start();
    }
    
    private static void showToast(Context context, String msg) {
        if (context instanceof android.app.Activity) {
            ((android.app.Activity) context).runOnUiThread(() -> 
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            );
        }
    }
    
    private static String detectGroupFromName(String name) {
        // "Febrero 25 001" -> "febrero2025"
        String[] parts = name.split(" ");
        if (parts.length >= 2) {
            String month = parts[0].toLowerCase();
            String yearPart = parts[1];
            if (yearPart.length() == 2) yearPart = "20" + yearPart;
            return month + yearPart;
        }
        return "importados";
    }
}
