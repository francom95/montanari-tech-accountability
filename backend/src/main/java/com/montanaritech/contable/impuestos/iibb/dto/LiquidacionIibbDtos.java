package com.montanaritech.contable.impuestos.iibb.dto;

import com.montanaritech.contable.impuestos.iibb.TipoComponenteIibb;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** DTOs de la liquidación de IIBB (F6.2). Agrupados por ser records chicos de un mismo flujo. */
public final class LiquidacionIibbDtos {

    private LiquidacionIibbDtos() {
    }

    public record CrearRequest(
            @NotNull @Min(2000) @Max(2100) Integer anio,
            @NotNull @Min(1) @Max(12) Integer mes) {
    }

    /** Edita el coeficiente de Convenio Multilateral y/o la alícuota de una jurisdicción. */
    public record EditarJurisdiccionRequest(
            @NotNull @DecimalMin("0.0") BigDecimal coeficiente,
            @NotNull @DecimalMin("0.0") BigDecimal alicuota) {
    }

    /** Carga o corrige el importe de una deducción. Motivo obligatorio solo si corrige un valor que el sistema calculó. */
    public record AjustarComponenteRequest(
            @NotNull BigDecimal importeAjuste,
            String motivoAjuste) {
    }

    public record AgregarComponenteRequest(
            @NotNull TipoComponenteIibb tipo,
            @NotBlank String descripcion,
            @NotNull BigDecimal importe,
            @NotNull Long cuentaContableId,
            String motivo) {
    }

    public record AnularRequest(@NotBlank String motivo) {
    }

    public record ComponenteResponse(
            Long id,
            TipoComponenteIibb tipo,
            String descripcion,
            BigDecimal importeCalculado,
            BigDecimal importeAjuste,
            BigDecimal importeFinal,
            BigDecimal aporte,
            String motivoAjuste,
            Long cuentaContableId,
            String cuentaContableCodigo,
            String cuentaContableNombre,
            boolean manual,
            Integer orden) {
    }

    public record JurisdiccionResponse(
            Long id,
            Long jurisdiccionId,
            String jurisdiccionCodigo,
            String jurisdiccionNombre,
            BigDecimal coeficiente,
            BigDecimal baseImponible,
            BigDecimal alicuota,
            BigDecimal impuestoDeterminado,
            BigDecimal saldoAPagar,
            BigDecimal saldoAFavor,
            Integer orden,
            List<ComponenteResponse> componentes) {
    }

    public record LiquidacionResponse(
            Long id,
            Integer anio,
            Integer mes,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            String estado,
            BigDecimal baseTotal,
            BigDecimal saldoAPagarTotal,
            BigDecimal saldoAFavorTotal,
            Long asientoId,
            Long asientoNumero,
            String observaciones,
            List<JurisdiccionResponse> jurisdicciones,
            List<String> advertencias) {
    }

    // --- previsualización (no persiste) ---

    public record PrevisualizacionResponse(
            Integer anio,
            Integer mes,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            BigDecimal baseTotal,
            BigDecimal deduccionesDisponibles,
            List<JurisdiccionPrevisualizadaResponse> jurisdicciones,
            List<String> advertencias) {
    }

    public record JurisdiccionPrevisualizadaResponse(
            Long jurisdiccionId,
            String jurisdiccionCodigo,
            String jurisdiccionNombre,
            BigDecimal ventasDestino,
            BigDecimal coeficiente,
            BigDecimal baseImponible,
            BigDecimal alicuota,
            BigDecimal impuestoDeterminado,
            BigDecimal saldoAFavorAnterior) {
    }
}
