package com.montanaritech.contable.compromiso.importacion;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** F10.2: carga histórica única desde "Presupuesto de Pagos" §1, sin UI (decisión del equipo). */
@RestController
@RequestMapping("/api/v1/importacion/compromisos")
@RequiredArgsConstructor
@Tag(name = "Importación F10.2")
public class CompromisoImportController {
    private final CompromisoImportService service;

    @PostMapping("/previsualizar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public List<CompromisoImportFilaDto> previsualizar(@RequestParam("archivo") MultipartFile archivo) {
        return service.previsualizar(archivo);
    }

    @PostMapping("/confirmar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public CompromisoImportResultado confirmar(
            @RequestParam Long monedaId, @RequestBody List<CompromisoImportFilaDto> filas) {
        return service.confirmar(monedaId, filas);
    }
}
