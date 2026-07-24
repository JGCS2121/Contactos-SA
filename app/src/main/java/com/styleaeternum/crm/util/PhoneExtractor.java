package com.styleaeternum.crm.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extrae números de teléfono (internacionales y locales) de cadenas de texto.
 */
public class PhoneExtractor {

    // Detecta: +34 612 345 678 | +34612345678 | 612345678 | 34612345678
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "(?:\\+?\\d[\\s\\-.]?){7,15}\\d"
    );

    public static String extract(String text) {
        if (text == null || text.isEmpty()) return null;
        Matcher m = PHONE_PATTERN.matcher(text);
        if (m.find()) {
            // Limpiar espacios/guiones para guardar sólo dígitos + posible +
            String raw = m.group().replaceAll("[\\s\\-.]", "");
            // Solo aceptar números internacionales con prefijo '+'
            if (!raw.startsWith("+")) return null;
            return raw;
        }
        return null;
    }
}
