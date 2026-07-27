package com.montanaritech.contable.maestros.proveedor.importacion;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** F10.2: carga histórica única desde la hoja "Proveedores de servicios", sin UI (decisión del equipo). */
@RestController
@RequestMapping("/api/v1/importacion/proveedores")
@RequiredArgsConstructor
@Tag(name = "Importación F10.2")
public class ProveedorImportController {
    private final ProveedorImportService service;

    @PostMapping("/previsualizar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public List<ProveedorImportFilaDto> previsualizar(@RequestParam("archivo") MultipartFile archivo) {
        return service.previsualizar(archivo);
    }

    @PostMapping("/confirmar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ProveedorImportResultado confirmar(
            @RequestParam Long jurisdiccionIdPorDefecto, @RequestBody List<ProveedorImportFilaDto> filas) {
        return service.confirmar(jurisdiccionIdPorDefecto, filas);
    }
}
