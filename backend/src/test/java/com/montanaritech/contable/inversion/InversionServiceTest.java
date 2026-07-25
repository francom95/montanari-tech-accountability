package com.montanaritech.contable.inversion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.ConflictoException;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.compromiso.Compromiso;
import com.montanaritech.contable.compromiso.CompromisoService;
import com.montanaritech.contable.inversion.dto.InversionCrearRequest;
import com.montanaritech.contable.inversion.dto.InversionEditarRequest;
import com.montanaritech.contable.inversion.dto.InversionResponse;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancaria;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancariaRepository;
import com.montanaritech.contable.vencimientos.VencimientoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InversionServiceTest {

    @Mock private InversionRepository repo;
    @Mock private MovimientoInversionRepository movimientoRepo;
    @Mock private CuentaBancariaRepository cuentaBancariaRepo;
    @Mock private CompromisoService compromisoService;
    @Mock private VencimientoService vencimientoService;
    @Mock private AuditoriaService auditoria;

    private InversionService service;
    private CuentaBancaria cuenta;

    @BeforeEach
    void setUp() {
        service = new InversionService(repo, movimientoRepo, cuentaBancariaRepo, compromisoService,
                vencimientoService, auditoria);
        cuenta = new CuentaBancaria();
        cuenta.setId(10L);
        cuenta.setAlias("Banco Galicia CC");
    }

    private Inversion nuevaInversion() {
        Inversion inv = new Inversion();
        inv.setId(1L);
        inv.setInstrumento("Fondo Fima");
        inv.setCuentaOrigen(cuenta);
        inv.setEstado(EstadoInversion.ACTIVA);
        inv.setActivo(true);
        return inv;
    }

    private MovimientoInversion movimiento(TipoMovimientoInversion tipo, BigDecimal cuotapartes,
            BigDecimal montoAplicado, BigDecimal valorCuotaparte, LocalDate fecha) {
        MovimientoInversion m = new MovimientoInversion();
        m.setTipo(tipo);
        m.setCuotapartes(cuotapartes);
        m.setMontoAplicado(montoAplicado);
        m.setValorCuotaparte(valorCuotaparte);
        m.setFecha(fecha);
        return m;
    }

    @Test
    void aResponseSinMovimientosDaValuacionYRendimientoEnCero() {
        Inversion inv = nuevaInversion();
        when(movimientoRepo.findByInversion_Id(1L)).thenReturn(List.of());
        when(movimientoRepo.findFirstByInversion_IdOrderByFechaDescIdDesc(1L)).thenReturn(Optional.empty());

        InversionResponse r = service.aResponse(inv);

        assertThat(r.cuotapartesAcumuladas()).isEqualByComparingTo("0");
        assertThat(r.valuacionActual()).isEqualByComparingTo("0");
        assertThat(r.rendimiento()).isEqualByComparingTo("0");
    }

    @Test
    void aResponseConSoloSuscripcionesAcumulaCuotapartesYValuacion() {
        Inversion inv = nuevaInversion();
        MovimientoInversion m1 = movimiento(TipoMovimientoInversion.SUSCRIPCION, new BigDecimal("100.000000"),
                new BigDecimal("10000.00"), new BigDecimal("100.000000"), LocalDate.of(2026, 7, 1));
        MovimientoInversion m2 = movimiento(TipoMovimientoInversion.SUSCRIPCION, new BigDecimal("50.000000"),
                new BigDecimal("5100.00"), new BigDecimal("102.000000"), LocalDate.of(2026, 7, 15));
        when(movimientoRepo.findByInversion_Id(1L)).thenReturn(List.of(m1, m2));
        when(movimientoRepo.findFirstByInversion_IdOrderByFechaDescIdDesc(1L)).thenReturn(Optional.of(m2));

        InversionResponse r = service.aResponse(inv);

        assertThat(r.cuotapartesAcumuladas()).isEqualByComparingTo("150.000000");
        assertThat(r.montoNetoAplicado()).isEqualByComparingTo("15100.00");
        // 150 cuotapartes x 102.00 (último valor cargado) = 15300.00
        assertThat(r.valuacionActual()).isEqualByComparingTo("15300.00");
        assertThat(r.rendimiento()).isEqualByComparingTo("200.00");
    }

    @Test
    void aResponseConRescateParcialRestaCuotapartesYMontoNeto() {
        Inversion inv = nuevaInversion();
        MovimientoInversion suscripcion = movimiento(TipoMovimientoInversion.SUSCRIPCION, new BigDecimal("100.000000"),
                new BigDecimal("10000.00"), new BigDecimal("100.000000"), LocalDate.of(2026, 7, 1));
        MovimientoInversion rescate = movimiento(TipoMovimientoInversion.RESCATE, new BigDecimal("40.000000"),
                new BigDecimal("4080.00"), new BigDecimal("102.000000"), LocalDate.of(2026, 7, 20));
        when(movimientoRepo.findByInversion_Id(1L)).thenReturn(List.of(suscripcion, rescate));
        when(movimientoRepo.findFirstByInversion_IdOrderByFechaDescIdDesc(1L)).thenReturn(Optional.of(rescate));

        InversionResponse r = service.aResponse(inv);

        assertThat(r.cuotapartesAcumuladas()).isEqualByComparingTo("60.000000");
        assertThat(r.montoNetoAplicado()).isEqualByComparingTo("5920.00");
        // 60 cuotapartes x 102.00 = 6120.00
        assertThat(r.valuacionActual()).isEqualByComparingTo("6120.00");
    }

    @Test
    void crearConVinculoACompromisoInexistenteLanzaNoEncontrado() {
        when(cuentaBancariaRepo.findById(10L)).thenReturn(Optional.of(cuenta));
        when(compromisoService.obtener(999L)).thenThrow(new RecursoNoEncontradoException("Compromiso 999 no encontrado"));

        assertThatThrownBy(() -> service.crear(new InversionCrearRequest("Fondo Fima", 10L, null,
                TipoVinculoInversion.COMPROMISO, 999L)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void crearConVinculoACompromisoExistenteLoGuarda() {
        when(cuentaBancariaRepo.findById(10L)).thenReturn(Optional.of(cuenta));
        when(compromisoService.obtener(55L)).thenReturn(new Compromiso());
        when(repo.save(any(Inversion.class))).thenAnswer(inv -> {
            Inversion i = inv.getArgument(0);
            i.setId(1L);
            return i;
        });
        when(movimientoRepo.findByInversion_Id(1L)).thenReturn(List.of());
        when(movimientoRepo.findFirstByInversion_IdOrderByFechaDescIdDesc(1L)).thenReturn(Optional.empty());

        Inversion creada = service.crear(new InversionCrearRequest("Fondo Fima", 10L, "IVA marzo",
                TipoVinculoInversion.COMPROMISO, 55L));

        assertThat(creada.getVinculoTipo()).isEqualTo(TipoVinculoInversion.COMPROMISO);
        assertThat(creada.getVinculoRefId()).isEqualTo(55L);
    }

    @Test
    void editarActualizaEstado() {
        Inversion existente = nuevaInversion();
        when(repo.findById(1L)).thenReturn(Optional.of(existente));
        when(cuentaBancariaRepo.findById(10L)).thenReturn(Optional.of(cuenta));
        when(movimientoRepo.findByInversion_Id(1L)).thenReturn(List.of());
        when(movimientoRepo.findFirstByInversion_IdOrderByFechaDescIdDesc(1L)).thenReturn(Optional.empty());

        service.editar(1L, new InversionEditarRequest("Fondo Fima", 10L, null, null, null, EstadoInversion.CANCELADA));

        assertThat(existente.getEstado()).isEqualTo(EstadoInversion.CANCELADA);
    }

    @Test
    void eliminarConMovimientosAsociadosLanzaConflicto() {
        Inversion existente = nuevaInversion();
        when(repo.findById(1L)).thenReturn(Optional.of(existente));
        when(movimientoRepo.countByInversion_Id(1L)).thenReturn(3L);

        assertThatThrownBy(() -> service.eliminar(1L)).isInstanceOf(ConflictoException.class);
    }

    @Test
    void eliminarSinMovimientosAsociadosFunciona() {
        Inversion existente = nuevaInversion();
        when(repo.findById(1L)).thenReturn(Optional.of(existente));
        when(movimientoRepo.countByInversion_Id(1L)).thenReturn(0L);
        when(movimientoRepo.findByInversion_Id(1L)).thenReturn(List.of());
        when(movimientoRepo.findFirstByInversion_IdOrderByFechaDescIdDesc(1L)).thenReturn(Optional.empty());

        service.eliminar(1L);

        verify(repo).delete(existente);
    }
}
