package com.montanaritech.contable.maestros.tipocambio;
import com.montanaritech.contable.maestros.tipocambio.dto.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/tipo-cambios")
@RequiredArgsConstructor
@Tag(name = "TipoCambio")
public class TipoCambioController {
    private final TipoCambioService service;
    private final TipoCambioMapper mapper;
    private final ConfiguracionTipoCambioRepository configuracionRepo;

    @GetMapping("/configuracion")
    public ConfiguracionTipoCambioDtos.Response obtenerConfiguracion() {
        String criterio = configuracionRepo.findFirstByOrderByIdAsc()
                .map(ConfiguracionTipoCambio::getCriterioPorDefecto)
                .orElse(null);
        return new ConfiguracionTipoCambioDtos.Response(criterio);
    }

    @PutMapping("/configuracion")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ConfiguracionTipoCambioDtos.Response actualizarConfiguracion(@RequestBody ConfiguracionTipoCambioDtos.Request req) {
        ConfiguracionTipoCambio config = configuracionRepo.findFirstByOrderByIdAsc().orElseGet(ConfiguracionTipoCambio::new);
        config.setCriterioPorDefecto(req.criterioPorDefecto());
        configuracionRepo.save(config);
        return new ConfiguracionTipoCambioDtos.Response(config.getCriterioPorDefecto());
    }

    @GetMapping
    public Page<TipoCambioResponse> listar(@RequestParam(required = false) String texto, @RequestParam(required = false) Boolean activo, Pageable p) {
        return service.listar(texto, activo, p).map(mapper::aResponse);
    }
    @GetMapping("/{id}")
    public TipoCambioResponse obtener(@PathVariable Long id) {
        return mapper.aResponse(service.obtener(id));
    }
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public TipoCambioResponse crear(@Valid @RequestBody TipoCambioCrearRequest req) {
        return mapper.aResponse(service.crear(req));
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public TipoCambioResponse editar(@PathVariable Long id, @Valid @RequestBody TipoCambioEditarRequest req) {
        return mapper.aResponse(service.editar(id, req));
    }
    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public TipoCambioResponse activar(@PathVariable Long id) {
        return mapper.aResponse(service.activar(id));
    }
    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public TipoCambioResponse desactivar(@PathVariable Long id) {
        return mapper.aResponse(service.desactivar(id));
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
