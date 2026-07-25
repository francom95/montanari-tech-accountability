package com.montanaritech.contable.flujocaja;

import com.montanaritech.contable.bancos.movimientobancario.EstadoMovimientoBancario;
import com.montanaritech.contable.bancos.movimientobancario.MovimientoBancarioRepository;
import com.montanaritech.contable.bancos.tarjetacredito.ConsumoTarjetaRepository;
import com.montanaritech.contable.bancos.tarjetacredito.PagoTarjetaRepository;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.common.saldo.RecalculoSaldoService;
import com.montanaritech.contable.compromiso.Compromiso;
import com.montanaritech.contable.compromiso.CompromisoService;
import com.montanaritech.contable.compromiso.EstadoCompromiso;
import com.montanaritech.contable.facturacion.cobro.CobroImputacion;
import com.montanaritech.contable.facturacion.cobro.CobroImputacionRepository;
import com.montanaritech.contable.facturacion.cuentasporpagar.CuentaPorPagarService;
import com.montanaritech.contable.facturacion.cuentasporpagar.dto.CuentaPorPagarFilaResponse;
import com.montanaritech.contable.facturacion.facturaventa.FacturaVenta;
import com.montanaritech.contable.facturacion.facturaventa.FacturaVentaRepository;
import com.montanaritech.contable.flujocaja.dto.FlujoCajaResponse;
import com.montanaritech.contable.flujocaja.dto.PuntoFlujoCaja;
import com.montanaritech.contable.inversion.Inversion;
import com.montanaritech.contable.inversion.InversionService;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancaria;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancariaRepository;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.proyecto.Proyecto;
import com.montanaritech.contable.maestros.proyecto.ProyectoCuota;
import com.montanaritech.contable.maestros.proyecto.ProyectoRepository;
import com.montanaritech.contable.maestros.tarjetacredito.TarjetaCredito;
import com.montanaritech.contable.maestros.tarjetacredito.TarjetaCreditoRepository;
import com.montanaritech.contable.maestros.tipocambio.TipoCambio;
import com.montanaritech.contable.maestros.tipocambio.TipoCambioRepository;
import com.montanaritech.contable.vencimientos.EstadoVencimientoObligacion;
import com.montanaritech.contable.vencimientos.Vencimiento;
import com.montanaritech.contable.vencimientos.VencimientoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Flujo de caja real y proyectado (F8.3, más inversiones F8.4). Agregador de
 * solo lectura sobre fuentes ya cerradas — no crea ninguna entidad propia. La
 * fórmula del real queda en {@code saldo inicial + cobrado − pagado} (sin
 * "± financiación": no aparece en ningún paso futuro del plan; decisión
 * confirmada con el usuario, ver outputs/F8_3_...md). El proyectado suma una
 * 5ª fuente desde F8.4: si una {@code Inversion} activa tiene un vínculo
 * (Compromiso/Vencimiento) pendiente dentro de la ventana, su valuación
 * actual se proyecta como ingreso en la fecha de esa obligación — "rescate
 * planificado como ingreso proyectado" (decisión confirmada con el usuario,
 * ver outputs/F8_4_...md).
 */
@Service
@RequiredArgsConstructor
public class FlujoCajaService {

    private final CuentaBancariaRepository cuentaBancariaRepo;
    private final TarjetaCreditoRepository tarjetaCreditoRepo;
    private final MovimientoBancarioRepository movimientoBancarioRepo;
    private final ConsumoTarjetaRepository consumoTarjetaRepo;
    private final PagoTarjetaRepository pagoTarjetaRepo;
    private final RecalculoSaldoService recalculoSaldoService;
    private final TipoCambioRepository tipoCambioRepo;
    private final CompromisoService compromisoService;
    private final VencimientoService vencimientoService;
    private final CuentaPorPagarService cuentaPorPagarService;
    private final ProyectoRepository proyectoRepo;
    private final FacturaVentaRepository facturaVentaRepo;
    private final CobroImputacionRepository cobroImputacionRepo;
    private final InversionService inversionService;

    private record MovimientoDatado(LocalDate fecha, BigDecimal monto) {}

    /**
     * Flujo real por cuenta (moneda nativa) y consolidado (ARS). Los
     * movimientos ya traen su propio {@code importeArs} histórico; solo el
     * saldo inicial de una cuenta no-ARS se convierte con la última
     * cotización cargada (F8.3 §2 — no hay TC de proyección configurable).
     */
    @Transactional(readOnly = true)
    public FlujoCajaResponse flujoReal(LocalDate desde, LocalDate hasta, Granularidad granularidad) {
        List<String> advertencias = new ArrayList<>();
        Map<Long, BigDecimal> tcCache = new HashMap<>();
        Map<String, List<PuntoFlujoCaja>> porCuenta = new LinkedHashMap<>();
        List<MovimientoDatado> nativosConsolidadosArs = new ArrayList<>();
        BigDecimal saldoInicialConsolidadoArs = BigDecimal.ZERO;
        boolean huboContribucionConsolidado = false;

        for (CuentaBancaria cuenta : cuentaBancariaRepo.findByActivoTrue()) {
            BigDecimal saldoInicialNativo = recalculoSaldoService.recalcularCuentaBancariaHasta(cuenta, desde.minusDays(1));
            List<MovimientoDatado> nativos = movimientoBancarioRepo.buscarParaConciliacion(cuenta.getId(), desde, hasta).stream()
                    .filter(m -> m.getEstado() != EstadoMovimientoBancario.DESCARTADO && m.getFecha() != null)
                    .map(m -> new MovimientoDatado(m.getFecha(), m.getImporte()))
                    .toList();
            porCuenta.put(cuenta.getAlias(), construirSerie(saldoInicialNativo, nativos, desde, hasta, granularidad));

            BigDecimal tc = tipoCambioMasReciente(cuenta.getMoneda(), advertencias, tcCache);
            if (tc == null) {
                continue;
            }
            huboContribucionConsolidado = true;
            saldoInicialConsolidadoArs = saldoInicialConsolidadoArs.add(saldoInicialNativo.multiply(tc));
            List<MovimientoDatado> enArs = movimientoBancarioRepo.buscarParaConciliacion(cuenta.getId(), desde, hasta).stream()
                    .filter(m -> m.getEstado() != EstadoMovimientoBancario.DESCARTADO && m.getFecha() != null)
                    .map(m -> new MovimientoDatado(m.getFecha(), m.getImporteArs()))
                    .toList();
            nativosConsolidadosArs.addAll(enArs);
        }

        for (TarjetaCredito tarjeta : tarjetaCreditoRepo.findByActivoTrue()) {
            BigDecimal saldoInicialNativo = saldoInicialTarjetaHasta(tarjeta, desde.minusDays(1));
            List<MovimientoDatado> nativos = movimientosTarjeta(tarjeta, desde, hasta, false);
            porCuenta.put(tarjeta.getEntidad(), construirSerie(saldoInicialNativo, nativos, desde, hasta, granularidad));

            BigDecimal tc = tipoCambioMasReciente(tarjeta.getMoneda(), advertencias, tcCache);
            if (tc == null) {
                continue;
            }
            huboContribucionConsolidado = true;
            saldoInicialConsolidadoArs = saldoInicialConsolidadoArs.add(saldoInicialNativo.multiply(tc));
            nativosConsolidadosArs.addAll(movimientosTarjeta(tarjeta, desde, hasta, true));
        }

        // Sin ninguna cuenta contribuyendo (todas excluidas por falta de TC), una
        // serie "en cero" sería engañosa — se ve como saldo real conocido en vez de
        // "no hay datos". Vacío + advertencias explica la ausencia sin inventar dato.
        List<PuntoFlujoCaja> consolidado = huboContribucionConsolidado
                ? construirSerie(saldoInicialConsolidadoArs, nativosConsolidadosArs, desde, hasta, granularidad)
                : List.of();
        return new FlujoCajaResponse(consolidado, porCuenta, advertencias);
    }

    /**
     * Flujo proyectado desde hoy: parte del saldo real de caja/banco (no
     * incluye tarjetas — su próximo pago ya entra como Vencimiento tipo
     * TARJETA, sumarlo aparte duplicaría el egreso) y agrega 5 fuentes ya
     * cerradas: cuotas de proyecto pendientes (emparejadas contra cobros
     * confirmados, F8.3 §1), compromisos pendientes (F8.2), vencimientos
     * pendientes (F8.1), CxP pendientes (F4.5), e inversiones con rescate
     * planificado (F8.4 — vínculo a un Compromiso/Vencimiento pendiente).
     * Siempre granularidad DIARIO — la detección de saldo negativo es por
     * día, no por semana/mes.
     */
    @Transactional(readOnly = true)
    public FlujoCajaResponse flujoProyectado(int dias) {
        List<String> advertencias = new ArrayList<>();
        Map<Long, BigDecimal> tcCache = new HashMap<>();
        LocalDate hoy = LocalDate.now();
        LocalDate hasta = hoy.plusDays(dias);

        BigDecimal saldoInicialArs = BigDecimal.ZERO;
        for (CuentaBancaria cuenta : cuentaBancariaRepo.findByActivoTrue()) {
            BigDecimal tc = tipoCambioMasReciente(cuenta.getMoneda(), advertencias, tcCache);
            if (tc == null) {
                continue;
            }
            BigDecimal saldo = recalculoSaldoService.recalcularCuentaBancariaHasta(cuenta, hoy);
            saldoInicialArs = saldoInicialArs.add(saldo.multiply(tc));
        }

        List<MovimientoDatado> movimientos = new ArrayList<>();

        for (Compromiso c : compromisoService.porRangoDeFechas(hoy, hasta)) {
            if (c.getEstado() != EstadoCompromiso.PENDIENTE || !c.isActivo()) {
                continue;
            }
            BigDecimal tc = tipoCambioMasReciente(c.getMoneda(), advertencias, tcCache);
            if (tc == null) {
                continue;
            }
            movimientos.add(new MovimientoDatado(c.getFechaPrevista(), c.getImporte().multiply(tc).negate()));
        }

        for (Vencimiento v : vencimientoService.porRangoDeFechas(hoy, hasta)) {
            if (v.getImporteEstimado() == null) {
                continue;
            }
            BigDecimal tc = tipoCambioMasReciente(v.getMoneda(), advertencias, tcCache);
            if (tc == null) {
                continue;
            }
            movimientos.add(new MovimientoDatado(v.getFecha(), v.getImporteEstimado().multiply(tc).negate()));
        }

        for (CuentaPorPagarFilaResponse fila : cuentaPorPagarService.calcular(null, null, null, null, null, null).filas()) {
            if (fila.fechaVencimiento() == null || fila.fechaVencimiento().isBefore(hoy) || fila.fechaVencimiento().isAfter(hasta)) {
                continue;
            }
            movimientos.add(new MovimientoDatado(fila.fechaVencimiento(), fila.saldoArs().negate()));
        }

        for (Proyecto proyecto : proyectoRepo.findByActivoTrueOrderByNombreAsc()) {
            movimientos.addAll(cuotasPendientes(proyecto, hoy, hasta, advertencias, tcCache));
        }

        for (Inversion inv : inversionService.buscarActivasConVinculo()) {
            LocalDate fechaVinculo = resolverFechaVinculoPendiente(inv, advertencias);
            if (fechaVinculo == null || fechaVinculo.isBefore(hoy) || fechaVinculo.isAfter(hasta)) {
                continue;
            }
            BigDecimal valuacion = inversionService.aResponse(inv).valuacionActual();
            movimientos.add(new MovimientoDatado(fechaVinculo, valuacion));
        }

        List<PuntoFlujoCaja> consolidado = construirSerie(saldoInicialArs, movimientos, hoy, hasta, Granularidad.DIARIO, false);
        return new FlujoCajaResponse(consolidado, Map.of(), advertencias);
    }

    /**
     * Concatena real (hasta ayer) + proyectado (desde hoy) en una sola serie
     * consolidada, con {@code esReal} distinguiendo cada punto — el frontend
     * dibuja la línea de "hoy" en el borde entre ambos tramos.
     */
    @Transactional(readOnly = true)
    public FlujoCajaResponse combinado(int diasAtras, int diasAdelante) {
        LocalDate hoy = LocalDate.now();
        List<PuntoFlujoCaja> consolidado = new ArrayList<>();
        Map<String, List<PuntoFlujoCaja>> porCuenta = Map.of();
        Set<String> advertencias = new LinkedHashSet<>();
        if (diasAtras > 0) {
            FlujoCajaResponse real = flujoReal(hoy.minusDays(diasAtras), hoy.minusDays(1), Granularidad.DIARIO);
            consolidado.addAll(real.consolidado());
            porCuenta = real.porCuenta();
            advertencias.addAll(real.advertencias());
        }
        FlujoCajaResponse proyectado = flujoProyectado(diasAdelante);
        consolidado.addAll(proyectado.consolidado());
        advertencias.addAll(proyectado.advertencias());
        return new FlujoCajaResponse(consolidado, porCuenta, new ArrayList<>(advertencias));
    }

    /**
     * Cuotas de un proyecto todavía no cobradas (F8.3 §1): se emparejan
     * ordinalmente cuotas (por número) contra cobros confirmados distintos
     * que imputaron contra las facturas de venta confirmadas del proyecto
     * (por cantidad, no por fecha — solo importa cuántas ya se cobraron para
     * saber cuántas, desde el principio de la lista, quedan pendientes).
     */
    private List<MovimientoDatado> cuotasPendientes(Proyecto proyecto, LocalDate desde, LocalDate hasta,
            List<String> advertencias, Map<Long, BigDecimal> tcCache) {
        List<ProyectoCuota> cuotasOrdenadas = proyecto.getCuotas().stream()
                .sorted(Comparator.comparing(ProyectoCuota::getNumero))
                .toList();
        if (cuotasOrdenadas.isEmpty()) {
            return List.of();
        }
        List<FacturaVenta> facturas = facturaVentaRepo.buscarConfirmadasParaReporte(null, proyecto.getId(), null, null, null);
        Set<Long> cobrosDistintos = new LinkedHashSet<>();
        for (FacturaVenta factura : facturas) {
            for (CobroImputacion ci : cobroImputacionRepo.findByFacturaVenta_IdAndCobro_EstadoOrderByIdAsc(
                    factura.getId(), EstadoDocumento.CONFIRMADO)) {
                cobrosDistintos.add(ci.getCobro().getId());
            }
        }
        int cobradas = Math.min(cuotasOrdenadas.size(), cobrosDistintos.size());
        List<ProyectoCuota> pendientes = cuotasOrdenadas.subList(cobradas, cuotasOrdenadas.size());
        if (pendientes.isEmpty() || proyecto.getMoneda() == null) {
            return List.of();
        }
        BigDecimal tc = tipoCambioMasReciente(proyecto.getMoneda(), advertencias, tcCache);
        if (tc == null) {
            return List.of();
        }
        List<MovimientoDatado> resultado = new ArrayList<>();
        for (ProyectoCuota cuota : pendientes) {
            if (cuota.getFechaEstimadaCobro().isBefore(desde) || cuota.getFechaEstimadaCobro().isAfter(hasta)) {
                continue;
            }
            resultado.add(new MovimientoDatado(cuota.getFechaEstimadaCobro(), cuota.getImporte().multiply(tc)));
        }
        return resultado;
    }

    /**
     * Fecha de la obligación vinculada a una Inversión (F8.4), solo si sigue
     * pendiente — si ya se resolvió/canceló, no se proyecta (evitaría un
     * ingreso fantasma para una obligación que ya no existe como tal).
     */
    private LocalDate resolverFechaVinculoPendiente(Inversion inv, List<String> advertencias) {
        try {
            return switch (inv.getVinculoTipo()) {
                case COMPROMISO -> {
                    Compromiso c = compromisoService.obtener(inv.getVinculoRefId());
                    yield (c.getEstado() == EstadoCompromiso.PENDIENTE && c.isActivo()) ? c.getFechaPrevista() : null;
                }
                case VENCIMIENTO -> {
                    Vencimiento v = vencimientoService.obtener(inv.getVinculoRefId());
                    yield v.getEstado() == EstadoVencimientoObligacion.PENDIENTE ? v.getFecha() : null;
                }
            };
        } catch (RecursoNoEncontradoException e) {
            advertencias.add("La inversión \"" + inv.getInstrumento() + "\" está vinculada a un "
                    + inv.getVinculoTipo() + " (id " + inv.getVinculoRefId() + ") que ya no existe — se excluye de la proyección.");
            return null;
        }
    }

    /** Consumos (egreso) + pagos confirmados (ingreso a favor de la tarjeta) de una tarjeta en una ventana, nativo o ARS. */
    private List<MovimientoDatado> movimientosTarjeta(TarjetaCredito tarjeta, LocalDate desde, LocalDate hasta, boolean enArs) {
        List<MovimientoDatado> movimientos = new ArrayList<>();
        for (var consumo : consumoTarjetaRepo.findByTarjetaCredito_IdAndFechaBetween(tarjeta.getId(), desde, hasta)) {
            movimientos.add(new MovimientoDatado(consumo.getFecha(), enArs ? consumo.getImporteArs() : consumo.getImporte()));
        }
        for (var pago : pagoTarjetaRepo.findByTarjetaCredito_IdAndEstadoAndFechaBetween(
                tarjeta.getId(), EstadoDocumento.CONFIRMADO, desde, hasta)) {
            movimientos.add(new MovimientoDatado(pago.getFecha(), enArs ? pago.getImporteArs() : pago.getImporte()));
        }
        return movimientos;
    }

    /** Saldo de tarjeta a una fecha de corte (no hay variante acotada en {@code RecalculoSaldoService}, solo "hasta hoy"). */
    private BigDecimal saldoInicialTarjetaHasta(TarjetaCredito tarjeta, LocalDate fechaHasta) {
        BigDecimal saldo = tarjeta.getSaldoInicial();
        if (fechaHasta.isBefore(tarjeta.getFechaSaldoInicial())) {
            return saldo;
        }
        for (var consumo : consumoTarjetaRepo.findByTarjetaCredito_IdAndFechaBetween(
                tarjeta.getId(), tarjeta.getFechaSaldoInicial(), fechaHasta)) {
            saldo = saldo.add(consumo.getImporte());
        }
        for (var pago : pagoTarjetaRepo.findByTarjetaCredito_IdAndEstadoAndFechaBetween(
                tarjeta.getId(), EstadoDocumento.CONFIRMADO, tarjeta.getFechaSaldoInicial(), fechaHasta)) {
            saldo = saldo.add(pago.getImporte());
        }
        return saldo;
    }

    /** Última cotización cargada para la moneda (F8.3 §2); ARS es identidad. Null + advertencia si no hay ninguna. */
    private BigDecimal tipoCambioMasReciente(Moneda moneda, List<String> advertencias, Map<Long, BigDecimal> cache) {
        if ("ARS".equals(moneda.getCodigo())) {
            return BigDecimal.ONE;
        }
        return cache.computeIfAbsent(moneda.getId(), id -> tipoCambioRepo.findFirstByMonedaIdAndActivoTrueOrderByFechaDesc(id)
                .map(TipoCambio::getValorVenta)
                .orElseGet(() -> {
                    advertencias.add("No hay ninguna cotización cargada para " + moneda.getCodigo()
                            + " — se excluye del consolidado en ARS (el detalle por cuenta en su moneda nativa sí está completo).");
                    return null;
                }));
    }

    /** Bucketea movimientos datados en una serie continua (incluye buckets sin movimientos) con saldo corrido. */
    private List<PuntoFlujoCaja> construirSerie(BigDecimal saldoInicial, List<MovimientoDatado> movimientos,
            LocalDate desde, LocalDate hasta, Granularidad granularidad) {
        return construirSerie(saldoInicial, movimientos, desde, hasta, granularidad, true);
    }

    private List<PuntoFlujoCaja> construirSerie(BigDecimal saldoInicial, List<MovimientoDatado> movimientos,
            LocalDate desde, LocalDate hasta, Granularidad granularidad, boolean esReal) {
        TreeMap<LocalDate, BigDecimal> ingresosPorBucket = new TreeMap<>();
        TreeMap<LocalDate, BigDecimal> egresosPorBucket = new TreeMap<>();
        for (LocalDate d = inicioBucket(desde, granularidad); !d.isAfter(hasta); d = siguienteBucket(d, granularidad)) {
            ingresosPorBucket.put(d, BigDecimal.ZERO);
            egresosPorBucket.put(d, BigDecimal.ZERO);
        }
        for (MovimientoDatado mov : movimientos) {
            if (mov.fecha().isBefore(desde) || mov.fecha().isAfter(hasta)) {
                continue;
            }
            LocalDate clave = inicioBucket(mov.fecha(), granularidad);
            if (mov.monto().signum() >= 0) {
                ingresosPorBucket.merge(clave, mov.monto(), BigDecimal::add);
            } else {
                egresosPorBucket.merge(clave, mov.monto().abs(), BigDecimal::add);
            }
        }
        List<PuntoFlujoCaja> serie = new ArrayList<>();
        BigDecimal acumulado = saldoInicial;
        for (LocalDate clave : ingresosPorBucket.keySet()) {
            BigDecimal ingresos = ingresosPorBucket.get(clave);
            BigDecimal egresos = egresosPorBucket.get(clave);
            BigDecimal saldoInicialBucket = acumulado;
            BigDecimal saldoFinalBucket = saldoInicialBucket.add(ingresos).subtract(egresos);
            serie.add(new PuntoFlujoCaja(clave, saldoInicialBucket, ingresos, egresos, saldoFinalBucket, esReal, saldoFinalBucket.signum() < 0));
            acumulado = saldoFinalBucket;
        }
        return serie;
    }

    private LocalDate inicioBucket(LocalDate fecha, Granularidad granularidad) {
        return switch (granularidad) {
            case DIARIO -> fecha;
            case SEMANAL -> fecha.minusDays(fecha.getDayOfWeek().getValue() - 1);
            case MENSUAL -> fecha.withDayOfMonth(1);
        };
    }

    private LocalDate siguienteBucket(LocalDate inicioActual, Granularidad granularidad) {
        return switch (granularidad) {
            case DIARIO -> inicioActual.plusDays(1);
            case SEMANAL -> inicioActual.plusWeeks(1);
            case MENSUAL -> inicioActual.plusMonths(1);
        };
    }
}
