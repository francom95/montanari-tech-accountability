package com.montanaritech.contable.busqueda;

import com.montanaritech.contable.busqueda.dto.BusquedaGlobalResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Búsqueda global "Lupita" (F9.2): lectura para cualquier rol autenticado, igual que el resto de los GET. */
@RestController
@RequestMapping("/api/v1/busqueda")
@RequiredArgsConstructor
@Tag(name = "BusquedaGlobal")
public class BusquedaGlobalController {

    private final BusquedaGlobalService service;

    @GetMapping
    public BusquedaGlobalResponse buscar(@RequestParam String q) {
        if (q == null || q.trim().length() < 2) {
            return new BusquedaGlobalResponse(Map.of());
        }
        return service.buscar(q);
    }
}
