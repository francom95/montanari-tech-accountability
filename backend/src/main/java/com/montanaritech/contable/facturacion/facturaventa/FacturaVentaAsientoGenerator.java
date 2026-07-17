package com.montanaritech.contable.facturacion.facturaventa;

import com.montanaritech.contable.common.asiento.AsientoGenerado;
import com.montanaritech.contable.common.asiento.LineaAsientoGenerada;
import com.montanaritech.contable.common.asiento.AsientoGenerator;
import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.contabilidad.mapeocuenta.ConceptoContable;
import com.montanaritech.contable.contabilidad.mapeocuenta.ResolutorCuentas;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Genera el asiento de una factura de venta confirmada (F4.1 §4): Debe
 * Créditos por Ventas (CxC del cliente) / Haber Ingresos por ventas (por
 * línea) + Haber IVA Débito Fiscal (si corresponde). Una nota de crédito
 * (F1.1 ADR-13) es el mismo modelo con los lados invertidos.
 *
 * <p>Cada línea (ingresos, IVA, CxC) materializa su propio importe original
 * a ARS de forma independiente ({@code round2(importe × TC)}) — igual que
 * cualquier línea manual (F3.1 §3.3): así cada una pasa su propio chequeo
 * {@code MONTO_ARS_INCONSISTENTE} en {@code AsientoService}. Si el reparto
 * en varias líneas hiciera que los redondeos independientes no sumen
 * exactamente el total (caso de borde, prácticamente inexistente con los
 * importes de dos decimales habituales), el asiento no balancea y
 * {@code ValidadorBalanceAsiento} lo rechaza con un error claro — no se
 * fuerza un ajuste silencioso en ninguna línea.
 */
@Component
@RequiredArgsConstructor
public class FacturaVentaAsientoGenerator implements AsientoGenerator<FacturaVenta> {

    private final ResolutorCuentas resolutorCuentas;

    @Override
    public AsientoGenerado generar(FacturaVenta factura) {
        boolean notaCredito = factura.getTipoComprobante().name().startsWith("NOTA_CREDITO");
        Long monedaId = factura.getMoneda().getId();
        BigDecimal tc = factura.getTipoCambio();
        Long clienteId = factura.getCliente().getId();
        Long proyectoId = factura.getProyecto() != null ? factura.getProyecto().getId() : null;
        String fuenteTc = factura.getFuenteTc() != null ? factura.getFuenteTc().name() : "MANUAL";

        if (factura.getTotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new NegocioException("FACTURA_SIN_IMPORTE", "La factura no tiene ningún importe a contabilizar");
        }

        List<LineaAsientoGenerada> lineas = new ArrayList<>();

        for (FacturaVentaLinea linea : factura.getLineas()) {
            CuentaContable cuentaIngreso = linea.getCuentaContable() != null
                    ? linea.getCuentaContable()
                    : resolutorCuentas.resolver(ConceptoContable.INGRESO_VENTA, "TIPO_INGRESO", linea.getTipoIngreso().name());
            BigDecimal importeArs = round2(linea.getImporteNeto().multiply(tc));
            lineas.add(lineaCredito(cuentaIngreso.getCodigo(), importeArs, linea.getDescripcion(), notaCredito,
                    monedaId, linea.getImporteNeto(), tc, fuenteTc, proyectoId, clienteId));
        }

        if (factura.getImporteIva().compareTo(BigDecimal.ZERO) > 0) {
            CuentaContable cuentaIva = resolutorCuentas.resolver(ConceptoContable.IVA_DEBITO_FISCAL);
            BigDecimal ivaArs = round2(factura.getImporteIva().multiply(tc));
            lineas.add(lineaCredito(cuentaIva.getCodigo(), ivaArs, "IVA Débito Fiscal", notaCredito,
                    monedaId, factura.getImporteIva(), tc, fuenteTc, proyectoId, clienteId));
        }

        CuentaContable cuentaCxc = factura.getCliente().getCuentaCxc() != null
                ? factura.getCliente().getCuentaCxc()
                : resolutorCuentas.resolver(ConceptoContable.CREDITO_POR_VENTA);
        BigDecimal totalArs = round2(factura.getTotal().multiply(tc));
        lineas.add(lineaCxc(cuentaCxc.getCodigo(), totalArs, "Factura " + factura.getNumero(), notaCredito,
                monedaId, factura.getTotal(), tc, fuenteTc, proyectoId, clienteId));

        return new AsientoGenerado(
                factura.getFecha(),
                "Factura de venta " + factura.getNumero() + " - " + factura.getCliente().getNombre(),
                "FACTURA_VENTA",
                lineas,
                "FacturaVenta",
                factura.getId());
    }

    /** Ingreso/IVA: Haber en factura normal, Debe en nota de crédito (ADR-13). */
    private LineaAsientoGenerada lineaCredito(String cuentaCodigo, BigDecimal importeArs, String descripcion,
            boolean notaCredito, Long monedaId, BigDecimal importeOriginal, BigDecimal tc, String fuenteTc,
            Long proyectoId, Long clienteId) {
        return notaCredito
                ? new LineaAsientoGenerada(cuentaCodigo, importeArs, BigDecimal.ZERO, descripcion, monedaId, importeOriginal, tc, fuenteTc, proyectoId, null, clienteId, null, null)
                : new LineaAsientoGenerada(cuentaCodigo, BigDecimal.ZERO, importeArs, descripcion, monedaId, importeOriginal, tc, fuenteTc, proyectoId, null, clienteId, null, null);
    }

    /** CxC: Debe en factura normal, Haber en nota de crédito (se invierte respecto de las líneas de crédito). */
    private LineaAsientoGenerada lineaCxc(String cuentaCodigo, BigDecimal importeArs, String descripcion,
            boolean notaCredito, Long monedaId, BigDecimal importeOriginal, BigDecimal tc, String fuenteTc,
            Long proyectoId, Long clienteId) {
        return notaCredito
                ? new LineaAsientoGenerada(cuentaCodigo, BigDecimal.ZERO, importeArs, descripcion, monedaId, importeOriginal, tc, fuenteTc, proyectoId, null, clienteId, null, null)
                : new LineaAsientoGenerada(cuentaCodigo, importeArs, BigDecimal.ZERO, descripcion, monedaId, importeOriginal, tc, fuenteTc, proyectoId, null, clienteId, null, null);
    }

    private static BigDecimal round2(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
