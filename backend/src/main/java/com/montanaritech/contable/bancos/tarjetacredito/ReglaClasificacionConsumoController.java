package com.montanaritech.contable.bancos.tarjetacredito;

import com.montanaritech.contable.bancos.tarjetacredito.dto.ReglaClasificacionCrearRequest;
import com.montanaritech.contable.bancos.tarjetacredito.dto.ReglaClasificacionResponse;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Reglas de clasificación masiva de consumos de tarjeta (F5.4 §2). */
@RestController
@RequestMapping("/api/v1/reglas-clasificacion-consumo")
@RequiredArgsConstructor
@Tag(name = "ReglaClasificacionConsumo")
public class ReglaClasificacionConsumoController {

    private final ReglaClasificacionConsumoService service;
    private final ReglaClasificacionMapper mapper;

    @GetMapping
    public Page<ReglaClasificacionResponse> listar(@RequestParam(required = false) Boolean activo, Pageable p) {
        return service.listar(activo, p).map(mapper::aResponse);
    }

    @GetMapping("/{id}")
    public ReglaClasificacionResponse obtener(@PathVariable Long id) {
        return mapper.aResponse(service.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ReglaClasificacionResponse crear(@Valid @RequestBody ReglaClasificacionCrearRequest req) {
        return mapper.aResponse(service.crear(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ReglaClasificacionResponse editar(@PathVariable Long id, @Valid @RequestBody ReglaClasificacionCrearRequest req) {
        return mapper.aResponse(service.editar(id, req));
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ReglaClasificacionResponse desactivar(@PathVariable Long id) {
        return mapper.aResponse(service.desactivar(id));
    }

    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ReglaClasificacionResponse activar(@PathVariable Long id) {
        return mapper.aResponse(service.activar(id));
    }
}
