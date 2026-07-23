package com.montanaritech.contable.contabilidad.estadoresultados;

import com.montanaritech.contable.contabilidad.estadoresultados.dto.MapeoRubroLineaErDtos.CrearRequest;
import com.montanaritech.contable.contabilidad.estadoresultados.dto.MapeoRubroLineaErDtos.EditarRequest;
import com.montanaritech.contable.contabilidad.estadoresultados.dto.MapeoRubroLineaErDtos.Response;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mapeo rubro→línea del estado de resultados (F7.3): lectura para cualquier
 * rol autenticado (el motor de cálculo lo consulta); escritura solo
 * ADMINISTRADOR, mismo criterio que {@code MapeoCuentaController}.
 */
@RestController
@RequestMapping("/api/v1/mapeos-rubro-linea-er")
@RequiredArgsConstructor
@Tag(name = "MapeoRubroLineaEstadoResultados")
public class MapeoRubroLineaEstadoResultadosController {

    private final MapeoRubroLineaEstadoResultadosService service;
    private final MapeoRubroLineaEstadoResultadosMapper mapper;

    @GetMapping
    public List<Response> listar() {
        return service.listar().stream().map(mapper::aResponse).toList();
    }

    @GetMapping("/{id}")
    public Response obtener(@PathVariable Long id) {
        return mapper.aResponse(service.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Response crear(@Valid @RequestBody CrearRequest req) {
        return mapper.aResponse(service.crear(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Response editar(@PathVariable Long id, @Valid @RequestBody EditarRequest req) {
        return mapper.aResponse(service.editar(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
