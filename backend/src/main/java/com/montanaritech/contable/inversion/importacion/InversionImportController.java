package com.montanaritech.contable.inversion.importacion;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** F10.2: carga histórica única desde "Inversiones en Fondos Fima", sin UI (decisión del equipo). */
@RestController
@RequestMapping("/api/v1/importacion/inversiones")
@RequiredArgsConstructor
@Tag(name = "Importación F10.2")
public class InversionImportController {
    private final InversionImportService service;

    @PostMapping("/previsualizar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public List<InversionImportFilaDto> previsualizar(@RequestParam("archivo") MultipartFile archivo) {
        return service.previsualizar(archivo);
    }

    @PostMapping("/confirmar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public InversionImportResultado confirmar(
            @RequestParam Long cuentaOrigenId, @RequestBody List<InversionImportFilaDto> filas) {
        return service.confirmar(cuentaOrigenId, filas);
    }
}
