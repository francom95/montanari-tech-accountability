package com.montanaritech.contable.facturacion.facturacompra;

import com.montanaritech.contable.common.asiento.AsientoGenerado;
import com.montanaritech.contable.common.asiento.AsientoGenerator;
import com.montanaritech.contable.common.asiento.LineaAsientoGenerada;
import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.contabilidad.mapeocuenta.ConceptoContable;
import com.montanaritech.contable.contabilidad.mapeocuenta.ResolutorCuentas;
import com.montanaritech.contable.facturacion.TipoComprobante;
import com.montanaritech.contable.facturacion.comprobantetributo.ComprobanteTipo;
import com.montanaritech.contable.facturacion.comprobantetributo.ComprobanteTributo;
import com.montanaritech.contable.facturacion.comprobantetributo.ComprobanteTributoRepository;
import com.montanaritech.contable.maestros.proveedor.CondicionIva;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Generador de asiento para factura de compra (F4.1 §5, F4.3), simétrico a
 * {@code FacturaVentaAsientoGenerator}. El crédito fiscal es condicional
 * (F4.1 §5): comprobante {@code FACTURA_C} o proveedor no responsable
 * inscripto ⇒ el IVA se absorbe en el costo, no genera línea propia.
 *
 * <p>Al igual que en F4.2, cada línea materializa su propio importe a ARS de
 * forma independiente (no se calcula el CxP como suma de las demás), para no
 * chocar con el chequeo {@code MONTO_ARS_INCONSISTENTE} de
 * {@code AsientoService} sobre líneas en moneda extranjera. Si el redondeo
 * independiente no cierra exacto, {@code ValidadorBalanceAsiento} rechaza el
 * asiento — no se fuerza un ajuste silencioso.
 */
@Component
@RequiredArgsConstructor
public class FacturaCompraAsientoGenerator implements AsientoGenerator<FacturaCompra> {

    private final ResolutorCuentas resolutorCuentas;
    private final ComprobanteTributoRepository comprobanteTributoRepo;

    @Override
    public AsientoGenerado generar(FacturaCompra factura) {
        boolean notaCredito = factura.getTipoComprobante().name().startsWith("NOTA_CREDITO");
        boolean computaCreditoFiscal = computaCreditoFiscal(factura);
        Long monedaId = factura.getMoneda().getId();
        BigDecimal tc = factura.getTipoCambio();
        Long proveedorId = factura.getProveedor().getId();
        Long proyectoId = factura.getProyecto() != null ? factura.getProyecto().getId() : null;
        String fuenteTc = factura.getFuenteTc() != null ? factura.getFuenteTc().name() : "MANUAL";

        if (factura.getTotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new NegocioException("FACTURA_SIN_IMPORTE", "La factura no tiene ningún importe a contabilizar");
        }

        List<LineaAsientoGenerada> lineas = new ArrayList<>();
        BigDecimal totalIva = BigDecimal.ZERO;

        for (FacturaCompraLinea linea : factura.getLineas()) {
            CuentaContable cuentaCosto = linea.getCuentaContable() != null
                    ? linea.getCuentaContable()
                    : resolutorCuentas.resolver(ConceptoContable.COSTO_GASTO, "CATEGORIA", linea.getTipoCosto().getNombre());

            BigDecimal importeOriginalLinea = computaCreditoFiscal
                    ? linea.getImporteNeto()
                    : linea.getImporteNeto().add(linea.getImporteIva());
            BigDecimal importeArs = round2(importeOriginalLinea.multiply(tc));
            lineas.add(lineaDebito(cuentaCosto.getCodigo(), importeArs, linea.getDescripcion(), notaCredito,
                    monedaId, importeOriginalLinea, tc, fuenteTc, proyectoId, proveedorId));

            if (computaCreditoFiscal) {
                totalIva = totalIva.add(linea.getImporteIva());
            }
        }

        if (computaCreditoFiscal && totalIva.compareTo(BigDecimal.ZERO) > 0) {
            CuentaContable cuentaIva = resolutorCuentas.resolver(ConceptoContable.IVA_CREDITO_FISCAL);
            BigDecimal ivaArs = round2(totalIva.multiply(tc));
            lineas.add(lineaDebito(cuentaIva.getCodigo(), ivaArs, "IVA Crédito Fiscal", notaCredito,
                    monedaId, totalIva, tc, fuenteTc, proyectoId, proveedorId));
        }

        for (ComprobanteTributo tributo : comprobanteTributoRepo.findByComprobanteTipoAndComprobanteIdOrderByIdAsc(
                ComprobanteTipo.FACTURA_COMPRA, factura.getId())) {
            ConceptoContable concepto = switch (tributo.getTipo()) {
                case PERCEPCION_IVA -> ConceptoContable.PERCEPCION_IVA_SUFRIDA;
                case PERCEPCION_IIBB -> ConceptoContable.PERCEPCION_IIBB_SUFRIDA;
                default -> throw new NegocioException("TRIBUTO_NO_APLICABLE_A_COMPRA",
                        "El tributo %s no genera línea de asiento en una factura de compra".formatted(tributo.getTipo()));
            };
            CuentaContable cuentaTributo = tributo.getJurisdiccion() != null
                    ? resolutorCuentas.resolver(concepto, "JURISDICCION", tributo.getJurisdiccion().getId().toString())
                    : resolutorCuentas.resolver(concepto);
            BigDecimal importeArs = round2(tributo.getImporte().multiply(tc));
            lineas.add(lineaDebito(cuentaTributo.getCodigo(), importeArs, "Percepción " + tributo.getTipo(), notaCredito,
                    monedaId, tributo.getImporte(), tc, fuenteTc, proyectoId, proveedorId));
        }

        CuentaContable cuentaCxp = factura.getProveedor().getCuentaCxp() != null
                ? factura.getProveedor().getCuentaCxp()
                : resolutorCuentas.resolver(ConceptoContable.DEUDA_COMERCIAL);
        BigDecimal totalArs = round2(factura.getTotal().multiply(tc));
        lineas.add(lineaCredito(cuentaCxp.getCodigo(), totalArs, "Factura " + factura.getNumero(), notaCredito,
                monedaId, factura.getTotal(), tc, fuenteTc, proyectoId, proveedorId));

        return new AsientoGenerado(
                factura.getFecha(),
                "Factura de compra " + factura.getNumero() + " - " + factura.getProveedor().getNombre(),
                "FACTURA_COMPRA",
                lineas,
                "FacturaCompra",
                factura.getId());
    }

    /**
     * Comprobante tipo C o proveedor no responsable inscripto ⇒ no computa
     * crédito fiscal (F4.1 §5): el IVA discriminado, si lo hay, se absorbe
     * en el costo.
     */
    private boolean computaCreditoFiscal(FacturaCompra factura) {
        if (factura.getTipoComprobante() == TipoComprobante.FACTURA_C) {
            return false;
        }
        return factura.getProveedor().getCondicionIva() == CondicionIva.RESPONSABLE_INSCRIPTO;
    }

    private LineaAsientoGenerada lineaDebito(String cuentaCodigo, BigDecimal importeArs, String descripcion,
            boolean notaCredito, Long monedaId, BigDecimal importeOriginal, BigDecimal tc, String fuenteTc,
            Long proyectoId, Long proveedorId) {
        return notaCredito
                ? new LineaAsientoGenerada(cuentaCodigo, BigDecimal.ZERO, importeArs, descripcion, monedaId, importeOriginal, tc, fuenteTc, proyectoId, null, null, proveedorId, null)
                : new LineaAsientoGenerada(cuentaCodigo, importeArs, BigDecimal.ZERO, descripcion, monedaId, importeOriginal, tc, fuenteTc, proyectoId, null, null, proveedorId, null);
    }

    private LineaAsientoGenerada lineaCredito(String cuentaCodigo, BigDecimal importeArs, String descripcion,
            boolean notaCredito, Long monedaId, BigDecimal importeOriginal, BigDecimal tc, String fuenteTc,
            Long proyectoId, Long proveedorId) {
        return notaCredito
                ? new LineaAsientoGenerada(cuentaCodigo, importeArs, BigDecimal.ZERO, descripcion, monedaId, importeOriginal, tc, fuenteTc, proyectoId, null, null, proveedorId, null)
                : new LineaAsientoGenerada(cuentaCodigo, BigDecimal.ZERO, importeArs, descripcion, monedaId, importeOriginal, tc, fuenteTc, proyectoId, null, null, proveedorId, null);
    }

    private static BigDecimal round2(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
