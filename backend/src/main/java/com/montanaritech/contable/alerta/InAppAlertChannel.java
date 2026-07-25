package com.montanaritech.contable.alerta;

import org.springframework.stereotype.Component;

/**
 * El canal in-app no notifica nada por sí mismo: existir en la tabla
 * {@code alerta} y no tener {@link AlertaLectura} para un usuario YA ES la
 * notificación in-app (la lee el badge/panel del frontend). No-op real,
 * documentado a propósito para que quede claro que no falta código acá.
 */
@Component
public class InAppAlertChannel implements AlertChannel {

    @Override
    public void notificar(Alerta alerta) {
        // Sin efecto: la persistencia de la alerta es la notificación in-app.
    }
}
