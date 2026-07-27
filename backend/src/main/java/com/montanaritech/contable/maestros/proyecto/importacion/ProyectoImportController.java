package com.montanaritech.contable.maestros.proyecto.importacion;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** F10.2: carga histórica única desde la hoja "Clientes", sin UI (decisión del equipo). */
@RestController
@RequestMapping("/api/v1/importacion/proyectos")
@RequiredArgsConstructor
@Tag(name = "Importación F10.2")
public class ProyectoImportController {
    private final ProyectoImportService service;

    @PostMapping("/previsualizar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public List<ProyectoImportFilaDto> previsualizar(@RequestParam("archivo") MultipartFile archivo) {
        return service.previsualizar(archivo);
    }

    @PostMapping("/confirmar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ProyectoImportResultado confirmar(
            @RequestParam Long monedaIdUsd, @RequestBody List<ProyectoImportFilaDto> filas) {
        return service.confirmar(monedaIdUsd, filas);
    }
}
