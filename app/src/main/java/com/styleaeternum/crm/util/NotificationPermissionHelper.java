package com.styleaeternum.crm.util;

import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;

/**
 * Comprueba si la app tiene permiso de NotificationListenerService activo.
 */
public class NotificationPermissionHelper {

    public static boolean isGranted(Context context) {
        String pkgName  = context.getPackageName();
        String flat     = Settings.Secure.getString(
                context.getContentResolver(),
                "enabled_notification_listeners");
        return flat != null && flat.contains(pkgName);
    }
}
