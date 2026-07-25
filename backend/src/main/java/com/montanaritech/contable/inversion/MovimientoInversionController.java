package com.montanaritech.contable.inversion;

import com.montanaritech.contable.inversion.dto.MovimientoInversionCrearRequest;
import com.montanaritech.contable.inversion.dto.MovimientoInversionResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Movimientos (suscripción/rescate) de una Inversion (F8.4). */
@RestController
@RequestMapping("/api/v1/movimientos-inversion")
@RequiredArgsConstructor
@Tag(name = "MovimientoInversion")
public class MovimientoInversionController {

    private final MovimientoInversionService service;

    @GetMapping
    public Page<MovimientoInversionResponse> listar(@RequestParam Long inversionId, Pageable p) {
        return service.listar(inversionId, p).map(service::aResponse);
    }

    @GetMapping("/{id}")
    public MovimientoInversionResponse obtener(@PathVariable Long id) {
        return service.aResponse(service.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public MovimientoInversionResponse crear(@Valid @RequestBody MovimientoInversionCrearRequest req) {
        return service.aResponse(service.crear(req));
    }
}
