package com.montanaritech.contable.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * F11.2 B2 (efecto colateral del fix de aislamiento multi-tenant): el login busca por
 * email SIN filtrar por tenant a propósito (ver Javadoc de {@code findByEmailGlobalParaLogin}) —
 * antes de autenticar no se sabe a qué tenant pertenece el usuario. Verificado en vivo que,
 * tras arreglar el orden del interceptor de tenant (B2), el login se rompía para cualquier
 * tenant salvo el default hasta agregar este bypass explícito.
 */
@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Test
    void cargaUnUsuarioActivoSinImportarDeQueTenantSeaViaLaBusquedaGlobal() {
        CustomUserDetailsService service = new CustomUserDetailsService(usuarioRepository);
        Usuario u = new Usuario();
        u.setEmail("admin2@tenanttest.com");
        u.setPasswordHash("hash");
        u.setRol(RolUsuario.ADMINISTRADOR);
        u.setActivo(true);
        when(usuarioRepository.findByEmailGlobalParaLogin("admin2@tenanttest.com")).thenReturn(List.of(u));

        UserDetails detalle = service.loadUserByUsername("admin2@tenanttest.com");

        assertThat(detalle.getUsername()).isEqualTo("admin2@tenanttest.com");
        assertThat(detalle.getPassword()).isEqualTo("hash");
        assertThat(detalle.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_ADMINISTRADOR");
    }

    @Test
    void unUsuarioInactivoNoPuedeAutenticar() {
        CustomUserDetailsService service = new CustomUserDetailsService(usuarioRepository);
        Usuario u = new Usuario();
        u.setEmail("baja@test.com");
        u.setActivo(false);
        when(usuarioRepository.findByEmailGlobalParaLogin("baja@test.com")).thenReturn(List.of(u));

        assertThatThrownBy(() -> service.loadUserByUsername("baja@test.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void unEmailInexistenteEnNingunTenantFalla() {
        CustomUserDetailsService service = new CustomUserDetailsService(usuarioRepository);
        when(usuarioRepository.findByEmailGlobalParaLogin("nadie@test.com")).thenReturn(List.of());

        assertThatThrownBy(() -> service.loadUserByUsername("nadie@test.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
