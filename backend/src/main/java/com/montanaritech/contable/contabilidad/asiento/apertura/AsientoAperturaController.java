package com.montanaritech.contable.contabilidad.asiento.apertura;

import com.montanaritech.contable.contabilidad.asiento.Asiento;
import com.montanaritech.contable.contabilidad.asiento.AsientoMapper;
import com.montanaritech.contable.contabilidad.asiento.dto.AsientoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * F10.3: genera el BORRADOR del asiento de apertura (checkpoint humano — el
 * contador lo revisa y lo confirma con el endpoint genérico
 * {@code POST /api/v1/asientos/{id}/confirmar} ya existente).
 */
@RestController
@RequestMapping("/api/v1/asientos/apertura")
@RequiredArgsConstructor
public class AsientoAperturaController {

    private final AsientoAperturaService service;
    private final AsientoMapper mapper;

    @PostMapping("/generar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public AsientoResponse generar() {
        Asiento borrador = service.generarBorrador();
        return mapper.aResponse(borrador);
    }
}
