package com.montanaritech.contable.common.audit;

import com.montanaritech.contable.common.audit.dto.AuditoriaLogResponse;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Pantalla de consulta de auditoría (F1.6): solo administrador. */
@RestController
@RequestMapping("/api/v1/auditoria")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AuditoriaController {

    private final AuditoriaLogRepository auditoriaLogRepository;
    private final AuditoriaLogMapper auditoriaLogMapper;

    @GetMapping
    public Page<AuditoriaLogResponse> buscar(
            @RequestParam(required = false) String entidadTipo,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) AccionAuditoria accion,
            @RequestParam(required = false) Instant desde,
            @RequestParam(required = false) Instant hasta,
            @PageableDefault(size = 50, sort = "fechaHora", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return auditoriaLogRepository.buscar(entidadTipo, usuarioId, accion, desde, hasta, pageable)
                .map(auditoriaLogMapper::aResponse);
    }
}
