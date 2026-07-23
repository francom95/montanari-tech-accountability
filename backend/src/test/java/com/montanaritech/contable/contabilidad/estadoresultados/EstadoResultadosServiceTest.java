package com.montanaritech.contable.contabilidad.estadoresultados;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.contabilidad.asiento.AsientoLineaRepository;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContableRepository;
import com.montanaritech.contable.contabilidad.estadoresultados.dto.EstadoResultadosDtos.EstadoResultadosPorProyectoResponse;
import com.montanaritech.contable.contabilidad.estadoresultados.dto.EstadoResultadosDtos.EstadoResultadosResponse;
import com.montanaritech.contable.maestros.categoria.Categoria;
import com.montanaritech.contable.maestros.proyecto.Proyecto;
import com.montanaritech.contable.maestros.proyecto.ProyectoRepository;
import com.montanaritech.contable.maestros.rubro.Rubro;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Estado de resultados (F7.3): subtotales, 4 vistas, comparativo, mapeo configurable. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EstadoResultadosServiceTest {

    @Mock private AsientoLineaRepository lineaRepo;
    @Mock private CuentaContableRepository cuentaRepo;
    @Mock private MapeoRubroLineaEstadoResultadosRepository mapeoRepo;
    @Mock private ProyectoRepository proyectoRepo;

    @InjectMocks
    private EstadoResultadosService service;

    private Rubro rubroVentas, rubroOtrasVentas, rubroCostos, rubroComercializacion, rubroAdministracion,
            rubroFinancieros, rubroImpuestos, rubroOtros, rubroSinMapeo;
    private CuentaContable cVentas, cOtrasVentas, cCostos, cComercializacion, cAdministracion, cFinancieros,
            cImpuestos, cOtrosIngresos, cOtrosEgresos, cSinMapear, cActivo, cSinRubro;

    @BeforeEach
    void setUp() {
        rubroVentas = rubro(1L);
        rubroOtrasVentas = rubro(2L);
        rubroCostos = rubro(3L);
        rubroComercializacion = rubro(4L);
        rubroAdministracion = rubro(5L);
        rubroFinancieros = rubro(6L);
        rubroImpuestos = rubro(7L);
        rubroOtros = rubro(8L);
        rubroSinMapeo = rubro(9L);

        cVentas = cuenta(101L, "4.1.2001", rubroVentas, Categoria.TipoCategoria.RP);
        cOtrasVentas = cuenta(102L, "4.1.2002", rubroOtrasVentas, Categoria.TipoCategoria.RP);
        cCostos = cuenta(103L, "5.1.2001", rubroCostos, Categoria.TipoCategoria.RN);
        cComercializacion = cuenta(104L, "5.2.2001", rubroComercializacion, Categoria.TipoCategoria.RN);
        cAdministracion = cuenta(105L, "5.3.2001", rubroAdministracion, Categoria.TipoCategoria.RN);
        cFinancieros = cuenta(106L, "5.3.3001", rubroFinancieros, Categoria.TipoCategoria.RN);
        cImpuestos = cuenta(107L, "5.3.4001", rubroImpuestos, Categoria.TipoCategoria.RN);
        cOtrosIngresos = cuenta(108L, "6.4001", rubroOtros, Categoria.TipoCategoria.RP);
        cOtrosEgresos = cuenta(109L, "6.4003", rubroOtros, Categoria.TipoCategoria.RN);
        cSinMapear = cuenta(110L, "5.9.9999", rubroSinMapeo, Categoria.TipoCategoria.RN);
        cActivo = cuenta(111L, "1.1.2001", rubroVentas, Categoria.TipoCategoria.ACTIVO);
        cSinRubro = cuenta(200L, "5.9.0001", null, Categoria.TipoCategoria.RN);

        List<CuentaContable> todas = List.of(cVentas, cOtrasVentas, cCostos, cComercializacion, cAdministracion,
                cFinancieros, cImpuestos, cOtrosIngresos, cOtrosEgresos, cSinMapear, cActivo, cSinRubro);
        when(cuentaRepo.findAllById(any())).thenAnswer(inv -> {
            List<Long> ids = inv.getArgument(0);
            return todas.stream().filter(c -> ids.contains(c.getId())).toList();
        });

        mapear(rubroVentas, Categoria.TipoCategoria.RP, LineaEstadoResultados.INGRESOS_POR_VENTAS);
        mapear(rubroOtrasVentas, Categoria.TipoCategoria.RP, LineaEstadoResultados.OTROS_INGRESOS_POR_VENTAS);
        mapear(rubroCostos, Categoria.TipoCategoria.RN, LineaEstadoResultados.COSTOS_DE_PRESTACION_DE_SERVICIOS);
        mapear(rubroComercializacion, Categoria.TipoCategoria.RN, LineaEstadoResultados.GASTOS_DE_COMERCIALIZACION);
        mapear(rubroAdministracion, Categoria.TipoCategoria.RN, LineaEstadoResultados.GASTOS_DE_ADMINISTRACION);
        mapear(rubroFinancieros, Categoria.TipoCategoria.RN, LineaEstadoResultados.GASTOS_FINANCIEROS);
        mapear(rubroImpuestos, Categoria.TipoCategoria.RN, LineaEstadoResultados.IMPUESTOS);
        mapear(rubroOtros, Categoria.TipoCategoria.RP, LineaEstadoResultados.OTROS_INGRESOS);
        mapear(rubroOtros, Categoria.TipoCategoria.RN, LineaEstadoResultados.OTROS_EGRESOS);
        when(mapeoRepo.findByRubroIdAndNaturaleza(eq(9L), any())).thenReturn(Optional.empty());
    }

    private Rubro rubro(Long id) {
        Rubro r = new Rubro();
        r.setId(id);
        r.setNombre("Rubro " + id);
        return r;
    }

    private CuentaContable cuenta(Long id, String codigo, Rubro rubro, Categoria.TipoCategoria naturaleza) {
        CuentaContable c = new CuentaContable();
        c.setId(id);
        c.setCodigo(codigo);
        c.setNombre("Cuenta " + codigo);
        c.setRubro(rubro);
        c.setNaturaleza(naturaleza);
        return c;
    }

    private void mapear(Rubro rubro, Categoria.TipoCategoria naturaleza, LineaEstadoResultados linea) {
        MapeoRubroLineaEstadoResultados m = new MapeoRubroLineaEstadoResultados();
        m.setRubro(rubro);
        m.setNaturaleza(naturaleza);
        m.setLinea(linea);
        when(mapeoRepo.findByRubroIdAndNaturaleza(rubro.getId(), naturaleza)).thenReturn(Optional.of(m));
    }

    private Object[] fila(Long cuentaId, String debe, String haber) {
        return new Object[] {cuentaId, new BigDecimal(debe), new BigDecimal(haber)};
    }

    @Test
    void calculaLosTresSubtotalesCorrectamenteConLasNueveLineas() {
        LocalDate desde = LocalDate.of(2026, 5, 1);
        LocalDate hasta = LocalDate.of(2026, 5, 31);
        when(lineaRepo.sumarDebeHaberPorCuenta(desde, hasta, null, EstadoDocumento.CONFIRMADO)).thenReturn(List.<Object[]>of(
                fila(101L, "0", "100000"),   // ingresos por ventas: RP, monto = haber-debe = 100000
                fila(102L, "0", "5000"),     // otros ingresos por ventas: 5000
                fila(103L, "30000", "0"),    // costos: RN, monto = debe-haber = 30000
                fila(104L, "8000", "0"),     // comercializacion: 8000
                fila(105L, "12000", "0"),    // administracion: 12000
                fila(106L, "2000", "0"),     // financieros: 2000
                fila(107L, "3000", "0"),     // impuestos: 3000
                fila(108L, "0", "1000"),     // otros ingresos: 1000
                fila(109L, "500", "0"),      // otros egresos: 500
                fila(110L, "700", "0"),      // sin mapear: 700
                fila(111L, "999999", "0")    // cuenta ACTIVO: fuera del ER, no debe afectar nada
        ));

        EstadoResultadosResponse resp = service.porMes(2026, 5);
        var c = resp.calculado();

        assertThat(c.resultadoBruto()).isEqualByComparingTo("75000");
        assertThat(c.resultadoOperativo()).isEqualByComparingTo("55000");
        assertThat(c.resultadoFinal()).isEqualByComparingTo("50500");
        assertThat(c.montoSinMapear()).isEqualByComparingTo("700");
        assertThat(c.cuentasSinMapear()).hasSize(1);
        assertThat(c.cuentasSinMapear().get(0).cuentaId()).isEqualTo(110L);
        assertThat(c.tieneMovimiento()).isTrue();
    }

    @Test
    void cuentaConRubroNuloVaAlBucketSinMapear() {
        LocalDate desde = LocalDate.of(2026, 5, 1);
        LocalDate hasta = LocalDate.of(2026, 5, 31);
        when(lineaRepo.sumarDebeHaberPorCuenta(desde, hasta, null, EstadoDocumento.CONFIRMADO))
                .thenReturn(List.<Object[]>of(fila(200L, "300", "0")));

        var c = service.porMes(2026, 5).calculado();

        assertThat(c.montoSinMapear()).isEqualByComparingTo("300");
        assertThat(c.resultadoFinal()).isEqualByComparingTo("0");
    }

    @Test
    void comparativoMesAnteriorCalculaVariacionAbsolutaYPorcentual() {
        LocalDate mayoDesde = LocalDate.of(2026, 5, 1);
        LocalDate mayoHasta = LocalDate.of(2026, 5, 31);
        LocalDate abrilDesde = LocalDate.of(2026, 4, 1);
        LocalDate abrilHasta = LocalDate.of(2026, 4, 30);
        when(lineaRepo.sumarDebeHaberPorCuenta(mayoDesde, mayoHasta, null, EstadoDocumento.CONFIRMADO))
                .thenReturn(List.<Object[]>of(fila(101L, "0", "100000")));
        when(lineaRepo.sumarDebeHaberPorCuenta(abrilDesde, abrilHasta, null, EstadoDocumento.CONFIRMADO))
                .thenReturn(List.<Object[]>of(fila(101L, "0", "50000")));

        EstadoResultadosResponse resp = service.porMes(2026, 5);

        assertThat(resp.calculado().resultadoFinal()).isEqualByComparingTo("100000");
        assertThat(resp.comparativoMesAnterior().mesAnterior()).isEqualTo(4);
        assertThat(resp.comparativoMesAnterior().anioAnterior()).isEqualTo(2026);
        assertThat(resp.comparativoMesAnterior().resultadoFinalAnterior()).isEqualByComparingTo("50000");
        assertThat(resp.comparativoMesAnterior().variacionAbsoluta()).isEqualByComparingTo("50000");
        assertThat(resp.comparativoMesAnterior().variacionPorcentual()).isEqualByComparingTo("100.0000");
    }

    @Test
    void variacionPorcentualEsNuloCuandoElMesAnteriorDaCero() {
        LocalDate mayoDesde = LocalDate.of(2026, 5, 1);
        LocalDate mayoHasta = LocalDate.of(2026, 5, 31);
        LocalDate abrilDesde = LocalDate.of(2026, 4, 1);
        LocalDate abrilHasta = LocalDate.of(2026, 4, 30);
        when(lineaRepo.sumarDebeHaberPorCuenta(mayoDesde, mayoHasta, null, EstadoDocumento.CONFIRMADO))
                .thenReturn(List.<Object[]>of(fila(101L, "0", "10000")));
        when(lineaRepo.sumarDebeHaberPorCuenta(abrilDesde, abrilHasta, null, EstadoDocumento.CONFIRMADO))
                .thenReturn(List.of());

        EstadoResultadosResponse resp = service.porMes(2026, 5);

        assertThat(resp.comparativoMesAnterior().resultadoFinalAnterior()).isEqualByComparingTo("0");
        assertThat(resp.comparativoMesAnterior().variacionPorcentual()).isNull();
        assertThat(resp.comparativoMesAnterior().variacionAbsoluta()).isEqualByComparingTo("10000");
    }

    @Test
    void vistaPorAnioUsaElRangoCompletoDelAnio() {
        when(lineaRepo.sumarDebeHaberPorCuenta(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), null, EstadoDocumento.CONFIRMADO))
                .thenReturn(List.<Object[]>of(fila(101L, "0", "240000")));

        EstadoResultadosResponse resp = service.porAnio(2026);

        assertThat(resp.calculado().resultadoFinal()).isEqualByComparingTo("240000");
        assertThat(resp.comparativoMesAnterior()).isNull();
    }

    @Test
    void vistaAcumuladaVaDesdeEneroHastaFinDelMesPedido() {
        when(lineaRepo.sumarDebeHaberPorCuenta(LocalDate.of(2026, 1, 1), YearMonth.of(2026, 5).atEndOfMonth(), null, EstadoDocumento.CONFIRMADO))
                .thenReturn(List.<Object[]>of(fila(101L, "0", "300000")));

        EstadoResultadosResponse resp = service.acumulado(2026, 5);

        assertThat(resp.calculado().resultadoFinal()).isEqualByComparingTo("300000");
    }

    @Test
    void vistaPorProyectoRepartePorProyectoYOmiteElQueNoTuvoMovimiento() {
        Proyecto alfa = new Proyecto();
        alfa.setId(1L);
        alfa.setNombre("Alfa");
        Proyecto beta = new Proyecto();
        beta.setId(2L);
        beta.setNombre("Beta");
        when(proyectoRepo.findByActivoTrueOrderByNombreAsc()).thenReturn(List.of(alfa, beta));

        LocalDate desde = LocalDate.of(2026, 5, 1);
        LocalDate hasta = LocalDate.of(2026, 5, 31);
        when(lineaRepo.sumarDebeHaberPorCuenta(desde, hasta, 1L, EstadoDocumento.CONFIRMADO))
                .thenReturn(List.<Object[]>of(fila(101L, "0", "60000")));
        when(lineaRepo.sumarDebeHaberPorCuenta(desde, hasta, 2L, EstadoDocumento.CONFIRMADO))
                .thenReturn(List.of());
        when(lineaRepo.sumarDebeHaberPorCuentaSinProyecto(desde, hasta, EstadoDocumento.CONFIRMADO))
                .thenReturn(List.<Object[]>of(fila(105L, "5000", "0")));

        EstadoResultadosPorProyectoResponse resp = service.porProyecto(2026, 5);

        assertThat(resp.porProyecto()).hasSize(1);
        assertThat(resp.porProyecto().get(0).proyectoNombre()).isEqualTo("Alfa");
        assertThat(resp.porProyecto().get(0).calculado().resultadoFinal()).isEqualByComparingTo("60000");
        assertThat(resp.sinProyecto().resultadoFinal()).isEqualByComparingTo("-5000");
    }
}
