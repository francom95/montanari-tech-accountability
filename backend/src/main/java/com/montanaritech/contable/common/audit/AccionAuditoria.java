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
    CAMBIO_ESTADO,
    EXPORTAR_SENSIBLE
}
