package com.montanaritech.contable.impuestos.iva;

import com.montanaritech.contable.impuestos.iva.dto.LiquidacionIvaDtos.ComponentePrevisualizadoResponse;
import com.montanaritech.contable.impuestos.iva.dto.LiquidacionIvaDtos.ComponenteResponse;
import com.montanaritech.contable.impuestos.iva.dto.LiquidacionIvaDtos.DetalleImputacionResponse;
import com.montanaritech.contable.impuestos.iva.dto.LiquidacionIvaDtos.LiquidacionResponse;
import com.montanaritech.contable.impuestos.iva.dto.LiquidacionIvaDtos.PrevisualizacionResponse;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Mapper escrito a mano en vez de MapStruct (a diferencia del resto del
 * proyecto): la respuesta lleva campos derivados ({@code importeFinal},
 * {@code aporte}) y una lista de advertencias que no vive en la entidad sino
 * que llega del motor de cálculo, así que la declaración MapStruct necesitaría
 * más expresiones que las que ahorraría.
 */
@Component
public class LiquidacionIvaMapper {

    public LiquidacionResponse aResponse(LiquidacionIva l, List<String> advertencias) {
        return new LiquidacionResponse(
                l.getId(), l.getAnio(), l.getMes(), l.getFechaDesde(), l.getFechaHasta(),
                l.getEstado().name(), l.getSaldoAPagar(), l.getSaldoAFavor(), l.getSaldoLibreDisponibilidad(),
                l.getAsiento() != null ? l.getAsiento().getId() : null,
                l.getAsiento() != null ? l.getAsiento().getNumero() : null,
                l.getObservaciones(),
                l.getComponentes().stream().map(this::aComponenteResponse).toList(),
                advertencias);
    }

    private ComponenteResponse aComponenteResponse(LiquidacionIvaComponente c) {
        return new ComponenteResponse(
                c.getId(), c.getTipo(), c.getDescripcion(),
                c.getImporteCalculado(), c.getImporteAjuste(), c.getImporteFinal(), c.getAporte(),
                c.getMotivoAjuste(),
                c.getCuentaContable() != null ? c.getCuentaContable().getId() : null,
                c.getCuentaContable() != null ? c.getCuentaContable().getCodigo() : null,
                c.getCuentaContable() != null ? c.getCuentaContable().getNombre() : null,
                c.isManual(), c.getOrden());
    }

    /**
     * Previsualización: mismo resultado que tendría el borrador si se creara,
     * calculado sin persistir nada.
     */
    public PrevisualizacionResponse aPrevisualizacion(CalculoIva calculo) {
        ResultadoIva r = ResultadoIva.calcular(calculo.componentes(),
                c -> c.tipo().getEtapa(),
                c -> c.importe().multiply(BigDecimal.valueOf(c.tipo().getSigno())));

        return new PrevisualizacionResponse(
                calculo.anio(), calculo.mes(), calculo.fechaDesde(), calculo.fechaHasta(),
                r.saldoAPagar(), r.saldoTecnico(), r.saldoLibreDisponibilidad(),
                calculo.componentes().stream().map(this::aComponentePrevisualizado).toList(),
                calculo.advertencias());
    }

    private ComponentePrevisualizadoResponse aComponentePrevisualizado(CalculoIva.ComponenteCalculado c) {
        return new ComponentePrevisualizadoResponse(
                c.tipo(), c.descripcion(), c.importe(),
                c.importe().multiply(BigDecimal.valueOf(c.tipo().getSigno())),
                c.detalle().stream()
                        .map(d -> new DetalleImputacionResponse(d.asientoId(), d.asientoNumero(), d.fecha(),
                                d.descripcion(), d.documentoOrigenTipo(), d.documentoOrigenId(), d.importe()))
                        .toList());
    }
}
