package com.montanaritech.contable.common.tenant;

/**
 * Tenant activo del hilo actual. Hasta F1.5 (autenticación) no existe JWT del
 * cual leer el claim {@code tenant}, así que se resuelve siempre al tenant
 * por defecto (fila 1, Montanari Tech). F1.5 debe reemplazar la fuente de
 * este valor por el claim del token, sin tocar el resto de {@code common.tenant}.
 */
public final class TenantContext {

    public static final long TENANT_POR_DEFECTO = 1L;

    private static final ThreadLocal<Long> ACTUAL = ThreadLocal.withInitial(() -> TENANT_POR_DEFECTO);

    private TenantContext() {
    }

    public static Long getTenantId() {
        return ACTUAL.get();
    }

    public static void setTenantId(Long tenantId) {
        ACTUAL.set(tenantId != null ? tenantId : TENANT_POR_DEFECTO);
    }

    public static void clear() {
        ACTUAL.remove();
    }
}
