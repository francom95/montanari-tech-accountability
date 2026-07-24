package com.montanaritech.contable.maestros.proyecto.presupuesto;

import com.montanaritech.contable.maestros.proyecto.presupuesto.dto.PresupuestoProyectoDtos.GuardarRequest;
import com.montanaritech.contable.maestros.proyecto.presupuesto.dto.PresupuestoProyectoDtos.Response;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Presupuesto estimado por proyecto (F2.6): editor en vivo, sin ciclo de
 * estados. Lectura para cualquier rol autenticado; escritura para
 * ADMINISTRADOR/CARGA (carga de datos del proyecto, igual criterio que la
 * edición de cuotas/etapas del propio proyecto).
 */
@RestController
@RequestMapping("/api/v1/proyectos/{proyectoId}/presupuesto")
@RequiredArgsConstructor
@Tag(name = "PresupuestoProyecto")
public class PresupuestoProyectoController {

    private final PresupuestoProyectoService service;
    private final PresupuestoProyectoMapper mapper;

    @GetMapping
    public ResponseEntity<Response> obtener(@PathVariable Long proyectoId) {
        return service.obtener(proyectoId)
                .map(p -> ResponseEntity.ok(mapper.aResponse(p, service.calcular(p))))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public Response guardar(@PathVariable Long proyectoId, @Valid @RequestBody GuardarRequest req) {
        PresupuestoProyecto presupuesto = service.guardar(proyectoId, req);
        return mapper.aResponse(presupuesto, service.calcular(presupuesto));
    }
}
