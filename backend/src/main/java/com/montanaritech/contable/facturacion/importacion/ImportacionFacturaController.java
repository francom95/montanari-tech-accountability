package com.montanaritech.contable.facturacion.importacion;

import com.montanaritech.contable.common.reporte.ReportExportService;
import com.montanaritech.contable.facturacion.importacion.dto.FilaImportacionConfirmarRequest;
import com.montanaritech.contable.facturacion.importacion.dto.FilaImportacionPreviewResponse;
import com.montanaritech.contable.facturacion.importacion.dto.FilaImportacionResultadoResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/** Importador de facturas históricas desde PDF (F4.6, molde PL-4/PL-5 no aplica: es un asistente de carga, no un generador de asientos propio). */
@RestController
@RequestMapping("/api/v1/importacion-facturas")
@RequiredArgsConstructor
@Tag(name = "ImportacionFactura")
public class ImportacionFacturaController {

    private static final List<String> COLUMNAS_RECHAZOS = List.of(
            "Archivo", "Tipo", "Número", "Motivo de rechazo");

    private final ImportacionFacturaService service;
    private final ReportExportService reportExportService;

    @PostMapping(value = "/pdf/previsualizar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public List<FilaImportacionPreviewResponse> previsualizar(@RequestParam("archivos") List<MultipartFile> archivos) {
        return archivos.stream()
                .map(archivo -> service.previsualizar(archivo.getOriginalFilename(), leerBytes(archivo)))
                .toList();
    }

    @PostMapping("/confirmar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public List<FilaImportacionResultadoResponse> confirmar(@Valid @RequestBody List<@Valid FilaImportacionConfirmarRequest> filas) {
        return service.confirmar(filas);
    }

    @PostMapping("/rechazos/exportar/excel")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ResponseEntity<StreamingResponseBody> exportarRechazos(@RequestBody List<FilaImportacionResultadoResponse> rechazos) {
        List<List<Object>> filas = rechazos.stream()
                .<List<Object>>map(r -> List.of(r.nombreArchivo(), r.tipo(), r.numero(), r.motivoRechazo() == null ? "" : r.motivoRechazo()))
                .toList();
        StreamingResponseBody cuerpo = out -> reportExportService.exportarExcel("Rechazos de importación", COLUMNAS_RECHAZOS, filas, out);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"rechazos-importacion.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(cuerpo);
    }

    private byte[] leerBytes(MultipartFile archivo) {
        try {
            return archivo.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo " + archivo.getOriginalFilename(), e);
        }
    }
}
