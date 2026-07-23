package com.montanaritech.contable.contabilidad.balance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.contabilidad.asiento.AsientoLineaRepository;
import com.montanaritech.contable.contabilidad.balance.dto.BalanceSumasYSaldosNodo;
import com.montanaritech.contable.contabilidad.balance.dto.BalanceSumasYSaldosResponse;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContableRepository;
import com.montanaritech.contable.maestros.categoria.Categoria;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Balance de sumas y saldos (F7.2): roll-up madre = Σ hijas, verificación de
 * balanceo global, filtros de sin-movimiento y nivel máximo.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BalanceSumasYSaldosServiceTest {

    @Mock private CuentaContableRepository cuentaRepo;
    @Mock private AsientoLineaRepository lineaRepo;

    private BalanceSumasYSaldosService service;

    private CuentaContable activo;
    private CuentaContable activoCorriente;
    private CuentaContable caja;
    private CuentaContable banco;
    private CuentaContable deudores;
    private CuentaContable deudoresClienteA;

    @BeforeEach
    void setUp() {
        service = new BalanceSumasYSaldosService(cuentaRepo, lineaRepo);

        // 1 Activo (raíz, madre)
        //   1.1 Activo Corriente (madre intermedia)
        //     1.1.01 Caja (imputable)
        //     1.1.02 Banco (imputable)
        //   1.2 Deudores (madre)
        //     1.2.01 Cliente A (imputable)
        activo = crearCuenta(1L, "1", "Activo", null, false, CuentaContable.SaldoEsperado.DEUDOR);
        activoCorriente = crearCuenta(2L, "1.1", "Activo Corriente", activo, false, CuentaContable.SaldoEsperado.DEUDOR);
        caja = crearCuenta(3L, "1.1.01", "Caja", activoCorriente, true, CuentaContable.SaldoEsperado.DEUDOR);
        banco = crearCuenta(4L, "1.1.02", "Banco", activoCorriente, true, CuentaContable.SaldoEsperado.DEUDOR);
        deudores = crearCuenta(5L, "1.2", "Deudores", activo, false, CuentaContable.SaldoEsperado.DEUDOR);
        deudoresClienteA = crearCuenta(6L, "1.2.01", "Cliente A", deudores, true, CuentaContable.SaldoEsperado.DEUDOR);

        when(cuentaRepo.findAllByOrderByCodigoAsc())
                .thenReturn(List.of(activo, activoCorriente, caja, banco, deudores, deudoresClienteA));
    }

    private CuentaContable crearCuenta(Long id, String codigo, String nombre, CuentaContable padre, boolean imputable,
            CuentaContable.SaldoEsperado saldoEsperado) {
        CuentaContable c = new CuentaContable();
        c.setId(id);
        c.setCodigo(codigo);
        c.setNombre(nombre);
        c.setPadre(padre);
        c.setNaturaleza(Categoria.TipoCategoria.ACTIVO);
        c.setImputable(imputable);
        c.setSaldoEsperado(saldoEsperado);
        c.setActivo(true);
        return c;
    }

    private void stubSumas(Object[]... filas) {
        when(lineaRepo.sumarDebeHaberPorCuenta(any(), any(), eq(EstadoDocumento.CONFIRMADO)))
                .thenReturn(List.of(filas));
    }

    private BalanceSumasYSaldosNodo buscar(List<BalanceSumasYSaldosNodo> nodos, Long cuentaId) {
        for (BalanceSumasYSaldosNodo n : nodos) {
            if (n.cuentaId().equals(cuentaId)) {
                return n;
            }
            BalanceSumasYSaldosNodo enHijos = buscar(n.hijos(), cuentaId);
            if (enHijos != null) {
                return enHijos;
            }
        }
        return null;
    }

    @Test
    void cuentaMadreSumaSusHijasEnDosNiveles() {
        // Caja: debe 1000, Banco: debe 500 haber 200 -> Activo Corriente (madre) = debe 1500, haber 200
        stubSumas(
                new Object[] {3L, new BigDecimal("1000.00"), BigDecimal.ZERO},
                new Object[] {4L, new BigDecimal("500.00"), new BigDecimal("200.00")});

        BalanceSumasYSaldosResponse balance = service.calcular(null, null, true, null);

        BalanceSumasYSaldosNodo nodoActivoCorriente = buscar(balance.raices(), 2L);
        assertThat(nodoActivoCorriente.debe()).isEqualByComparingTo("1500.00");
        assertThat(nodoActivoCorriente.haber()).isEqualByComparingTo("200.00");
        assertThat(nodoActivoCorriente.saldo()).isEqualByComparingTo("1300.00");
        assertThat(nodoActivoCorriente.saldoEtiqueta()).isEqualTo("DEUDOR");
    }

    @Test
    void elRollUpPropagaATresNiveles() {
        // Caja 1000 debe, Cliente A 300 debe -> Activo (raíz) = debe 1300 (Activo Corriente 1000 + Deudores 300)
        stubSumas(
                new Object[] {3L, new BigDecimal("1000.00"), BigDecimal.ZERO},
                new Object[] {6L, new BigDecimal("300.00"), BigDecimal.ZERO});

        BalanceSumasYSaldosResponse balance = service.calcular(null, null, true, null);

        BalanceSumasYSaldosNodo raiz = buscar(balance.raices(), 1L);
        assertThat(raiz.debe()).isEqualByComparingTo("1300.00");
        assertThat(raiz.haber()).isEqualByComparingTo("0.00");
    }

    @Test
    void balanceaCuandoSumaDebeIgualSumaHaber() {
        stubSumas(
                new Object[] {3L, new BigDecimal("1000.00"), BigDecimal.ZERO},
                new Object[] {6L, BigDecimal.ZERO, new BigDecimal("1000.00")});

        BalanceSumasYSaldosResponse balance = service.calcular(null, null, true, null);

        assertThat(balance.balancea()).isTrue();
        assertThat(balance.diferencia()).isEqualByComparingTo("0.00");
        assertThat(balance.totalDebe()).isEqualByComparingTo("1000.00");
        assertThat(balance.totalHaber()).isEqualByComparingTo("1000.00");
    }

    @Test
    void noBalanceaEsSeñalDeBugYSeExponeLaDiferencia() {
        // Escenario que nunca debería pasar en producción (todo asiento balancea, PL-4/PL-5):
        // simula una corrupción de datos para probar que el chequeo la detecta.
        stubSumas(new Object[] {3L, new BigDecimal("1000.00"), new BigDecimal("700.00")});

        BalanceSumasYSaldosResponse balance = service.calcular(null, null, true, null);

        assertThat(balance.balancea()).isFalse();
        assertThat(balance.diferencia()).isEqualByComparingTo("300.00");
    }

    @Test
    void excluyeCuentasSinMovimientoPorDefecto() {
        // Solo Caja tiene movimiento; Banco, Deudores y Cliente A quedan en cero.
        stubSumas(new Object[] {3L, new BigDecimal("500.00"), BigDecimal.ZERO});

        BalanceSumasYSaldosResponse balance = service.calcular(null, null, false, null);

        assertThat(buscar(balance.raices(), 4L)).as("Banco sin movimiento, no debería aparecer").isNull();
        assertThat(buscar(balance.raices(), 5L)).as("Deudores sin movimiento, no debería aparecer").isNull();
        assertThat(buscar(balance.raices(), 3L)).as("Caja con movimiento, debe aparecer").isNotNull();
    }

    @Test
    void incluyeCuentasSinMovimientoCuandoSePide() {
        stubSumas(new Object[] {3L, new BigDecimal("500.00"), BigDecimal.ZERO});

        BalanceSumasYSaldosResponse balance = service.calcular(null, null, true, null);

        assertThat(buscar(balance.raices(), 4L)).as("Banco sin movimiento, pero incluirSinMovimiento=true").isNotNull();
        BalanceSumasYSaldosNodo banco = buscar(balance.raices(), 4L);
        assertThat(banco.debe()).isEqualByComparingTo("0.00");
        assertThat(banco.saldoEtiqueta()).isEqualTo("SALDADA");
    }

    @Test
    void nivelMaximoCortaElArbolPeroConservaElRollUpCompleto() {
        stubSumas(
                new Object[] {3L, new BigDecimal("1000.00"), BigDecimal.ZERO},
                new Object[] {6L, new BigDecimal("300.00"), BigDecimal.ZERO});

        // nivel 1 = solo la raíz "Activo"; sus descendientes se pliegan en el propio total de la raíz.
        BalanceSumasYSaldosResponse balance = service.calcular(null, null, true, 1);

        assertThat(balance.raices()).hasSize(1);
        BalanceSumasYSaldosNodo raiz = balance.raices().get(0);
        assertThat(raiz.cuentaId()).isEqualTo(1L);
        assertThat(raiz.hijos()).isEmpty();
        assertThat(raiz.debe()).isEqualByComparingTo("1300.00");
    }

    @Test
    void marcaContrarioAlEsperadoCuandoElSaldoQuedaDelLadoOpuesto() {
        // Caja espera saldo DEUDOR pero queda ACREEDOR (haber > debe): señal informativa, no bloquea.
        stubSumas(new Object[] {3L, new BigDecimal("100.00"), new BigDecimal("500.00")});

        BalanceSumasYSaldosResponse balance = service.calcular(null, null, true, null);

        BalanceSumasYSaldosNodo caja = buscar(balance.raices(), 3L);
        assertThat(caja.saldoEtiqueta()).isEqualTo("ACREEDOR");
        assertThat(caja.contrarioAlEsperado()).isTrue();
    }

    @Test
    void cuentaImputableSinNingunMovimientoQuedaEnCeroSinExplotar() {
        stubSumas(new Object[] {3L, new BigDecimal("100.00"), BigDecimal.ZERO});

        BalanceSumasYSaldosResponse balance = service.calcular(null, null, true, null);

        BalanceSumasYSaldosNodo banco = buscar(balance.raices(), 4L);
        assertThat(banco.debe()).isEqualByComparingTo("0.00");
        assertThat(banco.haber()).isEqualByComparingTo("0.00");
        assertThat(banco.saldoEtiqueta()).isEqualTo("SALDADA");
    }
}
