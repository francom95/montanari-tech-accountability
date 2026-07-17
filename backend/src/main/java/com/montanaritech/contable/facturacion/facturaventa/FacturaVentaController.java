package com.montanaritech.contable.facturacion.facturaventa;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.facturacion.facturaventa.dto.FacturaVentaAnularRequest;
import com.montanaritech.contable.facturacion.facturaventa.dto.FacturaVentaCrearRequest;
import com.montanaritech.contable.facturacion.facturaventa.dto.FacturaVentaEditarRequest;
import com.montanaritech.contable.facturacion.facturaventa.dto.FacturaVentaResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/facturas-venta")
@RequiredArgsConstructor
@Tag(name = "FacturaVenta")
public class FacturaVentaController {

    private final FacturaVentaService service;
    private final FacturaVentaMapper mapper;

    @GetMapping
    public Page<FacturaVentaResponse> listar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) EstadoDocumento estado,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Long proyectoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            Pageable p) {
        return service.listar(texto, estado, clienteId, proyectoId, fechaDesde, fechaHasta, p).map(mapper::aResponse);
    }

    @GetMapping("/{id}")
    public FacturaVentaResponse obtener(@PathVariable Long id) {
        return mapper.aResponse(service.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public FacturaVentaResponse crear(@Valid @RequestBody FacturaVentaCrearRequest req) {
        return mapper.aResponse(service.crearBorrador(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public FacturaVentaResponse editar(@PathVariable Long id, @Valid @RequestBody FacturaVentaEditarRequest req) {
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
    public FacturaVentaResponse confirmar(@PathVariable Long id) {
        return mapper.aResponse(service.confirmar(id));
    }

    @PatchMapping("/{id}/anular")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public FacturaVentaResponse anular(@PathVariable Long id, @Valid @RequestBody FacturaVentaAnularRequest req) {
        return mapper.aResponse(service.anular(id, req.motivo()));
    }
}
