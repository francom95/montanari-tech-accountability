package com.montanaritech.contable.common.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * API única para dejar rastro de auditoría (F1.1 §4 / ADR-14): sincrónica,
 * en la misma transacción que la operación que audita, invocada
 * explícitamente (por {@link Auditado} en el caso simple de "solo importa
 * el resultado", o directamente desde el service cuando hace falta un
 * "antes" real).
 */
@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final AuditoriaLogRepository auditoriaLogRepository;
    private final ObjectMapper objectMapper;

    public void registrar(AccionAuditoria accion, String entidadTipo, Long entidadId, Object antes, Object despues) {
        registrar(accion, entidadTipo, entidadId, antes, despues, false, null);
    }

    public void registrar(
            AccionAuditoria accion,
            String entidadTipo,
            Long entidadId,
            Object antes,
            Object despues,
            boolean sobrePeriodoCerrado,
            String detalle
    ) {
        registrarComo(usuarioActualId(), accion, entidadTipo, entidadId, antes, despues, sobrePeriodoCerrado, detalle);
    }

    /**
     * Para el único caso donde el actor no sale del {@code SecurityContext}:
     * el login exitoso ocurre antes de que exista una autenticación JWT en
     * el request (el endpoint es público), pero el usuario que se logueó
     * ya se conoce en el momento de auditar.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarComo(
            Long usuarioId,
            AccionAuditoria accion,
            String entidadTipo,
            Long entidadId,
            Object antes,
            Object despues,
            boolean sobrePeriodoCerrado,
            String detalle
    ) {
        AuditoriaLog log = new AuditoriaLog();
        log.setAccion(accion);
        log.setEntidadTipo(entidadTipo);
        log.setEntidadId(entidadId);
        log.setUsuarioId(usuarioId);
        log.setDatosAntes(aJson(antes));
        log.setDatosDespues(aJson(despues));
        log.setSobrePeriodoCerrado(sobrePeriodoCerrado);
        log.setDetalle(detalle);
        auditoriaLogRepository.save(log);
    }

    private Long usuarioActualId() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !autenticacion.isAuthenticated()) {
            return null;
        }
        try {
            return Long.valueOf(autenticacion.getName());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String aJson(Object valor) {
        if (valor == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(valor);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el valor para auditoría", e);
        }
    }
}
