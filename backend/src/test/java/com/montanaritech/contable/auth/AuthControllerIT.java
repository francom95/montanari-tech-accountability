package com.montanaritech.contable.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.montanaritech.contable.AbstractIntegrationTest;
import com.montanaritech.contable.auth.dto.LoginRequest;
import com.montanaritech.contable.auth.dto.RefreshRequest;
import com.montanaritech.contable.auth.dto.TokenPairResponse;
import com.montanaritech.contable.auth.dto.UsuarioCrearRequest;
import com.montanaritech.contable.auth.dto.UsuarioResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Matriz de permisos por rol de F1.5, contra el endpoint representativo
 * (gestión de usuarios, admin-only). Usa el admin sembrado por la migración
 * V2 (admin@montanaritech.com / changeme123) como punto de partida.
 */
class AuthControllerIT extends AbstractIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@montanaritech.com";
    private static final String ADMIN_PASSWORD = "changeme123";

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void loginConCredencialesCorrectasDevuelveParDeTokens() {
        TokenPairResponse tokens = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(tokens.refreshToken()).isNotBlank();
    }

    @Test
    void loginConPasswordIncorrectaDevuelve401() {
        ResponseEntity<String> respuesta = restTemplate.postForEntity(
                "/api/v1/auth/login",
                new LoginRequest(ADMIN_EMAIL, "password-incorrecta"),
                String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void endpointProtegidoSinTokenDevuelve401() {
        ResponseEntity<String> respuesta = restTemplate.getForEntity("/api/v1/usuarios", String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void adminAccedeAGestionDeUsuarios() {
        TokenPairResponse tokens = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        ResponseEntity<String> respuesta = restTemplate.exchange(
                "/api/v1/usuarios", HttpMethod.GET, autenticado(tokens.accessToken()), String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void usuarioDeSoloLecturaRecibe403EnGestionDeUsuarios() {
        TokenPairResponse tokensAdmin = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        String emailLectura = "lectura-it@montanaritech.com";
        restTemplate.exchange(
                "/api/v1/usuarios",
                HttpMethod.POST,
                new HttpEntity<>(
                        new UsuarioCrearRequest(emailLectura, "Usuario Lectura", "password123", RolUsuario.LECTURA),
                        headersConToken(tokensAdmin.accessToken())),
                UsuarioResponse.class);

        TokenPairResponse tokensLectura = login(emailLectura, "password123");

        ResponseEntity<String> respuesta = restTemplate.exchange(
                "/api/v1/usuarios",
                HttpMethod.GET,
                autenticado(tokensLectura.accessToken()),
                String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void refreshRotaElTokenYElAnteriorQuedaInutilizable() {
        TokenPairResponse tokens = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        ResponseEntity<TokenPairResponse> primerRefresh = restTemplate.postForEntity(
                "/api/v1/auth/refresh", new RefreshRequest(tokens.refreshToken()), TokenPairResponse.class);
        assertThat(primerRefresh.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> segundoRefreshConTokenViejo = restTemplate.postForEntity(
                "/api/v1/auth/refresh", new RefreshRequest(tokens.refreshToken()), String.class);
        assertThat(segundoRefreshConTokenViejo.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private TokenPairResponse login(String email, String password) {
        ResponseEntity<TokenPairResponse> respuesta = restTemplate.postForEntity(
                "/api/v1/auth/login", new LoginRequest(email, password), TokenPairResponse.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        return respuesta.getBody();
    }

    private HttpEntity<Void> autenticado(String accessToken) {
        return new HttpEntity<>(headersConToken(accessToken));
    }

    private HttpHeaders headersConToken(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return headers;
    }
}
