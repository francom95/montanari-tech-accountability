package com.montanaritech.contable.maestros.proyecto.comision;

import com.montanaritech.contable.maestros.proyecto.comision.dto.ComisionProyectoResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta transversal de comisiones devengadas/pendientes por proyecto y
 * por período (F2.7): punto de extensión para que CxP (F4), rentabilidad
 * por proyecto (F7.4) y flujo de caja proyectado (F8.3) reutilicen esta
 * misma consulta en vez de reimplementar el filtro.
 */
@RestController
@RequestMapping("/api/v1/comisiones")
@RequiredArgsConstructor
@Tag(name = "ComisionConsulta")
public class ComisionConsultaController {
    private final ComisionProyectoService service;
    private final ComisionProyectoMapper mapper;

    @GetMapping
    public Page<ComisionProyectoResponse> consultar(
            @RequestParam(required = false) Long proyectoId,
            @RequestParam(required = false) Long comisionistaId,
            @RequestParam(required = false) String estadoPago,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Pageable p) {
        return service.consultar(proyectoId, comisionistaId, estadoPago, desde, hasta, p).map(mapper::aResponse);
    }
}
