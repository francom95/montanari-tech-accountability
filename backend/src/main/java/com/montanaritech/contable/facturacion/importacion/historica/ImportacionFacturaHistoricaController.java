package com.montanaritech.contable.facturacion.importacion.historica;

import com.montanaritech.contable.facturacion.importacion.dto.FilaImportacionConfirmarRequest;
import com.montanaritech.contable.facturacion.importacion.dto.FilaImportacionResultadoResponse;
import com.montanaritech.contable.facturacion.importacion.historica.dto.FilaImportacionHistoricaPreviewResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * F10.3: reconstrucción histórica de facturas de venta/compra reales sobre
 * el importador de F4.6 (ver Javadoc de {@code ImportacionFacturaHistoricaService}).
 */
@RestController
@RequestMapping("/api/v1/importacion-facturas/historica")
@RequiredArgsConstructor
@Tag(name = "ImportacionFacturaHistorica")
public class ImportacionFacturaHistoricaController {

    private final ImportacionFacturaHistoricaService service;

    @PostMapping(value = "/pdf/previsualizar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<FilaImportacionHistoricaPreviewResponse> previsualizar(@RequestParam("archivos") List<MultipartFile> archivos) {
        return archivos.stream()
                .map(archivo -> service.previsualizar(archivo.getOriginalFilename(), leerBytes(archivo)))
                .toList();
    }

    @PostMapping("/confirmar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<FilaImportacionResultadoResponse> confirmar(@Valid @RequestBody List<@Valid FilaImportacionConfirmarRequest> filas) {
        return service.confirmar(filas);
    }

    private byte[] leerBytes(MultipartFile archivo) {
        try {
            return archivo.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo " + archivo.getOriginalFilename(), e);
        }
    }
}
