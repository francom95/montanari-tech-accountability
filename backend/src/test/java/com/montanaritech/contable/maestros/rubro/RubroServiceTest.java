package com.montanaritech.contable.maestros.rubro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.maestros.categoria.Categoria;
import com.montanaritech.contable.maestros.categoria.CategoriaRepository;
import com.montanaritech.contable.maestros.rubro.dto.RubroCrearRequest;
import com.montanaritech.contable.maestros.rubro.dto.RubroEditarRequest;
import com.montanaritech.contable.maestros.rubro.dto.RubroResponse;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RubroServiceTest {

    @Mock
    private RubroRepository repo;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private RubroMapper mapper;

    @Mock
    private AuditoriaService auditoria;

    @InjectMocks
    private RubroService service;

    private Rubro entidad;
    private Categoria categoria;

    @BeforeEach
    void setUp() {
        categoria = new Categoria();
        categoria.setId(10L);
        categoria.setNombre("Activo Corriente");
        categoria.setTipo(Categoria.TipoCategoria.ACTIVO);

        entidad = new Rubro();
        entidad.setId(1L);
        entidad.setNombre("Caja y Bancos");
        entidad.setCategoria(categoria);
        entidad.setOrden(1);
        entidad.setActivo(true);
    }

    @Test
    void crearConCategoriaInexistenteLanzaNoEncontrado() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(new RubroCrearRequest("Rubro X", 99L, 1)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void crearResuelveLaCategoriaYGuarda() {
        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(categoria));
        when(repo.save(any(Rubro.class))).thenAnswer(inv -> inv.getArgument(0));

        Rubro creado = service.crear(new RubroCrearRequest("Inversiones", 10L, 2));

        assertThat(creado.getCategoria()).isEqualTo(categoria);
        assertThat(creado.getOrden()).isEqualTo(2);
    }

    @Test
    void obtenerConIdInexistenteLanzaNoEncontrado() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(99L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void editarActualizaOrdenYAudita() {
        when(repo.findById(1L)).thenReturn(Optional.of(entidad));
        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(categoria));
        when(mapper.aResponse(any(Rubro.class))).thenAnswer(inv -> {
            Rubro r = inv.getArgument(0);
            return new RubroResponse(r.getId(), r.getNombre(), r.getCategoria().getId(), r.getOrden(), r.isActivo());
        });

        service.editar(1L, new RubroEditarRequest("Caja y Bancos (editado)", 10L, 5));

        assertThat(entidad.getOrden()).isEqualTo(5);
        verify(auditoria).registrar(
                eq(AccionAuditoria.EDITAR), eq("Rubro"), eq(1L), any(RubroResponse.class), any(RubroResponse.class));
    }
}
