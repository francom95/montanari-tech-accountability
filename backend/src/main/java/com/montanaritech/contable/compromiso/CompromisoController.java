package com.montanaritech.contable.compromiso;

import com.montanaritech.contable.compromiso.dto.CompromisoCrearRequest;
import com.montanaritech.contable.compromiso.dto.CompromisoEditarRequest;
import com.montanaritech.contable.compromiso.dto.CompromisoResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Compromisos de pago futuro (F8.2). */
@RestController
@RequestMapping("/api/v1/compromisos")
@RequiredArgsConstructor
@Tag(name = "Compromiso")
public class CompromisoController {

    private final CompromisoService service;
    private final CompromisoMapper mapper;

    @GetMapping
    public Page<CompromisoResponse> listar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) EstadoCompromiso estado,
            @RequestParam(required = false) Boolean activo,
            Pageable p) {
        return service.listar(texto, estado, activo, p).map(mapper::aResponse);
    }

    @GetMapping("/por-rango-de-fechas")
    public List<CompromisoResponse> porRangoDeFechas(
            @RequestParam LocalDate desde, @RequestParam LocalDate hasta) {
        return service.porRangoDeFechas(desde, hasta).stream().map(mapper::aResponse).toList();
    }

    @GetMapping("/{id}")
    public CompromisoResponse obtener(@PathVariable Long id) {
        return mapper.aResponse(service.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public CompromisoResponse crear(@Valid @RequestBody CompromisoCrearRequest req) {
        return mapper.aResponse(service.crear(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public CompromisoResponse editar(@PathVariable Long id, @Valid @RequestBody CompromisoEditarRequest req) {
        return mapper.aResponse(service.editar(id, req));
    }

    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public CompromisoResponse activar(@PathVariable Long id) {
        return mapper.aResponse(service.activar(id));
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public CompromisoResponse desactivar(@PathVariable Long id) {
        return mapper.aResponse(service.desactivar(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
