package com.montanaritech.contable.common.asiento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.contabilidad.asiento.Asiento;
import com.montanaritech.contable.contabilidad.asiento.AsientoLineaRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * F10.3: verifica que el patrón de búsqueda se arma con el mismo formato
 * ("FC: PPPPP-NNNNNNNN") que usa el Libro Diario real migrado en F10.2, y
 * que solo busca entre asientos CONFIRMADOS.
 */
class BuscarAsientoPorComprobanteTest {

    private AsientoLineaRepository asientoLineaRepo;
    private BuscarAsientoPorComprobante buscar;

    @BeforeEach
    void setUp() {
        asientoLineaRepo = mock(AsientoLineaRepository.class);
        buscar = new BuscarAsientoPorComprobante(asientoLineaRepo);
    }

    @Test
    void armaElPatronConCerosALaIzquierdaComoElLibroDiarioReal() {
        Asiento esperado = new Asiento();
        esperado.setId(1L);
        when(asientoLineaRepo.buscarAsientosPorLeyendaConteniendo("FC: 00001-00000015", EstadoDocumento.CONFIRMADO))
                .thenReturn(List.of(esperado));

        List<Asiento> resultado = buscar.buscar("1", "15");

        assertThat(resultado).containsExactly(esperado);
        verify(asientoLineaRepo).buscarAsientosPorLeyendaConteniendo(eq("FC: 00001-00000015"), eq(EstadoDocumento.CONFIRMADO));
    }

    @Test
    void sinCoincidenciaDevuelveListaVacia() {
        when(asientoLineaRepo.buscarAsientosPorLeyendaConteniendo("FC: 00002-00000099", EstadoDocumento.CONFIRMADO))
                .thenReturn(List.of());

        List<Asiento> resultado = buscar.buscar("0002", "99");

        assertThat(resultado).isEmpty();
    }

    @Test
    void variasCoincidenciasDevuelveTodasSinDesambiguar() {
        Asiento a1 = new Asiento();
        a1.setId(1L);
        Asiento a2 = new Asiento();
        a2.setId(2L);
        when(asientoLineaRepo.buscarAsientosPorLeyendaConteniendo("FC: 00001-00000001", EstadoDocumento.CONFIRMADO))
                .thenReturn(List.of(a1, a2));

        List<Asiento> resultado = buscar.buscar("00001", "00000001");

        assertThat(resultado).hasSize(2);
    }
}
