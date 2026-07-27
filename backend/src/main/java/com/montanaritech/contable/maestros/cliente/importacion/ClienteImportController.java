package com.montanaritech.contable.maestros.cliente.importacion;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** F10.2: carga histórica única desde la hoja "Base de datos - Clientes", sin UI (decisión del equipo). */
@RestController
@RequestMapping("/api/v1/importacion/clientes")
@RequiredArgsConstructor
@Tag(name = "Importación F10.2")
public class ClienteImportController {
    private final ClienteImportService service;

    @PostMapping("/previsualizar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public List<ClienteImportFilaDto> previsualizar(@RequestParam("archivo") MultipartFile archivo) {
        return service.previsualizar(archivo);
    }

    @PostMapping("/confirmar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ClienteImportResultado confirmar(
            @RequestParam Long jurisdiccionIdPorDefecto, @RequestBody List<ClienteImportFilaDto> filas) {
        return service.confirmar(jurisdiccionIdPorDefecto, filas);
    }
}
