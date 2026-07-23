package com.montanaritech.contable.impuestos.atribucion;

import com.montanaritech.contable.impuestos.atribucion.dto.AtribucionImpuestoDtos.AtribucionResponse;
import com.montanaritech.contable.impuestos.atribucion.dto.AtribucionImpuestoDtos.CalcularRequest;
import com.montanaritech.contable.impuestos.atribucion.dto.AtribucionImpuestoDtos.ConfiguracionRequest;
import com.montanaritech.contable.impuestos.atribucion.dto.AtribucionImpuestoDtos.ConfiguracionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/impuestos/atribuciones")
@RequiredArgsConstructor
public class AtribucionImpuestoController {

    private final AtribucionImpuestoService service;
    private final AtribucionImpuestoMapper mapper;

    /** Criterio de prorrateo por defecto del sistema. */
    @GetMapping("/configuracion")
    public ConfiguracionResponse configuracion() {
        return new ConfiguracionResponse(service.criterioPorDefecto());
    }

    @PutMapping("/configuracion")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ConfiguracionResponse actualizarConfiguracion(@Valid @RequestBody ConfiguracionRequest req) {
        return new ConfiguracionResponse(service.actualizarCriterioPorDefecto(req.criterioPorDefecto()));
    }

    /** Atribución guardada de la liquidación; si no hay, una previsualización con el criterio por defecto. */
    @GetMapping("/{liquidacionTipo}/{liquidacionId}")
    public AtribucionResponse obtener(@PathVariable TipoLiquidacion liquidacionTipo,
                                      @PathVariable Long liquidacionId) {
        return mapper.aResponse(service.obtener(liquidacionTipo, liquidacionId));
    }

    /** Calcula una distribución con un criterio dado, sin persistir (para ver antes de guardar). */
    @PostMapping("/{liquidacionTipo}/{liquidacionId}/previsualizar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public AtribucionResponse previsualizar(@PathVariable TipoLiquidacion liquidacionTipo,
                                            @PathVariable Long liquidacionId,
                                            @Valid @RequestBody CalcularRequest req) {
        return mapper.aResponse(service.previsualizar(liquidacionTipo, liquidacionId, req));
    }

    @PostMapping("/{liquidacionTipo}/{liquidacionId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public AtribucionResponse guardar(@PathVariable TipoLiquidacion liquidacionTipo,
                                      @PathVariable Long liquidacionId,
                                      @Valid @RequestBody CalcularRequest req) {
        return mapper.aResponse(service.guardar(liquidacionTipo, liquidacionId, req));
    }
}
