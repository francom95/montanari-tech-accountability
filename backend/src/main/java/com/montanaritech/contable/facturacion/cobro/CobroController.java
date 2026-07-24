package com.montanaritech.contable.facturacion.cobro;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.facturacion.cobro.dto.AplicarAnticipoRequest;
import com.montanaritech.contable.facturacion.cobro.dto.CobroAnularRequest;
import com.montanaritech.contable.facturacion.cobro.dto.CobroCrearRequest;
import com.montanaritech.contable.facturacion.cobro.dto.CobroEditarRequest;
import com.montanaritech.contable.facturacion.cobro.dto.CobroResponse;
import com.montanaritech.contable.facturacion.cobro.dto.ConfiguracionCobranzaDtos;
import com.montanaritech.contable.facturacion.cobro.dto.SaldoFacturaResponse;
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
@RequestMapping("/api/v1/cobros")
@RequiredArgsConstructor
@Tag(name = "Cobro")
public class CobroController {

    private final CobroService service;
    private final CobroMapper mapper;
    private final ConfiguracionCobranzaRepository configuracionCobranzaRepo;

    @GetMapping("/configuracion-cobranza")
    public ConfiguracionCobranzaDtos.Response obtenerConfiguracionCobranza() {
        ConfiguracionCobranza c = configuracionCobranzaRepo.findFirstByOrderByIdAsc().orElseGet(ConfiguracionCobranza::new);
        return new ConfiguracionCobranzaDtos.Response(c.getDiasGraciaMora(), c.getTasaMoraDiariaPorcentaje());
    }

    @PutMapping("/configuracion-cobranza")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ConfiguracionCobranzaDtos.Response actualizarConfiguracionCobranza(@Valid @RequestBody ConfiguracionCobranzaDtos.Request req) {
        ConfiguracionCobranza c = configuracionCobranzaRepo.findFirstByOrderByIdAsc().orElseGet(ConfiguracionCobranza::new);
        c.setDiasGraciaMora(req.diasGraciaMora());
        c.setTasaMoraDiariaPorcentaje(req.tasaMoraDiariaPorcentaje());
        configuracionCobranzaRepo.save(c);
        return new ConfiguracionCobranzaDtos.Response(c.getDiasGraciaMora(), c.getTasaMoraDiariaPorcentaje());
    }

    @GetMapping
    public Page<CobroResponse> listar(
            @RequestParam(required = false) EstadoDocumento estado,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            Pageable p) {
        return service.listar(estado, clienteId, fechaDesde, fechaHasta, p).map(this::aResponse);
    }

    @GetMapping("/{id}")
    public CobroResponse obtener(@PathVariable Long id) {
        return aResponse(service.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public CobroResponse crear(@Valid @RequestBody CobroCrearRequest req) {
        return aResponse(service.crearBorrador(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public CobroResponse editar(@PathVariable Long id, @Valid @RequestBody CobroEditarRequest req) {
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
    public CobroResponse confirmar(@PathVariable Long id) {
        return aResponse(service.confirmar(id));
    }

    @PatchMapping("/{id}/anular")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public CobroResponse anular(@PathVariable Long id, @Valid @RequestBody CobroAnularRequest req) {
        return aResponse(service.anular(id, req.motivo()));
    }

    @PostMapping("/{id}/aplicar-anticipo")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public CobroResponse aplicarAnticipo(@PathVariable Long id, @Valid @RequestBody AplicarAnticipoRequest req) {
        service.aplicarAnticipo(id, req);
        return aResponse(service.obtener(id));
    }

    @GetMapping("/saldo-venta/{facturaVentaId}")
    public SaldoFacturaResponse saldoVenta(@PathVariable Long facturaVentaId) {
        return service.saldoFacturaVenta(facturaVentaId);
    }

    private CobroResponse aResponse(Cobro c) {
        return mapper.aResponse(c, service.tributosDe(c.getId()), service.aplicacionesDe(c.getId()), service.montoAnticipoDisponible(c));
    }
}
