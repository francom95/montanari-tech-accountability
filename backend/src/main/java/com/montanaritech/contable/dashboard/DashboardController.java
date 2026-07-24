package com.montanaritech.contable.dashboard;

import com.montanaritech.contable.dashboard.dto.DashboardResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Pantalla de inicio del sistema (F7.5): los 12 indicadores de un período en un único endpoint agregado. */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard")
public class DashboardController {

    private final DashboardService service;

    @GetMapping
    public DashboardResponse obtener(@RequestParam int anio, @RequestParam int mes) {
        return service.obtener(anio, mes);
    }
}
