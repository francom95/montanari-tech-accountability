package com.montanaritech.contable.contabilidad.asiento.importacion;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** F10.2: carga histórica única del Libro Diario, sin UI (decisión del equipo). */
@RestController
@RequestMapping("/api/v1/importacion/asientos")
@RequiredArgsConstructor
@Tag(name = "Importación F10.2")
public class AsientoImportController {
    private final AsientoImportService service;

    @PostMapping("/previsualizar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public List<AsientoImportFilaDto> previsualizar(@RequestParam("archivo") MultipartFile archivo) {
        return service.previsualizar(archivo);
    }

    @PostMapping("/confirmar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public AsientoImportResultado confirmar(
            @RequestParam Long monedaIdArs, @RequestBody List<AsientoImportFilaDto> filas) {
        return service.confirmar(monedaIdArs, filas);
    }
}
