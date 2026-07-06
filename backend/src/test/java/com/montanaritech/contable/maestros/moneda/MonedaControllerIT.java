package com.montanaritech.contable.maestros.moneda;

import static org.assertj.core.api.Assertions.assertThat;

import com.montanaritech.contable.AbstractIntegrationTest;
import com.montanaritech.contable.auth.dto.LoginRequest;
import com.montanaritech.contable.auth.dto.TokenPairResponse;
import com.montanaritech.contable.maestros.moneda.dto.MonedaCrearRequest;
import com.montanaritech.contable.maestros.moneda.dto.MonedaEditarRequest;
import com.montanaritech.contable.maestros.moneda.dto.MonedaResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Molde de referencia de PL-1 (F1.8): happy path completo contra MySQL real
 * (Testcontainers) — crear, listar/filtrar, editar, desactivar/activar,
 * eliminar, y el 409 de código duplicado.
 */
class MonedaControllerIT extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void cicloDeVidaCompletoDeUnaMoneda() {
        HttpHeaders headers = headersConToken(loginAdmin());

        ResponseEntity<MonedaResponse> creada = restTemplate.exchange(
                "/api/v1/monedas",
                HttpMethod.POST,
                new HttpEntity<>(new MonedaCrearRequest("EUR", "Euro", "€"), headers),
                MonedaResponse.class);
        assertThat(creada.getStatusCode()).isEqualTo(HttpStatus.OK);
        Long id = creada.getBody().id();
        assertThat(creada.getBody().activo()).isTrue();

        ResponseEntity<MonedaResponse> duplicada = restTemplate.exchange(
                "/api/v1/monedas",
                HttpMethod.POST,
                new HttpEntity<>(new MonedaCrearRequest("EUR", "Euro otra vez", "€"), headers),
                MonedaResponse.class);
        assertThat(duplicada.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        ResponseEntity<MonedaResponse> editada = restTemplate.exchange(
                "/api/v1/monedas/" + id,
                HttpMethod.PUT,
                new HttpEntity<>(new MonedaEditarRequest("Euro Editado", "EU$"), headers),
                MonedaResponse.class);
        assertThat(editada.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(editada.getBody().nombre()).isEqualTo("Euro Editado");

        ResponseEntity<MonedaResponse> desactivada = restTemplate.exchange(
                "/api/v1/monedas/" + id + "/desactivar",
                HttpMethod.PATCH,
                new HttpEntity<>(headers),
                MonedaResponse.class);
        assertThat(desactivada.getBody().activo()).isFalse();

        ResponseEntity<MonedaResponse> reactivada = restTemplate.exchange(
                "/api/v1/monedas/" + id + "/activar",
                HttpMethod.PATCH,
                new HttpEntity<>(headers),
                MonedaResponse.class);
        assertThat(reactivada.getBody().activo()).isTrue();

        ResponseEntity<String> listado = restTemplate.exchange(
                "/api/v1/monedas?texto=Euro", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(listado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listado.getBody()).contains("Euro Editado");

        ResponseEntity<Void> eliminada = restTemplate.exchange(
                "/api/v1/monedas/" + id, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
        assertThat(eliminada.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> despuesDeEliminar = restTemplate.exchange(
                "/api/v1/monedas/" + id, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(despuesDeEliminar.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private String loginAdmin() {
        ResponseEntity<TokenPairResponse> respuesta = restTemplate.postForEntity(
                "/api/v1/auth/login",
                new LoginRequest("admin@montanaritech.com", "changeme123"),
                TokenPairResponse.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        return respuesta.getBody().accessToken();
    }

    private HttpHeaders headersConToken(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return headers;
    }
}
