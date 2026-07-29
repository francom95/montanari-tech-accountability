package com.montanaritech.contable.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * F11.2 A3: {@code consumirYRotar} debía rechazar un refresh token válido pero
 * cuyo usuario ya fue desactivado — antes eso solo se chequeaba en el login por
 * password, así que un usuario dado de baja podía seguir renovando su sesión
 * indefinidamente mientras no dejara vencer su refresh token.
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService service;

    private RefreshTokenService crear() {
        return new RefreshTokenService(refreshTokenRepository);
    }

    private RefreshToken tokenValido(Usuario usuario) {
        RefreshToken rt = new RefreshToken();
        rt.setUsuario(usuario);
        rt.setExpiraEn(Instant.now().plus(1, ChronoUnit.DAYS));
        rt.setRevocado(false);
        return rt;
    }

    @Test
    void consumirYRotarConUsuarioActivoDevuelveElUsuario() {
        service = crear();
        Usuario u = new Usuario();
        u.setActivo(true);
        when(refreshTokenRepository.findByTokenHashAndRevocadoFalse(any())).thenReturn(Optional.of(tokenValido(u)));

        Optional<Usuario> resultado = service.consumirYRotar("token-crudo");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().isActivo()).isTrue();
    }

    @Test
    void consumirYRotarConUsuarioDesactivadoNoDevuelveNada() {
        service = crear();
        Usuario u = new Usuario();
        u.setActivo(false);
        when(refreshTokenRepository.findByTokenHashAndRevocadoFalse(any())).thenReturn(Optional.of(tokenValido(u)));

        Optional<Usuario> resultado = service.consumirYRotar("token-crudo");

        assertThat(resultado).isEmpty();
    }

    @Test
    void consumirYRotarConTokenVencidoNoDevuelveNada() {
        service = crear();
        Usuario u = new Usuario();
        u.setActivo(true);
        RefreshToken vencido = tokenValido(u);
        vencido.setExpiraEn(Instant.now().minus(1, ChronoUnit.DAYS));
        when(refreshTokenRepository.findByTokenHashAndRevocadoFalse(any())).thenReturn(Optional.of(vencido));

        Optional<Usuario> resultado = service.consumirYRotar("token-crudo");

        assertThat(resultado).isEmpty();
    }
}
