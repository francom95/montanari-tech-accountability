package com.montanaritech.contable.maestros.categoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.maestros.categoria.dto.CategoriaCrearRequest;
import com.montanaritech.contable.maestros.categoria.dto.CategoriaEditarRequest;
import com.montanaritech.contable.maestros.categoria.dto.CategoriaResponse;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock
    private CategoriaRepository repo;

    @Mock
    private CategoriaMapper mapper;

    @Mock
    private AuditoriaService auditoria;

    @InjectMocks
    private CategoriaService service;

    private Categoria entidad;

    @BeforeEach
    void setUp() {
        entidad = new Categoria();
        entidad.setId(1L);
        entidad.setNombre("Activo Corriente");
        entidad.setTipo(Categoria.TipoCategoria.ACTIVO);
        entidad.setActivo(true);
    }

    @Test
    void crearGuardaLaEntidadConTipoConvertido() {
        when(repo.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

        Categoria creada = service.crear(new CategoriaCrearRequest("Pasivo Corriente", "desc", "PASIVO"));

        assertThat(creada.getTipo()).isEqualTo(Categoria.TipoCategoria.PASIVO);
        assertThat(creada.isActivo()).isTrue();
    }

    @Test
    void obtenerConIdInexistenteLanzaNoEncontrado() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(99L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void editarActualizaTipoYAudita() {
        when(repo.findById(1L)).thenReturn(Optional.of(entidad));
        when(mapper.aResponse(any(Categoria.class))).thenAnswer(inv -> {
            Categoria c = inv.getArgument(0);
            return new CategoriaResponse(c.getId(), c.getNombre(), c.getDescripcion(), c.getTipo().name(), c.isActivo());
        });

        service.editar(1L, new CategoriaEditarRequest("Renombrada", "desc2", "RMINUS"));

        assertThat(entidad.getNombre()).isEqualTo("Renombrada");
        assertThat(entidad.getTipo()).isEqualTo(Categoria.TipoCategoria.RMINUS);
        verify(auditoria).registrar(
                eq(AccionAuditoria.EDITAR), eq("Categoria"), eq(1L), any(CategoriaResponse.class), any(CategoriaResponse.class));
    }
}
