package com.montanaritech.contable.flujocaja.dto;

import java.util.List;
import java.util.Map;

/**
 * {@code porCuenta}: serie en moneda nativa de cada cuenta/tarjeta, clave =
 * alias/entidad. {@code consolidado}: serie única en ARS (movimientos ya
 * traen su propio {@code importeArs} histórico; solo el saldo inicial de
 * cuentas no-ARS se convierte con la última cotización cargada — F8.3 §2).
 */
public record FlujoCajaResponse(
        List<PuntoFlujoCaja> consolidado,
        Map<String, List<PuntoFlujoCaja>> porCuenta,
        List<String> advertencias
) {}
