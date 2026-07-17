package com.montanaritech.contable.common.adjunto;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Adjuntos genéricos (F1.1 §6.5). {@code entidadTipo}/{@code entidadId}
 * identifican la entidad dueña (p. ej. "FacturaVenta"/123) sin acoplar este
 * módulo a ningún dominio concreto.
 */
@RestController
@RequestMapping("/api/v1/adjuntos")
@RequiredArgsConstructor
@Tag(name = "Adjunto")
public class AdjuntoController {

    private final AdjuntoService service;

    @GetMapping
    public List<AdjuntoResponse> listar(@RequestParam String entidadTipo, @RequestParam Long entidadId) {
        return service.listar(entidadTipo, entidadId).stream().map(AdjuntoResponse::de).toList();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public AdjuntoResponse subir(
            @RequestParam String entidadTipo,
            @RequestParam Long entidadId,
            @RequestParam("archivo") MultipartFile archivo) {
        return AdjuntoResponse.de(service.subir(entidadTipo, entidadId, archivo));
    }

    @GetMapping("/{id}/descargar")
    public ResponseEntity<Resource> descargar(@PathVariable Long id) {
        Adjunto adjunto = service.obtener(id);
        Resource recurso = service.descargar(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + adjunto.getNombreArchivo() + "\"")
                .contentType(MediaType.parseMediaType(adjunto.getMime()))
                .body(recurso);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    public record AdjuntoResponse(Long id, String entidadTipo, Long entidadId, String nombreArchivo, String mime, long tamanio) {
        static AdjuntoResponse de(Adjunto a) {
            return new AdjuntoResponse(a.getId(), a.getEntidadTipo(), a.getEntidadId(), a.getNombreArchivo(), a.getMime(), a.getTamanio());
        }
    }
}
