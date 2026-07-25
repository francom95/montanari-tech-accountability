package com.montanaritech.contable.busqueda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.bancos.movimientobancario.MovimientoBancarioRepository;
import com.montanaritech.contable.common.tenant.TenantContext;
import com.montanaritech.contable.contabilidad.asiento.AsientoRepository;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContableRepository;
import com.montanaritech.contable.facturacion.cobro.CobroRepository;
import com.montanaritech.contable.facturacion.facturacompra.FacturaCompraRepository;
import com.montanaritech.contable.facturacion.facturaventa.FacturaVenta;
import com.montanaritech.contable.facturacion.facturaventa.FacturaVentaRepository;
import com.montanaritech.contable.facturacion.pago.PagoRepository;
import com.montanaritech.contable.impuestos.iibb.LiquidacionIibbRepository;
import com.montanaritech.contable.impuestos.iva.LiquidacionIvaRepository;
import com.montanaritech.contable.maestros.cliente.Cliente;
import com.montanaritech.contable.maestros.cliente.ClienteRepository;
import com.montanaritech.contable.maestros.proveedor.ProveedorRepository;
import com.montanaritech.contable.maestros.proyecto.Proyecto;
import com.montanaritech.contable.maestros.proyecto.ProyectoRepository;
import com.montanaritech.contable.maestros.proyecto.etapa.EtapaRepository;
import com.montanaritech.contable.maestros.tarjetacredito.TarjetaCreditoRepository;
import com.montanaritech.contable.pendiente.PendienteAdministrativoRepository;
import com.montanaritech.contable.vencimientos.VencimientoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class BusquedaGlobalServiceTest {

    @Mock private AsientoRepository asientoRepo;
    @Mock private ClienteRepository clienteRepo;
    @Mock private ProveedorRepository proveedorRepo;
    @Mock private ProyectoRepository proyectoRepo;
    @Mock private EtapaRepository etapaRepo;
    @Mock private CuentaContableRepository cuentaContableRepo;
    @Mock private PagoRepository pagoRepo;
    @Mock private CobroRepository cobroRepo;
    @Mock private FacturaVentaRepository facturaVentaRepo;
    @Mock private FacturaCompraRepository facturaCompraRepo;
    @Mock private MovimientoBancarioRepository movimientoBancarioRepo;
    @Mock private TarjetaCreditoRepository tarjetaCreditoRepo;
    @Mock private VencimientoRepository vencimientoRepo;
    @Mock private PendienteAdministrativoRepository pendienteRepo;
    @Mock private LiquidacionIvaRepository liquidacionIvaRepo;
    @Mock private LiquidacionIibbRepository liquidacionIibbRepo;

    private BusquedaGlobalService service;

    @BeforeEach
    void setUp() {
        service = new BusquedaGlobalService(asientoRepo, clienteRepo, proveedorRepo, proyectoRepo, etapaRepo,
                cuentaContableRepo, pagoRepo, cobroRepo, facturaVentaRepo, facturaCompraRepo, movimientoBancarioRepo,
                tarjetaCreditoRepo, vencimientoRepo, pendienteRepo, liquidacionIvaRepo, liquidacionIibbRepo);
        TenantContext.setTenantId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private <T> Page<T> paginaVacia() {
        return Page.empty();
    }

    @Test
    void buscarDetectaCuitYSoloConsultaLosReposRelevantes() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNombre("Cliente Uno");
        cliente.setCuit("20-12345678-9");

        when(clienteRepo.findByCuit("20-12345678-9")).thenReturn(Optional.of(cliente));
        when(proveedorRepo.findByCuit("20-12345678-9")).thenReturn(Optional.empty());
        when(facturaVentaRepo.findByCliente_Cuit(eq("20-12345678-9"), any())).thenReturn(paginaVacia());
        when(facturaCompraRepo.findByProveedor_Cuit(eq("20-12345678-9"), any())).thenReturn(paginaVacia());
        when(pagoRepo.findByProveedor_Cuit(eq("20-12345678-9"), any())).thenReturn(paginaVacia());
        when(cobroRepo.findByCliente_Cuit(eq("20-12345678-9"), any())).thenReturn(paginaVacia());

        var respuesta = service.buscar("20-12345678-9");

        assertThat(respuesta.grupos()).containsOnlyKeys(TipoEntidadBusqueda.CLIENTE);
        assertThat(respuesta.grupos().get(TipoEntidadBusqueda.CLIENTE).items()).hasSize(1);
        verifyNoInteractions(proyectoRepo, etapaRepo, cuentaContableRepo, movimientoBancarioRepo,
                tarjetaCreditoRepo, vencimientoRepo, pendienteRepo, liquidacionIvaRepo, liquidacionIibbRepo);
    }

    @Test
    void buscarDetectaFechaYSoloConsultaLosReposConFecha() {
        LocalDate fecha = LocalDate.of(2026, 7, 28);
        lenient().when(asientoRepo.findByFecha(eq(fecha), any())).thenReturn(paginaVacia());
        lenient().when(facturaVentaRepo.findByFecha(eq(fecha), any())).thenReturn(paginaVacia());
        lenient().when(facturaCompraRepo.findByFecha(eq(fecha), any())).thenReturn(paginaVacia());
        lenient().when(pagoRepo.findByFecha(eq(fecha), any())).thenReturn(paginaVacia());
        lenient().when(cobroRepo.findByFecha(eq(fecha), any())).thenReturn(paginaVacia());
        lenient().when(movimientoBancarioRepo.findByFecha(eq(fecha), any())).thenReturn(paginaVacia());
        lenient().when(vencimientoRepo.findByFecha(eq(fecha), any())).thenReturn(paginaVacia());
        lenient().when(pendienteRepo.findByFechaEstimadaResolucion(eq(fecha), any())).thenReturn(paginaVacia());
        lenient().when(proyectoRepo.findByFechaEstimadaFinalizacionOrFechaRealFinalizacion(eq(fecha), eq(fecha), any())).thenReturn(paginaVacia());
        lenient().when(etapaRepo.findByFechaInicioOrFechaEstimadaFin(eq(fecha), eq(fecha), any())).thenReturn(paginaVacia());
        lenient().when(liquidacionIvaRepo.findByFechaDesdeLessThanEqualAndFechaHastaGreaterThanEqual(eq(fecha), eq(fecha), any())).thenReturn(paginaVacia());
        lenient().when(liquidacionIibbRepo.findByFechaDesdeLessThanEqualAndFechaHastaGreaterThanEqual(eq(fecha), eq(fecha), any())).thenReturn(paginaVacia());

        service.buscar("28/07/2026");

        verify(asientoRepo).findByFecha(eq(fecha), any());
        verifyNoInteractions(clienteRepo, proveedorRepo, cuentaContableRepo, tarjetaCreditoRepo);
    }

    @Test
    void buscarDetectaImporteYAplicaTolerancia() {
        ArgumentCaptor<BigDecimal> desdeCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        ArgumentCaptor<BigDecimal> hastaCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        lenient().when(asientoRepo.buscarGlobalPorImporte(any(), any(), any())).thenReturn(paginaVacia());
        lenient().when(facturaVentaRepo.findByTotalArsBetween(any(), any(), any())).thenReturn(paginaVacia());
        lenient().when(facturaCompraRepo.findByTotalArsBetween(any(), any(), any())).thenReturn(paginaVacia());
        lenient().when(pagoRepo.findByTotalArsBetween(any(), any(), any())).thenReturn(paginaVacia());
        lenient().when(cobroRepo.findByTotalArsBetween(any(), any(), any())).thenReturn(paginaVacia());
        lenient().when(movimientoBancarioRepo.findByImporteArsBetween(any(), any(), any())).thenReturn(paginaVacia());
        lenient().when(vencimientoRepo.findByImporteEstimadoBetween(any(), any(), any())).thenReturn(paginaVacia());
        lenient().when(proyectoRepo.findByMontoTotalBetween(desdeCaptor.capture(), hastaCaptor.capture(), any())).thenReturn(paginaVacia());
        lenient().when(etapaRepo.buscarGlobalPorImporte(any(), any(), any())).thenReturn(paginaVacia());
        lenient().when(tarjetaCreditoRepo.findBySaldoActualBetween(any(), any(), any())).thenReturn(paginaVacia());
        lenient().when(liquidacionIvaRepo.findBySaldoAPagarBetween(any(), any(), any())).thenReturn(paginaVacia());
        lenient().when(liquidacionIibbRepo.findBySaldoAPagarTotalBetween(any(), any(), any())).thenReturn(paginaVacia());

        service.buscar("10000");

        assertThat(desdeCaptor.getValue()).isEqualByComparingTo("9900");
        assertThat(hastaCaptor.getValue()).isEqualByComparingTo("10100");
        verifyNoInteractions(clienteRepo, proveedorRepo, cuentaContableRepo, pendienteRepo);
    }

    @Test
    void buscarUsaTextoLibreComoFallbackYConsultaTodosLosTiposConTexto() {
        lenient().when(asientoRepo.buscarGlobalPorTexto(any(), anyString(), any())).thenReturn(paginaVacia());
        lenient().when(facturaVentaRepo.buscar(anyString(), any(), any(), any(), any(), any(), any())).thenReturn(paginaVacia());
        lenient().when(facturaCompraRepo.buscar(anyString(), any(), any(), any(), any(), any(), any())).thenReturn(paginaVacia());
        lenient().when(clienteRepo.buscar(anyString(), any(), any())).thenReturn(paginaVacia());
        lenient().when(proveedorRepo.buscar(anyString(), any(), any())).thenReturn(paginaVacia());
        lenient().when(proyectoRepo.buscar(anyString(), any(), any(), any())).thenReturn(paginaVacia());
        lenient().when(etapaRepo.buscarGlobalPorTexto(anyString(), any())).thenReturn(paginaVacia());
        lenient().when(cuentaContableRepo.buscar(anyString(), any(), any())).thenReturn(paginaVacia());
        lenient().when(pagoRepo.buscarGlobalPorTexto(anyString(), any())).thenReturn(paginaVacia());
        lenient().when(cobroRepo.buscarGlobalPorTexto(anyString(), any())).thenReturn(paginaVacia());
        lenient().when(movimientoBancarioRepo.buscarGlobalPorTexto(any(), anyString(), any())).thenReturn(paginaVacia());
        lenient().when(tarjetaCreditoRepo.buscar(anyString(), any(), any())).thenReturn(paginaVacia());
        lenient().when(vencimientoRepo.buscarGlobalPorTexto(any(), anyString(), any())).thenReturn(paginaVacia());
        lenient().when(pendienteRepo.buscar(anyString(), any(), any(), any(), any(), any(), any())).thenReturn(paginaVacia());

        service.buscar("Fima");

        verify(asientoRepo).buscarGlobalPorTexto(any(), eq("Fima"), any());
        verify(clienteRepo).buscar(eq("Fima"), any(), any());
        verifyNoInteractions(liquidacionIvaRepo, liquidacionIibbRepo);
    }

    @Test
    void tenantSeResuelveDesdeElContextoEnCadaCorridaDeTextoLibre() {
        lenient().when(asientoRepo.buscarGlobalPorTexto(any(), anyString(), any())).thenReturn(paginaVacia());
        lenient().when(movimientoBancarioRepo.buscarGlobalPorTexto(any(), anyString(), any())).thenReturn(paginaVacia());
        lenient().when(vencimientoRepo.buscarGlobalPorTexto(any(), anyString(), any())).thenReturn(paginaVacia());
        lenient().when(facturaVentaRepo.buscar(anyString(), any(), any(), any(), any(), any(), any())).thenReturn(paginaVacia());
        lenient().when(facturaCompraRepo.buscar(anyString(), any(), any(), any(), any(), any(), any())).thenReturn(paginaVacia());
        lenient().when(clienteRepo.buscar(anyString(), any(), any())).thenReturn(paginaVacia());
        lenient().when(proveedorRepo.buscar(anyString(), any(), any())).thenReturn(paginaVacia());
        lenient().when(proyectoRepo.buscar(anyString(), any(), any(), any())).thenReturn(paginaVacia());
        lenient().when(etapaRepo.buscarGlobalPorTexto(anyString(), any())).thenReturn(paginaVacia());
        lenient().when(cuentaContableRepo.buscar(anyString(), any(), any())).thenReturn(paginaVacia());
        lenient().when(pagoRepo.buscarGlobalPorTexto(anyString(), any())).thenReturn(paginaVacia());
        lenient().when(cobroRepo.buscarGlobalPorTexto(anyString(), any())).thenReturn(paginaVacia());
        lenient().when(tarjetaCreditoRepo.buscar(anyString(), any(), any())).thenReturn(paginaVacia());
        lenient().when(pendienteRepo.buscar(anyString(), any(), any(), any(), any(), any(), any())).thenReturn(paginaVacia());

        TenantContext.setTenantId(7L);
        service.buscar("Fima");
        verify(asientoRepo).buscarGlobalPorTexto(eq(7L), eq("Fima"), any());

        TenantContext.setTenantId(9L);
        service.buscar("Fima");
        verify(asientoRepo).buscarGlobalPorTexto(eq(9L), eq("Fima"), any());
    }

    @Test
    void grupoResultadoExponeElTotalDeLaPaginaNoSoloElContenidoLimitado() {
        FacturaVenta f = new FacturaVenta();
        f.setId(1L);
        f.setNumero("0001-00000001");
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNombre("Cliente Uno");
        f.setCliente(cliente);
        f.setFecha(LocalDate.of(2026, 7, 28));

        Pageable pageable = PageRequest.of(0, 5);
        Page<FacturaVenta> pagina = new PageImpl<>(List.of(f), pageable, 37);
        lenient().when(facturaVentaRepo.findByFecha(any(), any())).thenReturn(pagina);
        lenient().when(asientoRepo.findByFecha(any(), any())).thenReturn(paginaVacia());
        lenient().when(facturaCompraRepo.findByFecha(any(), any())).thenReturn(paginaVacia());
        lenient().when(pagoRepo.findByFecha(any(), any())).thenReturn(paginaVacia());
        lenient().when(cobroRepo.findByFecha(any(), any())).thenReturn(paginaVacia());
        lenient().when(movimientoBancarioRepo.findByFecha(any(), any())).thenReturn(paginaVacia());
        lenient().when(vencimientoRepo.findByFecha(any(), any())).thenReturn(paginaVacia());
        lenient().when(pendienteRepo.findByFechaEstimadaResolucion(any(), any())).thenReturn(paginaVacia());
        lenient().when(proyectoRepo.findByFechaEstimadaFinalizacionOrFechaRealFinalizacion(any(), any(), any())).thenReturn(paginaVacia());
        lenient().when(etapaRepo.findByFechaInicioOrFechaEstimadaFin(any(), any(), any())).thenReturn(paginaVacia());
        lenient().when(liquidacionIvaRepo.findByFechaDesdeLessThanEqualAndFechaHastaGreaterThanEqual(any(), any(), any())).thenReturn(paginaVacia());
        lenient().when(liquidacionIibbRepo.findByFechaDesdeLessThanEqualAndFechaHastaGreaterThanEqual(any(), any(), any())).thenReturn(paginaVacia());

        var respuesta = service.buscar("28/07/2026");

        var grupo = respuesta.grupos().get(TipoEntidadBusqueda.FACTURA_VENTA);
        assertThat(grupo.items()).hasSize(1);
        assertThat(grupo.total()).isEqualTo(37);
    }
}
