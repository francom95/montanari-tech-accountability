package com.montanaritech.contable.facturacion.pago;

import com.montanaritech.contable.common.asiento.AsientoGenerado;
import com.montanaritech.contable.common.asiento.AsientoGenerator;
import com.montanaritech.contable.common.asiento.CalculoImputacion;
import com.montanaritech.contable.common.asiento.LineaAsientoGenerada;
import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.contabilidad.mapeocuenta.ConceptoContable;
import com.montanaritech.contable.contabilidad.mapeocuenta.ResolutorCuentas;
import com.montanaritech.contable.facturacion.facturacompra.FacturaCompra;
import com.montanaritech.contable.maestros.moneda.MonedaRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Generador de asiento para pago (F4.1 §6.2/§6.3/§6.5, F4.4), simétrico a
 * {@code CobroAsientoGenerator} salvo dos diferencias: no hay retenciones
 * (Montanari no es agente de retención) y el signo de la diferencia de
 * cambio está invertido (F4.1 §6.3: TC subió ⇒ pérdida en un pago, ganancia
 * en un cobro). Igual que el generador de cobro, es impuro a propósito:
 * fija {@code montoArsCancelado}/{@code montoAnticipo} en las entidades ya
 * administradas por JPA mientras calcula. También comparte los dos ajustes
 * de precisión de {@code CobroAsientoGenerator} (ver su Javadoc): la línea
 * de diferencia de cambio es siempre en ARS, y la línea de CxP usa un TC
 * efectivo derivado del monto ya cancelado por la regla del residuo.
 */
@Component
@RequiredArgsConstructor
public class PagoAsientoGenerator implements AsientoGenerator<Pago> {

    private final ResolutorCuentas resolutorCuentas;
    private final PagoImputacionRepository pagoImputacionRepo;
    private final AplicacionAnticipoProveedorRepository aplicacionAnticipoRepo;
    private final MonedaRepository monedaRepo;

    @Override
    public AsientoGenerado generar(Pago pago) {
        if (pago.getTotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new NegocioException("PAGO_SIN_IMPORTE", "El pago no tiene ningún importe a contabilizar");
        }

        Long monedaId = pago.getMoneda().getId();
        BigDecimal tc = pago.getTipoCambio();
        Long proveedorId = pago.getProveedor().getId();
        String fuenteTc = pago.getFuenteTc() != null ? pago.getFuenteTc().name() : "MANUAL";

        List<LineaAsientoGenerada> lineas = new ArrayList<>();
        BigDecimal sumaImputadoOriginal = BigDecimal.ZERO;

        for (PagoImputacion imputacion : pago.getLineas()) {
            FacturaCompra factura = imputacion.getFacturaCompra();
            if (!factura.getMoneda().getId().equals(monedaId)) {
                throw new NegocioException("IMPUTACION_MONEDA_NO_COINCIDE",
                        "La factura %s está en otra moneda distinta a la del pago".formatted(factura.getNumero()));
            }
            if (factura.getEstado() != EstadoDocumento.CONFIRMADO) {
                throw new NegocioException("IMPUTACION_FACTURA_NO_CONFIRMADA",
                        "La factura %s no está confirmada".formatted(factura.getNumero()));
            }

            List<PagoImputacion> previasPago = pagoImputacionRepo.findByFacturaCompra_IdAndPago_EstadoOrderByIdAsc(
                    factura.getId(), EstadoDocumento.CONFIRMADO);
            List<AplicacionAnticipoProveedor> previasAnticipo = aplicacionAnticipoRepo.findByFacturaCompra_IdOrderByIdAsc(factura.getId());

            BigDecimal sumaOrigPrevio = previasPago.stream().map(PagoImputacion::getMontoImputadoOriginal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .add(previasAnticipo.stream().map(AplicacionAnticipoProveedor::getMontoOriginal).reduce(BigDecimal.ZERO, BigDecimal::add));
            BigDecimal sumaArsPrevio = previasPago.stream().map(PagoImputacion::getMontoArsCancelado)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .add(previasAnticipo.stream().map(AplicacionAnticipoProveedor::getMontoArsCancelado).reduce(BigDecimal.ZERO, BigDecimal::add));
            BigDecimal saldoOrigAntes = factura.getTotal().subtract(sumaOrigPrevio);

            if (imputacion.getMontoImputadoOriginal().compareTo(saldoOrigAntes) > 0) {
                throw new NegocioException("IMPUTACION_EXCEDE_SALDO",
                        "La imputación supera el saldo pendiente de la factura %s".formatted(factura.getNumero()));
            }

            CalculoImputacion.Resultado resultado = CalculoImputacion.calcular(
                    imputacion.getMontoImputadoOriginal(), tc, factura.getTipoCambio(), saldoOrigAntes, factura.getTotalArs(), sumaArsPrevio);
            imputacion.setMontoArsCancelado(resultado.montoCanceladoArs());

            CuentaContable cuentaCxp = factura.getProveedor().getCuentaCxp() != null
                    ? factura.getProveedor().getCuentaCxp()
                    : resolutorCuentas.resolver(ConceptoContable.DEUDA_COMERCIAL);
            Long proyectoId = factura.getProyecto() != null ? factura.getProyecto().getId() : null;
            BigDecimal tcEfectivoCxp = tipoCambioEfectivo(resultado.montoCanceladoArs(), imputacion.getMontoImputadoOriginal());
            lineas.add(new LineaAsientoGenerada(cuentaCxp.getCodigo(), resultado.montoCanceladoArs(), BigDecimal.ZERO,
                    "Pago factura " + factura.getNumero(), monedaId, imputacion.getMontoImputadoOriginal(), tcEfectivoCxp, fuenteTc,
                    proyectoId, null, null, proveedorId, null));

            BigDecimal dif = resultado.diferenciaCambioArs();
            if (dif.compareTo(BigDecimal.ZERO) > 0) {
                CuentaContable cuentaPerdida = resolutorCuentas.resolver(ConceptoContable.DIF_CAMBIO_PERDIDA);
                lineas.add(new LineaAsientoGenerada(cuentaPerdida.getCodigo(), dif, BigDecimal.ZERO,
                        "Diferencia de cambio perdida", monedaArsId(), dif, BigDecimal.ONE, fuenteTc, proyectoId, null, null, proveedorId, null));
            } else if (dif.compareTo(BigDecimal.ZERO) < 0) {
                CuentaContable cuentaGanada = resolutorCuentas.resolver(ConceptoContable.DIF_CAMBIO_GANADA);
                lineas.add(new LineaAsientoGenerada(cuentaGanada.getCodigo(), BigDecimal.ZERO, dif.negate(),
                        "Diferencia de cambio ganada", monedaArsId(), dif.negate(), BigDecimal.ONE, fuenteTc, proyectoId, null, null, proveedorId, null));
            }

            sumaImputadoOriginal = sumaImputadoOriginal.add(imputacion.getMontoImputadoOriginal());
        }

        BigDecimal montoAnticipoNuevo = pago.getTotal().subtract(sumaImputadoOriginal);
        pago.setMontoAnticipo(montoAnticipoNuevo);
        if (montoAnticipoNuevo.compareTo(BigDecimal.ZERO) > 0) {
            CuentaContable cuentaAnticipo = resolutorCuentas.resolver(ConceptoContable.ANTICIPO_PROVEEDOR);
            BigDecimal anticipoArs = CalculoImputacion.round2(montoAnticipoNuevo.multiply(tc));
            lineas.add(new LineaAsientoGenerada(cuentaAnticipo.getCodigo(), anticipoArs, BigDecimal.ZERO,
                    "Anticipo a " + pago.getProveedor().getNombre(), monedaId, montoAnticipoNuevo, tc, fuenteTc,
                    null, null, null, proveedorId, null));
        }

        CuentaContable cuentaFondos = pago.getCuentaBancaria().getCuentaContable();
        BigDecimal montoFondosArs = CalculoImputacion.round2(pago.getTotal().multiply(tc));
        lineas.add(new LineaAsientoGenerada(cuentaFondos.getCodigo(), BigDecimal.ZERO, montoFondosArs,
                "Pago a " + pago.getProveedor().getNombre(), monedaId, pago.getTotal(), tc, fuenteTc,
                null, null, null, null, pago.getCuentaBancaria().getId()));

        return new AsientoGenerado(pago.getFecha(), "Pago - " + pago.getProveedor().getNombre(), "PAGO",
                lineas, "Pago", pago.getId());
    }

    /** Ajuste de aplicación posterior de anticipo (F4.1 §6.5), simétrico al de cobro. */
    public ResultadoAjusteAnticipo generarAjusteAplicacionAnticipo(Pago anticipo, FacturaCompra factura, BigDecimal monto, LocalDate fecha) {
        Long monedaId = anticipo.getMoneda().getId();
        BigDecimal tcAnticipo = anticipo.getTipoCambio();
        Long proveedorId = anticipo.getProveedor().getId();
        String fuenteTc = anticipo.getFuenteTc() != null ? anticipo.getFuenteTc().name() : "MANUAL";
        Long proyectoId = factura.getProyecto() != null ? factura.getProyecto().getId() : null;

        List<PagoImputacion> previasPago = pagoImputacionRepo.findByFacturaCompra_IdAndPago_EstadoOrderByIdAsc(
                factura.getId(), EstadoDocumento.CONFIRMADO);
        List<AplicacionAnticipoProveedor> previasAnticipo = aplicacionAnticipoRepo.findByFacturaCompra_IdOrderByIdAsc(factura.getId());

        BigDecimal sumaOrigPrevio = previasPago.stream().map(PagoImputacion::getMontoImputadoOriginal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(previasAnticipo.stream().map(AplicacionAnticipoProveedor::getMontoOriginal).reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal sumaArsPrevio = previasPago.stream().map(PagoImputacion::getMontoArsCancelado)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(previasAnticipo.stream().map(AplicacionAnticipoProveedor::getMontoArsCancelado).reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal saldoOrigAntes = factura.getTotal().subtract(sumaOrigPrevio);

        if (monto.compareTo(saldoOrigAntes) > 0) {
            throw new NegocioException("IMPUTACION_EXCEDE_SALDO",
                    "La aplicación supera el saldo pendiente de la factura %s".formatted(factura.getNumero()));
        }

        CalculoImputacion.Resultado resultado = CalculoImputacion.calcular(
                monto, tcAnticipo, factura.getTipoCambio(), saldoOrigAntes, factura.getTotalArs(), sumaArsPrevio);

        CuentaContable cuentaAnticipo = resolutorCuentas.resolver(ConceptoContable.ANTICIPO_PROVEEDOR);
        CuentaContable cuentaCxp = factura.getProveedor().getCuentaCxp() != null
                ? factura.getProveedor().getCuentaCxp()
                : resolutorCuentas.resolver(ConceptoContable.DEUDA_COMERCIAL);

        List<LineaAsientoGenerada> lineas = new ArrayList<>();
        BigDecimal tcEfectivoCxp = tipoCambioEfectivo(resultado.montoCanceladoArs(), monto);
        lineas.add(new LineaAsientoGenerada(cuentaCxp.getCodigo(), resultado.montoCanceladoArs(), BigDecimal.ZERO,
                "Aplicación de anticipo a factura " + factura.getNumero(), monedaId, monto, tcEfectivoCxp, fuenteTc,
                proyectoId, null, null, proveedorId, null));

        BigDecimal dif = resultado.diferenciaCambioArs();
        if (dif.compareTo(BigDecimal.ZERO) > 0) {
            CuentaContable cuentaPerdida = resolutorCuentas.resolver(ConceptoContable.DIF_CAMBIO_PERDIDA);
            lineas.add(new LineaAsientoGenerada(cuentaPerdida.getCodigo(), dif, BigDecimal.ZERO,
                    "Diferencia de cambio perdida", monedaArsId(), dif, BigDecimal.ONE, fuenteTc, proyectoId, null, null, proveedorId, null));
        } else if (dif.compareTo(BigDecimal.ZERO) < 0) {
            CuentaContable cuentaGanada = resolutorCuentas.resolver(ConceptoContable.DIF_CAMBIO_GANADA);
            lineas.add(new LineaAsientoGenerada(cuentaGanada.getCodigo(), BigDecimal.ZERO, dif.negate(),
                    "Diferencia de cambio ganada", monedaArsId(), dif.negate(), BigDecimal.ONE, fuenteTc, proyectoId, null, null, proveedorId, null));
        }

        lineas.add(new LineaAsientoGenerada(cuentaAnticipo.getCodigo(), BigDecimal.ZERO, resultado.montoFondosArs(),
                "Aplicación de anticipo a factura " + factura.getNumero(), monedaId, monto, tcAnticipo, fuenteTc,
                proyectoId, null, null, proveedorId, null));

        AsientoGenerado generado = new AsientoGenerado(fecha, "Aplicación de anticipo - " + factura.getNumero(),
                "AJUSTE", lineas, "Pago", anticipo.getId());
        return new ResultadoAjusteAnticipo(generado, resultado.montoCanceladoArs());
    }

    /** TC "efectivo" que reproduce exactamente el monto ARS ya calculado (regla del residuo, ver Javadoc de la clase). */
    private static BigDecimal tipoCambioEfectivo(BigDecimal montoArs, BigDecimal montoOriginal) {
        return montoArs.divide(montoOriginal, 6, RoundingMode.HALF_UP);
    }

    private Long monedaArsId() {
        return monedaRepo.findByCodigo("ARS")
                .orElseThrow(() -> new NegocioException("MONEDA_ARS_NO_CONFIGURADA", "No existe la moneda ARS"))
                .getId();
    }

    public record ResultadoAjusteAnticipo(AsientoGenerado asientoGenerado, BigDecimal montoArsCancelado) {
    }
}
