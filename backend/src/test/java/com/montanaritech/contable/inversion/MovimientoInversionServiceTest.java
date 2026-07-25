package com.montanaritech.contable.inversion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.bancos.movimientobancario.MovimientoBancario;
import com.montanaritech.contable.bancos.movimientobancario.MovimientoBancarioService;
import com.montanaritech.contable.bancos.movimientobancario.dto.CrearMovimientoBancarioRequest;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.ConflictoException;
import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.inversion.dto.InversionResponse;
import com.montanaritech.contable.inversion.dto.MovimientoInversionCrearRequest;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancaria;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.tipocambio.TipoCambio;
import com.montanaritech.contable.maestros.tipocambio.TipoCambioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MovimientoInversionServiceTest {

    @Mock private MovimientoInversionRepository repo;
    @Mock private InversionService inversionService;
    @Mock private MovimientoBancarioService movimientoBancarioService;
    @Mock private TipoCambioRepository tipoCambioRepo;
    @Mock private AuditoriaService auditoria;

    private MovimientoInversionService service;
    private Moneda ars;
    private Moneda usd;
    private CuentaBancaria cuenta;
    private Inversion inversion;

    @BeforeEach
    void setUp() {
        service = new MovimientoInversionService(repo, inversionService, movimientoBancarioService, tipoCambioRepo,
                auditoria);
        ars = new Moneda();
        ars.setId(1L);
        ars.setCodigo("ARS");
        usd = new Moneda();
        usd.setId(2L);
        usd.setCodigo("USD");

        cuenta = new CuentaBancaria();
        cuenta.setId(10L);
        cuenta.setMoneda(ars);

        inversion = new Inversion();
        inversion.setId(1L);
        inversion.setInstrumento("Fondo Fima");
        inversion.setCuentaOrigen(cuenta);
        inversion.setEstado(EstadoInversion.ACTIVA);

        when(inversionService.obtener(1L)).thenReturn(inversion);
    }

    private InversionResponse respuestaCon(BigDecimal cuotapartesAcumuladas) {
        return new InversionResponse(1L, "Fondo Fima", 10L, "Banco Galicia CC", null, null, null,
                EstadoInversion.ACTIVA, true, cuotapartesAcumuladas, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    private MovimientoInversionCrearRequest requestSuscripcion() {
        return new MovimientoInversionCrearRequest(1L, TipoMovimientoInversion.SUSCRIPCION, LocalDate.of(2026, 7, 1),
                new BigDecimal("10000.00"), new BigDecimal("100.000000"), new BigDecimal("100.000000"), null, null);
    }

    private MovimientoInversionCrearRequest requestRescate(BigDecimal cuotapartes) {
        return new MovimientoInversionCrearRequest(1L, TipoMovimientoInversion.RESCATE, LocalDate.of(2026, 7, 20),
                new BigDecimal("4080.00"), cuotapartes, new BigDecimal("102.000000"), null, null);
    }

    @Test
    void crearSuscripcionGeneraMovimientoBancarioConImporteNegativo() {
        when(inversionService.aResponse(inversion)).thenReturn(respuestaCon(BigDecimal.ZERO));
        MovimientoBancario mb = new MovimientoBancario();
        mb.setId(500L);
        when(movimientoBancarioService.crear(any())).thenReturn(mb);
        when(repo.save(any(MovimientoInversion.class))).thenAnswer(inv -> {
            MovimientoInversion m = inv.getArgument(0);
            m.setId(1L);
            return m;
        });

        service.crear(requestSuscripcion());

        ArgumentCaptor<CrearMovimientoBancarioRequest> captor = ArgumentCaptor.forClass(CrearMovimientoBancarioRequest.class);
        org.mockito.Mockito.verify(movimientoBancarioService).crear(captor.capture());
        assertThat(captor.getValue().importe()).isEqualByComparingTo("-10000.00");
        assertThat(captor.getValue().cuentaBancariaId()).isEqualTo(10L);
        assertThat(captor.getValue().tipoCambio()).isEqualByComparingTo("1");
    }

    @Test
    void crearRescateGeneraMovimientoBancarioConImportePositivo() {
        when(inversionService.aResponse(inversion)).thenReturn(respuestaCon(new BigDecimal("100.000000")));
        MovimientoBancario mb = new MovimientoBancario();
        mb.setId(501L);
        when(movimientoBancarioService.crear(any())).thenReturn(mb);
        when(repo.save(any(MovimientoInversion.class))).thenAnswer(inv -> {
            MovimientoInversion m = inv.getArgument(0);
            m.setId(2L);
            return m;
        });

        service.crear(requestRescate(new BigDecimal("40.000000")));

        ArgumentCaptor<CrearMovimientoBancarioRequest> captor = ArgumentCaptor.forClass(CrearMovimientoBancarioRequest.class);
        org.mockito.Mockito.verify(movimientoBancarioService).crear(captor.capture());
        assertThat(captor.getValue().importe()).isEqualByComparingTo("4080.00");
    }

    @Test
    void crearRescateQueExcedeCuotapartesDisponiblesLanzaConflicto() {
        when(inversionService.aResponse(inversion)).thenReturn(respuestaCon(new BigDecimal("30.000000")));

        assertThatThrownBy(() -> service.crear(requestRescate(new BigDecimal("40.000000"))))
                .isInstanceOf(ConflictoException.class);
        org.mockito.Mockito.verify(movimientoBancarioService, org.mockito.Mockito.never()).crear(any());
    }

    @Test
    void crearRescateQueConsumeTodasLasCuotapartesMarcaInversionRescatadaTotal() {
        // Primera llamada (validación pre-guardado): 40 disponibles. Segunda
        // llamada (post-guardado, para decidir RESCATADA_TOTAL): 0 restantes.
        when(inversionService.aResponse(inversion))
                .thenReturn(respuestaCon(new BigDecimal("40.000000")))
                .thenReturn(respuestaCon(BigDecimal.ZERO));
        MovimientoBancario mb = new MovimientoBancario();
        mb.setId(502L);
        when(movimientoBancarioService.crear(any())).thenReturn(mb);
        when(repo.save(any(MovimientoInversion.class))).thenAnswer(inv -> {
            MovimientoInversion m = inv.getArgument(0);
            m.setId(3L);
            return m;
        });

        service.crear(requestRescate(new BigDecimal("40.000000")));

        assertThat(inversion.getEstado()).isEqualTo(EstadoInversion.RESCATADA_TOTAL);
    }

    @Test
    void crearSobreInversionNoActivaLanzaNegocio() {
        inversion.setEstado(EstadoInversion.RESCATADA_TOTAL);

        assertThatThrownBy(() -> service.crear(requestSuscripcion())).isInstanceOf(NegocioException.class);
    }

    @Test
    void crearConMonedaNoArsSinCotizacionLanzaNegocio() {
        cuenta.setMoneda(usd);
        when(inversionService.aResponse(inversion)).thenReturn(respuestaCon(BigDecimal.ZERO));
        when(tipoCambioRepo.findFirstByMonedaIdAndActivoTrueOrderByFechaDesc(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(requestSuscripcion())).isInstanceOf(NegocioException.class);
    }

    @Test
    void crearConMonedaNoArsConCotizacionUsaValorVenta() {
        cuenta.setMoneda(usd);
        when(inversionService.aResponse(inversion)).thenReturn(respuestaCon(BigDecimal.ZERO));
        TipoCambio tc = new TipoCambio();
        tc.setValorVenta(new BigDecimal("1200.50"));
        when(tipoCambioRepo.findFirstByMonedaIdAndActivoTrueOrderByFechaDesc(2L)).thenReturn(Optional.of(tc));
        MovimientoBancario mb = new MovimientoBancario();
        mb.setId(503L);
        when(movimientoBancarioService.crear(any())).thenReturn(mb);
        when(repo.save(any(MovimientoInversion.class))).thenAnswer(inv -> {
            MovimientoInversion m = inv.getArgument(0);
            m.setId(4L);
            return m;
        });

        service.crear(requestSuscripcion());

        ArgumentCaptor<CrearMovimientoBancarioRequest> captor = ArgumentCaptor.forClass(CrearMovimientoBancarioRequest.class);
        org.mockito.Mockito.verify(movimientoBancarioService).crear(captor.capture());
        assertThat(captor.getValue().tipoCambio()).isEqualByComparingTo("1200.50");
    }
}
