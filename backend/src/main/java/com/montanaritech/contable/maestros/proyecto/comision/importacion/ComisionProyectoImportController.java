package com.montanaritech.contable.maestros.proyecto.comision.importacion;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** F10.2: carga histórica única desde la hoja "Comisiones por ventas", sin UI (decisión del equipo). */
@RestController
@RequestMapping("/api/v1/importacion/comisiones")
@RequiredArgsConstructor
@Tag(name = "Importación F10.2")
public class ComisionProyectoImportController {
    private final ComisionProyectoImportService service;

    @PostMapping("/previsualizar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public List<ComisionProyectoImportFilaDto> previsualizar(@RequestParam("archivo") MultipartFile archivo) {
        return service.previsualizar(archivo);
    }

    @PostMapping("/confirmar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ComisionProyectoImportResultado confirmar(
            @RequestParam Long monedaIdArs, @RequestParam Long monedaIdUsd,
            @RequestBody List<ComisionProyectoImportFilaDto> filas) {
        return service.confirmar(monedaIdArs, monedaIdUsd, filas);
    }
}
