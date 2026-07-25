package com.montanaritech.contable.alerta;

/**
 * Canal de notificación de una alerta recién creada (F9.1). Hoy solo existe
 * {@link InAppAlertChannel}; el día que exista un canal de email, este
 * servicio pasa a inyectar {@code List<AlertChannel>} e iterarlos — la firma
 * ya está lista para eso, no hace falta tocar {@code MotorAlertasService}.
 */
public interface AlertChannel {

    void notificar(Alerta alerta);
}
