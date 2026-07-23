package com.montanaritech.contable.bancos.tarjetacredito;

import com.montanaritech.contable.bancos.tarjetacredito.dto.PagoTarjetaCrearRequest;
import com.montanaritech.contable.bancos.tarjetacredito.dto.PagoTarjetaResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Pago del resumen de tarjeta (F5.4 §3, PL-4/PL-5). */
@RestController
@RequestMapping("/api/v1/pagos-tarjeta")
@RequiredArgsConstructor
@Tag(name = "PagoTarjeta")
public class PagoTarjetaController {

    private final PagoTarjetaService service;
    private final PagoTarjetaMapper mapper;

    @GetMapping
    public Page<PagoTarjetaResponse> listar(@RequestParam Long tarjetaCreditoId, Pageable p) {
        return service.listar(tarjetaCreditoId, p).map(mapper::aResponse);
    }

    @GetMapping("/{id}")
    public PagoTarjetaResponse obtener(@PathVariable Long id) {
        return mapper.aResponse(service.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public PagoTarjetaResponse crear(@Valid @RequestBody PagoTarjetaCrearRequest req) {
        return mapper.aResponse(service.crearBorrador(req));
    }

    @PatchMapping("/{id}/confirmar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public PagoTarjetaResponse confirmar(@PathVariable Long id) {
        return mapper.aResponse(service.confirmar(id));
    }

    @PatchMapping("/{id}/anular")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public PagoTarjetaResponse anular(@PathVariable Long id, @RequestBody MotivoRequest req) {
        return mapper.aResponse(service.anular(id, req.motivo()));
    }

    public record MotivoRequest(String motivo) {}
}
