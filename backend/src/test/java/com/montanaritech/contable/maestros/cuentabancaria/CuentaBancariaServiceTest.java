package com.montanaritech.contable.maestros.cuentabancaria;

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
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContableRepository;
import com.montanaritech.contable.maestros.cuentabancaria.dto.CuentaBancariaCrearRequest;
import com.montanaritech.contable.maestros.cuentabancaria.dto.CuentaBancariaEditarRequest;
import com.montanaritech.contable.maestros.cuentabancaria.dto.CuentaBancariaResponse;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.moneda.MonedaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CuentaBancariaServiceTest {

    @Mock
    private CuentaBancariaRepository repo;

    @Mock
    private MonedaRepository monedaRepository;

    @Mock
    private CuentaContableRepository cuentaContableRepository;

    @Mock
    private CuentaBancariaMapper mapper;

    @Mock
    private AuditoriaService auditoria;

    // CuentaBancaria no es TarjetaCredito: la rama que usa estos repos nunca se ejecuta acá.
    private final RecalculoSaldoService recalculoSaldoService = new RecalculoSaldoService(null, null);

    private CuentaBancariaService service;

    private Moneda ars;
    private CuentaContable cuentaContable;
    private CuentaBancaria entidad;

    @BeforeEach
    void setUp() {
        service = new CuentaBancariaService(repo, monedaRepository, cuentaContableRepository, mapper, auditoria, recalculoSaldoService);

        ars = new Moneda();
        ars.setId(1L);
        ars.setCodigo("ARS");

        cuentaContable = new CuentaContable();
        cuentaContable.setId(1L);
        cuentaContable.setCodigo("1.1.2001");

        entidad = new CuentaBancaria();
        entidad.setId(1L);
        entidad.setEntidad("Banco Galicia");
        entidad.setAlias("Banco Galicia CC");
        entidad.setMoneda(ars);
        entidad.setTipo(CuentaBancaria.TipoCuenta.CUENTA_CORRIENTE);
        entidad.setEstadoConciliacion(CuentaBancaria.EstadoConciliacion.PENDIENTE);
        entidad.setSaldoInicial(new BigDecimal("1000.00"));
        entidad.setFechaSaldoInicial(LocalDate.of(2026, 1, 1));
        entidad.setSaldoActual(new BigDecimal("1000.00"));
        entidad.setCuentaContable(cuentaContable);
        entidad.setActivo(true);
    }

    @Test
    void crearConMonedaInexistenteLanzaNoEncontrado() {
        when(monedaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(new CuentaBancariaCrearRequest(
                "Banco X", "Alias X", 99L, "CUENTA_CORRIENTE", null, new BigDecimal("500"), LocalDate.now(), 1L)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void crearResuelveLaMonedaYCalculaElSaldoActual() {
        when(monedaRepository.findById(1L)).thenReturn(Optional.of(ars));
        when(cuentaContableRepository.findById(1L)).thenReturn(Optional.of(cuentaContable));
        when(repo.save(any(CuentaBancaria.class))).thenAnswer(inv -> inv.getArgument(0));

        CuentaBancaria creada = service.crear(new CuentaBancariaCrearRequest(
                "Mercado Pago", "Mercado Pago", 1L, "MERCADO_PAGO", "PENDIENTE", new BigDecimal("750.50"), LocalDate.of(2026, 3, 1), 1L));

        assertThat(creada.getMoneda()).isEqualTo(ars);
        assertThat(creada.getSaldoInicial()).isEqualByComparingTo("750.50");
        assertThat(creada.getSaldoActual()).isEqualByComparingTo("750.50");
        assertThat(creada.getTipo()).isEqualTo(CuentaBancaria.TipoCuenta.MERCADO_PAGO);
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
        when(cuentaContableRepository.findById(1L)).thenReturn(Optional.of(cuentaContable));
        when(mapper.aResponse(any(CuentaBancaria.class))).thenAnswer(inv -> {
            CuentaBancaria c = inv.getArgument(0);
            return new CuentaBancariaResponse(c.getId(), c.getEntidad(), c.getAlias(), c.getMoneda().getId(),
                    c.getMoneda().getCodigo(), c.getTipo().name(), c.getEstadoConciliacion().name(),
                    c.getSaldoInicial(), c.getFechaSaldoInicial(), c.getSaldoActual(),
                    c.getCuentaContable().getId(), c.getCuentaContable().getCodigo(), c.isActivo());
        });

        service.editar(1L, new CuentaBancariaEditarRequest(
                "Banco Galicia", "Banco Galicia CC", 1L, "CUENTA_CORRIENTE", "CONCILIADA",
                new BigDecimal("3200.75"), LocalDate.of(2026, 4, 1), 1L));

        assertThat(entidad.getSaldoInicial()).isEqualByComparingTo("3200.75");
        assertThat(entidad.getFechaSaldoInicial()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(entidad.getSaldoActual()).isEqualByComparingTo("3200.75");
        assertThat(entidad.getEstadoConciliacion()).isEqualTo(CuentaBancaria.EstadoConciliacion.CONCILIADA);
        verify(auditoria).registrar(
                eq(AccionAuditoria.EDITAR), eq("CuentaBancaria"), eq(1L), any(CuentaBancariaResponse.class), any(CuentaBancariaResponse.class));
    }

    @Test
    void desactivarCambiaElEstadoYAudita() {
        when(repo.findById(1L)).thenReturn(Optional.of(entidad));
        when(mapper.aResponse(any(CuentaBancaria.class))).thenReturn(null);

        service.desactivar(1L);

        assertThat(entidad.isActivo()).isFalse();
        verify(auditoria).registrar(eq(AccionAuditoria.CAMBIO_ESTADO), eq("CuentaBancaria"), eq(1L), any(), any());
    }

    @Test
    void eliminarBorraYAudita() {
        when(repo.findById(1L)).thenReturn(Optional.of(entidad));
        when(mapper.aResponse(any(CuentaBancaria.class))).thenReturn(null);

        service.eliminar(1L);

        verify(repo).delete(entidad);
        verify(auditoria).registrar(eq(AccionAuditoria.ELIMINAR), eq("CuentaBancaria"), eq(1L), any(), eq(null));
    }
}
