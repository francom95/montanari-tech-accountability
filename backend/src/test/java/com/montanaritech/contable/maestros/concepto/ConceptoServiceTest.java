package com.montanaritech.contable.maestros.concepto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.maestros.concepto.dto.ConceptoCrearRequest;
import com.montanaritech.contable.maestros.concepto.dto.ConceptoEditarRequest;
import com.montanaritech.contable.maestros.concepto.dto.ConceptoResponse;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.moneda.MonedaRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConceptoServiceTest {

    @Mock
    private ConceptoRepository repo;

    @Mock
    private MonedaRepository monedaRepository;

    @Mock
    private ConceptoMapper mapper;

    @Mock
    private AuditoriaService auditoria;

    @InjectMocks
    private ConceptoService service;

    private Concepto entidad;
    private Moneda moneda;

    @BeforeEach
    void setUp() {
        moneda = new Moneda();
        moneda.setId(5L);
        moneda.setCodigo("ARS");

        entidad = new Concepto();
        entidad.setId(1L);
        entidad.setNombre("Alquiler oficina");
        entidad.setMoneda(moneda);
        entidad.setActivo(true);
    }

    @Test
    void crearSinMonedaNoConsultaElRepositorio() {
        when(repo.save(any(Concepto.class))).thenAnswer(inv -> inv.getArgument(0));

        Concepto creado = service.crear(new ConceptoCrearRequest("Sueldos", null, null, "mensual", null, null));

        assertThat(creado.getMoneda()).isNull();
        assertThat(creado.getPeriodicidad()).isEqualTo("mensual");
    }

    @Test
    void crearConMonedaInexistenteLanzaNoEncontrado() {
        when(monedaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(new ConceptoCrearRequest("X", null, null, null, null, 99L)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void crearConMonedaLaResuelveYGuardaImporte() {
        when(monedaRepository.findById(5L)).thenReturn(Optional.of(moneda));
        when(repo.save(any(Concepto.class))).thenAnswer(inv -> inv.getArgument(0));

        Concepto creado = service.crear(
                new ConceptoCrearRequest("Alquiler", "desc", "6.1.1", "mensual", new BigDecimal("1000.00"), 5L));

        assertThat(creado.getMoneda()).isEqualTo(moneda);
        assertThat(creado.getImporte()).isEqualByComparingTo("1000.00");
    }

    @Test
    void obtenerConIdInexistenteLanzaNoEncontrado() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(99L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void editarActualizaYAudita() {
        when(repo.findById(1L)).thenReturn(Optional.of(entidad));
        when(monedaRepository.findById(5L)).thenReturn(Optional.of(moneda));
        when(mapper.aResponse(any(Concepto.class))).thenAnswer(inv -> {
            Concepto c = inv.getArgument(0);
            Long monedaId = c.getMoneda() == null ? null : c.getMoneda().getId();
            return new ConceptoResponse(c.getId(), c.getNombre(), c.getDescripcion(), c.getCuentaSugerida(), c.getPeriodicidad(), c.getImporte(), monedaId, c.isActivo());
        });

        service.editar(1L, new ConceptoEditarRequest("Alquiler editado", "d", "c", "anual", new BigDecimal("500"), 5L));

        assertThat(entidad.getNombre()).isEqualTo("Alquiler editado");
        assertThat(entidad.getPeriodicidad()).isEqualTo("anual");
        verify(auditoria).registrar(
                eq(AccionAuditoria.EDITAR), eq("Concepto"), eq(1L), any(ConceptoResponse.class), any(ConceptoResponse.class));
    }
}
