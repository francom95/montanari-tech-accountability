package com.montanaritech.contable.maestros.tipocambio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.moneda.MonedaRepository;
import com.montanaritech.contable.maestros.tipocambio.dto.TipoCambioCrearRequest;
import com.montanaritech.contable.maestros.tipocambio.dto.TipoCambioEditarRequest;
import com.montanaritech.contable.maestros.tipocambio.dto.TipoCambioResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TipoCambioServiceTest {

    @Mock
    private TipoCambioRepository repo;

    @Mock
    private MonedaRepository monedaRepository;

    @Mock
    private TipoCambioMapper mapper;

    @Mock
    private AuditoriaService auditoria;

    @InjectMocks
    private TipoCambioService service;

    private TipoCambio entidad;
    private Moneda moneda;

    @BeforeEach
    void setUp() {
        moneda = new Moneda();
        moneda.setId(1L);
        moneda.setCodigo("USD");

        entidad = new TipoCambio();
        entidad.setId(1L);
        entidad.setFecha(LocalDate.of(2026, 1, 1));
        entidad.setMoneda(moneda);
        entidad.setCriterio("BNA_VENTA");
        entidad.setValorCompra(new BigDecimal("900.00"));
        entidad.setValorVenta(new BigDecimal("950.00"));
        entidad.setActivo(true);
    }

    @Test
    void crearConMonedaInexistenteLanzaNoEncontrado() {
        when(monedaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(new TipoCambioCrearRequest(
                LocalDate.now(), 99L, "OFICIAL", BigDecimal.ONE, BigDecimal.ONE, null, null)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void crearResuelveLaMonedaYGuarda() {
        when(monedaRepository.findById(1L)).thenReturn(Optional.of(moneda));
        when(repo.save(any(TipoCambio.class))).thenAnswer(inv -> inv.getArgument(0));

        TipoCambio creado = service.crear(new TipoCambioCrearRequest(
                LocalDate.of(2026, 2, 1), 1L, "MANUAL", new BigDecimal("1000"), new BigDecimal("1050"), "manual", null));

        assertThat(creado.getMoneda()).isEqualTo(moneda);
        assertThat(creado.getCriterio()).isEqualTo("MANUAL");
    }

    @Test
    void obtenerConIdInexistenteLanzaNoEncontrado() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(99L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void editarActualizaValoresYAudita() {
        when(repo.findById(1L)).thenReturn(Optional.of(entidad));
        when(mapper.aResponse(any(TipoCambio.class))).thenAnswer(inv -> {
            TipoCambio tc = inv.getArgument(0);
            return new TipoCambioResponse(tc.getId(), tc.getFecha(), tc.getMoneda().getId(), tc.getCriterio(),
                    tc.getValorCompra(), tc.getValorVenta(), tc.getFuente(), tc.getObservaciones(), tc.isActivo());
        });

        service.editar(1L, new TipoCambioEditarRequest(new BigDecimal("910"), new BigDecimal("960"), "BNA", "obs"));

        assertThat(entidad.getValorCompra()).isEqualByComparingTo("910");
        verify(auditoria).registrar(
                eq(AccionAuditoria.EDITAR), eq("TipoCambio"), eq(1L), any(TipoCambioResponse.class), any(TipoCambioResponse.class));
    }
}
