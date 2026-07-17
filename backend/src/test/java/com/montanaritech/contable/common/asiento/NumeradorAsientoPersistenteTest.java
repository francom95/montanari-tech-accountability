package com.montanaritech.contable.common.asiento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.secuencia.Secuencia;
import com.montanaritech.contable.common.secuencia.SecuenciaRepository;
import com.montanaritech.contable.common.tenant.TenantContext;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * CP-04 (F3.1 §10): la numeración corre por orden de CONFIRMACIÓN, no de
 * creación, y un borrador descartado antes de pedir número nunca consume
 * uno — no deja huecos. Como {@code AsientoService} solo pide número una
 * vez, al confirmar, esa garantía se reduce a que la secuencia avanza de a
 * uno por cada llamada real a {@link NumeradorAsientoPersistente#siguienteNumero()}.
 */
@ExtendWith(MockitoExtension.class)
class NumeradorAsientoPersistenteTest {

    @Mock
    private SecuenciaRepository repo;

    private NumeradorAsientoPersistente numerador;
    private Secuencia secuencia;

    @BeforeEach
    void setUp() {
        numerador = new NumeradorAsientoPersistente(repo);
        secuencia = new Secuencia();
        secuencia.setNombre("ASIENTO");
        secuencia.setValorActual(0L);
        when(repo.buscarParaActualizar(TenantContext.TENANT_POR_DEFECTO, "ASIENTO")).thenReturn(Optional.of(secuencia));
    }

    @AfterEach
    void limpiarTenant() {
        TenantContext.clear();
    }

    @Test
    void numerosCorrelativosSinHuecosPorBorradoresDescartados() {
        // Se crean borradores A, B, C (orden de creación) pero solo B y luego A
        // llegan a pedir número (C se descarta sin confirmar nunca): el número
        // de B es el primero en pedirse y el de A el segundo, sin huecos.
        Long numeroDeB = numerador.siguienteNumero();
        Long numeroDeA = numerador.siguienteNumero();

        assertThat(numeroDeB).isEqualTo(1L);
        assertThat(numeroDeA).isEqualTo(2L);
        assertThat(secuencia.getValorActual()).isEqualTo(2L);
    }
}
