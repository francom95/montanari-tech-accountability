package com.montanaritech.contable.periodo;

import com.montanaritech.contable.periodo.dto.PeriodoDtos.GenerarAutomaticosResponse;
import com.montanaritech.contable.periodo.dto.PeriodoDtos.MotivoRequest;
import com.montanaritech.contable.periodo.dto.PeriodoDtos.PeriodoResponse;
import com.montanaritech.contable.periodo.dto.PeriodoDtos.PeriodoResumenResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Gestión de períodos contables (F9.3): pantalla admin-only por diseño (el cierre es un control interno, no una consulta). */
@RestController
@RequestMapping("/api/v1/periodos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
@Tag(name = "Periodo")
public class PeriodoController {

    private final PeriodoService service;
    private final PeriodoMapper mapper;

    @GetMapping
    public Page<PeriodoResponse> listar(@RequestParam(required = false) EstadoPeriodo estado, Pageable p) {
        return service.listar(estado, p).map(mapper::aResponse);
    }

    @GetMapping("/{id}")
    public PeriodoResponse obtener(@PathVariable Long id) {
        return mapper.aResponse(service.obtener(id));
    }

    @GetMapping("/{id}/resumen")
    public PeriodoResumenResponse resumen(@PathVariable Long id) {
        return service.resumen(id);
    }

    @PostMapping("/generar-automaticos")
    public GenerarAutomaticosResponse generarAutomaticos() {
        return new GenerarAutomaticosResponse(service.generarAutomaticos());
    }

    @PatchMapping("/{id}/cerrar")
    public PeriodoResponse cerrar(@PathVariable Long id, @Valid @RequestBody MotivoRequest req) {
        return mapper.aResponse(service.cerrar(id, req.motivo()));
    }

    @PatchMapping("/{id}/marcar-en-revision")
    public PeriodoResponse marcarEnRevision(@PathVariable Long id) {
        return mapper.aResponse(service.marcarEnRevision(id));
    }

    @PatchMapping("/{id}/reabrir")
    public PeriodoResponse reabrir(@PathVariable Long id, @Valid @RequestBody MotivoRequest req) {
        return mapper.aResponse(service.reabrir(id, req.motivo()));
    }
}
