package com.montanaritech.contable.alerta;

import com.montanaritech.contable.alerta.dto.ConfiguracionAlertasDtos.Request;
import com.montanaritech.contable.alerta.dto.ConfiguracionAlertasDtos.Response;
import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Parámetros del motor de alertas (F9.1). Lectura para cualquier rol autenticado; escritura solo ADMINISTRADOR. */
@RestController
@RequestMapping("/api/v1/configuracion-alertas")
@RequiredArgsConstructor
@Tag(name = "ConfiguracionAlertas")
public class ConfiguracionAlertasController {

    private final ConfiguracionAlertasRepository repo;
    private final AuditoriaService auditoria;

    @GetMapping
    @Transactional(readOnly = true)
    public Response obtener() {
        return aResponse(obtenerConfiguracion());
    }

    /** F11.2 A12: esta escritura de configuración no dejaba rastro de auditoría. */
    @PutMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public Response actualizar(@Valid @RequestBody Request req) {
        ConfiguracionAlertas config = obtenerConfiguracion();
        Response antes = aResponse(config);
        config.setDiasAnticipacion(req.diasAnticipacion());
        config.setDiasAtrasoCxc(req.diasAtrasoCxc());
        Response despues = aResponse(repo.save(config));
        auditoria.registrar(AccionAuditoria.EDITAR, "ConfiguracionAlertas", config.getId(), antes, despues);
        return despues;
    }

    private ConfiguracionAlertas obtenerConfiguracion() {
        return repo.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new RecursoNoEncontradoException("Configuración de alertas no encontrada"));
    }

    private Response aResponse(ConfiguracionAlertas c) {
        return new Response(c.getDiasAnticipacion(), c.getDiasAtrasoCxc());
    }
}
