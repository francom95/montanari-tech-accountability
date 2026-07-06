package com.montanaritech.contable.maestros.moneda;

import com.montanaritech.contable.maestros.moneda.dto.MonedaCrearRequest;
import com.montanaritech.contable.maestros.moneda.dto.MonedaEditarRequest;
import com.montanaritech.contable.maestros.moneda.dto.MonedaResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Molde de referencia de PL-1 (F1.8). Lectura para cualquier rol
 * autenticado; escritura (crear/editar/activar/desactivar/eliminar)
 * restringida a ADMINISTRADOR/CARGA — CARGA puede cargar datos, LECTURA
 * solo consulta. Ajustar los roles de escritura por entidad según el
 * funcional (algunos maestros pueden ser admin-only).
 */
@RestController
@RequestMapping("/api/v1/monedas")
@RequiredArgsConstructor
@Tag(name = "Monedas")
public class MonedaController {

    private final MonedaService monedaService;
    private final MonedaMapper monedaMapper;

    @GetMapping
    public Page<MonedaResponse> listar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) Boolean activo,
            Pageable pageable
    ) {
        return monedaService.listar(texto, activo, pageable).map(monedaMapper::aResponse);
    }

    @GetMapping("/{id}")
    public MonedaResponse obtener(@PathVariable Long id) {
        return monedaMapper.aResponse(monedaService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public MonedaResponse crear(@Valid @RequestBody MonedaCrearRequest request) {
        return monedaMapper.aResponse(monedaService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public MonedaResponse editar(@PathVariable Long id, @Valid @RequestBody MonedaEditarRequest request) {
        return monedaMapper.aResponse(monedaService.editar(id, request));
    }

    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public MonedaResponse activar(@PathVariable Long id) {
        return monedaMapper.aResponse(monedaService.activar(id));
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public MonedaResponse desactivar(@PathVariable Long id) {
        return monedaMapper.aResponse(monedaService.desactivar(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        monedaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
