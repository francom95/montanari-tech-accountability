package com.montanaritech.contable.facturacion.cobro;

import com.montanaritech.contable.common.asiento.AsientoGenerado;
import com.montanaritech.contable.common.asiento.AsientoGenerator;
import com.montanaritech.contable.common.asiento.CalculoImputacion;
import com.montanaritech.contable.common.asiento.LineaAsientoGenerada;
import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.contabilidad.mapeocuenta.ConceptoContable;
import com.montanaritech.contable.contabilidad.mapeocuenta.ResolutorCuentas;
import com.montanaritech.contable.facturacion.comprobantetributo.ComprobanteTipo;
import com.montanaritech.contable.facturacion.comprobantetributo.ComprobanteTributo;
import com.montanaritech.contable.facturacion.comprobantetributo.ComprobanteTributoRepository;
import com.montanaritech.contable.facturacion.facturaventa.FacturaVenta;
import com.montanaritech.contable.maestros.moneda.MonedaRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Generador de asiento para cobro (F4.1 §6.1/§6.3/§6.5, F4.4). A diferencia
 * de los generadores de F4.2/F4.3, este es <b>impuro a propósito</b>: fija
 * {@code montoArsCancelado} en cada {@link CobroImputacion} y {@code
 * montoAnticipo} en el {@link Cobro} mismo mientras calcula, porque son
 * entidades ya administradas por JPA dentro de la misma transacción de
 * confirmación — evita recalcular el algoritmo de residuo una segunda vez en
 * el service solo para persistirlo.
 *
 * <p>Dos ajustes de precisión que no hacían falta en F4.2/F4.3 (que nunca
 * generaban un monto ARS que no fuera un producto directo original × TC):
 * la línea de diferencia de cambio es siempre en ARS (es una ganancia/pérdida
 * en pesos, no "algo en USD convertido" — así evita el chequeo
 * {@code MONTO_ARS_INCONSISTENTE} de {@code AsientoService}, que no aplica a
 * líneas en la moneda de libro). La línea de CxC usa un TC "efectivo"
 * (monto cancelado ARS ÷ monto imputado original) en vez del TC real de la
 * factura: en la imputación que cierra el saldo (regla del residuo, F3.1
 * §6.3), el monto cancelado no es necesariamente {@code original × TC_factura}
 * exacto, así que el TC real fallaría ese mismo chequeo.
 */
@Component
@RequiredArgsConstructor
public class CobroAsientoGenerator implements AsientoGenerator<Cobro> {

    private final ResolutorCuentas resolutorCuentas;
    private final ComprobanteTributoRepository comprobanteTributoRepo;
    private final CobroImputacionRepository cobroImputacionRepo;
    private final AplicacionAnticipoClienteRepository aplicacionAnticipoRepo;
    private final MonedaRepository monedaRepo;

    @Override
    public AsientoGenerado generar(Cobro cobro) {
        if (cobro.getTotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new NegocioException("COBRO_SIN_IMPORTE", "El cobro no tiene ningún importe a contabilizar");
        }

        Long monedaId = cobro.getMoneda().getId();
        BigDecimal tc = cobro.getTipoCambio();
        Long clienteId = cobro.getCliente().getId();
        String fuenteTc = cobro.getFuenteTc() != null ? cobro.getFuenteTc().name() : "MANUAL";

        List<LineaAsientoGenerada> lineas = new ArrayList<>();

        // 1) Retenciones sufridas (Debe) — determinan lo que efectivamente ingresa a fondos.
        List<ComprobanteTributo> retenciones = comprobanteTributoRepo.findByComprobanteTipoAndComprobanteIdOrderByIdAsc(
                ComprobanteTipo.COBRO, cobro.getId());
        BigDecimal sumaRetencionesOriginal = BigDecimal.ZERO;
        List<LineaAsientoGenerada> lineasRetenciones = new ArrayList<>();
        for (ComprobanteTributo retencion : retenciones) {
            ConceptoContable concepto = switch (retencion.getTipo()) {
                case RETENCION_GANANCIAS -> ConceptoContable.RETENCION_GANANCIAS_SUFRIDA;
                case RETENCION_IVA -> ConceptoContable.RETENCION_IVA_SUFRIDA;
                default -> throw new NegocioException("TRIBUTO_NO_APLICABLE_A_COBRO",
                        "El tributo %s no genera línea de asiento en un cobro".formatted(retencion.getTipo()));
            };
            CuentaContable cuentaRetencion = resolutorCuentas.resolver(concepto);
            BigDecimal importeArs = CalculoImputacion.round2(retencion.getImporte().multiply(tc));
            lineasRetenciones.add(new LineaAsientoGenerada(cuentaRetencion.getCodigo(), importeArs, BigDecimal.ZERO,
                    "Retención " + retencion.getTipo(), monedaId, retencion.getImporte(), tc, fuenteTc, null, null, clienteId, null, null));
            sumaRetencionesOriginal = sumaRetencionesOriginal.add(retencion.getImporte());
        }

        // 2) Fondos (Debe) — lo efectivamente ingresado, neto de retenciones.
        BigDecimal montoFondosOriginal = cobro.getTotal().subtract(sumaRetencionesOriginal);
        if (montoFondosOriginal.compareTo(BigDecimal.ZERO) < 0) {
            throw new NegocioException("RETENCIONES_EXCEDEN_TOTAL_COBRADO",
                    "Las retenciones informadas superan el total cobrado");
        }
        CuentaContable cuentaFondos = cobro.getCuentaBancaria().getCuentaContable();
        BigDecimal montoFondosArs = CalculoImputacion.round2(montoFondosOriginal.multiply(tc));
        lineas.add(new LineaAsientoGenerada(cuentaFondos.getCodigo(), montoFondosArs, BigDecimal.ZERO,
                "Cobro de " + cobro.getCliente().getNombre(), monedaId, montoFondosOriginal, tc, fuenteTc,
                null, null, null, null, cobro.getCuentaBancaria().getId()));
        lineas.addAll(lineasRetenciones);

        // 3) Imputaciones contra facturas (Haber CxC + dif. de cambio, F3.1 §6.3 regla del residuo).
        BigDecimal sumaImputadoOriginal = BigDecimal.ZERO;
        for (CobroImputacion imputacion : cobro.getLineas()) {
            FacturaVenta factura = imputacion.getFacturaVenta();
            if (!factura.getMoneda().getId().equals(monedaId)) {
                throw new NegocioException("IMPUTACION_MONEDA_NO_COINCIDE",
                        "La factura %s está en otra moneda distinta a la del cobro".formatted(factura.getNumero()));
            }
            if (factura.getEstado() != EstadoDocumento.CONFIRMADO) {
                throw new NegocioException("IMPUTACION_FACTURA_NO_CONFIRMADA",
                        "La factura %s no está confirmada".formatted(factura.getNumero()));
            }

            List<CobroImputacion> previasCobro = cobroImputacionRepo.findByFacturaVenta_IdAndCobro_EstadoOrderByIdAsc(
                    factura.getId(), EstadoDocumento.CONFIRMADO);
            List<AplicacionAnticipoCliente> previasAnticipo = aplicacionAnticipoRepo.findByFacturaVenta_IdOrderByIdAsc(factura.getId());

            BigDecimal sumaOrigPrevio = previasCobro.stream().map(CobroImputacion::getMontoImputadoOriginal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .add(previasAnticipo.stream().map(AplicacionAnticipoCliente::getMontoOriginal).reduce(BigDecimal.ZERO, BigDecimal::add));
            BigDecimal sumaArsPrevio = previasCobro.stream().map(CobroImputacion::getMontoArsCancelado)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .add(previasAnticipo.stream().map(AplicacionAnticipoCliente::getMontoArsCancelado).reduce(BigDecimal.ZERO, BigDecimal::add));
            BigDecimal saldoOrigAntes = factura.getTotal().subtract(sumaOrigPrevio);

            if (imputacion.getMontoImputadoOriginal().compareTo(saldoOrigAntes) > 0) {
                throw new NegocioException("IMPUTACION_EXCEDE_SALDO",
                        "La imputación supera el saldo pendiente de la factura %s".formatted(factura.getNumero()));
            }

            CalculoImputacion.Resultado resultado = CalculoImputacion.calcular(
                    imputacion.getMontoImputadoOriginal(), tc, factura.getTipoCambio(), saldoOrigAntes, factura.getTotalArs(), sumaArsPrevio);
            imputacion.setMontoArsCancelado(resultado.montoCanceladoArs());

            CuentaContable cuentaCxc = factura.getCliente().getCuentaCxc() != null
                    ? factura.getCliente().getCuentaCxc()
                    : resolutorCuentas.resolver(ConceptoContable.CREDITO_POR_VENTA);
            Long proyectoId = factura.getProyecto() != null ? factura.getProyecto().getId() : null;
            BigDecimal tcEfectivoCxc = tipoCambioEfectivo(resultado.montoCanceladoArs(), imputacion.getMontoImputadoOriginal());
            lineas.add(new LineaAsientoGenerada(cuentaCxc.getCodigo(), BigDecimal.ZERO, resultado.montoCanceladoArs(),
                    "Cobro factura " + factura.getNumero(), monedaId, imputacion.getMontoImputadoOriginal(), tcEfectivoCxc, fuenteTc,
                    proyectoId, null, clienteId, null, null));

            BigDecimal dif = resultado.diferenciaCambioArs();
            if (dif.compareTo(BigDecimal.ZERO) > 0) {
                CuentaContable cuentaGanada = resolutorCuentas.resolver(ConceptoContable.DIF_CAMBIO_GANADA);
                lineas.add(new LineaAsientoGenerada(cuentaGanada.getCodigo(), BigDecimal.ZERO, dif,
                        "Diferencia de cambio ganada", monedaArsId(), dif, BigDecimal.ONE, fuenteTc, proyectoId, null, clienteId, null, null));
            } else if (dif.compareTo(BigDecimal.ZERO) < 0) {
                CuentaContable cuentaPerdida = resolutorCuentas.resolver(ConceptoContable.DIF_CAMBIO_PERDIDA);
                lineas.add(new LineaAsientoGenerada(cuentaPerdida.getCodigo(), dif.negate(), BigDecimal.ZERO,
                        "Diferencia de cambio perdida", monedaArsId(), dif.negate(), BigDecimal.ONE, fuenteTc, proyectoId, null, clienteId, null, null));
            }

            BigDecimal recargo = imputacion.getRecargoMoraOriginal();
            if (recargo != null && recargo.compareTo(BigDecimal.ZERO) > 0) {
                CuentaContable cuentaMora = resolutorCuentas.resolver(ConceptoContable.INTERES_POR_MORA_GANADO);
                BigDecimal recargoArs = CalculoImputacion.round2(recargo.multiply(tc));
                lineas.add(new LineaAsientoGenerada(cuentaMora.getCodigo(), BigDecimal.ZERO, recargoArs,
                        "Recargo por mora factura " + factura.getNumero(), monedaId, recargo, tc, fuenteTc,
                        proyectoId, null, clienteId, null, null));
                sumaImputadoOriginal = sumaImputadoOriginal.add(recargo);
            }

            sumaImputadoOriginal = sumaImputadoOriginal.add(imputacion.getMontoImputadoOriginal());
        }

        // 4) Remanente no imputado = anticipo (Haber), congelado en el propio Cobro.
        BigDecimal montoAnticipoNuevo = cobro.getTotal().subtract(sumaImputadoOriginal);
        cobro.setMontoAnticipo(montoAnticipoNuevo);
        if (montoAnticipoNuevo.compareTo(BigDecimal.ZERO) > 0) {
            CuentaContable cuentaAnticipo = resolutorCuentas.resolver(ConceptoContable.ANTICIPO_CLIENTE);
            BigDecimal anticipoArs = CalculoImputacion.round2(montoAnticipoNuevo.multiply(tc));
            lineas.add(new LineaAsientoGenerada(cuentaAnticipo.getCodigo(), BigDecimal.ZERO, anticipoArs,
                    "Anticipo de " + cobro.getCliente().getNombre(), monedaId, montoAnticipoNuevo, tc, fuenteTc,
                    null, null, clienteId, null, null));
        }

        return new AsientoGenerado(cobro.getFecha(), "Cobro - " + cobro.getCliente().getNombre(), "COBRO",
                lineas, "Cobro", cobro.getId());
    }

    /**
     * Ajuste de aplicación posterior de anticipo (F4.1 §6.5, CO-5): cancela
     * el anticipo contra el CxC de la factura, materializando la diferencia
     * de cambio entre el TC del anticipo (TC_operación) y el de la factura
     * (TC_comprobante) — mismo {@link CalculoImputacion} que una imputación
     * normal, mismo criterio de signo que un cobro (nunca edita el asiento
     * original del anticipo).
     */
    public ResultadoAjusteAnticipo generarAjusteAplicacionAnticipo(Cobro anticipo, FacturaVenta factura, BigDecimal monto, java.time.LocalDate fecha) {
        Long monedaId = anticipo.getMoneda().getId();
        BigDecimal tcAnticipo = anticipo.getTipoCambio();
        Long clienteId = anticipo.getCliente().getId();
        String fuenteTc = anticipo.getFuenteTc() != null ? anticipo.getFuenteTc().name() : "MANUAL";
        Long proyectoId = factura.getProyecto() != null ? factura.getProyecto().getId() : null;

        List<CobroImputacion> previasCobro = cobroImputacionRepo.findByFacturaVenta_IdAndCobro_EstadoOrderByIdAsc(
                factura.getId(), EstadoDocumento.CONFIRMADO);
        List<AplicacionAnticipoCliente> previasAnticipo = aplicacionAnticipoRepo.findByFacturaVenta_IdOrderByIdAsc(factura.getId());

        BigDecimal sumaOrigPrevio = previasCobro.stream().map(CobroImputacion::getMontoImputadoOriginal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(previasAnticipo.stream().map(AplicacionAnticipoCliente::getMontoOriginal).reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal sumaArsPrevio = previasCobro.stream().map(CobroImputacion::getMontoArsCancelado)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(previasAnticipo.stream().map(AplicacionAnticipoCliente::getMontoArsCancelado).reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal saldoOrigAntes = factura.getTotal().subtract(sumaOrigPrevio);

        if (monto.compareTo(saldoOrigAntes) > 0) {
            throw new NegocioException("IMPUTACION_EXCEDE_SALDO",
                    "La aplicación supera el saldo pendiente de la factura %s".formatted(factura.getNumero()));
        }

        CalculoImputacion.Resultado resultado = CalculoImputacion.calcular(
                monto, tcAnticipo, factura.getTipoCambio(), saldoOrigAntes, factura.getTotalArs(), sumaArsPrevio);

        CuentaContable cuentaAnticipo = resolutorCuentas.resolver(ConceptoContable.ANTICIPO_CLIENTE);
        CuentaContable cuentaCxc = factura.getCliente().getCuentaCxc() != null
                ? factura.getCliente().getCuentaCxc()
                : resolutorCuentas.resolver(ConceptoContable.CREDITO_POR_VENTA);

        List<LineaAsientoGenerada> lineas = new ArrayList<>();
        lineas.add(new LineaAsientoGenerada(cuentaAnticipo.getCodigo(), resultado.montoFondosArs(), BigDecimal.ZERO,
                "Aplicación de anticipo a factura " + factura.getNumero(), monedaId, monto, tcAnticipo, fuenteTc,
                proyectoId, null, clienteId, null, null));

        BigDecimal dif = resultado.diferenciaCambioArs();
        if (dif.compareTo(BigDecimal.ZERO) > 0) {
            CuentaContable cuentaGanada = resolutorCuentas.resolver(ConceptoContable.DIF_CAMBIO_GANADA);
            lineas.add(new LineaAsientoGenerada(cuentaGanada.getCodigo(), BigDecimal.ZERO, dif,
                    "Diferencia de cambio ganada", monedaArsId(), dif, BigDecimal.ONE, fuenteTc, proyectoId, null, clienteId, null, null));
        } else if (dif.compareTo(BigDecimal.ZERO) < 0) {
            CuentaContable cuentaPerdida = resolutorCuentas.resolver(ConceptoContable.DIF_CAMBIO_PERDIDA);
            lineas.add(new LineaAsientoGenerada(cuentaPerdida.getCodigo(), dif.negate(), BigDecimal.ZERO,
                    "Diferencia de cambio perdida", monedaArsId(), dif.negate(), BigDecimal.ONE, fuenteTc, proyectoId, null, clienteId, null, null));
        }

        BigDecimal tcEfectivoCxc = tipoCambioEfectivo(resultado.montoCanceladoArs(), monto);
        lineas.add(new LineaAsientoGenerada(cuentaCxc.getCodigo(), BigDecimal.ZERO, resultado.montoCanceladoArs(),
                "Aplicación de anticipo a factura " + factura.getNumero(), monedaId, monto, tcEfectivoCxc, fuenteTc,
                proyectoId, null, clienteId, null, null));

        AsientoGenerado generado = new AsientoGenerado(fecha, "Aplicación de anticipo - " + factura.getNumero(),
                "AJUSTE", lineas, "Cobro", anticipo.getId());
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
