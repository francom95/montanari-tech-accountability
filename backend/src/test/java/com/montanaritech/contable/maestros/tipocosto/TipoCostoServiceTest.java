package com.montanaritech.contable.maestros.tipocosto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.maestros.tipocosto.dto.TipoCostoCrearRequest;
import com.montanaritech.contable.maestros.tipocosto.dto.TipoCostoEditarRequest;
import com.montanaritech.contable.maestros.tipocosto.dto.TipoCostoResponse;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TipoCostoServiceTest {

    @Mock
    private TipoCostoRepository repo;

    @Mock
    private TipoCostoMapper mapper;

    @Mock
    private AuditoriaService auditoria;

    @InjectMocks
    private TipoCostoService service;

    private TipoCosto entidad;

    @BeforeEach
    void setUp() {
        entidad = new TipoCosto();
        entidad.setId(1L);
        entidad.setNombre("Diseño");
        entidad.setActivo(true);
    }

    @Test
    void crearGuardaLaEntidadActiva() {
        when(repo.save(any(TipoCosto.class))).thenAnswer(inv -> inv.getArgument(0));

        TipoCosto creada = service.crear(new TipoCostoCrearRequest("Programación", "Desarrollo de software"));

        assertThat(creada.getNombre()).isEqualTo("Programación");
        assertThat(creada.isActivo()).isTrue();
    }

    @Test
    void obtenerConIdInexistenteLanzaNoEncontrado() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(99L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void editarActualizaYAuditaConAntesYDespues() {
        when(repo.findById(1L)).thenReturn(Optional.of(entidad));
        when(mapper.aResponse(any(TipoCosto.class))).thenAnswer(inv -> {
            TipoCosto e = inv.getArgument(0);
            return new TipoCostoResponse(e.getId(), e.getNombre(), e.getDescripcion(), e.isActivo());
        });

        service.editar(1L, new TipoCostoEditarRequest("Diseño Editado", "Descripción nueva"));

        assertThat(entidad.getNombre()).isEqualTo("Diseño Editado");
        verify(auditoria).registrar(
                eq(AccionAuditoria.EDITAR), eq("TipoCosto"), eq(1L), any(TipoCostoResponse.class), any(TipoCostoResponse.class));
    }

    @Test
    void eliminarBorraYAuditaConAccionEliminar() {
        when(repo.findById(1L)).thenReturn(Optional.of(entidad));
        when(mapper.aResponse(any(TipoCosto.class))).thenReturn(
                new TipoCostoResponse(1L, "Diseño", null, true));

        service.eliminar(1L);

        verify(repo).delete(entidad);
        verify(auditoria).registrar(
                eq(AccionAuditoria.ELIMINAR), eq("TipoCosto"), eq(1L), any(TipoCostoResponse.class), eq(null));
    }
}
