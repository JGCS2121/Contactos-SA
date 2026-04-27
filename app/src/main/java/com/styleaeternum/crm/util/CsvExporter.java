package com.styleaeternum.crm.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.styleaeternum.crm.data.CapturedContact;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CsvExporter {

    private static final String TAG = "CsvExporter";
    
    // Columnas exactas de Google Contacts
    private static final String CSV_HEADER =
        "Name,Given Name,Additional Name,Family Name,Yomi Name,Given Name Yomi,Additional Name Yomi,Family Name Yomi,Name Prefix,Name Suffix,Initials,Nickname,Short Name,Maiden Name,Birthday,Gender,Location,Billing Information,Directory Server,Mileage,Occupation,Hobby,Sensitivity,Priority,Subject,Notes,Language,Photo,Group Membership,Phone 1 - Type,Phone 1 - Value,Custom Field 1 - Type,Custom Field 1 - Value,Custom Field 2 - Type,Custom Field 2 - Value\n";

    public static void export(Context context, List<CapturedContact> contacts) {
        String timestamp = new SimpleDateFormat("MMM_yyyy", Locale.getDefault()).format(new Date());
        String fileName = "StyleAeternum_" + timestamp + ".csv";

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = context.getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
                if (uri != null) {
                    try (OutputStream os = resolver.openOutputStream(uri);
                         OutputStreamWriter writer = new OutputStreamWriter(os)) {
                        writeCsvData(writer, contacts);
                        Toast.makeText(context, "Guardado en Descargas: " + fileName, Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!dir.exists()) dir.mkdirs();
                File outFile = new File(dir, fileName);
                try (FileWriter writer = new FileWriter(outFile)) {
                    writeCsvData(writer, contacts);
                    Toast.makeText(context, "Guardado en Descargas: " + fileName, Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error exportando CSV", e);
            Toast.makeText(context, "Error exportando: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private static void writeCsvData(OutputStreamWriter writer, List<CapturedContact> contacts) throws IOException {
        writer.write(CSV_HEADER);
        for (CapturedContact c : contacts) {
            StringBuilder sb = new StringBuilder();
            // Name, Given Name, Additional Name, Family Name
            sb.append(escapeCsv(c.name)).append(",").append(escapeCsv(c.name)).append(",,,");
            // Yomi... hasta Location (13 comas)
            sb.append(",,,,,,,,,,,,,");
            // Billing... hasta Notes (8 comas)
            sb.append(",,,,,,,,").append(escapeCsv(c.notes)).append(",");
            // Language, Photo, Group Membership
            sb.append(",,").append(escapeCsv(c.groupMembership)).append(",");
            // Phone 1 Type, Value
            sb.append("WhatsApp Business,").append(escapeCsv(c.phone)).append(",");
            // Custom Fields (4 comas al final)
            sb.append(",,,\n");
            writer.write(sb.toString());
        }
        writer.flush();
    }

    private static String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
