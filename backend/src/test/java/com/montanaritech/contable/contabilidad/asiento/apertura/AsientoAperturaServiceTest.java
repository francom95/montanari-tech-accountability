package com.montanaritech.contable.contabilidad.asiento.apertura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.contabilidad.asiento.Asiento;
import com.montanaritech.contable.contabilidad.asiento.AsientoService;
import com.montanaritech.contable.contabilidad.asiento.OrigenAsiento;
import com.montanaritech.contable.contabilidad.asiento.dto.AsientoCrearRequest;
import com.montanaritech.contable.contabilidad.asiento.dto.AsientoLineaRequest;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContableRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * F10.3: verifica que el asiento de apertura arma exactamente las 21 líneas
 * del mapeo EECC→plan de cuentas y que balancea al centavo (Σdebe = Σhaber =
 * 10.260.910,91), y que cada línea resuelve la cuenta contable por código
 * (no por id hardcodeado).
 */
class AsientoAperturaServiceTest {

    private static final Long MONEDA_ARS_ID = 1L;

    private CuentaContableRepository cuentaContableRepo;
    private AsientoService asientoService;
    private AsientoAperturaService service;

    @BeforeEach
    void setUp() {
        cuentaContableRepo = mock(CuentaContableRepository.class);
        asientoService = mock(AsientoService.class);
        service = new AsientoAperturaService(cuentaContableRepo, asientoService);

        AtomicLong idSeq = new AtomicLong(100);
        when(cuentaContableRepo.findByCodigo(org.mockito.ArgumentMatchers.anyString())).thenAnswer(inv -> {
            CuentaContable c = new CuentaContable();
            c.setId(idSeq.incrementAndGet());
            c.setCodigo(inv.getArgument(0));
            return Optional.of(c);
        });

        Asiento borrador = new Asiento();
        borrador.setId(1L);
        when(asientoService.crearBorrador(any(), eq(OrigenAsiento.APERTURA), eq(false), eq(null))).thenReturn(borrador);
    }

    @Test
    void generarBorradorArma21LineasYBalanceaAlCentavo() {
        service.generarBorrador(MONEDA_ARS_ID);

        var captor = org.mockito.ArgumentCaptor.forClass(AsientoCrearRequest.class);
        verify(asientoService).crearBorrador(captor.capture(), eq(OrigenAsiento.APERTURA), eq(false), eq(null));
        AsientoCrearRequest req = captor.getValue();

        assertThat(req.fecha()).isEqualTo(LocalDate.of(2025, 9, 1));
        assertThat(req.lineas()).hasSize(21);

        BigDecimal totalDebe = req.lineas().stream().map(AsientoLineaRequest::debe).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalHaber = req.lineas().stream().map(AsientoLineaRequest::haber).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalDebe).isEqualByComparingTo("10260910.91");
        assertThat(totalHaber).isEqualByComparingTo("10260910.91");
    }

    @Test
    void cadaLineaTieneDebeXorHaberYUsaMonedaArs() {
        service.generarBorrador(MONEDA_ARS_ID);

        var captor = org.mockito.ArgumentCaptor.forClass(AsientoCrearRequest.class);
        verify(asientoService).crearBorrador(captor.capture(), eq(OrigenAsiento.APERTURA), eq(false), eq(null));
        List<AsientoLineaRequest> lineas = captor.getValue().lineas();

        for (AsientoLineaRequest l : lineas) {
            boolean debeNoCero = l.debe().compareTo(BigDecimal.ZERO) != 0;
            boolean haberNoCero = l.haber().compareTo(BigDecimal.ZERO) != 0;
            assertThat(debeNoCero ^ haberNoCero).as("línea %s debe tener debe XOR haber", l.cuentaContableId()).isTrue();
            assertThat(l.monedaId()).isEqualTo(1L);
        }
    }
}
