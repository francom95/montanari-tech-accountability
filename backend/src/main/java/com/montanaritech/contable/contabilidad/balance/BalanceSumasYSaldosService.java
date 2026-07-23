package com.montanaritech.contable.contabilidad.balance;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.contabilidad.asiento.AsientoLineaRepository;
import com.montanaritech.contable.contabilidad.balance.dto.BalanceSumasYSaldosNodo;
import com.montanaritech.contable.contabilidad.balance.dto.BalanceSumasYSaldosResponse;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContableRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Balance de sumas y saldos (F7.2): todas las cuentas del plan, con su
 * jerarquía (misma técnica que {@code CuentaContableService.arbol()}) y el
 * debe/haber acumulado del período — las madres son el roll-up de sus
 * descendientes imputables, nunca reciben líneas propias (F3.4).
 */
@Service
@RequiredArgsConstructor
public class BalanceSumasYSaldosService {

    private final CuentaContableRepository cuentaRepo;
    private final AsientoLineaRepository lineaRepo;

    @Transactional(readOnly = true)
    public BalanceSumasYSaldosResponse calcular(LocalDate fechaDesde, LocalDate fechaHasta,
            boolean incluirSinMovimiento, Integer nivelMaximo) {
        List<CuentaContable> todas = cuentaRepo.findAllByOrderByCodigoAsc();
        Map<Long, List<CuentaContable>> hijosPorPadre = todas.stream()
                .filter(c -> c.getPadre() != null)
                .collect(Collectors.groupingBy(c -> c.getPadre().getId()));

        Map<Long, BigDecimal[]> sumasPorCuenta = new HashMap<>();
        BigDecimal totalDebe = BigDecimal.ZERO;
        BigDecimal totalHaber = BigDecimal.ZERO;
        for (Object[] fila : lineaRepo.sumarDebeHaberPorCuenta(fechaDesde, fechaHasta, null, EstadoDocumento.CONFIRMADO)) {
            Long cuentaId = (Long) fila[0];
            BigDecimal debe = (BigDecimal) fila[1];
            BigDecimal haber = (BigDecimal) fila[2];
            sumasPorCuenta.put(cuentaId, new BigDecimal[] {debe, haber});
            totalDebe = totalDebe.add(debe);
            totalHaber = totalHaber.add(haber);
        }

        List<BalanceSumasYSaldosNodo> raices = todas.stream()
                .filter(c -> c.getPadre() == null)
                .map(raiz -> construirNodo(raiz, hijosPorPadre, sumasPorCuenta, 1, nivelMaximo))
                .toList();

        if (!incluirSinMovimiento) {
            raices = raices.stream()
                    .map(this::filtrarSinMovimiento)
                    .filter(Objects::nonNull)
                    .toList();
        }

        BigDecimal diferencia = totalDebe.subtract(totalHaber);
        boolean balancea = diferencia.compareTo(BigDecimal.ZERO) == 0;
        return new BalanceSumasYSaldosResponse(raices, totalDebe, totalHaber, balancea, diferencia);
    }

    private BalanceSumasYSaldosNodo construirNodo(CuentaContable cuenta, Map<Long, List<CuentaContable>> hijosPorPadre,
            Map<Long, BigDecimal[]> sumasPorCuenta, int nivel, Integer nivelMaximo) {
        List<CuentaContable> hijosEntidad = hijosPorPadre.getOrDefault(cuenta.getId(), List.of());
        boolean cortarAca = hijosEntidad.isEmpty() || (nivelMaximo != null && nivel >= nivelMaximo);

        BigDecimal debe;
        BigDecimal haber;
        List<BalanceSumasYSaldosNodo> hijos;
        if (cortarAca) {
            BigDecimal[] rollup = sumarSubarbol(cuenta, hijosPorPadre, sumasPorCuenta);
            debe = rollup[0];
            haber = rollup[1];
            hijos = List.of();
        } else {
            hijos = hijosEntidad.stream()
                    .map(hijo -> construirNodo(hijo, hijosPorPadre, sumasPorCuenta, nivel + 1, nivelMaximo))
                    .toList();
            debe = hijos.stream().map(BalanceSumasYSaldosNodo::debe).reduce(BigDecimal.ZERO, BigDecimal::add);
            haber = hijos.stream().map(BalanceSumasYSaldosNodo::haber).reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal saldo = debe.subtract(haber);
        String etiqueta = saldo.compareTo(BigDecimal.ZERO) > 0 ? "DEUDOR"
                : saldo.compareTo(BigDecimal.ZERO) < 0 ? "ACREEDOR" : "SALDADA";
        boolean esDeudor = saldo.compareTo(BigDecimal.ZERO) > 0;
        boolean esAcreedor = saldo.compareTo(BigDecimal.ZERO) < 0;
        boolean contrarioAlEsperado = (cuenta.getSaldoEsperado() == CuentaContable.SaldoEsperado.DEUDOR && esAcreedor)
                || (cuenta.getSaldoEsperado() == CuentaContable.SaldoEsperado.ACREEDOR && esDeudor);

        return new BalanceSumasYSaldosNodo(cuenta.getId(), cuenta.getCodigo(), cuenta.getNombre(), cuenta.isImputable(),
                debe, haber, saldo, etiqueta, cuenta.getSaldoEsperado().name(), contrarioAlEsperado, hijos);
    }

    /** Suma propia + de todo el subárbol (para el corte por {@code nivelMaximo} y para las hojas reales). */
    private BigDecimal[] sumarSubarbol(CuentaContable cuenta, Map<Long, List<CuentaContable>> hijosPorPadre,
            Map<Long, BigDecimal[]> sumasPorCuenta) {
        BigDecimal[] propia = sumasPorCuenta.getOrDefault(cuenta.getId(), new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
        BigDecimal debe = propia[0];
        BigDecimal haber = propia[1];
        for (CuentaContable hijo : hijosPorPadre.getOrDefault(cuenta.getId(), List.of())) {
            BigDecimal[] sub = sumarSubarbol(hijo, hijosPorPadre, sumasPorCuenta);
            debe = debe.add(sub[0]);
            haber = haber.add(sub[1]);
        }
        return new BigDecimal[] {debe, haber};
    }

    private BalanceSumasYSaldosNodo filtrarSinMovimiento(BalanceSumasYSaldosNodo nodo) {
        List<BalanceSumasYSaldosNodo> hijosFiltrados = nodo.hijos().stream()
                .map(this::filtrarSinMovimiento)
                .filter(Objects::nonNull)
                .toList();
        boolean sinMovimiento = nodo.debe().compareTo(BigDecimal.ZERO) == 0 && nodo.haber().compareTo(BigDecimal.ZERO) == 0;
        if (sinMovimiento && hijosFiltrados.isEmpty()) {
            return null;
        }
        return new BalanceSumasYSaldosNodo(nodo.cuentaId(), nodo.codigo(), nodo.nombre(), nodo.imputable(),
                nodo.debe(), nodo.haber(), nodo.saldo(), nodo.saldoEtiqueta(), nodo.saldoEsperado(),
                nodo.contrarioAlEsperado(), hijosFiltrados);
    }
}
