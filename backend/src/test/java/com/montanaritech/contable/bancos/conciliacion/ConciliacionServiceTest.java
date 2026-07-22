package com.montanaritech.contable.bancos.conciliacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.bancos.movimientobancario.EstadoMovimientoBancario;
import com.montanaritech.contable.bancos.movimientobancario.MovimientoBancario;
import com.montanaritech.contable.bancos.movimientobancario.MovimientoBancarioRepository;
import com.montanaritech.contable.bancos.movimientobancario.OrigenImportacionMovimiento;
import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.contabilidad.asiento.Asiento;
import com.montanaritech.contable.contabilidad.asiento.AsientoLinea;
import com.montanaritech.contable.contabilidad.asiento.AsientoLineaRepository;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.contabilidad.mayor.MayorService;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancaria;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancariaRepository;
import com.montanaritech.contable.maestros.moneda.Moneda;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Resumen de conciliación (F5.3): saldo banco vs. sistema, matching por importe+tolerancia de fecha, imputación rápida. */
@ExtendWith(MockitoExtension.class)
class ConciliacionServiceTest {

    @Mock private MovimientoBancarioRepository movimientoRepo;
    @Mock private AsientoLineaRepository asientoLineaRepo;
    @Mock private CuentaBancariaRepository cuentaBancariaRepo;
    @Mock private MayorService mayorService;
    @Mock private ClasificadorMovimientoBancario clasificador;

    private ConciliacionService service;
    private CuentaBancaria cuentaBancaria;
    private CuentaContable cuentaFondos;
    private Moneda ars;

    @BeforeEach
    void setUp() {
        service = new ConciliacionService(movimientoRepo, asientoLineaRepo, cuentaBancariaRepo, mayorService, clasificador);

        cuentaFondos = new CuentaContable();
        cuentaFondos.setId(1L);
        cuentaFondos.setCodigo("1.1.2001");

        ars = new Moneda();
        ars.setId(1L);
        ars.setCodigo("ARS");

        cuentaBancaria = new CuentaBancaria();
        cuentaBancaria.setId(10L);
        cuentaBancaria.setAlias("Banco Galicia CC");
        cuentaBancaria.setMoneda(ars);
        cuentaBancaria.setCuentaContable(cuentaFondos);
        cuentaBancaria.setSaldoInicial(new BigDecimal("1000.00"));
        cuentaBancaria.setFechaSaldoInicial(LocalDate.of(2026, 1, 1));

        lenient().when(cuentaBancariaRepo.findById(10L)).thenReturn(Optional.of(cuentaBancaria));
    }

    private MovimientoBancario movimiento(Long id, LocalDate fecha, String descripcion, BigDecimal importe, EstadoMovimientoBancario estado) {
        MovimientoBancario m = new MovimientoBancario();
        m.setId(id);
        m.setCuentaBancaria(cuentaBancaria);
        m.setFecha(fecha);
        m.setDescripcion(descripcion);
        m.setImporte(importe);
        m.setImporteArs(importe);
        m.setMoneda(ars);
        m.setTipoCambio(BigDecimal.ONE);
        m.setEstado(estado);
        m.setOrigenImportacion(OrigenImportacionMovimiento.GALICIA);
        return m;
    }

    private AsientoLinea lineaFondos(Long asientoId, Long numero, LocalDate fecha, BigDecimal debe, BigDecimal haber, String origenTipo, Long origenId) {
        Asiento a = new Asiento();
        a.setId(asientoId);
        a.setNumero(numero);
        a.setFecha(fecha);
        a.setEstado(EstadoDocumento.CONFIRMADO);
        a.setOrigenTipo(origenTipo);
        a.setOrigenId(origenId);
        a.setDescripcion("Cobro de Cliente Ejemplo");

        AsientoLinea l = new AsientoLinea();
        l.setAsiento(a);
        l.setCuentaBancaria(cuentaBancaria);
        l.setDebe(debe);
        l.setHaber(haber);
        return l;
    }

    private MayorService.MayorCompleto mayorConSaldo(BigDecimal saldo) {
        return new MayorService.MayorCompleto(cuentaFondos, false, List.of(), saldo, "DEUDOR", false);
    }

    @Test
    void rangoDeFechasInvalidoLanzaError() {
        assertThatThrownBy(() -> service.resumen(10L, LocalDate.of(2026, 6, 30), LocalDate.of(2026, 6, 1), 3))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("RANGO_FECHAS_INVALIDO");
    }

    @Test
    void movimientoPendienteConMatchExactoDentroDeLaToleranciaSugiereElAsiento() {
        MovimientoBancario m = movimiento(1L, LocalDate.of(2026, 6, 5), "Pago Tarjeta Visa", new BigDecimal("-1000.00"), EstadoMovimientoBancario.PENDIENTE);
        when(movimientoRepo.buscarParaConciliacion(eq(10L), any(), any())).thenReturn(List.of(m));

        AsientoLinea candidato = lineaFondos(99L, 5L, LocalDate.of(2026, 6, 6), BigDecimal.ZERO, new BigDecimal("1000.00"), "Pago", 7L);
        when(asientoLineaRepo.buscarCandidatosConciliacion(eq(10L), any(), any(), eq(EstadoDocumento.CONFIRMADO)))
                .thenReturn(List.of(candidato));
        when(mayorService.calcular(eq(1L), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(mayorConSaldo(BigDecimal.ZERO));

        var resumen = service.resumen(10L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), 3);

        assertThat(resumen.movimientos()).hasSize(1);
        var fila = resumen.movimientos().get(0);
        assertThat(fila.matchSugerido()).isNotNull();
        assertThat(fila.matchSugerido().asientoNumero()).isEqualTo(5L);
        assertThat(fila.matchSugerido().origenTipo()).isEqualTo("Pago");
        assertThat(fila.cuentaSugerida()).isNull();
    }

    @Test
    void movimientoFueraDeLaToleranciaDeFechaNoMatcheaYCaeAlClasificador() {
        MovimientoBancario m = movimiento(2L, LocalDate.of(2026, 6, 1), "Comision Mantenimiento", new BigDecimal("-500.00"), EstadoMovimientoBancario.PENDIENTE);
        when(movimientoRepo.buscarParaConciliacion(eq(10L), any(), any())).thenReturn(List.of(m));

        // Mismo importe pero 10 días de diferencia, tolerancia = 3.
        AsientoLinea candidatoLejano = lineaFondos(100L, 6L, LocalDate.of(2026, 6, 11), BigDecimal.ZERO, new BigDecimal("500.00"), "Pago", 8L);
        when(asientoLineaRepo.buscarCandidatosConciliacion(eq(10L), any(), any(), eq(EstadoDocumento.CONFIRMADO)))
                .thenReturn(List.of(candidatoLejano));
        when(mayorService.calcular(eq(1L), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(mayorConSaldo(BigDecimal.ZERO));

        CuentaContable comisiones = new CuentaContable();
        comisiones.setId(2L);
        comisiones.setCodigo("6.4003");
        comisiones.setNombre("Comisiones bancarias");
        when(clasificador.clasificar(eq("Comision Mantenimiento"), eq(OrigenImportacionMovimiento.GALICIA)))
                .thenReturn(Optional.of(new ClasificadorMovimientoBancario.CuentaSugerida(comisiones, com.montanaritech.contable.contabilidad.mapeocuenta.ConceptoContable.COMISION_BANCARIA)));

        var resumen = service.resumen(10L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), 3);

        var fila = resumen.movimientos().get(0);
        assertThat(fila.matchSugerido()).isNull();
        assertThat(fila.cuentaSugerida()).isNotNull();
        assertThat(fila.cuentaSugerida().cuentaContableCodigo()).isEqualTo("6.4003");
    }

    @Test
    void unAsientoNoSeSugiereDosVecesParaDosMovimientosDistintos() {
        MovimientoBancario m1 = movimiento(3L, LocalDate.of(2026, 6, 5), "Transf A", new BigDecimal("-1000.00"), EstadoMovimientoBancario.PENDIENTE);
        MovimientoBancario m2 = movimiento(4L, LocalDate.of(2026, 6, 5), "Transf B", new BigDecimal("-1000.00"), EstadoMovimientoBancario.PENDIENTE);
        when(movimientoRepo.buscarParaConciliacion(eq(10L), any(), any())).thenReturn(List.of(m1, m2));

        AsientoLinea unicoCandidato = lineaFondos(101L, 7L, LocalDate.of(2026, 6, 5), BigDecimal.ZERO, new BigDecimal("1000.00"), "Pago", 9L);
        when(asientoLineaRepo.buscarCandidatosConciliacion(eq(10L), any(), any(), eq(EstadoDocumento.CONFIRMADO)))
                .thenReturn(new java.util.ArrayList<>(List.of(unicoCandidato)));
        when(mayorService.calcular(eq(1L), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(mayorConSaldo(BigDecimal.ZERO));
        when(clasificador.clasificar(any(), any())).thenReturn(Optional.empty());

        var resumen = service.resumen(10L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), 3);

        long conMatch = resumen.movimientos().stream().filter(f -> f.matchSugerido() != null).count();
        assertThat(conMatch).isEqualTo(1);
    }

    @Test
    void movimientoSinFechaNuncaMatchea() {
        MovimientoBancario m = movimiento(5L, null, "Sin fecha", new BigDecimal("-1000.00"), EstadoMovimientoBancario.PENDIENTE);
        // buscarParaConciliacion nunca devolvería un movimiento sin fecha en la realidad (filtro BETWEEN),
        // pero el matching en memoria igual debe ser defensivo si llegara uno.
        when(movimientoRepo.buscarParaConciliacion(eq(10L), any(), any())).thenReturn(List.of(m));
        when(asientoLineaRepo.buscarCandidatosConciliacion(eq(10L), any(), any(), eq(EstadoDocumento.CONFIRMADO)))
                .thenReturn(List.of(lineaFondos(102L, 8L, LocalDate.of(2026, 6, 5), BigDecimal.ZERO, new BigDecimal("1000.00"), "Pago", 1L)));
        when(mayorService.calcular(eq(1L), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(mayorConSaldo(BigDecimal.ZERO));
        when(clasificador.clasificar(any(), any())).thenReturn(Optional.empty());

        var resumen = service.resumen(10L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), 3);

        assertThat(resumen.movimientos().get(0).matchSugerido()).isNull();
    }

    @Test
    void movimientoConciliadoMuestraElAsientoYaAsociadoSinSugerencias() {
        MovimientoBancario m = movimiento(6L, LocalDate.of(2026, 6, 5), "Ya resuelto", new BigDecimal("-1000.00"), EstadoMovimientoBancario.CONCILIADO);
        Asiento asientoAsociado = new Asiento();
        asientoAsociado.setId(200L);
        asientoAsociado.setNumero(15L);
        m.setAsiento(asientoAsociado);
        when(movimientoRepo.buscarParaConciliacion(eq(10L), any(), any())).thenReturn(List.of(m));
        when(asientoLineaRepo.buscarCandidatosConciliacion(eq(10L), any(), any(), eq(EstadoDocumento.CONFIRMADO))).thenReturn(List.of());
        when(mayorService.calcular(eq(1L), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(mayorConSaldo(BigDecimal.ZERO));

        var resumen = service.resumen(10L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), 3);

        var fila = resumen.movimientos().get(0);
        assertThat(fila.asientoIdAsociado()).isEqualTo(200L);
        assertThat(fila.asientoNumeroAsociado()).isEqualTo(15L);
        assertThat(fila.matchSugerido()).isNull();
        assertThat(fila.cuentaSugerida()).isNull();
    }

    @Test
    void saldoBancoSumaSaldoInicialMasMovimientosNoDescartados() {
        MovimientoBancario ingreso = movimiento(7L, LocalDate.of(2026, 6, 5), "Ingreso", new BigDecimal("500.00"), EstadoMovimientoBancario.CONCILIADO);
        MovimientoBancario egreso = movimiento(8L, LocalDate.of(2026, 6, 10), "Egreso", new BigDecimal("-200.00"), EstadoMovimientoBancario.PENDIENTE);
        MovimientoBancario descartado = movimiento(9L, LocalDate.of(2026, 6, 15), "Duplicado", new BigDecimal("999.00"), EstadoMovimientoBancario.DESCARTADO);
        when(movimientoRepo.buscarParaConciliacion(eq(10L), any(), any())).thenReturn(List.of(ingreso, egreso, descartado));
        when(asientoLineaRepo.buscarCandidatosConciliacion(eq(10L), any(), any(), eq(EstadoDocumento.CONFIRMADO))).thenReturn(List.of());
        when(mayorService.calcular(eq(1L), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(mayorConSaldo(new BigDecimal("1300.00")));
        when(clasificador.clasificar(any(), any())).thenReturn(Optional.empty());

        var resumen = service.resumen(10L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), 3);

        // saldoInicial 1000 + 500 - 200 (el descartado 999 no suma) = 1300
        assertThat(resumen.saldoBanco()).isEqualByComparingTo("1300.00");
        assertThat(resumen.saldoSistema()).isEqualByComparingTo("1300.00");
        assertThat(resumen.diferencia()).isEqualByComparingTo("0.00");
    }
}
