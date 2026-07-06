package com.montanaritech.contable.common.error;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce excepciones a {@code ProblemDetail} (RFC 7807) con una extensión
 * {@code codigo} de catálogo estable (F1.1 §1.3). Los CRUDs de las
 * plantillas PL-1..PL-5 no necesitan manejo de errores propio: basta con
 * lanzar la excepción de negocio correspondiente.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NegocioException.class)
    public ProblemDetail manejarNegocio(NegocioException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problema.setProperty("codigo", ex.getCodigo());
        return problema;
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ProblemDetail manejarNoEncontrado(RecursoNoEncontradoException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problema.setProperty("codigo", ex.getCodigo());
        return problema;
    }

    @ExceptionHandler(ConflictoException.class)
    public ProblemDetail manejarConflicto(ConflictoException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problema.setProperty("codigo", ex.getCodigo());
        return problema;
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    public ProblemDetail manejarAccesoDenegado(AccesoDenegadoException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problema.setProperty("codigo", ex.getCodigo());
        return problema;
    }

    /**
     * {@code @PreAuthorize} deniega lanzando
     * {@code org.springframework.security.authorization.AuthorizationDeniedException},
     * que extiende esta clase. Sin este handler, el catch-all de abajo la
     * agarra primero (matchea {@code Exception}) y la convierte en un 500
     * antes de que el {@code AccessDeniedHandler} de {@code SecurityConfig}
     * llegue siquiera a verla.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail manejarAccesoDenegadoSeguridad(AccessDeniedException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, "No tenés permisos para esta operación");
        problema.setProperty("codigo", "ACCESO_DENEGADO");
        return problema;
    }

    /**
     * {@code authenticationManager.authenticate(...)} en {@code AuthService.login}
     * lanza esto (subclases como BadCredentialsException) cuando el email o la
     * contraseña no matchean. No es un error del servidor: es un 401 más.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail manejarAutenticacion(AuthenticationException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, "Email o contraseña incorrectos");
        problema.setProperty("codigo", "CREDENCIALES_INVALIDAS");
        return problema;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail manejarValidacion(MethodArgumentNotValidException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Error de validación");
        problema.setProperty("codigo", "VALIDACION");
        List<Map<String, String>> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of(
                        "campo", fe.getField(),
                        "mensaje", Objects.requireNonNullElse(fe.getDefaultMessage(), "inválido")))
                .toList();
        problema.setProperty("detalles", detalles);
        return problema;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail manejarGenerico(Exception ex) {
        log.error("Error no controlado", ex);
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno");
        problema.setProperty("codigo", "ERROR_INTERNO");
        return problema;
    }
}
