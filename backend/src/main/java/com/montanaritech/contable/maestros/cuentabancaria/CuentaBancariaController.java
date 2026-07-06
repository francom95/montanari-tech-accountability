package com.montanaritech.contable.maestros.cuentabancaria;

import com.montanaritech.contable.maestros.cuentabancaria.dto.CuentaBancariaCrearRequest;
import com.montanaritech.contable.maestros.cuentabancaria.dto.CuentaBancariaEditarRequest;
import com.montanaritech.contable.maestros.cuentabancaria.dto.CuentaBancariaResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cuentas-bancarias")
@RequiredArgsConstructor
@Tag(name = "CuentaBancaria")
public class CuentaBancariaController {
    private final CuentaBancariaService service;
    private final CuentaBancariaMapper mapper;

    @GetMapping
    public Page<CuentaBancariaResponse> listar(@RequestParam(required = false) String texto, @RequestParam(required = false) Boolean activo, Pageable p) {
        return service.listar(texto, activo, p).map(mapper::aResponse);
    }

    @GetMapping("/{id}")
    public CuentaBancariaResponse obtener(@PathVariable Long id) {
        return mapper.aResponse(service.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public CuentaBancariaResponse crear(@Valid @RequestBody CuentaBancariaCrearRequest req) {
        return mapper.aResponse(service.crear(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public CuentaBancariaResponse editar(@PathVariable Long id, @Valid @RequestBody CuentaBancariaEditarRequest req) {
        return mapper.aResponse(service.editar(id, req));
    }

    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public CuentaBancariaResponse activar(@PathVariable Long id) {
        return mapper.aResponse(service.activar(id));
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public CuentaBancariaResponse desactivar(@PathVariable Long id) {
        return mapper.aResponse(service.desactivar(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
