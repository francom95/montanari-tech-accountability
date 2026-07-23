package com.montanaritech.contable.bancos.tarjetacredito;

import com.montanaritech.contable.bancos.tarjetacredito.dto.ClasificarConsumoRequest;
import com.montanaritech.contable.bancos.tarjetacredito.dto.ConsumoTarjetaResponse;
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

/** Consumos de tarjeta (F5.4): clasificación manual y masiva por reglas. */
@RestController
@RequestMapping("/api/v1/consumos-tarjeta")
@RequiredArgsConstructor
@Tag(name = "ConsumoTarjeta")
public class ConsumoTarjetaController {

    private final ConsumoTarjetaService service;
    private final ConsumoTarjetaMapper mapper;

    @GetMapping
    public Page<ConsumoTarjetaResponse> listar(
            @RequestParam Long tarjetaCreditoId,
            @RequestParam(defaultValue = "false") boolean soloSinClasificar,
            Pageable p) {
        return service.listar(tarjetaCreditoId, soloSinClasificar, p).map(mapper::aResponse);
    }

    @GetMapping("/{id}")
    public ConsumoTarjetaResponse obtener(@PathVariable Long id) {
        return mapper.aResponse(service.obtener(id));
    }

    @GetMapping("/sin-clasificar/contador")
    public long contarSinClasificar(@RequestParam Long tarjetaCreditoId) {
        return service.contarSinClasificar(tarjetaCreditoId);
    }

    @PatchMapping("/{id}/clasificar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ConsumoTarjetaResponse clasificar(@PathVariable Long id, @Valid @RequestBody ClasificarConsumoRequest req) {
        return mapper.aResponse(service.clasificar(id, req));
    }

    @PostMapping("/clasificar-masivamente")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public int clasificarMasivamente(@RequestParam Long tarjetaCreditoId) {
        return service.clasificarMasivamente(tarjetaCreditoId);
    }
}
