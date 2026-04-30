package com.styleaeternum.crm.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Gestiona el prefijo personalizado del negocio.
 * Ej: "Tienda" → el próximo capturado será "Tienda 001"
 *     "Tienda Lunes" → "Tienda Lunes 001"
 */
public class PrefijosHelper {

    private static final String PREFS_NAME = "crm_config";
    private static final String KEY_PREFIJO = "prefijo_negocio";
    private static final String KEY_CONTADOR = "contador_global";
    private static final String DEFAULT_PREFIJO = "";

    public static String getPrefijo(Context ctx) {
        return getPrefs(ctx).getString(KEY_PREFIJO, DEFAULT_PREFIJO);
    }

    public static void setPrefijo(Context ctx, String prefijo) {
        getPrefs(ctx).edit().putString(KEY_PREFIJO, prefijo.trim()).apply();
    }

    /**
     * Genera el nombre automático para el próximo contacto capturado.
     * Si hay prefijo "Tienda" → "Tienda 001"
     * Si no hay prefijo → usa el ID original "abril2026_001"
     */
    public static String generarNombre(Context ctx, String idFallback, int numero) {
        String prefijo = getPrefijo(ctx).trim();
        if (prefijo.isEmpty()) {
            return idFallback; // sin prefijo: usa el ID clásico
        }
        return prefijo + " " + String.format("%03d", numero);
    }

    /**
     * Obtiene el siguiente número del contador global (independiente del mes).
     * El contador NO se reinicia cuando cambias el prefijo.
     */
    public static int getSiguienteNumero(Context ctx) {
        SharedPreferences prefs = getPrefs(ctx);
        int actual = prefs.getInt(KEY_CONTADOR, 0);
        int siguiente = actual + 1;
        prefs.edit().putInt(KEY_CONTADOR, siguiente).apply();
        return siguiente;
    }

    /**
     * Obtiene el valor actual del contador sin incrementarlo.
     */
    public static int getContadorActual(Context ctx) {
        return getPrefs(ctx).getInt(KEY_CONTADOR, 0);
    }

    /**
     * Establece un valor específico para el contador.
     * Útil para continuar desde un número específico (ej. 480).
     */
    public static void setContador(Context ctx, int valor) {
        getPrefs(ctx).edit().putInt(KEY_CONTADOR, valor).apply();
    }

    /**
     * Reinicia el contador (úsalo si cambias de prefijo y quieres empezar desde 001).
     */
    public static void reiniciarContador(Context ctx) {
        getPrefs(ctx).edit().putInt(KEY_CONTADOR, 0).apply();
    }

    private static SharedPreferences getPrefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
