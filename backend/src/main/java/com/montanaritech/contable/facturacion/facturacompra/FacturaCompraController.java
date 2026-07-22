package com.montanaritech.contable.facturacion.facturacompra;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.facturacion.facturacompra.dto.FacturaCompraAnularRequest;
import com.montanaritech.contable.facturacion.facturacompra.dto.FacturaCompraCrearRequest;
import com.montanaritech.contable.facturacion.facturacompra.dto.FacturaCompraEditarRequest;
import com.montanaritech.contable.facturacion.facturacompra.dto.FacturaCompraResponse;
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
@RequestMapping("/api/v1/facturas-compra")
@RequiredArgsConstructor
@Tag(name = "FacturaCompra")
public class FacturaCompraController {

    private final FacturaCompraService service;
    private final FacturaCompraMapper mapper;

    @GetMapping
    public Page<FacturaCompraResponse> listar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) EstadoDocumento estado,
            @RequestParam(required = false) Long proveedorId,
            @RequestParam(required = false) Long proyectoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            Pageable p) {
        return service.listar(texto, estado, proveedorId, proyectoId, fechaDesde, fechaHasta, p)
                .map(f -> mapper.aResponse(f, service.tributosDe(f.getId())));
    }

    @GetMapping("/{id}")
    public FacturaCompraResponse obtener(@PathVariable Long id) {
        return mapper.aResponse(service.obtener(id), service.tributosDe(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public FacturaCompraResponse crear(@Valid @RequestBody FacturaCompraCrearRequest req) {
        FacturaCompra f = service.crearBorrador(req);
        return mapper.aResponse(f, service.tributosDe(f.getId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public FacturaCompraResponse editar(@PathVariable Long id, @Valid @RequestBody FacturaCompraEditarRequest req) {
        FacturaCompra f = service.editarBorrador(id, req);
        return mapper.aResponse(f, service.tributosDe(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminarBorrador(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/confirmar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public FacturaCompraResponse confirmar(@PathVariable Long id) {
        FacturaCompra f = service.confirmar(id);
        return mapper.aResponse(f, service.tributosDe(id));
    }

    @PatchMapping("/{id}/anular")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public FacturaCompraResponse anular(@PathVariable Long id, @Valid @RequestBody FacturaCompraAnularRequest req) {
        FacturaCompra f = service.anular(id, req.motivo());
        return mapper.aResponse(f, service.tributosDe(id));
    }
}
