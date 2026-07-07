package com.montanaritech.contable.maestros.proyecto.etapa;

import com.montanaritech.contable.maestros.proyecto.etapa.dto.EtapaCrearRequest;
import com.montanaritech.contable.maestros.proyecto.etapa.dto.EtapaEditarRequest;
import com.montanaritech.contable.maestros.proyecto.etapa.dto.EtapaResponse;
import com.montanaritech.contable.maestros.proyecto.etapa.importacion.EtapaImportFilaDto;
import com.montanaritech.contable.maestros.proyecto.etapa.importacion.EtapaImportResultado;
import com.montanaritech.contable.maestros.proyecto.etapa.importacion.EtapaImportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/proyectos/{proyectoId}/etapas")
@RequiredArgsConstructor
@Tag(name = "Etapa")
public class EtapaController {
    private final EtapaService service;
    private final EtapaMapper mapper;
    private final EtapaImportService importService;

    @GetMapping
    public Page<EtapaResponse> listar(
            @PathVariable Long proyectoId,
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) Boolean activo,
            Pageable p) {
        return service.listar(proyectoId, texto, activo, p).map(mapper::aResponse);
    }

    @GetMapping("/{id}")
    public EtapaResponse obtener(@PathVariable Long proyectoId, @PathVariable Long id) {
        return mapper.aResponse(service.obtener(proyectoId, id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public EtapaResponse crear(@PathVariable Long proyectoId, @Valid @RequestBody EtapaCrearRequest req) {
        return mapper.aResponse(service.crear(proyectoId, req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public EtapaResponse editar(@PathVariable Long proyectoId, @PathVariable Long id, @Valid @RequestBody EtapaEditarRequest req) {
        return mapper.aResponse(service.editar(proyectoId, id, req));
    }

    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public EtapaResponse activar(@PathVariable Long proyectoId, @PathVariable Long id) {
        return mapper.aResponse(service.activar(proyectoId, id));
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public EtapaResponse desactivar(@PathVariable Long proyectoId, @PathVariable Long id) {
        return mapper.aResponse(service.desactivar(proyectoId, id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ResponseEntity<Void> eliminar(@PathVariable Long proyectoId, @PathVariable Long id) {
        service.eliminar(proyectoId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/importar/previsualizar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public List<EtapaImportFilaDto> previsualizarImportacion(
            @PathVariable Long proyectoId, @RequestParam("archivo") MultipartFile archivo) {
        return importService.previsualizar(archivo);
    }

    @PostMapping("/importar/confirmar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public EtapaImportResultado confirmarImportacion(
            @PathVariable Long proyectoId, @RequestBody List<EtapaImportFilaDto> filas) {
        return importService.confirmar(proyectoId, filas);
    }
}
