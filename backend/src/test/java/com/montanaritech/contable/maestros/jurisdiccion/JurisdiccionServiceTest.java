package com.montanaritech.contable.maestros.jurisdiccion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.maestros.jurisdiccion.dto.JurisdiccionCrearRequest;
import com.montanaritech.contable.maestros.jurisdiccion.dto.JurisdiccionEditarRequest;
import com.montanaritech.contable.maestros.jurisdiccion.dto.JurisdiccionResponse;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JurisdiccionServiceTest {

    @Mock
    private JurisdiccionRepository repo;

    @Mock
    private JurisdiccionMapper mapper;

    @Mock
    private AuditoriaService auditoria;

    @InjectMocks
    private JurisdiccionService service;

    private Jurisdiccion entidad;

    @BeforeEach
    void setUp() {
        entidad = new Jurisdiccion();
        entidad.setId(1L);
        entidad.setNombre("Buenos Aires");
        entidad.setCodigo("BA");
        entidad.setAlicuotaIIBB(new BigDecimal("3.50"));
        entidad.setActivo(true);
    }

    @Test
    void crearGuardaLaEntidadActiva() {
        when(repo.save(any(Jurisdiccion.class))).thenAnswer(inv -> inv.getArgument(0));

        Jurisdiccion creada = service.crear(new JurisdiccionCrearRequest("CABA", "CABA", new BigDecimal("3.00")));

        assertThat(creada.getCodigo()).isEqualTo("CABA");
        assertThat(creada.isActivo()).isTrue();
    }

    @Test
    void obtenerConIdInexistenteLanzaNoEncontrado() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(99L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void editarActualizaAlicuotaYAudita() {
        when(repo.findById(1L)).thenReturn(Optional.of(entidad));
        when(mapper.aResponse(any(Jurisdiccion.class))).thenAnswer(inv -> {
            Jurisdiccion j = inv.getArgument(0);
            return new JurisdiccionResponse(j.getId(), j.getNombre(), j.getCodigo(), j.getAlicuotaIIBB(), j.isActivo());
        });

        service.editar(1L, new JurisdiccionEditarRequest("Buenos Aires (editado)", new BigDecimal("4.00")));

        assertThat(entidad.getAlicuotaIIBB()).isEqualByComparingTo("4.00");
        verify(auditoria).registrar(
                eq(AccionAuditoria.EDITAR), eq("Jurisdiccion"), eq(1L), any(JurisdiccionResponse.class), any(JurisdiccionResponse.class));
    }
}
