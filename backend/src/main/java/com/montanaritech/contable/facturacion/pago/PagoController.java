package com.montanaritech.contable.facturacion.pago;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.facturacion.pago.dto.AplicarAnticipoProveedorRequest;
import com.montanaritech.contable.facturacion.pago.dto.PagoAnularRequest;
import com.montanaritech.contable.facturacion.pago.dto.PagoCrearRequest;
import com.montanaritech.contable.facturacion.pago.dto.PagoEditarRequest;
import com.montanaritech.contable.facturacion.pago.dto.PagoResponse;
import com.montanaritech.contable.facturacion.pago.dto.SaldoFacturaCompraResponse;
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
@RequestMapping("/api/v1/pagos")
@RequiredArgsConstructor
@Tag(name = "Pago")
public class PagoController {

    private final PagoService service;
    private final PagoMapper mapper;

    @GetMapping
    public Page<PagoResponse> listar(
            @RequestParam(required = false) EstadoDocumento estado,
            @RequestParam(required = false) Long proveedorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            Pageable p) {
        return service.listar(estado, proveedorId, fechaDesde, fechaHasta, p).map(this::aResponse);
    }

    @GetMapping("/{id}")
    public PagoResponse obtener(@PathVariable Long id) {
        return aResponse(service.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public PagoResponse crear(@Valid @RequestBody PagoCrearRequest req) {
        return aResponse(service.crearBorrador(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public PagoResponse editar(@PathVariable Long id, @Valid @RequestBody PagoEditarRequest req) {
        return aResponse(service.editarBorrador(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminarBorrador(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/confirmar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public PagoResponse confirmar(@PathVariable Long id) {
        return aResponse(service.confirmar(id));
    }

    @PatchMapping("/{id}/anular")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public PagoResponse anular(@PathVariable Long id, @Valid @RequestBody PagoAnularRequest req) {
        return aResponse(service.anular(id, req.motivo()));
    }

    @PostMapping("/{id}/aplicar-anticipo")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public PagoResponse aplicarAnticipo(@PathVariable Long id, @Valid @RequestBody AplicarAnticipoProveedorRequest req) {
        service.aplicarAnticipo(id, req);
        return aResponse(service.obtener(id));
    }

    @GetMapping("/saldo-compra/{facturaCompraId}")
    public SaldoFacturaCompraResponse saldoCompra(@PathVariable Long facturaCompraId) {
        return service.saldoFacturaCompra(facturaCompraId);
    }

    private PagoResponse aResponse(Pago p) {
        return mapper.aResponse(p, service.aplicacionesDe(p.getId()), service.montoAnticipoDisponible(p));
    }
}
