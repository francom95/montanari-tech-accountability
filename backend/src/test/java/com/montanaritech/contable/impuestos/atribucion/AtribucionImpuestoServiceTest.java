package com.montanaritech.contable.impuestos.atribucion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.facturacion.TipoComprobante;
import com.montanaritech.contable.facturacion.facturacompra.FacturaCompra;
import com.montanaritech.contable.facturacion.facturacompra.FacturaCompraRepository;
import com.montanaritech.contable.facturacion.facturaventa.FacturaVenta;
import com.montanaritech.contable.facturacion.facturaventa.FacturaVentaRepository;
import com.montanaritech.contable.impuestos.atribucion.dto.AtribucionImpuestoDtos.CalcularRequest;
import com.montanaritech.contable.impuestos.atribucion.dto.AtribucionImpuestoDtos.PorcentajeProyecto;
import com.montanaritech.contable.impuestos.iva.LiquidacionIva;
import com.montanaritech.contable.impuestos.iva.LiquidacionIvaRepository;
import com.montanaritech.contable.maestros.proyecto.Proyecto;
import com.montanaritech.contable.maestros.proyecto.ProyectoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Atribución de impuestos a proyectos (F6.3): prorrateo por facturación exacto, y los otros criterios. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AtribucionImpuestoServiceTest {

    @Mock private AtribucionImpuestoRepository repo;
    @Mock private ConfiguracionAtribucionRepository configRepo;
    @Mock private LiquidacionIvaRepository liquidacionIvaRepository;
    @Mock private com.montanaritech.contable.impuestos.iibb.LiquidacionIibbRepository liquidacionIibbRepository;
    @Mock private FacturaVentaRepository facturaVentaRepository;
    @Mock private FacturaCompraRepository facturaCompraRepository;
    @Mock private ProyectoRepository proyectoRepository;
    @Mock private com.montanaritech.contable.maestros.moneda.MonedaRepository monedaRepository;
    @Mock private com.montanaritech.contable.common.audit.AuditoriaService auditoria;

    private AtribucionImpuestoService service;
    private Proyecto p1;
    private Proyecto p2;
    private Proyecto p3;

    @BeforeEach
    void setUp() {
        service = new AtribucionImpuestoService(repo, configRepo, liquidacionIvaRepository, liquidacionIibbRepository,
                facturaVentaRepository, facturaCompraRepository, proyectoRepository, monedaRepository,
                new AtribucionImpuestoMapper(), auditoria);

        p1 = proyecto(1L, "Alfa");
        p2 = proyecto(2L, "Beta");
        p3 = proyecto(3L, "Gamma");
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(p1));
        when(proyectoRepository.findById(2L)).thenReturn(Optional.of(p2));
        when(proyectoRepository.findById(3L)).thenReturn(Optional.of(p3));
        when(proyectoRepository.getReferenceById(1L)).thenReturn(p1);
        when(proyectoRepository.getReferenceById(2L)).thenReturn(p2);
        when(proyectoRepository.getReferenceById(3L)).thenReturn(p3);

        // liquidación de IVA confirmada, saldo a pagar 118.500,50, período marzo 2026
        LiquidacionIva liq = new LiquidacionIva();
        liq.setAnio(2026);
        liq.setMes(3);
        liq.setFechaDesde(LocalDate.of(2026, 3, 1));
        liq.setFechaHasta(LocalDate.of(2026, 3, 31));
        liq.setEstado(EstadoDocumento.CONFIRMADO);
        liq.setSaldoAPagar(new BigDecimal("118500.50"));
        when(liquidacionIvaRepository.findById(10L)).thenReturn(Optional.of(liq));

        when(repo.findByLiquidacionTipoAndLiquidacionId(any(), any())).thenReturn(Optional.empty());
        when(repo.save(any(AtribucionImpuesto.class))).thenAnswer(inv -> {
            AtribucionImpuesto a = inv.getArgument(0);
            if (a.getId() == null) {
                a.setId(1L);
            }
            return a;
        });
        when(facturaCompraRepository.buscarConfirmadasParaReporte(any(), any(), any(), any(), any())).thenReturn(List.of());
        com.montanaritech.contable.maestros.moneda.Moneda ars = new com.montanaritech.contable.maestros.moneda.Moneda();
        ars.setId(1L);
        when(monedaRepository.findByCodigo("ARS")).thenReturn(Optional.of(ars));
    }

    @Test
    void prorrateoPorFacturacionConTresProyectosSumaExactamenteElImpuesto() {
        // ventas: Alfa 1.000.000, Beta 600.000, Gamma 400.000
        when(facturaVentaRepository.buscarConfirmadasParaReporte(any(), any(), any(), any(), any()))
                .thenReturn(List.of(venta("1000000.00", p1), venta("600000.00", p2), venta("400000.00", p3)));

        CalculoAtribucion c = service.guardar(TipoLiquidacion.IVA, 10L,
                new CalcularRequest(CriterioAtribucion.FACTURACION, null, null));

        assertThat(c.lineas()).hasSize(3);
        BigDecimal suma = c.lineas().stream().map(CalculoAtribucion.LineaCalculada::monto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(suma).as("la suma de las atribuciones es exactamente el impuesto")
                .isEqualByComparingTo("118500.50");
        assertThat(montoDe(c, "Alfa")).isEqualByComparingTo("59250.25");  // 50%
        assertThat(montoDe(c, "Beta")).isEqualByComparingTo("35550.15");  // 30%
        assertThat(montoDe(c, "Gamma")).isEqualByComparingTo("23700.10"); // 20% (residuo)
    }

    @Test
    void atribucionDirectaImputaElTotalAUnProyecto() {
        CalculoAtribucion c = service.guardar(TipoLiquidacion.IVA, 10L,
                new CalcularRequest(CriterioAtribucion.DIRECTO, 2L, null));

        assertThat(c.lineas()).singleElement().satisfies(l -> {
            assertThat(l.proyectoNombre()).isEqualTo("Beta");
            assertThat(l.monto()).isEqualByComparingTo("118500.50");
        });
    }

    @Test
    void porcentajeManualQueNoSuma100SeRechaza() {
        assertThatThrownBy(() -> service.guardar(TipoLiquidacion.IVA, 10L,
                new CalcularRequest(CriterioAtribucion.PORCENTAJE_MANUAL, null,
                        List.of(new PorcentajeProyecto(1L, new BigDecimal("60")),
                                new PorcentajeProyecto(2L, new BigDecimal("30"))))))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("deben sumar 100");
    }

    @Test
    void porcentajeManualQueSuma100RepartaExacto() {
        CalculoAtribucion c = service.guardar(TipoLiquidacion.IVA, 10L,
                new CalcularRequest(CriterioAtribucion.PORCENTAJE_MANUAL, null,
                        List.of(new PorcentajeProyecto(1L, new BigDecimal("33.33")),
                                new PorcentajeProyecto(2L, new BigDecimal("33.33")),
                                new PorcentajeProyecto(3L, new BigDecimal("33.34")))));

        BigDecimal suma = c.lineas().stream().map(CalculoAtribucion.LineaCalculada::monto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(suma).isEqualByComparingTo("118500.50");
    }

    @Test
    void margenReparteVentasMenosCompras() {
        when(facturaVentaRepository.buscarConfirmadasParaReporte(any(), any(), any(), any(), any()))
                .thenReturn(List.of(venta("1000000.00", p1), venta("500000.00", p2)));
        when(facturaCompraRepository.buscarConfirmadasParaReporte(any(), any(), any(), any(), any()))
                .thenReturn(List.of(compra("200000.00", p1))); // Alfa margen 800k, Beta margen 500k

        CalculoAtribucion c = service.guardar(TipoLiquidacion.IVA, 10L,
                new CalcularRequest(CriterioAtribucion.MARGEN, null, null));

        BigDecimal suma = c.lineas().stream().map(CalculoAtribucion.LineaCalculada::monto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(suma).isEqualByComparingTo("118500.50");
        // Alfa 800k/1.3M ≈ 61,538%, Beta 500k/1.3M ≈ 38,46%
        assertThat(montoDe(c, "Alfa")).isEqualByComparingTo("72923.38");
    }

    @Test
    void unaLiquidacionEnBorradorNoSePuedeAtribuir() {
        LiquidacionIva borrador = new LiquidacionIva();
        borrador.setEstado(EstadoDocumento.BORRADOR);
        when(liquidacionIvaRepository.findById(99L)).thenReturn(Optional.of(borrador));

        assertThatThrownBy(() -> service.guardar(TipoLiquidacion.IVA, 99L,
                new CalcularRequest(CriterioAtribucion.DIRECTO, 1L, null)))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("liquidación confirmada");
    }

    @Test
    void lasVentasSinProyectoNoEntranYAvisan() {
        when(facturaVentaRepository.buscarConfirmadasParaReporte(any(), any(), any(), any(), any()))
                .thenReturn(List.of(venta("1000000.00", p1), venta("500000.00", null)));

        CalculoAtribucion c = service.previsualizar(TipoLiquidacion.IVA, 10L,
                new CalcularRequest(CriterioAtribucion.FACTURACION, null, null));

        assertThat(c.lineas()).singleElement().satisfies(l -> assertThat(l.monto()).isEqualByComparingTo("118500.50"));
        assertThat(c.advertencias()).anyMatch(a -> a.contains("sin proyecto"));
    }

    // --- helpers ---

    private BigDecimal montoDe(CalculoAtribucion c, String proyecto) {
        return c.lineas().stream().filter(l -> l.proyectoNombre().equals(proyecto)).findFirst()
                .map(CalculoAtribucion.LineaCalculada::monto)
                .orElseThrow(() -> new AssertionError("Falta el proyecto " + proyecto));
    }

    private Proyecto proyecto(Long id, String nombre) {
        Proyecto p = new Proyecto();
        p.setId(id);
        p.setNombre(nombre);
        return p;
    }

    private FacturaVenta venta(String neto, Proyecto proyecto) {
        FacturaVenta f = new FacturaVenta();
        f.setNetoGravado(new BigDecimal(neto));
        f.setProyecto(proyecto);
        f.setTipoComprobante(TipoComprobante.FACTURA_A);
        return f;
    }

    private FacturaCompra compra(String neto, Proyecto proyecto) {
        FacturaCompra f = new FacturaCompra();
        f.setNeto(new BigDecimal(neto));
        f.setProyecto(proyecto);
        f.setTipoComprobante(TipoComprobante.FACTURA_A);
        return f;
    }
}
