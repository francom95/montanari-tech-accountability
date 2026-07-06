package com.montanaritech.contable.common.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.montanaritech.contable.AbstractIntegrationTest;
import com.montanaritech.contable.auth.RolUsuario;
import com.montanaritech.contable.auth.dto.LoginRequest;
import com.montanaritech.contable.auth.dto.TokenPairResponse;
import com.montanaritech.contable.auth.dto.UsuarioCrearRequest;
import com.montanaritech.contable.auth.dto.UsuarioEditarRequest;
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
 * F1.6: toda escritura sensible sobre usuarios deja rastro completo, sin
 * código extra en el controller/service más allá de la llamada a
 * {@code AuditoriaService} (o la anotación {@link Auditado} en el alta).
 */
class AuditoriaIT extends AbstractIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@montanaritech.com";
    private static final String ADMIN_PASSWORD = "changeme123";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AuditoriaLogRepository auditoriaLogRepository;

    @Test
    void crearYEditarUnUsuarioDejanRastroCompletoEnAuditoria() {
        TokenPairResponse tokensAdmin = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        String email = "auditoria-it@montanaritech.com";

        ResponseEntity<UsuarioResponse> creado = restTemplate.exchange(
                "/api/v1/usuarios",
                HttpMethod.POST,
                new HttpEntity<>(
                        new UsuarioCrearRequest(email, "Usuario Auditoria", "password123", RolUsuario.LECTURA),
                        headersConToken(tokensAdmin.accessToken())),
                UsuarioResponse.class);
        Long usuarioId = creado.getBody().id();

        AuditoriaLog logCreacion = auditoriaLogRepository.findAll().stream()
                .filter(l -> l.getEntidadTipo().equals("Usuario") && l.getEntidadId().equals(usuarioId)
                        && l.getAccion() == AccionAuditoria.CREAR)
                .findFirst()
                .orElseThrow();
        assertThat(logCreacion.getUsuarioId()).isEqualTo(1L); // admin sembrado
        assertThat(logCreacion.getDatosDespues()).contains(email);
        assertThat(logCreacion.getDatosDespues()).doesNotContain("password123"); // nunca el hash/password

        restTemplate.exchange(
                "/api/v1/usuarios/" + usuarioId,
                HttpMethod.PUT,
                new HttpEntity<>(
                        new UsuarioEditarRequest("Usuario Auditoria Editado", RolUsuario.CARGA),
                        headersConToken(tokensAdmin.accessToken())),
                UsuarioResponse.class);

        AuditoriaLog logEdicion = auditoriaLogRepository.findAll().stream()
                .filter(l -> l.getEntidadTipo().equals("Usuario") && l.getEntidadId().equals(usuarioId)
                        && l.getAccion() == AccionAuditoria.EDITAR)
                .findFirst()
                .orElseThrow();
        assertThat(logEdicion.getDatosAntes()).contains("LECTURA");
        assertThat(logEdicion.getDatosDespues()).contains("CARGA");
        assertThat(logEdicion.getDatosDespues()).contains("Usuario Auditoria Editado");
    }

    @Test
    void loginExitosoQuedaAuditado() {
        login(ADMIN_EMAIL, ADMIN_PASSWORD);

        boolean hayLoginDeAdmin = auditoriaLogRepository.findAll().stream()
                .anyMatch(l -> l.getAccion() == AccionAuditoria.LOGIN && l.getUsuarioId().equals(1L));
        assertThat(hayLoginDeAdmin).isTrue();
    }

    @Test
    void soloAdminConsultaLaAuditoria() {
        TokenPairResponse tokensAdmin = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        String emailLectura = "auditoria-lectura-it@montanaritech.com";
        restTemplate.exchange(
                "/api/v1/usuarios",
                HttpMethod.POST,
                new HttpEntity<>(
                        new UsuarioCrearRequest(emailLectura, "Lectura", "password123", RolUsuario.LECTURA),
                        headersConToken(tokensAdmin.accessToken())),
                UsuarioResponse.class);
        TokenPairResponse tokensLectura = login(emailLectura, "password123");

        ResponseEntity<String> comoLectura = restTemplate.exchange(
                "/api/v1/auditoria", HttpMethod.GET, autenticado(tokensLectura.accessToken()), String.class);
        assertThat(comoLectura.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> comoAdmin = restTemplate.exchange(
                "/api/v1/auditoria?entidadTipo=Usuario",
                HttpMethod.GET,
                autenticado(tokensAdmin.accessToken()),
                String.class);
        assertThat(comoAdmin.getStatusCode()).isEqualTo(HttpStatus.OK);
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
