package com.montanaritech.contable.common.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declarativo para el caso simple: operaciones donde solo importa el
 * resultado (CREAR, IMPORTAR, LOGIN, EXPORTAR_SENSIBLE — no tienen un
 * "antes" significativo). {@link AuditoriaAspect} registra el retorno del
 * método como "después" y resuelve el {@code entidadId} vía {@code getId()}
 * por reflexión.
 *
 * <p>Para EDITAR/ELIMINAR/ANULAR/CONFIRMAR/CAMBIO_ESTADO, donde el "antes"
 * importa para el diff, seguí llamando a {@link AuditoriaService#registrar}
 * a mano desde el service (ya tenés la entidad cargada antes de mutarla —
 * intentar automatizar ese caso con AOP es más frágil que una línea explícita).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Auditado {

    AccionAuditoria accion();

    String entidadTipo();
}
