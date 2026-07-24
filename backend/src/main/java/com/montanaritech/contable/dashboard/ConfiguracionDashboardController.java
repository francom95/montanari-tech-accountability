package com.montanaritech.contable.dashboard;

import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.dashboard.dto.ConfiguracionDashboardDtos.Request;
import com.montanaritech.contable.dashboard.dto.ConfiguracionDashboardDtos.Response;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Parámetros del dashboard (F7.5). Lectura para cualquier rol autenticado;
 * escritura solo ADMINISTRADOR — mismo criterio que
 * {@code ConfiguracionPresupuestoController}. Al guardar se limpia el caché
 * del dashboard para que el próximo pedido refleje los nuevos parámetros
 * sin esperar el TTL.
 */
@RestController
@RequestMapping("/api/v1/dashboard/configuracion")
@RequiredArgsConstructor
@Tag(name = "ConfiguracionDashboard")
public class ConfiguracionDashboardController {

    private final ConfiguracionDashboardRepository repo;
    private final CacheManager cacheManager;

    @GetMapping
    @Transactional(readOnly = true)
    public Response obtener() {
        return aResponse(obtenerConfiguracion());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public Response actualizar(@Valid @RequestBody Request req) {
        ConfiguracionDashboard config = obtenerConfiguracion();
        config.setDiaVencimientoIva(req.diaVencimientoIva());
        config.setDiaVencimientoIibb(req.diaVencimientoIibb());
        config.setVentanaObligacionesDias(req.ventanaObligacionesDias());
        Response resp = aResponse(repo.save(config));
        var cache = cacheManager.getCache("dashboard");
        if (cache != null) {
            cache.clear();
        }
        return resp;
    }

    private ConfiguracionDashboard obtenerConfiguracion() {
        return repo.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new RecursoNoEncontradoException("Configuración de dashboard no encontrada"));
    }

    private Response aResponse(ConfiguracionDashboard c) {
        return new Response(c.getDiaVencimientoIva(), c.getDiaVencimientoIibb(), c.getVentanaObligacionesDias());
    }
}
