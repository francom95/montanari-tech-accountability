package com.montanaritech.contable.common.audit;

/** Acciones auditables (F1.1 §4). La lista es cerrada a propósito. */
public enum AccionAuditoria {
    CREAR,
    EDITAR,
    ELIMINAR,
    CONFIRMAR,
    ANULAR,
    DUPLICAR,
    CERRAR_PERIODO,
    REABRIR_PERIODO,
    IMPORTAR,
    LOGIN,
    /** F11.2 A13: antes ningún intento de login fallido quedaba registrado. */
    LOGIN_FALLIDO,
    CAMBIO_ESTADO,
    EXPORTAR_SENSIBLE
}
