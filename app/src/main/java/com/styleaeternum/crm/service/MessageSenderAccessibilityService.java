package com.styleaeternum.crm.service;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

/**
 * Servicio de accesibilidad que detecta cuando WhatsApp está en primer plano
 * con un chat abierto y pulsa el botón Enviar automáticamente.
 *
 * Comunicación con CampaignService mediante Broadcasts:
 *   - Recibe: ACTION_SEND_MESSAGE (con el texto a escribir)
 *   - Envía:  ACTION_MESSAGE_SENT / ACTION_MESSAGE_FAILED
 */
public class MessageSenderAccessibilityService extends AccessibilityService {

    private static final String TAG = "MsgSenderA11y";

    public static final String ACTION_SEND_MESSAGE  = "com.styleaeternum.crm.SEND_MESSAGE";
    public static final String ACTION_MESSAGE_SENT  = "com.styleaeternum.crm.MESSAGE_SENT";
    public static final String ACTION_MESSAGE_FAILED = "com.styleaeternum.crm.MESSAGE_FAILED";
    public static final String EXTRA_MESSAGE_TEXT   = "msg_text";
    public static final String EXTRA_PHONE          = "phone";

    // Instancia estática para que CampaignService pueda verificar si está activo
    private static MessageSenderAccessibilityService instance;

    private String pendingMessage = null;
    private boolean waitingForChat = false;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Log.i(TAG, "MessageSenderAccessibilityService iniciado");
    }

    public static boolean isRunning() {
        return instance != null;
    }

    /**
     * CampaignService llama a esto para pedir el envío del siguiente mensaje.
     * El mensaje ya viene con las variaciones spintax aplicadas.
     */
    public static void requestSend(String messageText) {
        if (instance == null) {
            Log.e(TAG, "Servicio de accesibilidad no activo");
            return;
        }
        instance.pendingMessage = messageText;
        instance.waitingForChat = true;
        Log.d(TAG, "Mensaje pendiente registrado, esperando WhatsApp...");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!waitingForChat || pendingMessage == null) return;

        String pkg = event.getPackageName() != null ? event.getPackageName().toString() : "";
        boolean isWhatsApp = "com.whatsapp".equals(pkg) || "com.whatsapp.w4b".equals(pkg);
        if (!isWhatsApp) return;

        int eventType = event.getEventType();
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {

            // Intentar escribir el mensaje y pulsar enviar
            if (tryWriteAndSend(pendingMessage)) {
                pendingMessage = null;
                waitingForChat = false;
                broadcastResult(true);
            }
        }
    }

    private boolean tryWriteAndSend(String message) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;

        // 1. Buscamos directamente el botón Enviar.
        // Como mandamos el texto vía el Intent whatsapp://send?text=...
        // WhatsApp se encarga de rellenar el campo y cambiar el icono del micrófono al de Enviar.
        AccessibilityNodeInfo sendBtn = findSendButton(root);
        if (sendBtn == null) {
            // El botón de enviar no está visible aún (puede que esté cargando o el número sea inválido)
            root.recycle();
            return false;
        }

        boolean sent = sendBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        root.recycle();

        if (sent) {
            Log.i(TAG, "Mensaje enviado correctamente vía accesibilidad");
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            performGlobalAction(GLOBAL_ACTION_BACK);
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            performGlobalAction(GLOBAL_ACTION_BACK); // Dos veces para asegurar salir del chat a veces
        } else {
            Log.w(TAG, "No se pudo pulsar el botón Enviar");
        }
        return sent;
    }

    /**
     * Busca el campo de texto del chat de WhatsApp.
     * WhatsApp usa distintos IDs según la versión, así que buscamos por múltiples criterios.
     */
    private AccessibilityNodeInfo findInputField(AccessibilityNodeInfo root) {
        // Buscar por IDs conocidos de WhatsApp
        List<AccessibilityNodeInfo> nodes;

        nodes = root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/entry");
        if (nodes != null && !nodes.isEmpty()) return nodes.get(0);

        nodes = root.findAccessibilityNodeInfosByViewId("com.whatsapp.w4b:id/entry");
        if (nodes != null && !nodes.isEmpty()) return nodes.get(0);

        // Buscar por clase EditText como fallback
        return findByClassName(root, "android.widget.EditText");
    }

    /**
     * Busca el botón de enviar (ícono flecha/avión en WhatsApp).
     */
    private AccessibilityNodeInfo findSendButton(AccessibilityNodeInfo root) {
        List<AccessibilityNodeInfo> nodes;

        nodes = root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/send");
        if (nodes != null && !nodes.isEmpty()) return nodes.get(0);

        nodes = root.findAccessibilityNodeInfosByViewId("com.whatsapp.w4b:id/send");
        if (nodes != null && !nodes.isEmpty()) return nodes.get(0);

        // Buscar por descripción de contenido
        nodes = root.findAccessibilityNodeInfosByText("Enviar");
        if (nodes != null && !nodes.isEmpty()) return nodes.get(0);

        nodes = root.findAccessibilityNodeInfosByText("Send");
        if (nodes != null && !nodes.isEmpty()) return nodes.get(0);

        return null;
    }

    private AccessibilityNodeInfo findByClassName(AccessibilityNodeInfo node, String className) {
        if (node == null) return null;
        if (className.equals(node.getClassName() != null ? node.getClassName().toString() : "")) {
            if (node.isEditable()) return node;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo result = findByClassName(node.getChild(i), className);
            if (result != null) return result;
        }
        return null;
    }

    private void broadcastResult(boolean success) {
        Intent intent = new Intent(success ? ACTION_MESSAGE_SENT : ACTION_MESSAGE_FAILED);
        sendBroadcast(intent);
        Log.d(TAG, "Broadcast enviado: " + (success ? "SENT" : "FAILED"));
    }

    @Override
    public void onInterrupt() {
        Log.w(TAG, "Servicio de accesibilidad interrumpido");
    }

    @Override
    public void onDestroy() {
        instance = null;
        super.onDestroy();
    }
}
