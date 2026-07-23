package com.montanaritech.contable.impuestos.iibb;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.impuestos.iibb.dto.LiquidacionIibbDtos.AgregarComponenteRequest;
import com.montanaritech.contable.impuestos.iibb.dto.LiquidacionIibbDtos.AjustarComponenteRequest;
import com.montanaritech.contable.impuestos.iibb.dto.LiquidacionIibbDtos.AnularRequest;
import com.montanaritech.contable.impuestos.iibb.dto.LiquidacionIibbDtos.CrearRequest;
import com.montanaritech.contable.impuestos.iibb.dto.LiquidacionIibbDtos.EditarJurisdiccionRequest;
import com.montanaritech.contable.impuestos.iibb.dto.LiquidacionIibbDtos.LiquidacionResponse;
import com.montanaritech.contable.impuestos.iibb.dto.LiquidacionIibbDtos.PrevisualizacionResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/impuestos/liquidaciones-iibb")
@RequiredArgsConstructor
public class LiquidacionIibbController {

    private final LiquidacionIibbService service;
    private final LiquidacionIibbMapper mapper;

    @GetMapping
    public Page<LiquidacionResponse> listar(@RequestParam(required = false) Integer anio,
                                            @RequestParam(required = false) EstadoDocumento estado,
                                            @PageableDefault(size = 20) Pageable pageable) {
        return service.listar(anio, estado, pageable).map(l -> mapper.aResponse(l, List.of()));
    }

    @GetMapping("/{id}")
    public LiquidacionResponse obtener(@PathVariable Long id) {
        return mapper.aResponse(service.obtener(id), List.of());
    }

    @GetMapping("/previsualizar")
    public PrevisualizacionResponse previsualizar(@RequestParam int anio, @RequestParam int mes) {
        return mapper.aPrevisualizacion(service.previsualizar(anio, mes));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public LiquidacionResponse crear(@Valid @RequestBody CrearRequest req) {
        return mapper.aResponse(service.crearBorrador(req), List.of());
    }

    @PostMapping("/{id}/recalcular")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public LiquidacionResponse recalcular(@PathVariable Long id) {
        return mapper.aResponse(service.recalcular(id), List.of());
    }

    @PatchMapping("/{id}/jurisdicciones/{jurLiqId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public LiquidacionResponse editarJurisdiccion(@PathVariable Long id, @PathVariable Long jurLiqId,
                                                  @Valid @RequestBody EditarJurisdiccionRequest req) {
        return mapper.aResponse(service.editarJurisdiccion(id, jurLiqId, req), List.of());
    }

    @PatchMapping("/{id}/jurisdicciones/{jurLiqId}/componentes/{componenteId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public LiquidacionResponse ajustarComponente(@PathVariable Long id, @PathVariable Long jurLiqId,
                                                 @PathVariable Long componenteId,
                                                 @Valid @RequestBody AjustarComponenteRequest req) {
        return mapper.aResponse(service.ajustarComponente(id, jurLiqId, componenteId, req), List.of());
    }

    @PostMapping("/{id}/jurisdicciones/{jurLiqId}/componentes")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public LiquidacionResponse agregarComponente(@PathVariable Long id, @PathVariable Long jurLiqId,
                                                 @Valid @RequestBody AgregarComponenteRequest req) {
        return mapper.aResponse(service.agregarComponente(id, jurLiqId, req), List.of());
    }

    @DeleteMapping("/{id}/jurisdicciones/{jurLiqId}/componentes/{componenteId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public LiquidacionResponse eliminarComponente(@PathVariable Long id, @PathVariable Long jurLiqId,
                                                  @PathVariable Long componenteId) {
        return mapper.aResponse(service.eliminarComponente(id, jurLiqId, componenteId), List.of());
    }

    @PatchMapping("/{id}/confirmar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public LiquidacionResponse confirmar(@PathVariable Long id) {
        return mapper.aResponse(service.confirmar(id), List.of());
    }

    @PatchMapping("/{id}/anular")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public LiquidacionResponse anular(@PathVariable Long id, @Valid @RequestBody AnularRequest req) {
        return mapper.aResponse(service.anular(id, req.motivo()), List.of());
    }
}
