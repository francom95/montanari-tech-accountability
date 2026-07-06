package com.montanaritech.contable.maestros.moneda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.ConflictoException;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.maestros.moneda.dto.MonedaCrearRequest;
import com.montanaritech.contable.maestros.moneda.dto.MonedaEditarRequest;
import com.montanaritech.contable.maestros.moneda.dto.MonedaResponse;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Molde de referencia de PL-1 (F1.8): test unitario de service con
 * dependencias mockeadas (repository, mapper, auditoría). Cubre las
 * validaciones de negocio, no la persistencia real — eso lo hace
 * {@link MonedaControllerIT}.
 */
@ExtendWith(MockitoExtension.class)
class MonedaServiceTest {

    @Mock
    private MonedaRepository monedaRepository;

    @Mock
    private MonedaMapper monedaMapper;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private MonedaService monedaService;

    private Moneda moneda;

    @BeforeEach
    void setUp() {
        moneda = new Moneda();
        moneda.setId(1L);
        moneda.setCodigo("ARS");
        moneda.setNombre("Peso Argentino");
        moneda.setSimbolo("$");
        moneda.setActivo(true);
    }

    @Test
    void crearConCodigoDuplicadoLanzaConflicto() {
        MonedaCrearRequest request = new MonedaCrearRequest("ARS", "Peso Argentino", "$");
        when(monedaRepository.findByCodigo("ARS")).thenReturn(Optional.of(moneda));

        assertThatThrownBy(() -> monedaService.crear(request))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("Ya existe");

        verify(monedaRepository, never()).save(any());
    }

    @Test
    void crearGuardaLaMonedaActiva() {
        MonedaCrearRequest request = new MonedaCrearRequest("USD", "Dólar", "US$");
        when(monedaRepository.findByCodigo("USD")).thenReturn(Optional.empty());
        when(monedaRepository.save(any(Moneda.class))).thenAnswer(inv -> inv.getArgument(0));

        Moneda creada = monedaService.crear(request);

        assertThat(creada.getCodigo()).isEqualTo("USD");
        assertThat(creada.isActivo()).isTrue();
    }

    @Test
    void obtenerConIdInexistenteLanzaNoEncontrado() {
        when(monedaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> monedaService.obtener(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void editarActualizaCamposYAuditaConAntesYDespues() {
        when(monedaRepository.findById(1L)).thenReturn(Optional.of(moneda));
        when(monedaMapper.aResponse(any(Moneda.class))).thenAnswer(inv -> {
            Moneda m = inv.getArgument(0);
            return new MonedaResponse(m.getId(), m.getCodigo(), m.getNombre(), m.getSimbolo(), m.isActivo());
        });

        monedaService.editar(1L, new MonedaEditarRequest("Peso Arg. Editado", "AR$"));

        assertThat(moneda.getNombre()).isEqualTo("Peso Arg. Editado");
        assertThat(moneda.getSimbolo()).isEqualTo("AR$");
        verify(auditoriaService).registrar(
                eq(AccionAuditoria.EDITAR), eq("Moneda"), eq(1L), any(MonedaResponse.class), any(MonedaResponse.class));
    }

    @Test
    void eliminarBorraYAuditaCuandoNoHayMovimientosAsociados() {
        when(monedaRepository.findById(1L)).thenReturn(Optional.of(moneda));
        when(monedaMapper.aResponse(any(Moneda.class))).thenReturn(
                new MonedaResponse(1L, "ARS", "Peso Argentino", "$", true));

        monedaService.eliminar(1L);

        verify(monedaRepository).delete(moneda);
        verify(auditoriaService).registrar(
                eq(AccionAuditoria.ELIMINAR), eq("Moneda"), eq(1L), any(MonedaResponse.class), eq(null));
    }
}
