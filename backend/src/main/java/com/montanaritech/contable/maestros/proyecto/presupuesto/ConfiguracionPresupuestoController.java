package com.montanaritech.contable.maestros.proyecto.presupuesto;

import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.maestros.proyecto.presupuesto.dto.ConfiguracionPresupuestoDtos.Request;
import com.montanaritech.contable.maestros.proyecto.presupuesto.dto.ConfiguracionPresupuestoDtos.Response;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Alícuotas y esquema de comisión bancaria COMEX del presupuesto de proyecto
 * (F2.6). Lectura para cualquier rol autenticado (el motor de cálculo la
 * consulta); escritura solo ADMINISTRADOR — mismo criterio que {@code
 * AtribucionImpuestoController}/{@code MapeoCuentaController}.
 */
@RestController
@RequestMapping("/api/v1/presupuestos/configuracion")
@RequiredArgsConstructor
@Tag(name = "ConfiguracionPresupuesto")
public class ConfiguracionPresupuestoController {

    private final ConfiguracionPresupuestoRepository repo;

    @GetMapping
    @Transactional(readOnly = true)
    public Response obtener() {
        return aResponse(obtenerConfiguracion());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public Response actualizar(@Valid @RequestBody Request req) {
        ConfiguracionPresupuesto config = obtenerConfiguracion();
        config.setComisionVentaPorcentaje(req.comisionVentaPorcentaje());
        config.setColchonImpuestoGananciasPorcentaje(req.colchonImpuestoGananciasPorcentaje());
        config.setIibbConvenioMultilateralPorcentaje(req.iibbConvenioMultilateralPorcentaje());
        config.setImpuestoDebitosCreditosPorcentaje(req.impuestoDebitosCreditosPorcentaje());
        config.setIvaPorcentaje(req.ivaPorcentaje());
        config.setDiferenciaDolarComercializacionPorcentaje(req.diferenciaDolarComercializacionPorcentaje());
        config.setPercepcionIvaComexPorcentaje(req.percepcionIvaComexPorcentaje());
        config.setIibbSircrebComexPorcentaje(req.iibbSircrebComexPorcentaje());
        config.setComexUmbralUnoUsd(req.comexUmbralUnoUsd());
        config.setComexMontoUnoUsd(req.comexMontoUnoUsd());
        config.setComexUmbralDosUsd(req.comexUmbralDosUsd());
        config.setComexMontoDosUsd(req.comexMontoDosUsd());
        config.setComexUmbralTresUsd(req.comexUmbralTresUsd());
        config.setComexMontoTresUsd(req.comexMontoTresUsd());
        config.setComexPorcentajeExcedente(req.comexPorcentajeExcedente());
        return aResponse(repo.save(config));
    }

    private ConfiguracionPresupuesto obtenerConfiguracion() {
        return repo.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new RecursoNoEncontradoException("Configuración de presupuesto no encontrada"));
    }

    private Response aResponse(ConfiguracionPresupuesto c) {
        return new Response(
                c.getComisionVentaPorcentaje(), c.getColchonImpuestoGananciasPorcentaje(),
                c.getIibbConvenioMultilateralPorcentaje(), c.getImpuestoDebitosCreditosPorcentaje(), c.getIvaPorcentaje(),
                c.getDiferenciaDolarComercializacionPorcentaje(), c.getPercepcionIvaComexPorcentaje(),
                c.getIibbSircrebComexPorcentaje(), c.getComexUmbralUnoUsd(), c.getComexMontoUnoUsd(),
                c.getComexUmbralDosUsd(), c.getComexMontoDosUsd(), c.getComexUmbralTresUsd(), c.getComexMontoTresUsd(),
                c.getComexPorcentajeExcedente());
    }
}
