package com.styleaeternum.crm.util;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.styleaeternum.crm.data.CapturedContact;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Exporta la lista de contactos a un CSV compatible con Google Contacts.
 * Columnas: Name, Phone 1 - Value, Phone 1 - Type, Group Membership, Notes
 */
public class CsvExporter {

    private static final String TAG = "CsvExporter";
    private static final String CSV_HEADER =
        "Name,Phone 1 - Value,Phone 1 - Type,Group Membership,Notes\n";

    /**
     * Escribe el CSV en Downloads/ y devuelve la ruta del archivo.
     * @return File generado, o null si hay error.
     */
    public static File export(Context context, List<CapturedContact> contacts) {
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(new Date());
            String fileName  = "StyleAeternum_CRM_" + timestamp + ".csv";

            File dir;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ : usar directorio de documentos de la app
                dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            } else {
                dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            }
            if (dir != null && !dir.exists()) dir.mkdirs();

            File outFile = new File(dir, fileName);
            FileWriter fw = new FileWriter(outFile);
            fw.write(CSV_HEADER);

            for (CapturedContact c : contacts) {
                fw.write(escapeCsv(c.name) + ","
                        + escapeCsv(c.phone) + ","
                        + escapeCsv(c.phoneType) + ","
                        + escapeCsv(c.groupMembership) + ","
                        + escapeCsv(c.notes) + "\n");
            }
            fw.flush();
            fw.close();

            Log.i(TAG, "CSV exportado: " + outFile.getAbsolutePath());
            return outFile;

        } catch (IOException e) {
            Log.e(TAG, "Error exportando CSV", e);
            return null;
        }
    }

    private static String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
