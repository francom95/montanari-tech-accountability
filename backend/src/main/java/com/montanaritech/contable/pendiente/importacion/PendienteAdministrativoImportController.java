package com.montanaritech.contable.pendiente.importacion;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** F10.2: carga histórica única desde "PENDIENTES"/"PENDIENTES AHORA", sin UI (decisión del equipo). */
@RestController
@RequestMapping("/api/v1/importacion/pendientes")
@RequiredArgsConstructor
@Tag(name = "Importación F10.2")
public class PendienteAdministrativoImportController {
    private final PendienteAdministrativoImportService service;

    @PostMapping("/previsualizar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public List<PendienteAdministrativoImportFilaDto> previsualizar(@RequestParam("archivo") MultipartFile archivo) {
        return service.previsualizar(archivo);
    }

    @PostMapping("/confirmar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public PendienteAdministrativoImportResultado confirmar(@RequestBody List<PendienteAdministrativoImportFilaDto> filas) {
        return service.confirmar(filas);
    }
}
