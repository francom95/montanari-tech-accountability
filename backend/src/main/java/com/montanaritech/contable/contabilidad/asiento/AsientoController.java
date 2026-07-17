package com.montanaritech.contable.contabilidad.asiento;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.contabilidad.asiento.dto.AsientoAnularRequest;
import com.montanaritech.contable.contabilidad.asiento.dto.AsientoCrearRequest;
import com.montanaritech.contable.contabilidad.asiento.dto.AsientoEditarConfirmadoRequest;
import com.montanaritech.contable.contabilidad.asiento.dto.AsientoEditarRequest;
import com.montanaritech.contable.contabilidad.asiento.dto.AsientoResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/asientos")
@RequiredArgsConstructor
@Tag(name = "Asiento")
public class AsientoController {

    private final AsientoService service;
    private final AsientoMapper mapper;

    @GetMapping
    public Page<AsientoResponse> listar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) EstadoDocumento estado,
            @RequestParam(required = false) OrigenAsiento origen,
            @RequestParam(required = false) Long numero,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) Long cuentaContableId,
            @RequestParam(required = false) BigDecimal importe,
            @RequestParam(required = false) Long proyectoId,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Long proveedorId,
            Pageable p) {
        return service.listar(texto, estado, origen, numero, fechaDesde, fechaHasta, cuentaContableId, importe,
                proyectoId, clienteId, proveedorId, p).map(mapper::aResponse);
    }

    @GetMapping("/{id}")
    public AsientoResponse obtener(@PathVariable Long id) {
        return mapper.aResponse(service.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public AsientoResponse crear(@Valid @RequestBody AsientoCrearRequest req) {
        return mapper.aResponse(service.crearBorrador(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public AsientoResponse editar(@PathVariable Long id, @Valid @RequestBody AsientoEditarRequest req) {
        return mapper.aResponse(service.editarBorrador(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminarBorrador(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/confirmar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public AsientoResponse confirmar(@PathVariable Long id) {
        return mapper.aResponse(service.confirmar(id));
    }

    /**
     * Edita un asiento confirmado (F3.5, F3.1 §4.2). La restricción de que
     * solo ADMIN toque líneas {@code generada_auto = true} se valida dentro
     * del service (depende del contenido, no del endpoint).
     */
    @PutMapping("/{id}/confirmado")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public AsientoResponse editarConfirmado(@PathVariable Long id, @Valid @RequestBody AsientoEditarConfirmadoRequest req) {
        return mapper.aResponse(service.editarConfirmado(id, req));
    }

    @PostMapping("/{id}/duplicar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public AsientoResponse duplicar(@PathVariable Long id) {
        return mapper.aResponse(service.duplicar(id));
    }

    @PatchMapping("/{id}/anular")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public AsientoResponse anular(@PathVariable Long id, @Valid @RequestBody AsientoAnularRequest req) {
        return mapper.aResponse(service.anular(id, req.motivo()));
    }
}
