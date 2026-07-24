package com.montanaritech.contable.vencimientos;

import com.montanaritech.contable.vencimientos.dto.VencimientoResponse;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * Mapper manual (no MapStruct): {@code estado} necesita el cómputo de
 * VENCIDO en lectura (mismo criterio que {@code EstadoVencimiento} de F4.5),
 * algo que un mapper declarativo no expresa con claridad.
 */
@Component
public class VencimientoMapper {

    public VencimientoResponse aResponse(Vencimiento v) {
        return new VencimientoResponse(
                v.getId(),
                v.getDescripcion(),
                v.getTipo(),
                v.getFecha(),
                v.getImporteEstimado(),
                v.getMoneda().getId(),
                v.getMoneda().getCodigo(),
                v.getRecurrencia(),
                v.getIntervaloDiasPersonalizado(),
                estadoEfectivo(v),
                v.getCuentaContable() == null ? null : v.getCuentaContable().getId(),
                v.getCuentaContable() == null ? null : v.getCuentaContable().getCodigo(),
                v.getProveedor() == null ? null : v.getProveedor().getId(),
                v.getProveedor() == null ? null : v.getProveedor().getNombre(),
                v.getLiquidacionTipo(),
                v.getLiquidacionId(),
                v.getTarjetaCredito() == null ? null : v.getTarjetaCredito().getId(),
                v.getTarjetaCredito() == null ? null : v.getTarjetaCredito().getEntidad(),
                v.getProyecto() == null ? null : v.getProyecto().getId(),
                v.getProyecto() == null ? null : v.getProyecto().getNombre(),
                v.getConceptoRecurrente() == null ? null : v.getConceptoRecurrente().getId(),
                v.getConceptoRecurrente() == null ? null : v.getConceptoRecurrente().getNombre(),
                v.getAsientoVinculado() == null ? null : v.getAsientoVinculado().getId(),
                v.getAsientoVinculado() == null ? null : v.getAsientoVinculado().getNumero(),
                v.getOrigenGeneracion().name(),
                v.getOrigenGeneracionRefId(),
                v.getObservaciones(),
                v.getMotivoCancelacion());
    }

    /** VENCIDO nunca se persiste: se deriva en lectura, mismo criterio que EstadoVencimiento (F4.5). */
    private String estadoEfectivo(Vencimiento v) {
        if (v.getEstado() == EstadoVencimientoObligacion.PENDIENTE && v.getFecha().isBefore(LocalDate.now())) {
            return "VENCIDO";
        }
        return v.getEstado().name();
    }
}
