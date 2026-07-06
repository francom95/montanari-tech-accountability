package com.montanaritech.contable.maestros.tarjetacredito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.common.saldo.RecalculoSaldoService;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancaria;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancariaRepository;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.moneda.MonedaRepository;
import com.montanaritech.contable.maestros.tarjetacredito.dto.TarjetaCreditoCrearRequest;
import com.montanaritech.contable.maestros.tarjetacredito.dto.TarjetaCreditoEditarRequest;
import com.montanaritech.contable.maestros.tarjetacredito.dto.TarjetaCreditoResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TarjetaCreditoServiceTest {

    @Mock
    private TarjetaCreditoRepository repo;

    @Mock
    private MonedaRepository monedaRepository;

    @Mock
    private CuentaBancariaRepository cuentaBancariaRepository;

    @Mock
    private TarjetaCreditoMapper mapper;

    @Mock
    private AuditoriaService auditoria;

    private final RecalculoSaldoService recalculoSaldoService = new RecalculoSaldoService();

    private TarjetaCreditoService service;

    private Moneda ars;
    private CuentaBancaria cuentaDebito;
    private TarjetaCredito entidad;

    @BeforeEach
    void setUp() {
        service = new TarjetaCreditoService(repo, monedaRepository, cuentaBancariaRepository, mapper, auditoria, recalculoSaldoService);

        ars = new Moneda();
        ars.setId(1L);
        ars.setCodigo("ARS");

        cuentaDebito = new CuentaBancaria();
        cuentaDebito.setId(5L);
        cuentaDebito.setAlias("Banco Galicia CC");

        entidad = new TarjetaCredito();
        entidad.setId(1L);
        entidad.setEntidad("Visa Banco Galicia");
        entidad.setMoneda(ars);
        entidad.setDiaCierre(10);
        entidad.setDiaVencimiento(20);
        entidad.setCuentaBancariaDebito(cuentaDebito);
        entidad.setSaldoInicial(new BigDecimal("1000.00"));
        entidad.setFechaSaldoInicial(LocalDate.of(2026, 1, 1));
        entidad.setSaldoActual(new BigDecimal("1000.00"));
        entidad.setActivo(true);
    }

    @Test
    void crearConCuentaDebitoInexistenteLanzaNoEncontrado() {
        when(monedaRepository.findById(1L)).thenReturn(Optional.of(ars));
        when(cuentaBancariaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(new TarjetaCreditoCrearRequest(
                "Visa", 1L, 10, 20, 99L, new BigDecimal("0"), LocalDate.now())))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void crearResuelveFKsYCalculaElSaldoActual() {
        when(monedaRepository.findById(1L)).thenReturn(Optional.of(ars));
        when(cuentaBancariaRepository.findById(5L)).thenReturn(Optional.of(cuentaDebito));
        when(repo.save(any(TarjetaCredito.class))).thenAnswer(inv -> inv.getArgument(0));

        TarjetaCredito creada = service.crear(new TarjetaCreditoCrearRequest(
                "Mastercard Banco Galicia", 1L, 5, 15, 5L, new BigDecimal("-1500.00"), LocalDate.of(2026, 2, 1)));

        assertThat(creada.getCuentaBancariaDebito()).isEqualTo(cuentaDebito);
        assertThat(creada.getSaldoActual()).isEqualByComparingTo("-1500.00");
        assertThat(creada.getDiaCierre()).isEqualTo(5);
        assertThat(creada.getDiaVencimiento()).isEqualTo(15);
    }

    @Test
    void obtenerConIdInexistenteLanzaNoEncontrado() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(99L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void editarModificaElSaldoInicialYRecalculaElSaldoActual() {
        when(repo.findById(1L)).thenReturn(Optional.of(entidad));
        when(monedaRepository.findById(1L)).thenReturn(Optional.of(ars));
        when(cuentaBancariaRepository.findById(5L)).thenReturn(Optional.of(cuentaDebito));
        when(mapper.aResponse(any(TarjetaCredito.class))).thenAnswer(inv -> {
            TarjetaCredito t = inv.getArgument(0);
            return new TarjetaCreditoResponse(t.getId(), t.getEntidad(), t.getMoneda().getId(), t.getMoneda().getCodigo(),
                    t.getDiaCierre(), t.getDiaVencimiento(), t.getCuentaBancariaDebito().getId(),
                    t.getCuentaBancariaDebito().getAlias(), t.getSaldoInicial(), t.getFechaSaldoInicial(),
                    t.getSaldoActual(), t.isActivo());
        });

        service.editar(1L, new TarjetaCreditoEditarRequest(
                "Visa Banco Galicia", 1L, 12, 22, 5L, new BigDecimal("-4200.00"), LocalDate.of(2026, 5, 1)));

        assertThat(entidad.getSaldoInicial()).isEqualByComparingTo("-4200.00");
        assertThat(entidad.getSaldoActual()).isEqualByComparingTo("-4200.00");
        assertThat(entidad.getDiaCierre()).isEqualTo(12);
        assertThat(entidad.getDiaVencimiento()).isEqualTo(22);
        verify(auditoria).registrar(
                eq(AccionAuditoria.EDITAR), eq("TarjetaCredito"), eq(1L), any(TarjetaCreditoResponse.class), any(TarjetaCreditoResponse.class));
    }

    @Test
    void desactivarCambiaElEstadoYAudita() {
        when(repo.findById(1L)).thenReturn(Optional.of(entidad));
        when(mapper.aResponse(any(TarjetaCredito.class))).thenReturn(null);

        service.desactivar(1L);

        assertThat(entidad.isActivo()).isFalse();
        verify(auditoria).registrar(eq(AccionAuditoria.CAMBIO_ESTADO), eq("TarjetaCredito"), eq(1L), any(), any());
    }

    @Test
    void eliminarBorraYAudita() {
        when(repo.findById(1L)).thenReturn(Optional.of(entidad));
        when(mapper.aResponse(any(TarjetaCredito.class))).thenReturn(null);

        service.eliminar(1L);

        verify(repo).delete(entidad);
        verify(auditoria).registrar(eq(AccionAuditoria.ELIMINAR), eq("TarjetaCredito"), eq(1L), any(), eq(null));
    }
}
