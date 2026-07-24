package com.montanaritech.contable.vencimientos;

import com.montanaritech.contable.vencimientos.dto.GenerarAutomaticosResponse;
import com.montanaritech.contable.vencimientos.dto.VencimientoCrearRequest;
import com.montanaritech.contable.vencimientos.dto.VencimientoEditarRequest;
import com.montanaritech.contable.vencimientos.dto.VencimientoResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Calendario de vencimientos (F8.1). */
@RestController
@RequestMapping("/api/v1/vencimientos")
@RequiredArgsConstructor
@Tag(name = "Vencimiento")
public class VencimientoController {

    private final VencimientoService service;
    private final VencimientoMapper mapper;

    @GetMapping
    public Page<VencimientoResponse> listar(
            @RequestParam(required = false) TipoVencimiento tipo,
            @RequestParam(required = false) EstadoVencimientoObligacion estado,
            @RequestParam(required = false) LocalDate fechaDesde,
            @RequestParam(required = false) LocalDate fechaHasta,
            @RequestParam(required = false) Long proyectoId,
            @RequestParam(required = false) Long proveedorId,
            @RequestParam(required = false) Long tarjetaId,
            Pageable p) {
        return service.listar(tipo, estado, fechaDesde, fechaHasta, proyectoId, proveedorId, tarjetaId, p)
                .map(mapper::aResponse);
    }

    @GetMapping("/proximos")
    public List<VencimientoResponse> proximos(@RequestParam(defaultValue = "15") int dias) {
        return service.proximos(dias).stream().map(mapper::aResponse).toList();
    }

    @GetMapping("/{id}")
    public VencimientoResponse obtener(@PathVariable Long id) {
        return mapper.aResponse(service.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public VencimientoResponse crear(@Valid @RequestBody VencimientoCrearRequest req) {
        return mapper.aResponse(service.crear(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public VencimientoResponse editar(@PathVariable Long id, @Valid @RequestBody VencimientoEditarRequest req) {
        return mapper.aResponse(service.editar(id, req));
    }

    @PatchMapping("/{id}/marcar-pagado")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public VencimientoResponse marcarPagado(@PathVariable Long id, @RequestParam(required = false) Long asientoId) {
        return mapper.aResponse(service.marcarPagado(id, asientoId));
    }

    @PatchMapping("/{id}/reprogramar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public VencimientoResponse reprogramar(@PathVariable Long id, @RequestBody ReprogramarRequest req) {
        return mapper.aResponse(service.reprogramar(id, req.nuevaFecha(), req.motivo()));
    }

    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public VencimientoResponse cancelar(@PathVariable Long id, @RequestBody CancelarRequest req) {
        return mapper.aResponse(service.cancelar(id, req.motivo()));
    }

    @PostMapping("/generar-automaticos")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public GenerarAutomaticosResponse generarAutomaticos() {
        return service.generarAutomaticos();
    }

    public record ReprogramarRequest(LocalDate nuevaFecha, String motivo) {}

    public record CancelarRequest(String motivo) {}
}
