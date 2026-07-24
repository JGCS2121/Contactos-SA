package com.styleaeternum.crm.util;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Motor de variación de mensajes (Spintax).
 *
 * Formato: {opción1|opción2|opción3}
 * Ejemplo: "¡{Hola|Hey|Buenas}! Mira nuestras {ofertas|novedades} {🔥|✨}"
 * Cada llamada a spin() genera una combinación aleatoria diferente.
 *
 * Soporta grupos anidados y múltiples grupos en el mismo mensaje.
 */
public class SpintaxEngine {

    private static final Pattern GROUP_PATTERN = Pattern.compile("\\{([^{}]+)\\}");
    private static final Random RANDOM = new Random();

    /**
     * Genera una variación aleatoria del mensaje con formato spintax.
     * @param template Mensaje con grupos {op1|op2|op3}
     * @return Mensaje con una variación aleatoria seleccionada
     */
    public static String spin(String template) {
        if (template == null || template.isEmpty()) return template;

        String result = template;
        Matcher matcher = GROUP_PATTERN.matcher(result);

        // Procesar grupos repetidamente hasta que no haya más
        while (matcher.find()) {
            String group = matcher.group(1); // contenido sin llaves
            String[] options = group.split("\\|");
            String chosen = options[RANDOM.nextInt(options.length)].trim();
            result = result.substring(0, matcher.start())
                    + chosen
                    + result.substring(matcher.end());
            // Resetear el matcher con el nuevo string
            matcher = GROUP_PATTERN.matcher(result);
        }

        return result;
    }

    /**
     * Cuenta cuántas combinaciones únicas puede generar el template.
     * Útil para mostrar al usuario "Este mensaje generará ~X variaciones".
     * @param template Mensaje spintax
     * @return Número de combinaciones posibles (producto de todas las opciones)
     */
    public static long countCombinations(String template) {
        if (template == null || template.isEmpty()) return 1;
        long total = 1;
        Matcher matcher = GROUP_PATTERN.matcher(template);
        while (matcher.find()) {
            String[] options = matcher.group(1).split("\\|");
            total *= options.length;
        }
        return total;
    }

    /**
     * Comprueba si el template tiene al menos un grupo de variación.
     */
    public static boolean hasVariations(String template) {
        if (template == null) return false;
        return GROUP_PATTERN.matcher(template).find();
    }

    /**
     * Genera N variaciones diferentes del mensaje (para vista previa).
     */
    public static String[] preview(String template, int count) {
        String[] results = new String[count];
        for (int i = 0; i < count; i++) {
            results[i] = spin(template);
        }
        return results;
    }
}
