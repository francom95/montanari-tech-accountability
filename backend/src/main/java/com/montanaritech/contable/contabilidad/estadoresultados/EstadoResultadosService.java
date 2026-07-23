package com.montanaritech.contable.contabilidad.estadoresultados;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.contabilidad.asiento.AsientoLineaRepository;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContableRepository;
import com.montanaritech.contable.contabilidad.estadoresultados.dto.EstadoResultadosDtos.ComparativoMes;
import com.montanaritech.contable.contabilidad.estadoresultados.dto.EstadoResultadosDtos.CuentaMonto;
import com.montanaritech.contable.contabilidad.estadoresultados.dto.EstadoResultadosDtos.EstadoResultadosCalculado;
import com.montanaritech.contable.contabilidad.estadoresultados.dto.EstadoResultadosDtos.EstadoResultadosPorProyectoItem;
import com.montanaritech.contable.contabilidad.estadoresultados.dto.EstadoResultadosDtos.EstadoResultadosPorProyectoResponse;
import com.montanaritech.contable.contabilidad.estadoresultados.dto.EstadoResultadosDtos.EstadoResultadosResponse;
import com.montanaritech.contable.contabilidad.estadoresultados.dto.EstadoResultadosDtos.LineaCalculada;
import com.montanaritech.contable.maestros.categoria.Categoria;
import com.montanaritech.contable.maestros.proyecto.Proyecto;
import com.montanaritech.contable.maestros.proyecto.ProyectoRepository;
import com.montanaritech.contable.maestros.rubro.Rubro;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Estado de resultados (F7.3): agrupa las cuentas de naturaleza RP/RN por
 * rubro→línea (mapeo configurable, {@link MapeoRubroLineaEstadoResultados}) y
 * calcula los 3 subtotales fijos. Reusa la misma query agregada de F7.2
 * ({@code sumarDebeHaberPorCuenta}), pero agrupando por rubro en vez de por
 * jerarquía de cuenta.
 */
@Service
@RequiredArgsConstructor
public class EstadoResultadosService {

    private final AsientoLineaRepository lineaRepo;
    private final CuentaContableRepository cuentaRepo;
    private final MapeoRubroLineaEstadoResultadosRepository mapeoRepo;
    private final ProyectoRepository proyectoRepo;

    @Transactional(readOnly = true)
    public EstadoResultadosResponse porMes(int anio, int mes) {
        YearMonth periodo = YearMonth.of(anio, mes);
        EstadoResultadosCalculado actual = calcularParaPeriodo(periodo.atDay(1), periodo.atEndOfMonth(), null);
        return new EstadoResultadosResponse(actual, comparativoMesAnterior(anio, mes, actual));
    }

    @Transactional(readOnly = true)
    public EstadoResultadosResponse porAnio(int anio) {
        EstadoResultadosCalculado actual = calcularParaPeriodo(LocalDate.of(anio, 1, 1), LocalDate.of(anio, 12, 31), null);
        return new EstadoResultadosResponse(actual, null);
    }

    @Transactional(readOnly = true)
    public EstadoResultadosResponse acumulado(int anio, int mes) {
        YearMonth periodo = YearMonth.of(anio, mes);
        EstadoResultadosCalculado actual = calcularParaPeriodo(LocalDate.of(anio, 1, 1), periodo.atEndOfMonth(), null);
        return new EstadoResultadosResponse(actual, null);
    }

    @Transactional(readOnly = true)
    public EstadoResultadosPorProyectoResponse porProyecto(int anio, int mes) {
        YearMonth periodo = YearMonth.of(anio, mes);
        LocalDate desde = periodo.atDay(1);
        LocalDate hasta = periodo.atEndOfMonth();

        List<EstadoResultadosPorProyectoItem> items = new ArrayList<>();
        for (Proyecto proyecto : proyectoRepo.findByActivoTrueOrderByNombreAsc()) {
            EstadoResultadosCalculado calculado = calcularParaPeriodo(desde, hasta, proyecto.getId());
            if (calculado.tieneMovimiento()) {
                items.add(new EstadoResultadosPorProyectoItem(proyecto.getId(), proyecto.getNombre(), calculado));
            }
        }
        EstadoResultadosCalculado sinProyecto = calcularDesdeFilas(lineaRepo.sumarDebeHaberPorCuentaSinProyecto(desde, hasta, EstadoDocumento.CONFIRMADO));
        return new EstadoResultadosPorProyectoResponse(items, sinProyecto);
    }

    private ComparativoMes comparativoMesAnterior(int anio, int mes, EstadoResultadosCalculado actual) {
        YearMonth anterior = YearMonth.of(anio, mes).minusMonths(1);
        EstadoResultadosCalculado calculadoAnterior = calcularParaPeriodo(anterior.atDay(1), anterior.atEndOfMonth(), null);
        BigDecimal variacionAbsoluta = actual.resultadoFinal().subtract(calculadoAnterior.resultadoFinal());
        BigDecimal anteriorFinal = calculadoAnterior.resultadoFinal();
        BigDecimal variacionPorcentual = anteriorFinal.compareTo(BigDecimal.ZERO) == 0
                ? null
                : variacionAbsoluta.divide(anteriorFinal.abs(), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        return new ComparativoMes(anterior.getYear(), anterior.getMonthValue(), anteriorFinal, variacionAbsoluta, variacionPorcentual);
    }

    private EstadoResultadosCalculado calcularParaPeriodo(LocalDate fechaDesde, LocalDate fechaHasta, Long proyectoId) {
        return calcularDesdeFilas(lineaRepo.sumarDebeHaberPorCuenta(fechaDesde, fechaHasta, proyectoId, EstadoDocumento.CONFIRMADO));
    }

    private EstadoResultadosCalculado calcularDesdeFilas(List<Object[]> filas) {
        Map<LineaEstadoResultados, List<CuentaMonto>> porLinea = new EnumMap<>(LineaEstadoResultados.class);
        for (LineaEstadoResultados linea : LineaEstadoResultados.values()) {
            porLinea.put(linea, new ArrayList<>());
        }
        List<CuentaMonto> sinMapear = new ArrayList<>();

        if (!filas.isEmpty()) {
            List<Long> cuentaIds = filas.stream().map(f -> (Long) f[0]).toList();
            Map<Long, CuentaContable> cuentasPorId = cuentaRepo.findAllById(cuentaIds).stream()
                    .collect(java.util.stream.Collectors.toMap(CuentaContable::getId, c -> c));

            for (Object[] fila : filas) {
                CuentaContable cuenta = cuentasPorId.get((Long) fila[0]);
                if (cuenta == null) {
                    continue;
                }
                Categoria.TipoCategoria naturaleza = cuenta.getNaturaleza();
                if (naturaleza != Categoria.TipoCategoria.RP && naturaleza != Categoria.TipoCategoria.RN) {
                    continue; // cuenta de balance (activo/pasivo/PN): fuera del estado de resultados
                }
                BigDecimal debe = (BigDecimal) fila[1];
                BigDecimal haber = (BigDecimal) fila[2];
                BigDecimal monto = naturaleza == Categoria.TipoCategoria.RP ? haber.subtract(debe) : debe.subtract(haber);
                CuentaMonto cuentaMonto = new CuentaMonto(cuenta.getId(), cuenta.getCodigo(), cuenta.getNombre(), monto);

                Rubro rubro = cuenta.getRubro();
                Optional<MapeoRubroLineaEstadoResultados> mapeo = rubro == null
                        ? Optional.empty()
                        : mapeoRepo.findByRubroIdAndNaturaleza(rubro.getId(), naturaleza);
                if (mapeo.isPresent()) {
                    porLinea.get(mapeo.get().getLinea()).add(cuentaMonto);
                } else {
                    sinMapear.add(cuentaMonto);
                }
            }
        }

        List<LineaCalculada> lineas = new ArrayList<>();
        Map<LineaEstadoResultados, BigDecimal> montoPorLinea = new EnumMap<>(LineaEstadoResultados.class);
        for (LineaEstadoResultados linea : LineaEstadoResultados.values()) {
            BigDecimal monto = sumar(porLinea.get(linea));
            montoPorLinea.put(linea, monto);
            lineas.add(new LineaCalculada(linea, monto, porLinea.get(linea)));
        }

        BigDecimal resultadoBruto = montoPorLinea.get(LineaEstadoResultados.INGRESOS_POR_VENTAS)
                .add(montoPorLinea.get(LineaEstadoResultados.OTROS_INGRESOS_POR_VENTAS))
                .subtract(montoPorLinea.get(LineaEstadoResultados.COSTOS_DE_PRESTACION_DE_SERVICIOS));
        BigDecimal resultadoOperativo = resultadoBruto
                .subtract(montoPorLinea.get(LineaEstadoResultados.GASTOS_DE_COMERCIALIZACION))
                .subtract(montoPorLinea.get(LineaEstadoResultados.GASTOS_DE_ADMINISTRACION));
        BigDecimal resultadoFinal = resultadoOperativo
                .subtract(montoPorLinea.get(LineaEstadoResultados.GASTOS_FINANCIEROS))
                .subtract(montoPorLinea.get(LineaEstadoResultados.IMPUESTOS))
                .add(montoPorLinea.get(LineaEstadoResultados.OTROS_INGRESOS))
                .subtract(montoPorLinea.get(LineaEstadoResultados.OTROS_EGRESOS));

        return new EstadoResultadosCalculado(lineas, resultadoBruto, resultadoOperativo, resultadoFinal,
                sumar(sinMapear), sinMapear, !filas.isEmpty());
    }

    private BigDecimal sumar(List<CuentaMonto> cuentas) {
        return cuentas.stream().map(CuentaMonto::monto).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
