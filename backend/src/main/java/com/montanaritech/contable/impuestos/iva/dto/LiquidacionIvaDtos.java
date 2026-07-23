package com.montanaritech.contable.impuestos.iva.dto;

import com.montanaritech.contable.impuestos.iva.TipoComponenteIva;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTOs de la liquidación de IVA (F6.1). Agrupados en un archivo por ser
 * records chicos de un mismo flujo, a diferencia de los módulos anteriores
 * donde cada request/response tiene su propio archivo por tener más peso
 * individual.
 */
public final class LiquidacionIvaDtos {

    private LiquidacionIvaDtos() {
    }

    public record CrearRequest(
            @NotNull @Min(2000) @Max(2100) Integer anio,
            @NotNull @Min(1) @Max(12) Integer mes) {
    }

    /** Ajuste de un componente ya calculado. El motivo es obligatorio si el ajuste no es cero. */
    public record AjustarComponenteRequest(
            @NotNull BigDecimal importeAjuste,
            String motivoAjuste) {
    }

    /** Alta de un componente manual (restituciones u otro concepto). */
    public record AgregarComponenteRequest(
            @NotNull TipoComponenteIva tipo,
            @NotBlank String descripcion,
            @NotNull BigDecimal importe,
            @NotNull Long cuentaContableId,
            String motivo) {
    }

    public record AnularRequest(@NotBlank String motivo) {
    }

    public record ComponenteResponse(
            Long id,
            TipoComponenteIva tipo,
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

    public record LiquidacionResponse(
            Long id,
            Integer anio,
            Integer mes,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            String estado,
            BigDecimal saldoAPagar,
            BigDecimal saldoAFavor,
            Long asientoId,
            Long asientoNumero,
            String observaciones,
            List<ComponenteResponse> componentes,
            List<String> advertencias) {
    }

    /** Previsualización de un período todavía no liquidado (no persiste nada). */
    public record PrevisualizacionResponse(
            Integer anio,
            Integer mes,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            BigDecimal saldoAPagar,
            BigDecimal saldoAFavor,
            List<ComponentePrevisualizadoResponse> componentes,
            List<String> advertencias) {
    }

    public record ComponentePrevisualizadoResponse(
            TipoComponenteIva tipo,
            String descripcion,
            BigDecimal importe,
            BigDecimal aporte,
            List<DetalleImputacionResponse> detalle) {
    }

    public record DetalleImputacionResponse(
            Long asientoId,
            Long asientoNumero,
            LocalDate fecha,
            String descripcion,
            String documentoOrigenTipo,
            Long documentoOrigenId,
            BigDecimal importe) {
    }
}
