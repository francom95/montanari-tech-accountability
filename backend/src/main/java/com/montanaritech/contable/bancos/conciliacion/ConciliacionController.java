package com.montanaritech.contable.bancos.conciliacion;

import com.montanaritech.contable.bancos.conciliacion.dto.ConciliacionResumenResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Conciliación bancaria por cuenta y período (F5.3). Solo lectura — sin @PreAuthorize, mismo criterio que los GET de MovimientoBancarioController: cualquier rol autenticado (incl. LECTURA) puede consultar. */
@RestController
@RequestMapping("/api/v1/conciliacion")
@RequiredArgsConstructor
@Tag(name = "Conciliacion")
public class ConciliacionController {

    private final ConciliacionService service;

    @GetMapping("/resumen")
    public ConciliacionResumenResponse resumen(
            @RequestParam Long cuentaBancariaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(defaultValue = "3") int toleranciaDias) {
        return service.resumen(cuentaBancariaId, fechaDesde, fechaHasta, toleranciaDias);
    }
}
