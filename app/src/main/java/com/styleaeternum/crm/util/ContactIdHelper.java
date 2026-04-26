package com.styleaeternum.crm.util;

import java.util.Calendar;
import java.util.Locale;

/**
 * Genera IDs automáticos con formato: abril2026_001
 */
public class ContactIdHelper {

    private static final String[] MONTHS_ES = {
        "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
    };

    /** Devuelve el grupo actual: ej "abril2026" */
    public static String getCurrentGroup() {
        Calendar cal   = Calendar.getInstance();
        int month      = cal.get(Calendar.MONTH);      // 0-based
        int year       = cal.get(Calendar.YEAR);
        return MONTHS_ES[month] + year;
    }

    /** Construye el ID: abril2026_001 */
    public static String buildId(String group, int seq) {
        return group + "_" + String.format(Locale.US, "%03d", seq);
    }
}
